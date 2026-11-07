# Design

## Context

Il backend attuale (vedi `proposal.md` — Why) è un WAR Jakarta Servlet 6.1 su Tomcat condiviso: `AuthServlet`/`UtentiServlet`/`AdminUtenteServlet` gestiscono l'attribute di sessione `utente`, il DAO JDBC (`UtenteDao`) usa SQLite su file, lo schema è creato da `Database.init()` con parsing manuale di `database/schema.sql`, e il JSON è scritto a mano da `Json.java`. Il frontend Angular 22 non cambia: definisce il contratto (8 endpoint, corpi JSON e 19 stringhe di errore mappate da `ERRORI_IT` in `frontend/src/app/core/i18n.service.ts`).

Vincoli di contesto rilevanti per le scelte tecniche:

- Java 25, Maven, ambiente user-space senza `sudo` (toolchain già in `~/.local/opt`).
- Tomcat condiviso su `:8080` può ospitare altre app: il nuovo runtime deve poter cambiare porta via env.
- Spring Boot 4.1.2 porta embedded Tomcat 11.0.27 (stessa versione del Tomcat attuale) e gestisce le versioni di tutti i componenti scelti. Verifica effettuata sulle proprietà del BOM `spring-boot-dependencies:4.1.2`: Hibernate **7.4.13.Final** (`hibernate.version`), H2 **2.4.240** (`h2.version`), Flyway 12.4.0 (`flyway.version`), HikariCP 7.0.2. Le versioni indicate in D2/D3 corrispondono a quelle risolte dal BOM: nessun override manuale è necessario o consentito, le dipendenze sono dichiarate solo come `groupId:artifactId`.

## Goals / Non-Goals

Goals:

- Un solo artefatto deployabile su Linux: fat jar con embedded Tomcat + H2 su file + frontend Angular nel classpath.
- Architettura Controller / Service / Repository con `@Transactional` sul Service ed EntityManager disponibile per le operazioni con lock diretto.
- Schema creato da zero da Flyway (V1) con validazione dello schema all'avvio.
- Invarianti amministrative serialize via mutex applicativo con un'unica strategia transazionale.
- Contratto REST e messaggi di errore invariati, certificati da test di contratto basati sui golden F0 (valori misurati, non assunti).
- Documentazione `docs/` aggiornata entro il change (obbligatoria).

Non-Goals:

- Nessuna migrazione dati SQLite→H2 e nessun backup del vecchio SQLite (decisione definitiva).
- Nessun server database separato e nessuna configurazione multi-server (per esempio Spring Session Redis).
- Niente Spring Security 7 e niente Actuator (fuori scope, possibili fasi future).
- Nessun supporto a database multipli o dual-dialect: H2 su file è l'unico database dell'applicazione.
- Nessuna modifica al codice frontend (`frontend/` senza commit di codice).

## Decisions

### D1 — Runtime: fat jar Spring Boot (standalone) invece di WAR su Tomcat condiviso
L'applicazione diventa un eseguibile `prgweb.jar` (repackage di `spring-boot-maven-plugin`) con Tomcat embedded. Alternative: (a) WAR su Tomcat esterno (rifiutato: mantiene installazione e gestione di Tomcat e un secondo runtime sulla 8080); (b) WAR eseguibile ibrido (rifiuto: aggiunge complessità di packaging senza beneficio immediato). Porta e contesto (`/prgweb`) sono configurabili da `application.yml`/env; l'attuale build Angular con base-href `/prgweb/` resta valida.

### D2 — Persistenza: Spring Data JPA + Hibernate default del BOM
`spring-boot-starter-data-jpa` porta Hibernate **7.4.13.Final**, nella versione esatta risolta dal BOM Boot 4.1.2 (proprietà `hibernate.version`, verificata sul BOM pubblicato); il pom non dichiara alcun `<version>` per starter e Hibernate, quindi nessun override manuale è necessario. L'entity `Utente` (`@Entity`) è mappata su `utenti` in snake_case minuscolo (`id_utente`, `nome_utente`, `email`, `password`, `amministratore BOOLEAN`), con `ddl-auto=validate` (Flyway unico proprietario dello schema), `open-in-view=false`, isolation READ COMMITTED (default H2). ID strategy `IDENTITY` (identity column H2). Alternativa EclipseLink scartata per decisione utente: richiederebbe autoconfig custom; il lock pessimistico qui è standard JPA e funziona con Hibernate.

### D3 — H2 su file (embedded), versione 2.4.240 del BOM, single-instance
URL: `jdbc:h2:file:${PRGWEB_DB_PATH};DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=FALSE`. Il percorso predefinito in sviluppo è `<repo>/data/` (file `app.mv.db`); in deploy si usa la variabile d'ambiente `PRGWEB_DB_PATH` (il nome sostituisce `APP_DB_PATH`). La versione è **2.4.240 nella forma esatta risolta dal BOM Boot 4.1.2** (proprietà `h2.version`, verificata anche da `mvn dependency:tree` nel task 2.1): l'artefatto `com.h2database:h2` è dichiarato senza `<version>`, nessun override manuale. L'upgrade a nuove minor si documenta ma non si automatizza (il formato `.mv.db` non è garantito compatibile tra minor). Pool HikariCP di 10 connessioni (sufficiente per lo scenario); il backup (D8) verifica che nessun altro processo tenga aperto il file.

### D4 — Schema e migrazioni: Flyway V1 da zero
`V1__create_utenti.sql` crea `utenti` (colonne come D2, `email VARCHAR(255)` con validazione applicativa a 40 invariata, unique su `nome_utente` case-sensitive) più la tabella tecnica `app_lock` con la riga permanente (D5), inserita dalla stessa migrazione. Hibernate vale lo schema (`validate`): qualunque disallineamento schema/modello arresta l'avvio. Le successive modifiche allo schema sono nuove migrazioni `V2__...`. `database/schema.sql` viene rimossa; la V1 ne diventa il riferimento storico.

### D5 — Mutex applicativo: tabella tecnica `app_lock`
Una riga permanente (id=1) in `app_lock` è il mutex globale per le operazioni con invariante; accesso in transazione con `SELECT ... FOR UPDATE` nella stessa transazione di verifica+scrittura:

```
operazione amministrativa o di registrazione:
  [transazione aperta dalla @Transactional del Service]
  1) SELECT ... FROM app_lock WHERE id = 1 ... FOR UPDATE   <- acquisizione del mutex
  2) verifica dell'invariante (count / letture post-lock)
  3) scrittura (insert utente / update ruolo / delete utente)
  [commit rilascia il lock]
```

Il lock copre le tre operazioni decise: registrazione (la decisione "primo utente admin" è valutata con la lettura a lock acquisito, senza race su database vuoto), degradazione dell'ultimo amministratore ed eliminazione dell'ultimo amministratore. Alternative considerate e scartate: SERIALIZABLE per-metodo (isola l'intera transazione, più aperture di contesa senza vantaggio pratico) e Ottimismo con retry (rifiutato: l'operazione amministrativa è rara e la semantica d'errore va predictabile). Il timeout o il deadlock del lock produce `500 {"error":"Errore interno del database."}` (spec: Esiti delle attese sul mutex). Le operazioni non amministrative (login, elenco utenti) NON acquisiscono il mutex.

### D6 — Sessione: protezione dei valori misurati in F0
Il nuovo backend rende i valori di sessione attesi dall'attuale — 30 minuti di inattività, `Path=/`, `HttpOnly`, `SameSite=Lax`, tracking cookie-only — MA la fonte di verità è il golden F0 (rilevamento su app attiva via HTTP reale: header `Set-Cookie`, attributi del cookie, timeout di `web.xml`, `req.changeSessionId()` al login con confronto del session ID prima/dopo). **I valori effettivi rilevati in F0 prevalgono sempre sui valori indicati come default in questa decisione**: trattasi di valori attesi confermabili solo dal rilevamento, e qualunque dato del design (questa sezione, `application.yml` di riferimento, le delta spec, i test) viene riconciliato al golden F0 nel momento in cui il rilevamento è disponibile; nessun default teorico sopravvive a una divergenza col misurato. Per coerenza, la configurazione attuale che F0 misurerà è la seguente: `web.xml` con `session-timeout=30`, `cookie-config` `http-only=true` e `path=/`, `tracking-mode=COOKIE`, e in `META-INF/context.xml` `CookieProcessor sameSiteCookies="lax"`.

### D7 — Contratto REST: controller, DTO, exception advice
Tre `@RestController` (`AuthController`, `UtentiController`, `AdminUtenteController`) con mapping identico agli endpoint attuali; request e response come record (`LoginRequest`, `RegisterRequest`, `ModificaRequest`, `UtenteDto`, ...) con serializzazione Jackson 3 (Boot 4 default, package `tools.jackson`; `@JsonIgnore` non serve più perché le password restano fuori dai DTO). Un `@RestControllerAdvice` mappa le eccezioni di dominio tipo→(status, `{"error":"<messaggio>"}`) con le stesse stringhe di `ERRORI_IT`; la violazione dell'unique (`DataIntegrityViolationException`) → `409 {"error":"Nome utente già registrato."}`; il formato di errore default di Boot non compare mai agli endpoint applicativi.

### D8 — Backup / ripristino di H2 con il fat jar
Problema: le classi H2 sono nested jars nel fat jar Spring Boot e non sono referenziabili con `java -cp prgweb.jar org.h2.tools.Shell`. Inoltre D3 imposta `AUTO_SERVER=FALSE`, perciò il processo standalone H2 non può aprire il file mentre Spring Boot lo tiene aperto. Tutte le operazioni H2 standalone si svolgono quindi a database chiuso; il backup non è a caldo.

- Il `build.sh` copia il jar H2 standalone dalla local Maven repository (`~/.m2/repository/com/h2database/h2/<versione>/<versione>.jar`, stessa versione della dipendenza) in `scripts/lib/h2.jar`, dopo `mvn package`. Poiché la versione è congelata (D3), il build si interrompe con errore se lib e dipendenza non coincidono.
- **Backup offline** (`db-backup.sh`): invoca `scripts/stop.sh` per arrestare Spring Boot in modo controllato, verifica che il PID dell'applicazione sia terminato e che H2 abbia rilasciato il file, poi esegue
  `java -cp scripts/lib/h2.jar org.h2.tools.Shell -url "jdbc:h2:file:<PRGWEB_DB_PATH>" -user <user> -password <pw> -sql "BACKUP TO '<backupDir>/prgweb-<timestamp>.zip'"`.
  `BACKUP TO` opera con il database chiuso dall'applicazione; lo script verifica l'exit-code e che lo zip sia presente e non vuoto. Se lo stop o il backup falliscono, termina con errore esplicito e non avvia H2 Shell se l'applicazione risulta ancora attiva. Dopo un backup riuscito l'applicazione resta ferma; l'operatore può riavviarla con `scripts/start.sh` quando decide.
- **Ripristino** (`db-restore.sh`): richiede e verifica che l'applicazione sia arrestata; rifiuta con errore esplicito se il processo è attivo. Comando concreto sul JAR standalone H2 (gli zip creati da `BACKUP TO` sono i file di input nativi dello strumento `Restore` di H2):

  ```
  java -cp scripts/lib/h2.jar org.h2.tools.Restore \
       -file "<dirBackup>/prgweb-<timestamp>.zip" \
       -dir "<dir di lavoro>" -db app
  ```

  `org.h2.tools.Restore` estrae i file del database dallo zip nella directory indicata (`<dir>/app.mv.db`), ripristinando i nomi originali con il nome database `app`. Lo script sostituisce il file `app.mv.db` attivo, rimuove eventuali file `app.lock.db`/`app.trace.db` residui e resta a database fermo, salvo l'opzione esplicita `--start` al termine del ripristino riuscito. Se chiamato con `--start`, riavvia tramite `scripts/start.sh` solo dopo aver completato e verificato la sostituzione. L'esecuzione usa il JAR standalone perché H2 è annidato nel fat jar.
- **Console e recupero admin**: `db-console.sh` e `set-admin.sh` usano H2 Shell solo a database chiuso. `set-admin.sh` esegue lo stop controllato e verifica la terminazione prima dell'accesso SQL; dopo la modifica lascia l'applicazione ferma. `db-console.sh` rifiuta l'apertura se l'app risulta attiva.
- **Test di backup/ripristino (accettazione F3)** — esegue concretamente e in quest'ordine:
  1. avvio app;
  2. registrazione di un utente di prova;
  3. esecuzione di `db-backup.sh`, che arresta l'applicazione con SIGTERM e attende la terminazione;
  4. verifica che il processo sia assente e H2 abbia rilasciato il file;
  5. **creazione del backup** con H2 Shell e verifica di exit-code positivo e zip non vuoto; l'app resta ferma;
  6. **eliminazione del database** (`app.mv.db` e residui `.lock.db`/`.trace.db`);
  7. **ripristino dal backup** con `org.h2.tools.Restore` mentre l'app è ferma;
  8. riavvio controllato dell'applicazione;
  9. **autenticazione con l'utente precedentemente registrato**: login → `200` e `GET /api/me` coerente → `200`;
  10. arresto controllato finale;
  test negativo: restore con applicazione attiva deve rifiutare l'operazione prima di modificare il database.
- Pianificazione (cron/systemd timer), <i>owner</i> e retention restano dettagli operativi decidibili in F3; nel change è inclusa solo la procedura manuale e il test (decision #13).

### D9 — Build e distribuzione
`build.sh`: `ng build --configuration production --base-href "/$APP_CONTEXT/"` → copia della **directory di output reale di Angular 22** in `backend/target/classes/static` → `mvn package` con repackage. Verifica fatta su questa codebase: `angular.json` impiega il builder `@angular/build:application` (versione 22.2.5 in `frontend/node_modules`, package `@angular/build`) **senza la proprietà `outputPath`**, quindi Angular applica il default del builder e produce **`frontend/dist/frontend/` con gli asset nella sottocartella `browser/`** (`dist/frontend/browser/index.html`, riscontrato nella build esistente). Il task di copia usa quindi il percorso reale `frontend/dist/frontend/browser/*` → `backend/target/classes/static`; il frontend non viene modificato (né `outputPath` in `angular.json`, né config aggiuntiva). `PrgwebApplication` è una `@SpringBootApplication` standalone (niente `SpringBootServletInitializer`, non necessario per il jar). Avvio/stop: `scripts/start.sh` (nohup + PID file + log), `scripts/stop.sh` (SIGTERM → Spring chiude il DataSource, H2 esegue il checkpoint). Porta e contesto via env (`SERVER_PORT`, `SERVER_SERVLET_CONTEXT_PATH`).

### D10 — Strategia di test
1. **F0 golden**: rilevamento dell'app attuale (endpoint, errori, cookie, timeout, changeSessionId) registrato in un golden document, invariante per le verifiche di F1.
2. **Contract test (F1)**: `@SpringBootTest` con `@AutoConfigureMockMvc` su H2 file temporaneo, tutti gli 8 endpoint e i 19 errori, confronto con il golden F0.
3. **Test repository** (`@DataJpaTest` su H2): CRUD, identity, unique, ordine `lower(nome_utente)`.
4. **Test servizio/concorrenza (F2)**: regole amministrative e operazioni concorrenti (ExecutorService con richieste effettivamente simultanee). Concorrenza sulla **prima registrazione**: due richieste simultanee con **nomi utente differenti** su **database inizialmente vuoto** → entrambe rispondono `201`, **esattamente una** riceve `amministratore: true` e l'altra `amministratore: false`; il test verifica concretamente l'efficacia del mutex (mai due amministratori o nessuno) e viene ripetuto su più run. Inoltre: verifica del lock sulle operazioni amministrative (ultimo admin).
5. **Test SPA/infra (F1)**: fallback `index.html`, esclusione `/api/**`, assets statici, preflight CORS.
6. **Operazioni di sistema (F3)**: backup/restore offline end-to-end con H2 Shell e il comando concreto di ripristino (`org.h2.tools.Restore` sul JAR standalone H2, D8): arresto controllato e verificato → backup → eliminazione del DB → ripristino a processo fermo → riavvio controllato → autenticazione dell'utente precedentemente registrato; persistenza al restart e arresto pulito con checkpoint.
7. **Frontend (invariato)**: suite Vitest esistente (`npx ng test --watch=false`) senza modifiche, come garanzia di zero regressione.

## Risks / Trade-offs

- Single-process H2: nessuna ridondanza multi-istanza → [D3+D8]: singola istanza dichiarata in docs + backup testato.
- La porta 8080 del fat jar può collidere con il Tomcat condiviso in sviluppo → [D9]: `SERVER_PORT` via env e script di avvio che verificano la disponibilità della porta.
- Il formato `.mv.db` può cambiare tra versioni di H2 → [D3]: versione congelata al BOM; l'upgrade a 2.5.x richiede decisione esplicita.
- Cookie di sessione soggetto al context `/prgweb` e al proxy dev → [D6]: golden F0 come unica fonte di verità + test dedicato con `:4200`.
- Regressione del contratto REST con Jackson 3 (ordine campi, `true` boolean) → [D7]: record deterministici e contract tests confrontati con il golden.
- Mutex sulle operazioni amministrative può ridurre la paralleleismo di registrazione in caso di carico → [D5]: il mutex copre solo registrazione e operazioni amministrative (login e letture senza lock); rilevante solo in caso di carico tipico dimostrativo.
- Deadlock o timeout dei lock su MVCC → [D5+D7]: mapping a `500 {"error":"Errore interno del database."}` con test F2.
- Perdita delle sessioni al riavvio (in memoria) → accettato: tutti rifanno il login; documentato in `docs/auth.md`.

## Migration Plan

Fasi (dettaglio in `tasks.md`; ognuna è un set verificabile e completo):

1. **F0 — Contratti e golden**: rilevamento del comportamento reale attuale (endpoint, errori, cookie, timeout, changeSessionId), scrittura dei golden file, tag git dello stato pre-migrazione.
2. **F1 — Sostituzione backend** (unica fase disruptive, passaggi interni verificabili singolarmente):
   1.1 scaffolding Boot (pom + main class + `application.yml` + packaging jar + dist→static);
   1.2 persistenza target (V1 Flyway + `app_lock` + entity + repository + `ddl-auto=validate`);
   1.3 dominio (Service transazionali, DTO, eccezioni di dominio, mutex transazionale);
   1.4 API (controller, advice, sessione/cookie, CORS + SPA config);
   1.5 eliminazione totale del legacy (servlet, DAO, Database, Json, web.xml, sqlite-jdbc, packaging WAR, vecchi script) in un singolo passaggio;
   1.6 verifica completa (contract tests verdi, smoke manuale, scripts start/stop jar).
3. **F2 — Robustezza e concorrenza**: test di concorrenza reali (2 richieste contemporanee), mapping lock/deadlock → `500`, verificação della serializzazione dell'invariante.
4. **F3 — Operatività e backup**: script `db-backup.sh` arresta e verifica l'app prima di collegare H2 Shell, crea il backup offline e lascia l'app ferma; `db-restore.sh` e gli altri tool SQL operano a database chiuso. Test D8 eseguito, porta configurabile, doc ambiente.
5. **F4 — Documentazione finale**: aggiornamento completo `docs/*`, README, AGENTS, `.gitignore` (`*.mv.db`, `*.lock.db`, `*.trace.db`).

**Rollback**: solo a livello di codice e stato (decisione #2): ripristino dello stato pre-F1 con tag/branch; i dati creati su H2 dopo il cuttover non vengono trasferiti al vecchio SQLite. Nessuna procedura di downgrade dei dati; con rollback l'utente re-registra sul WAR/Tomcat/SQLite.

## Open Questions

Nessuna: le decisioni architetturali confermate includono mutex unico; rollback senza trasferimento dati; backup/restore offline realmente eseguibile con il fat jar mantenendo D3 (`AUTO_SERVER=FALSE`); sessioni da valori misurati in F0; F1 senza architettura intermedia; documentazione obbligatoria. Restano dettagli operativi non di design (owner e retention del backup periodico, eventuale unit systemd), decidibili in F3 senza modificare l'approccio.
