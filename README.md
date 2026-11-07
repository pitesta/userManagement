# prgWebAngularJava

Applicazione web demo con:

- **Front-end**: Angular 22 (standalone components + Router)
- **Back-end**: Java 25 LTS, Spring Boot 4.1.2, Spring MVC + JPA
- **Server**: Tomcat 11 embedded in the executable JAR
- **Database**: H2 embedded su file, schema gestito da Flyway
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
- Angular CLI 22

## Avvio rapido

```bash
# 1. Installa la toolchain (una sola volta)
./scripts/install-env.sh

# 2. Crea il fat JAR eseguibile con il frontend incluso
./scripts/build.sh

# 3. Avvia Spring Boot con Tomcat embedded e H2 su file
./scripts/start.sh
```

Apri: **http://localhost:8080/prgweb/**

Per arrestare l'applicazione in modo pulito:

```bash
./scripts/stop.sh
```

Al primo avvio accedi con l'account amministratore iniziale `admin` / `admin`
e modifica la password dalla gestione del tuo profilo. Le registrazioni
successive creano utenti senza privilegi amministrativi; l'amministratore può
assegnare o revocare i ruoli dalla pagina **Utenti**.

## Comandi

| Comando | Descrizione |
|---|---|
| `./scripts/install-env.sh` | Installa JDK, Node, Maven e Angular CLI |
| `./scripts/build.sh` | Compila Angular e crea `backend/target/prgweb.jar` eseguibile |
| `./scripts/start.sh` | Avvia direttamente il fat JAR Spring Boot già compilato |
| `./scripts/stop.sh` | Arresta Spring Boot con SIGTERM e chiusura pulita di H2 |
| `./scripts/db-backup.sh` | Arresta l'app e crea un backup H2 offline; lascia l'app ferma |
| `./scripts/db-restore.sh <zip>` | Ripristina un backup H2 con l'app ferma |
| `./scripts/db-console.sh` | Apre H2 Shell solo con l'app ferma |
| `./scripts/set-admin.sh <utente> [1|0]` | Aggiorna il ruolo amministratore a DB chiuso |
| `./scripts/dev-frontend.sh` | Dev server Angular su `:4200` con proxy API |

### Sviluppo front-end

Con Spring Boot avviato:

```bash
./scripts/dev-frontend.sh
```

Apri **http://localhost:4200/**. Le chiamate a `/api/*` vengono inoltrate a
`http://localhost:8080/prgweb/api/*` tramite `frontend/proxy.conf.json`.
Dopo una modifica al back-end:

```bash
./scripts/build.sh
./scripts/stop.sh
./scripts/start.sh
```

### Test

```bash
source scripts/env.sh

# Back-end (JUnit, H2 temporaneo)
cd backend && mvn test

# Un solo test
cd backend && mvn -Dtest=UtenteRepositoryTest test

# Front-end (Vitest)
cd frontend && npx ng test --watch=false
```

## Struttura

```
backend/                 API Spring Boot (fat JAR)
  src/main/java/it/prgweb/
    web/                 controller REST e gestione errori
    service/             regole di dominio transazionali
    repository/          Spring Data JPA e mutex applicativo
  src/main/resources/
    db/migration/        schema Flyway versionato
frontend/                applicazione Angular
data/app.mv.db           database H2 (generato, non versionato)
scripts/                 setup, build, avvio e arresto
```

## Database

H2 Embedded persiste in `data/app.mv.db` per impostazione predefinita.
Flyway crea lo schema da `backend/src/main/resources/db/migration/`; Hibernate
lo valida all'avvio. Il primo avvio non richiede un comando di inizializzazione.

Note:

- `idUtente` è la chiave primaria numerica progressiva automatica.
- `nomeUtente` è univoco ed è il campo usato per il login.
- `password` contiene un **hash BCrypt**, mai la password in chiaro.
  La lunghezza è stata portata da `varchar(20)` a `varchar(100)` perché
  un hash BCrypt non entra in 20 caratteri.
- L'email è obbligatoria e validata, ma non univoca.
- `amministratore` è booleano. In una nuova installazione Flyway crea
  l'account `admin`; le registrazioni successive non sono amministratori.
- Gli account SQLite precedenti non vengono migrati.

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
## Backup e ripristino

H2 Embedded usa `AUTO_SERVER=FALSE`: gli strumenti H2 standalone devono essere
eseguiti dopo la chiusura completa dell'applicazione. `db-backup.sh` arresta
Spring Boot, verifica il processo e salva uno ZIP sotto
`$PRGWEB_DB_PATH/backups/`; dopo il successo l'app resta ferma. Avviala
nuovamente con `./scripts/start.sh` quando vuoi.

Per ripristinare, con l'app ferma:

```bash
./scripts/db-restore.sh /percorso/prgweb-<timestamp>.zip
./scripts/start.sh
```

Dettagli e verifica del flusso in [docs/database.md](docs/database.md).

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
- `PRGWEB_DB_PATH`: directory del database H2 (default `data/`)
- `SERVER_PORT`: porta Spring Boot (default `8080`)
- `SERVER_SERVLET_CONTEXT_PATH`: context path (default `/prgweb`)
- `APP_CONTEXT`: base href Angular durante la build (default `prgweb`)

Se cambi `APP_CONTEXT`, esegui di nuovo `./scripts/build.sh` perché la
`base href` degli asset Angular venga rigenerata. Mantieni il context path del
server allineato tramite `SERVER_SERVLET_CONTEXT_PATH`.

Il JAR include Tomcat embedded: non è necessario installare o avviare un
Tomcat esterno.

## Riutilizzo dell'ambiente

- Il **file del database è per-applicazione**. Imposta `PRGWEB_DB_PATH` per
  salvarlo in un'altra directory; usa una directory dedicata all'istanza.

## Log e troubleshooting

- Log applicazione: `data/prgweb.log`
- Se l'app non si avvia, controlla che `backend/target/prgweb.jar` esista;
  in caso contrario esegui `./scripts/build.sh`.
- Se il login non mantiene la sessione in sviluppo, verifica che il cookie
  `JSESSIONID` abbia `Path=/` nella configurazione Spring Boot.
