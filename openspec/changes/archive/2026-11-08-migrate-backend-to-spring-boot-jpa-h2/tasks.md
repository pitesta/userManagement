# Tasks

Riferimenti: `proposal.md` (perché e cosa), `design.md` (come, decisioni D1–D10), `specs/{user-management,user-data-persistence,spa-serving}/spec.md` (contratto). Ogni fase F0–F4 è un gruppo; ogni task è chiuso dalla verifica indicata ("verificare ...").

## 1. F0 — Contratto e golden

- [x] 1.1 Rilevare su un Tomcat avviato con il WAR attuale il golden del cookie di sessione: richieste reali (`curl -i`) su `POST /api/login` per catturare `Set-Cookie` (nome, Path, HttpOnly, SameSite), il timeout da `web.xml` e il cambio di session ID al login (session ID prima/dopo); registrare i valori misurati come golden F0 nel change (`golden/f0-sessione.md`). Verificare che ogni attributo riporti il valore osservato, non quello atteso.
- [x] 1.2 Registrare il golden delle API: per gli 8 endpoint attuali (`register`, `login`, `logout`, `me`, `utenti`, `admin/utente`, `admin/utente/modifica`, `admin/utente/elimina`) status, corpo JSON e le 19 stringhe di errore italiane; riprodurre ogni caso con `curl` e annotare l'osservato nel golden (`golden/f0-api.md`). Verificare che le stringhe corrispondano esattamente a `ERRORI_IT` in `frontend/src/app/core/i18n.service.ts`.
- [x] 1.3 Creare il tag git `pre-jpa-h2` sul commit pre-migrazione. Verificare con `git tag -l pre-jpa-h2` che punti al commit e che rappresenti il punto di rollback (design: Migration Plan).

## 2. F1.1 — Scaffolding Spring Boot e build jar

- [x] 2.1 Aggiornare `backend/pom.xml`: BOM `spring-boot-dependencies:4.1.2`, starter `webmvc` + `data-jpa` + `flyway` + `validation`, `com.h2database:h2`, packaging `jar` e `spring-boot-maven-plugin`. Le versioni di starter, Hibernate e H2 sono risolte solo dal BOM: nessun `<version>` di override manuale nel pom. Verificare con `mvn -q -DskipTests package` che produca `backend/target/prgweb.jar` e con `mvn dependency:tree` che le versioni risolte combacino esattamente con il BOM (Hibernate `7.4.13.Final`, H2 `2.4.240`) e che non compaiano `sqlite-jdbc`, `jackson-databind`, `maven-war-plugin` né `jakarta.servlet-api`.
- [x] 2.2 Creare `PrgwebApplication` e `application.yml` (datasource `jdbc:h2:file:${PRGWEB_DB_PATH}/app;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=FALSE`, `ddl-auto=validate`, `open-in-view=false`, Flyway attivo, timeout sessione 30 min, cookie con Path=/, HttpOnly, SameSite=Lax, tracking cookie-only). Verificare con un test `@SpringBootTest` che l'application context si compone e le property vengono lette.
- [x] 2.3 Configurare la copia della directory di output reale di Angular 22 — **`frontend/dist/frontend/browser/`** (builder `@angular/build:application` senza `outputPath`: default `dist/<nomeProgetto>/browser`, riscontrato nella build esistente) — in `target/classes/static` (plugin Maven resources attivato dalla presenza della build) nel pom e in `scripts/build.sh`; il frontend non viene modificato. Verificare che dopo `ng build` esista `frontend/dist/frontend/browser/index.html` e che il jar contenga `BOOT-INF/classes/static/index.html` dopo `scripts/build.sh`.
- [x] 2.4 Scrivere `backend/src/main/resources/db/migration/V1__create_utenti.sql`: tabella `utenti` (`id_utente BIGINT IDENTITY`, colonne snake_case, `email VARCHAR(255)`, `amministratore BOOLEAN`, unique case-sensitive su `nome_utente`) e tabella tecnica `app_lock` con la riga permanente id=1 inserita dalla migrazione. Verificare dopo `mvn test` con la console H2 (`java -cp scripts/lib/h2.jar org.h2.tools.Shell`) che tabelle e riga-mutex esistano su un file nuovo.

## 3. F1.2 — Persistenza JPA/Hibernate target

- [x] 3.1 Definire l'entity `Utente` (`@Entity`, colonne snake_case minuscole, `@GeneratedValue(IDENTITY)`, `amministratore` boolean). Verificare con un test che la mappatura corrisponda alle colonne della V1 (contesto verde con `ddl-auto=validate` su schema appena migrato).
- [x] 3.2 Creare `UtenteRepository` (cerca per nome utente, per id, conteggio) e testarlo con `@DataJpaTest` su H2: CRUD, identity crescente, unique sul nome utente (violation → conflitto), mapping boolean. Verificare i quattro casi nei test.
- [x] 3.3 Aggiungere il repository custom con `@PersistenceContext EntityManager` per il lock `SELECT ... FOR UPDATE` sulla riga `app_lock`. Verificare con un test che il lock si acquisisce dentro la transazione (due accessi serializzati).
- [x] 3.4 Verificare il fail-fast dello schema: test che avvia il contesto su uno schema alterato a mano e attende l'errore esplicito di validazione.

## 4. F1.3 — Servizi di dominio, DTO, eccezioni

- [x] 4.1 Definire i DTO (record) e `UtenteSessione` con nomi dei campi JSON invariati (`idUtente`, `nomeUtente`, `email`, `amministratore`). Verificare con un test di serializzazione Jackson 3 che campi e formati corrispondano ai golden F0 e che nessuna chiave password compaia.
- [x] 4.2 Implementare `AuthService` transazionale: registrazione con valutazione "primo utente admin" a mutex acquisito e insert nella stessa transazione; login con `PasswordUtil.check` fuori transazione e utente in sessione senza hash; `me` e `logout`. Verificare con test di servizio (primo/successivo, duplicato → eccezione di conflitto).
- [x] 4.3 Implementare `UtenteService` transazionale: elenco ordinato per `lower(nome_utente)`, cambio ruolo, modifica anagrafica con password opzionale, elimina con self-delete → `logout: true`; invarianti ultimo admin valutate post-lock (D5). Verificare con test di servizio tutte le regole, incluso il caso "ultimo admin".
- [x] 4.4 Definire le eccezioni di dominio e il `@RestControllerAdvice` con mapping eccezione → (status, `{"error":"<messaggio>"}`) sulle stesse 19 stringhe di `ERRORI_IT` (inclusi `Richiesta non valida.` per corpo illeggibile, `Errore interno del database.` per errori DB, `409` per unique). Verificare con test che ogni coppia (eccezione, status, messaggio) sia coperta.

## 5. F1.4 — API MVC, sessione e infrastruttura web

- [x] 5.1 Implementare `AuthController`, `UtentiController`, `AdminUtenteController` con gli stessi percorsi e verbi dell'attuale (`POST /api/login|register|logout`, `GET /api/me`, `GET /api/utenti`, `POST /api/admin/utente[/modifica|/elimina]`) e `changeSessionId()` al login. Verificare con MockMvc (`@AutoConfigureMockMvc`, H2 su file temporaneo) ogni endpoint con gli status e i corpi del golden F0.
- [x] 5.2 Configurare CORS e SPA fallback in `WebConfig`: allow `:4200` con credenziali; fallback `index.html` per le GET senza estensione con esclusione di `/api/**`; assets statici; `404` per asset mancante. Verificare con MockMvc preflight, deep-link, assets ed esclusione API.
- [x] 5.3 Verificare sessioni e cookie contro il golden F0 (test MockMvc): attributi `Set-Cookie` (Path, HttpOnly, SameSite), session ID che cambia al login, timeout 30 minuti, cookie `Path=/` compatibile col proxy `:4200`. Verificare che qualunque scostamento dal gold faccia applicare il valore misurato (D6).

## 6. F1.5 — Eliminazione del legacy (passaggio unico)

- [x] 6.1 Rimuovere i file Java legacy (`Database`, `UtenteDao`, `Json`, le 3 servlet, `AppContextListener`, `CorsFilter`, `SpaFallbackFilter`), `backend/src/main/webapp/` (web.xml, context.xml) e `database/schema.sql`; persistere il riferimento storico nel design. Verificare con una ricerca che in `backend/src` non restino riferimenti a SQLite/JDBC legacy e che `mvn -q test` compili.
- [x] 6.2 Pulire `backend/pom.xml` dai residui (dipendenze sqlite/jackson2/servlet-api, resource verso `../database`, profilo `frontend-dist`, argLine `--enable-native-access=ALL-UNNAMED` in surefire). Verificare `mvn -q clean package` e `mvn dependency:tree`.
- [x] 6.3 Riscrivere `scripts/build.sh` (ng build → copia risorse → `mvn package`) e rimuovere `db-init.sh`/`deploy.sh`/`tomcat-*.sh`; aggiungere `scripts/start.sh` e `scripts/stop.sh` per il jar (PID file, SIGTERM graceful). Verificare: `start.sh` → `http://localhost:8080/prgweb/` raggiungibile; `stop.sh` → processo terminato e file `.mv.db` con checkpoint (nessun trace file).
- [x] 6.4 Aggiornare `README.md` e `AGENTS.md` alla nuova build/avvio (bozza; definizioni definitive in F4). Verificare che i comandi documentati funzionino (`./scripts/build.sh`, `./scripts/start.sh`).
- [x] 6.5 Eseguire la suite completa di contract test (MockMvc + H2 su file temporaneo) e la suite Vitest frontend (`npx ng test --watch=false`, senza modifiche al frontend): entrambe verdi. Questo è l'exit di F1.

## 7. F2 — Robustezza e concorrenza

- [x] 7.1 Test di concorrenza sulla prima registrazione con mutex verificato concretamente: due richieste simultanee (ExecutorService, latch per la partenza contestuale) con nomi utente differenti su database inizialmente vuoto → entrambe rispondono `201`; esattamente una riceve `amministratore: true` e l'altra `amministratore: false` (in qualunque ordine d'esito). I run vengono ripetuti più volte ricreando il database a ogni run: mai due amministratori, mai nessuno. Verificare dal log SQL (per esempio `org.hibernate.SQL` o trace H2) che le due transazioni si serializzino (una attende sul `SELECT ... FOR UPDATE` su `app_lock`) e che la lettura dell'invariante avvenga a lock acquisito.
- [x] 7.2 Test di concorrenza sugli amministratori: degradazione simultanea di due admin diversi, o degradazione + elimina contemporanee con un solo admin → una sola operazione va a buon fine e l'altra risponde `409 {"error":"Deve rimanere almeno un amministratore."}`. Verificare con lo stesso pattern ExecutorService.
- [x] 7.3 Test di timeout/deadlock del lock: forzatura del lock → risposta `500 {"error":"Errore interno del database."}` senza scritture parziali. Verificare con un test che simula il contesa sul lock o con un timeout configurato basso.
- [x] 7.4 Rieseguire la suite completa dopo le verifiche di concorrenza (contract + repository + servizi): tutto verde. Questo è l'exit di F2.

## 8. F3 — Operatività, backup, distribuzione

- [x] 8.1 Estendere `build.sh` a copiare il jar H2 standalone (stessa versione della dipendenza) in `scripts/lib/h2.jar`, con controllo di versione (build interrotta se non coincidono). Verificare che il file esista dopo il build e corrisponda alla versione dichiarata nel BOM.
- [x] 8.2 Creare `scripts/db-backup.sh`: eseguire `scripts/stop.sh` per l'arresto controllato dell'applicazione, verificare che il PID sia terminato e che il file H2 sia chiuso, quindi lanciare `org.h2.tools.Shell` con `BACKUP TO '<dir>/prgweb-<timestamp>.zip'` su `PRGWEB_DB_PATH`. Gestire stop/timeout/errori H2 con messaggio ed exit-code non zero; verificare exit-code positivo e zip non vuoto. L'applicazione resta ferma dopo il backup. Verificare con app inizialmente attiva che lo stop sia avvenuto prima della connessione Shell.
- [x] 8.3 Creare `scripts/db-restore.sh`: rifiutare con errore esplicito se l'applicazione è attiva e verificare che il processo sia terminato prima di H2 Shell/Restore. Usare `java -cp scripts/lib/h2.jar org.h2.tools.Restore -file "<dirBackup>/prgweb-<timestamp>.zip" -dir "<dir di lavoro>" -db app`, poi sostituire il file DB attivo e rimuovere residui `app.lock.db`/`app.trace.db`. Restare fermo per default; `--start` riavvia soltanto dopo il restore riuscito. Gestire errori senza avviare l'app o dichiarare successo prematuro. Verificare il caso negativo (app attiva → nessuna modifica) e il caso positivo (app ferma).
- [x] 8.4 Eseguire il test di accettazione backup/ripristino D8: avvio e registrazione utente → esecuzione `db-backup.sh` (arresto controllato e verifica processo terminato prima di Shell) → verifica zip e app ferma → eliminazione del database (`app.mv.db` e residui `.lock.db`/`.trace.db`) → ripristino con `org.h2.tools.Restore` mentre l'app è ferma → riavvio controllato → login `200` e `GET /api/me` coerente `200` → arresto controllato finale. Documentare in `docs/database.md` la procedura verificata e i comandi H2 di backup e restore.
- [x] 8.5 Creare `scripts/db-console.sh` e riscrivere `scripts/set-admin.sh` su SQL H2 (`org.h2.tools.Shell`) con il comportamento di recupero amministratore. `db-console.sh` rifiuta l'app attiva; `set-admin.sh` arresta in modo controllato, verifica la terminazione, esegue la modifica e lascia l'app ferma. Verificare entrambi su un database di prova.
- [x] 8.6 Verificare il deploy standalone: prerequisito solo JDK/JRE 25; porta configurabile via `SERVER_PORT` (collaudo su 8081, senza toccare il Tomcat condiviso); `SERVER_SERVLET_CONTEXT_PATH` resta `/prgweb`.

## 9. F4 — Documentazione tecnica finale (obbligatoria)

- [x] 9.1 Riscrivere `docs/architettura.md` e `docs/backend.md` (Boot → MVC → Service → JPA/Hibernate → H2 su file; responsabilità per livello; repository custom con EntityManager per il lock; indicazioni di debug). Verificare che i riferimenti siano coerenti col codice reale, percorso per percorso.
- [x] 9.2 Riscrivere `docs/database.md` (H2 su file `.mv.db`, versione congelata 2.4.240, policy upgrade, Flyway V1 e migrazioni future, `ddl-auto=validate`, backup/restore offline testati, stop e verifica processo prima di H2 Shell, app lasciata ferma, limite single-instance) e `docs/ambiente.md` (niente Tomcat né sqlite3, prerequisito JDK 25, env `PRGWEB_DB_PATH`, `SERVER_PORT`). Verificare che i comandi documentati funzionino.
- [x] 9.3 Riconciliare `docs/api.md` e `docs/test.md` con i golden F0 e la nuova suite (contratto invariato, zero scostamenti). Verificare che ogni endpoint/errore documentato abbia un test automatizzato.
- [x] 9.4 Aggiornare `docs/auth.md` (sessione e cookie con i valori misurati in F0), `docs/build-deploy.md` (fat jar e script) e `docs/index.md`/`docs/frontend.md` (frontend invariato). Verificare che diagramma di `architettura.md` e sequenze di `auth.md` restino corretti.
- [x] 9.5 Aggiornare `README.md`, `AGENTS.md` e `.gitignore` (aggiungendo `*.mv.db`, `*.lock.db`, `*.trace.db`). Verificare che non restino riferimenti a SQLite/Tomcat condiviso nelle doc e con `git status` che `data/` non compaia.
- [x] 9.6 Verifica finale end-to-end: build → start → smoke API (register/login/utenti/admin) → `db-backup.sh` (stop controllato e verifica processo terminato prima di H2 Shell → backup) → verifica zip e applicazione ferma. Tutto verificato manualmente coi log; questo è l'exit di F4.

## Workflow follow-up

- Archiviare il change dopo la revisione del progetto (`/opsx-archive` al completamento dei task).
- Verifica residua: `openspec validate` pulito e delta spec sincronizzate con le spec principali.
