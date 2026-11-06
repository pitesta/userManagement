# Backend Java e Servlet

*Verificato sul codice: 05/11/2026.*

## Fonti di verifica

- `backend/pom.xml` (dipendenze e profilo build, intero file rilevante)
- `backend/src/main/java/it/prgweb/web/AuthServlet.java`, `UtentiServlet.java`,
  `AdminUtenteServlet.java`, `AppContextListener.java`, `CorsFilter.java`,
  `SpaFallbackFilter.java` (interi file)
- `backend/src/main/java/it/prgweb/dao/UtenteDao.java` (intero file)
- `backend/src/main/java/it/prgweb/db/Database.java` (intero file)
- `backend/src/main/java/it/prgweb/model/Utente.java`, `util/Json.java`,
  `util/PasswordUtil.java` (interi file)
- `backend/src/main/webapp/WEB-INF/web.xml`, `META-INF/context.xml`

## Stack e dipendenze (da `pom.xml`)

| Dipendenza | Versione | Scope |
|---|---|---|
| Jakarta Servlet API | 6.1.0 | `provided` |
| `sqlite-jdbc` (org.xerial) | 3.54.1.0 | runtime |
| `jbcrypt` (mindrot) | 0.4 | runtime |
| `jackson-databind` | 2.22.4 | runtime |
| `junit-jupiter` | 6.1.3 | test |

- `<finalName>prgweb</finalName>`: WAR `backend/target/prgweb.war` (`pom.xml:54`).
- `maven.compiler.release=25`: bytecode per Java >= 25 (`pom.xml`, property).
- `schema.sql` è incluso nel classpath come risorsa da `database/`
  (`pom.xml:56-65`, `<directory>${project.basedir}/../database</directory>`).
- surefire riceve `--enable-native-access=ALL-UNNAMED` (`pom.xml:78-81`, per i warning
  di native access di sqlite-jdbc su JDK 25 in test).

## Package layout

```
backend/src/main/java/it/prgweb/
├── web/      ← servlet + filtri + listener (ingresso HTTP)
├── dao/      ← UtenteDao (JDBC puro, prepared statement)
├── db/       ← Database (connessione + init schema)
├── model/    ← Utente (POJO serializzabile)
└── util/     ← Json (Jackson wrapper), PasswordUtil (BCrypt)
```

## Servlet

Tutte le servlet estendono `HttpServlet`, rispondono JSON via `util.Json` e
instradano con `switch` su `req.getServletPath()`.

| Servlet | Mappatura `@WebServlet` | Metodi gestiti |
|---|---|---|
| `AuthServlet` | `/api/login`, `/api/register`, `/api/logout`, `/api/me` | `doGet` per `/api/me` solo; `doPost` per login/register/logout (`AuthServlet.java:23,36-69`) |
| `UtentiServlet` | `/api/utenti` | `doGet` solo (`UtentiServlet.java:19,25`) |
| `AdminUtenteServlet` | `/api/admin/utente`, `/api/admin/utente/modifica`, `/api/admin/utente/elimina` | `doPost` con switch a 3 vie (`AdminUtenteServlet.java:25-69`) |

Schema comune di gestione errori: `JsonProcessingException` → `400 "Richiesta
non valida."`, `SQLException` → log + `500 "Errore interno del database."`
(`AuthServlet.java:62-68`, `AdminUtenteServlet.java:62-68`).

### AuthServlet (righe chiave)

- Sessione corrente: `getSession(false)` + attributo `"utente"` (costante
  `SESSION_USER = "utente"`, `AuthServlet.java:26,176-183`).
- `login` (`71-96`): valida la presenza dei campi; `dao.findByNomeUtente()` +
  `PasswordUtil.check()` — in caso di fallimento → `401 "Credenziali non valide."`;
  memorizza `utente.senzaPassword()` in sessione, dopo `changeSessionId()`.
- `register` (`98-135`): validazioni di formato (nome <= 100, password >= 6 e <= 72
  **byte**, email <= 40 e formato `EMAIL` pattern riga `28`); primo utente →
  `amministratore = dao.count() == 0` (`:120`); `409 "Nome utente già registrato."`
  sia per lookup esplicito (`:110-113`) sia per `UNIQUE constraint failed`
  (`:123-129`).
- `logout` (`137-145`): `getSession(false)` + `session.invalidate()`, sempre `200`.
- `me` (`37-47`): `200` con `{"utente":…}` oppure `401 "Non autenticato."`.

### AdminUtenteServlet (righe chiave)

- Controllo d'accesso (`41-53`): nessuna sessione o nessun utente → `401`;
  `!corrente.isAmministratore()` → `403 "Operazione riservata agli amministratori."`.
- `cambiaRuolo` (`71-106`): protegge l'ultimo admin (`:87-92`, `409 "Deve rimanere
  almeno un amministratore."`), aggiorna via `dao.updateAmministratore` e, se
  l'utente modificato è l'admin in sessione, **sincronizza l'attributo di sessione**
  (`:101-103`).
- `modifica` (`108-168`): aggiorna nome/email/password; gestione `UNIQUE constraint
  failed` → `409` (`:152-160`); sincronizzazione in sessione come sopra (`:163-165`).
- `elimina` (`170-206`): mantiene il vincolo ultimo admin (`:186-190`); se l'admin
  elimina se stesso → `session.invalidate()` + `logout: true` nella risposta
  (`:197-204`).

## DAO — `UtenteDao`

Ogni operazione apre una connessione con `Database.getConnection()`; sempre prepared statement,
nessun ORM:

| Metodo | SQL (in breve) | Note |
|---|---|---|
| `findByNomeUtente` | `SELECT … WHERE nomeUtente = ?` | `Optional<Utente>` |
| `findById` | `SELECT … WHERE idUtente = ?` | `Optional<Utente>` |
| `findAll` | `SELECT … ORDER BY nomeUtente COLLATE NOCASE` | lista ordinata |
| `create` | `INSERT … ` RETURN_GENERATED_KEYS | valorizza `idUtente` (`UtenteDao.java:57-74`) |
| `count` / `countAmministratori` | `COUNT(*)` | conteggi per bootstrap e vincolo ultimo admin |
| `updateAmministratore` | `UPDATE amministratore …` | ritorna `true` se `executeUpdate == 1` |
| `update` | `UPDATE nomeUtente, password, email …` | password già hash dal chiamante (`:94-106`) |
| `deleteById` | `DELETE …` | idem |

Il `COLONNE` condiviso è `idUtente, nomeUtente, password, email, amministratore`
(`UtenteDao.java:18-19`); il mapping è manuale in `map(ResultSet)` (`:125-133`).

## Modello — `Utente`

POJO `Serializable` (`model/Utente.java:8`); `getPassword()` è `@JsonIgnore`
(`Utente.java:32-35`) → l'hash non finisce mai nel JSON di risposta anche se
un'istanza col campo pieno venisse serializzata. `senzaPassword()` (`:58-65`)
restituisce la copia gratuita di hash, usata per sessione e risposte.

## Database — `Database` e `AppContextListener`

- **Caricamento driver**: `static { Class.forName("org.sqlite.JDBC"); }`
  (`Database.java:19-27`) è esplicito perché in Tomcat il `DriverManager` può essere
  già inizializzato dal common classloader prima del deploy della webapp.
- **Percorso DB** (`getPath`, `:36-48`): `-Dapp.db.path` > `APP_DB_PATH` (env) >
  `${catalina.base}/data/app.db` > `./data/app.db` (assoluto).
- **Connessione** (`getConnection`, `:50-65`): crea le directory del file se
  mancano, apre la connessione e impone `PRAGMA foreign_keys = ON` e
  `PRAGMA busy_timeout = 5000`.
- **`init()`** (`:68-98`):
  1. legge `/schema.sql` dal classpath (`:113-120`) ed esegue gli statement
     separati dal `;`, ignorando righe vuote e commenti (`:70-86`);
  2. migrazione: se `PRAGMA table_info(UTENTI)` non mostra `amministratore`,
     aggiunge la colonna e promuove l'utente con `MIN(idUtente)` (`:87-97`).
- **Punto di ingresso**: `AppContextListener.contextInitialized` chiama
  `Database.init()` e fallisce il deployment con `IllegalStateException` in caso di
  errore (`AppContextListener.java:13-21`).

## Util — `Json` e `PasswordUtil`

- `Json.write/error/read` (`util/Json.java:19-33`): serializzazione Jackson con
  `application/json;charset=UTF-8`; `error` produce `{"error": <messaggio>}`.
- `PasswordUtil` (`util/PasswordUtil.java`): `hash()` con BCrypt `gensalt(COST)`
  dove `COST = 10` (`:8,13-15`); `check()` restituisce `false` (senza sollevare
  eccezioni) per password/hash null o malformati (`:17-26`).

## Filtri e listener (rimandi)

Dettagli su `SpaFallbackFilter`, `CorsFilter`, listener di startup e cookie config
in [architettura.md](architettura.md); comportamento di autenticazione in
[auth.md](auth.md).

## Informazioni non verificate dal codice

Nessuna.
