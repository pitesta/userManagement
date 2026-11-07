# Proposal

## Why

Il backend attuale è un WAR Jakarta Servlet (3 `@WebServlet`, un filter CORS, un filter SPA fallback) con JDBC puro verso SQLite: nessuno strato di dominio, una connessione per query, schema gestito a mano da `Database.init()` e da `db-init.sh`. Lo sviluppo di nuove funzionalità richiede servlet, mapping manuali e SQL scritti a mano, e SQLite pone le scritture concorrenti in un solo writer. Si migra a Spring Boot 4.1.2 (ultima stabile) con l'architettura Controller / Service / Repository, Spring Data JPA + Hibernate e H2 Embedded persistente su disco con Flyway, per avere una sola archivia eseguibile Linux (fat jar, `java -jar`) senza Tomcat né un database server da installare.

## What Changes

- **BREAKING** Il database parte **vuoto**: H2 viene creato da zero tramite Flyway. Non esiste alcuna migrazione dati da SQLite; gli utenti esistenti cessano, la prima registrazione dopo il cutover diventa amministratore (regola già presente in `AuthServlet.register`). L'eventuale rollback ripristina solo codice e stato precedente alla migrazione; i dati creati in H2 non vengono trasferiti nel vecchio SQLite.
- **BREAKING** Il runtime passa da WAR su Tomcat condiviso a un **fat jar Spring Boot** con Tomcat 11 embedded (nella versione 11.0.27, già coerente con l'attuale condiviso). Gli script `deploy.sh`, `tomcat-start.sh`, `tomcat-stop.sh`, `db-init.sh` vengono rimossi o riscritti.
- Il livello di accesso ai dati viene sostituito interamente: `UtenteDao` (JDBC) viene eliminato e sostituito da Spring Data JPA + Hibernate; `EntityManager` resta disponibile via `@PersistenceContext` in un repository custom per le operazioni che richiedono accesso diretto.
- Il codice web viene sostituito: 3 servlet → 3 `@RestController`; `CorsFilter` e `SpaFallbackFilter` → configurazione Spring MVC; `AppContextListener` + `Database` → autoconfig Boot + Flyway; `Json.java` (Jackson 2 manuale) → serializzazione Jackson 3 di Spring MVC.
- Nuova architettura Controller / Service / Repository: le regole di dominio (primo utente = admin; non eliminare/degradare l'ultimo admin; self-delete con `logout: true`) si spostano dai controller al Service transazionale.
- Le invarianti "prima registrazione" e "almeno un amministratore" sono protette da un **mutex applicativo**: una tabella tecnica con una riga permanente presa con `SELECT ... FOR UPDATE` dentro la stessa transazione, con un'unica strategia per tutte le operazioni amministrative.
- H2 Embedded in modalità file (`jdbc:h2:file:...;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=FALSE`), versione congelata alla 2.4.240 del BOM Boot; schema di riferimento in `V1__create_utenti.sql` (identity, `amministratore BOOLEAN`, `email` a 255 con validazione a 40 invariata, colonne snake_case minuscolo).
- Sessioni invariate: `HttpSession`, `JSESSIONID`, 30 minuti, cookie `Path=/` + `HttpOnly` + `SameSite=Lax`, cambio di session ID al login. In F0 si rileva il comportamento reale dell'app attuale con test golden e il nuovo backend riproduce **i valori misurati**, non valori assunti. I valori rilevati in F0 **prevalgono sempre** sui default indicati nel design, per qualunque attributo di sessione o cookie.
- Contratto REST invariato certificato dai test di contrato: 8 endpoint, status, corpi JSON e le 19 stringhe di errore italiane mappate in `ERRORI_IT` del frontend. Angular 22 e tutto il frontend non vengono toccati.
- Multiutenza: pool HikariCP, MVCC di H2, isolation READ COMMITTED default, `open-in-view=false`, check BCrypt fuori transazione.
- Backup di H2 definito come procedura offline realmente eseguibile con il fat jar: `db-backup.sh` arresta Spring Boot con `stop.sh`, verifica che il processo sia terminato, quindi esegue `BACKUP TO` via `org.h2.tools.Shell` sul **JAR standalone H2** e lascia l'applicazione ferma. Il ripristino usa `java -cp scripts/lib/h2.jar org.h2.tools.Restore -file <backup.zip> -dir <dir> -db app`, sempre con l'applicazione arrestata. Il test di accettazione esegue la catena: registrazione → arresto controllato e verifica → backup → eliminazione del database → ripristino → riavvio controllato → autenticazione con un utente precedentemente registrato.
- L'aggiornamento della documentazione tecnica in `docs/` è **obbligatorio e in scope del change**, non facoltativo.

## Capabilities

### New Capabilities

- `user-management`: registrazione, autenticazione con sessione HTTP, elenco utenti, cambio ruolo, modifica anagrafica, eliminazione; invarianti amministrative (primo utente admin, ultimo admin non degradabile/eliminabile, self-delete → logout) protette da mutex applicativo; contratto REST e messaggi di errore italiani invariati.
- `user-data-persistence`: persistenza utenti su H2 Embedded su file, schema creato e versionato da Flyway; ID identity, unique su nome utente, coordinamento concorrenza con tabella tecnica mutuamente esclusiva, durabilità oltre riavvio dell'applicazione.
- `spa-serving`: servire il frontend Angular con fallback `index.html` per le GET senza estensione escluse `/api/**`, servire assets statici e CORS per l'origin di sviluppo `:4200`.

### Modified Capabilities

(nessuna: `openspec/specs/` è vuoto, tutte le capability sono nuove.)

## Impact

- **Codice eliminato**: `backend/src/main/java/it/prgweb/{db/Database.java, dao/UtenteDao.java, util/Json.java, web/{AuthServlet, UtentiServlet, AdminUtenteServlet, AppContextListener, CorsFilter, SpaFallbackFilter}.java}`, `backend/src/main/webapp/` (web.xml, context.xml), `database/schema.sql` (riferimento storico in V1).
- **Dipendenze rimosse**: `jakarta.servlet-api`, `org.xerial:sqlite-jdbc` (e con esso l'argLine `--enable-native-access=ALL-UNNAMED` in surefire), `com.fasterxml.jackson.core:jackson-databind`, packaging `war` + `maven-war-plugin` + profilo `frontend-dist` e la risorsa Maven che copia `../database/schema.sql` nel classpath.
- **Dipendenze introdotte** (versioni tutte risolte dal BOM `spring-boot-dependencies:4.1.2`, verificato: `hibernate.version=7.4.13.Final`, `h2.version=2.4.240`; nessun override manuale di versione nel pom, le dipendenze sono dichiarate solo come `groupId:artifactId`): `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa` (Hibernate 7.4.13.Final come provider default del BOM), `spring-boot-starter-flyway` + `flyway-core` (12.4.0, supporto H2 incluso in core), `com.h2database:h2` (2.4.240), Jackson 3 via Boot, test starter `webmvc` e `data-jpa`.
- **Componenti introdotti**: `PrgwebApplication`, `config/WebConfig`, `model/Utente` (@Entity), `repository/UtenteRepository` (+ custom con EntityManager), `service/{AuthService, UtenteService}` + dto + eccezioni di dominio, `web/{AuthController, UtentiController, AdminUtenteController, GlobalExceptionHandler}`, `application.yml`, `db/migration/V1__create_utenti.sql`, suite di test MockMvc/repository/servizio.
- **Script**: `build.sh` riscritto (ng build → resource copy → `mvn package` jar); `start.sh`/`stop.sh` nuovi (PID, SIGTERM graceful → checkpoint H2); `db-console.sh` (org.h2.tools.Shell), `db-backup.sh` (`BACKUP TO`), `db-restore.sh` (app arrestata); `set-admin.sh` riscritto su SQL H2; `deploy.sh`, `tomcat-*.sh`, `db-init.sh` rimossi.
- **Frontend**: invariato a livello di codice (nessun commit in `frontend/`); riceve solo la build già esistente con base-href `/prgweb/` e il proxy dev `:4200`.
- **Sistema**: unico prerequisito JDK/JRE 25; nessun Tomcat né database da installare; porta 8080 non deve collidere con il Tomcat condiviso durante lo sviluppo (`server.port` configurabile da env); file DB locale `data/app.mv.db` (`.gitignore` esteso con `*.mv.db`, `*.lock.db`, `*.trace.db`).
- **Documentazione**: `docs/{architettura,backend,database,auth,api,build-deploy,ambiente,test,index,frontend}.md`, `README.md`, `AGENTS.md` aggiornati (in scope, obbligatori).
