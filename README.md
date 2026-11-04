# prgWebAngularJava

Applicazione web demo con:

- **Front-end**: Angular 22 (standalone components + Router)
- **Back-end**: Java 25 LTS, Jakarta Servlet 6.1, Maven
- **Server**: Apache Tomcat 11
- **Database**: SQLite 3
- **Sicurezza**: password con hash BCrypt, sessione HTTP

All'avvio l'applicazione mostra la schermata di **login / registrazione**.
Dopo l'accesso è disponibile un'area riservata con menu verticale a sinistra:
Home, Utenti, Nuovo utente, Impostazioni, Logout.

Ogni schermata ha in alto una barra comune con il titolo fisso
**Gestione Utenti** e, a destra, un selettore di lingua: italiano (predefinito),
inglese e francese. Il titolo resta sempre in italiano; tutte le altre
etichette e i messaggi dell'interfaccia vengono tradotti. La lingua scelta è
salvata nel `localStorage` del browser.

## Requisiti

Nessun prerequisito di sistema oltre a `wget`, `tar`, `unzip` e `xz`.
Lo script di setup installa tutto in user-space (`~/.local/opt`), senza `sudo`:

- JDK 25 (Temurin)
- Node.js 26 LTS
- Maven 3.9.x
- Apache Tomcat 11
- SQLite CLI
- Angular CLI 22

## Avvio rapido

```bash
# 1. Installa la toolchain (una sola volta)
./scripts/install-env.sh

# 2. Avvia tutto: database + build + deploy + Tomcat
./scripts/start.sh
```

Apri: **http://localhost:8080/prgweb/**

Alla prima apertura registra un nuovo utente dalla scheda **Registrati**,
poi effettua il login. Il **primo utente registrato diventa automaticamente
amministratore**; gli utenti successivi nascono senza privilegi di
amministrazione.

## Comandi

| Comando | Descrizione |
|---|---|
| `./scripts/install-env.sh` | Installa JDK, Node, Maven, Tomcat, SQLite e Angular CLI |
| `./scripts/db-init.sh` | Crea/aggiorna le tabelle SQLite da `database/schema.sql` |
| `./scripts/build.sh` | Compila Angular e crea `backend/target/prgweb.war` |
| `./scripts/deploy.sh` | Build + copia del WAR in Tomcat |
| `./scripts/deploy.sh --no-build` | Copia in Tomcat il WAR già compilato |
| `./scripts/tomcat-start.sh` | Avvia Tomcat |
| `./scripts/tomcat-stop.sh` | Ferma Tomcat |
| `./scripts/start.sh` | DB + build + deploy + avvio Tomcat |
| `./scripts/dev-frontend.sh` | Dev server Angular su `:4200` con proxy API |
| `./scripts/set-admin.sh <nomeUtente> [1\|0]` | Promuove/revoca un amministratore (recupero manuale) |

### Sviluppo front-end

Con Tomcat già avviato:

```bash
./scripts/dev-frontend.sh
```

Apri **http://localhost:4200/**. Le chiamate a `/api/*` vengono inoltrate a
`http://localhost:8080/prgweb/api/*` tramite `frontend/proxy.conf.json`.
Dopo una modifica al back-end:

```bash
./scripts/deploy.sh
```

### Test

```bash
source scripts/env.sh

# Back-end (JUnit 5/6, DB SQLite temporaneo)
cd backend && mvn test

# Un solo test
cd backend && mvn -Dtest=UtenteDaoTest test

# Front-end (Vitest)
cd frontend && npx ng test --watch=false
```

## Struttura

```
backend/                 API Java (WAR)
  src/main/java/it/prgweb/
    db/Database.java     connessione SQLite + init schema
    dao/UtenteDao.java   prepared statement sulla tabella UTENTI
    web/AuthServlet.java login, registrazione, logout, /me
    web/UtentiServlet.java elenco utenti (protetto da sessione)
    web/AdminUtenteServlet.java ruolo, modifica ed eliminazione utenti (solo admin)
database/schema.sql      schema canonico della tabella UTENTI
frontend/                applicazione Angular
data/app.db              database SQLite (generato, non versionato)
scripts/                 setup, build, deploy e avvio
```

## Database

SQLite, file `data/app.db`. La tabella richiesta è:

```sql
CREATE TABLE IF NOT EXISTS UTENTI (
  idUtente       INTEGER      PRIMARY KEY AUTOINCREMENT,
  nomeUtente     VARCHAR(100) NOT NULL UNIQUE,
  password       VARCHAR(100) NOT NULL,
  email          VARCHAR(40)  NOT NULL,
  amministratore INTEGER      NOT NULL DEFAULT 0
);
```

Note:

- `idUtente` è la chiave primaria numerica progressiva automatica.
- `nomeUtente` è univoco ed è il campo usato per il login.
- `password` contiene un **hash BCrypt**, mai la password in chiaro.
  La lunghezza è stata portata da `varchar(20)` a `varchar(100)` perché
  un hash BCrypt non entra in 20 caratteri.
- L'email è obbligatoria e validata, ma non univoca.
- `amministratore` vale `1` per gli amministratori e `0` per gli utenti
  normali. Il primo utente registrato è amministratore; gli altri no.
- Su un database creato prima dell'introduzione del flag, la colonna viene
  aggiunta automaticamente all'avvio e il primo utente (ID più basso) viene
  promosso ad amministratore, così l'applicazione resta governabile.

### Amministratori

- Solo un utente con `amministratore = 1` vede, nella pagina **Utenti**, sia
  l'interruttore del ruolo sia i pulsanti **Modifica** ed **Elimina** di ogni
  riga.
- La modifica apre una finestra popup con nome utente, email e password
  facoltativa (se lasciata vuota, l'hash esistente resta invariato).
- L'eliminazione richiede una conferma. Se un amministratore elimina il proprio
  account, la sessione viene invalidata e si torna al login.
- Un amministratore può promuovere o revocare altri utenti.
- Non è possibile revocare o eliminare l'ultimo amministratore rimasto
  (`409 Deve rimanere almeno un amministratore.`).
- Per recuperare l'accesso quando nessun amministratore è disponibile:

```bash
./scripts/set-admin.sh mario 1     # promuove mario
./scripts/set-admin.sh mario 0     # revoca (se resta almeno un admin)
```

Per ispezionare il database:

```bash
source scripts/env.sh
sqlite3 data/app.db "SELECT idUtente, nomeUtente, email, amministratore FROM UTENTI;"
```

## API

| Metodo | Percorso | Descrizione |
|---|---|---|
| `POST` | `/api/register` | Registra un utente |
| `POST` | `/api/login` | Autentica e crea la sessione |
| `POST` | `/api/logout` | Invalida la sessione |
| `GET` | `/api/me` | Utente della sessione corrente |
| `GET` | `/api/utenti` | Elenco utenti (richiede sessione) |
| `POST` | `/api/admin/utente` | Imposta il flag amministratore (solo admin) |
| `POST` | `/api/admin/utente/modifica` | Modifica nome, email e password (solo admin) |
| `POST` | `/api/admin/utente/elimina` | Elimina un utente (solo admin) |

Esempio di modifica del flag:

```bash
curl -b cookie.txt -H 'Content-Type: application/json' \
  -d '{"idUtente":2,"amministratore":true}' \
  http://localhost:8080/prgweb/api/admin/utente
```

Esempio di modifica dell'anagrafica (`password` può essere omessa):

```bash
curl -b cookie.txt -H 'Content-Type: application/json' \
  -d '{"idUtente":2,"nomeUtente":"mario","email":"mario@example.com","password":"nuova123"}' \
  http://localhost:8080/prgweb/api/admin/utente/modifica
```

Esempio di eliminazione:

```bash
curl -b cookie.txt -H 'Content-Type: application/json' \
  -d '{"idUtente":2}' \
  http://localhost:8080/prgweb/api/admin/utente/elimina
```

Risposte: `403` se il chiamante non è amministratore, `409` per nome utente
duplicato o quando si prova a revocare/eliminare l'ultimo amministratore.
L'eliminazione restituisce `{"ok":true,"logout":true}` se l'amministratore ha
eliminato il proprio account.

Esempio:

```bash
curl -c cookie.txt -H 'Content-Type: application/json' \
  -d '{"nomeUtente":"mario","password":"segreta123"}' \
  http://localhost:8080/prgweb/api/login
```

## Configurazione

Variabili in `scripts/env.sh`:

- `PROJECT_ROOT`: radice del progetto
- `APP_DB_PATH`: percorso del database SQLite (default `data/app.db`)
- `APP_CONTEXT`: contesto web/Tomcat (default `prgweb`)

Se cambi `APP_CONTEXT`, esegui di nuovo `./scripts/build.sh` perché la
`base href` di Angular e il nome del WAR vengono rigenerati.

Tomcat usa la porta **8080** (configurabile in
`~/.local/opt/apache-tomcat-11.0.27/conf/server.xml`).

## Riutilizzo dell'ambiente

- **Tomcat è condiviso** in `~/.local/opt/apache-tomcat-11.0.27`: altri WAR
  possono essere copiati in `webapps/` e serviti come contesti separati
  (es. `altraapp.war` → `http://localhost:8080/altraapp/`).
- **SQLite CLI e driver JDBC sono condivisi** in `~/.local/opt`.
- Il **file del database è per-applicazione**: `data/app.db`. Per condividere
  lo stesso DB con un'altra app, impostare `APP_DB_PATH` verso lo stesso file
  (o creare un nuovo file per l'altra applicazione).

## Log e troubleshooting

- Log Tomcat: `~/.local/opt/apache-tomcat-11.0.27/logs/catalina.out`
- Se l'app risponde 404 dopo il deploy, controlla che il WAR si chiami
  `prgweb.war` e che il contesto sia `/prgweb`.
- Se il login non mantiene la sessione in sviluppo, verifica che il cookie
  `JSESSIONID` abbia `Path=/` (`backend/src/main/webapp/WEB-INF/web.xml`).
