# Database SQLite

*Verificato sul codice: 05/11/2026.*

## Fonti di verifica

- `database/schema.sql` (intero file)
- `backend/src/main/java/it/prgweb/db/Database.java` (intero file)
- `backend/src/main/java/it/prgweb/web/AppContextListener.java` (intero file)
- `backend/pom.xml` (righe `56-65` risorse; `82-84` area surefire)
- `scripts/env.sh` (riga `23`: `APP_DB_PATH`), `scripts/db-init.sh` (intero file)
- `.gitignore` (sezione "Database SQLite")
- `backend/src/main/java/it/prgweb/dao/UtenteDao.java` (query)

## Schema canonico — `database/schema.sql`

Il file è **l'unica fonte dello schema**: lo stesso file viene usato

1. da Maven, che lo copia nel classpath del WAR (`pom.xml:56-65`, path
   `${project.basedir}/../database`) — poi letto da `Database.init()` come
   `/schema.sql`;
2. da `scripts/db-init.sh`, che lo applica con il client `sqlite3` prima del deploy
   (`db-init.sh:10-13`).

Contenuto integrale:

```sql
CREATE TABLE IF NOT EXISTS UTENTI (
  idUtente      INTEGER      PRIMARY KEY AUTOINCREMENT,
  nomeUtente    VARCHAR(100) NOT NULL UNIQUE,
  password      VARCHAR(100) NOT NULL,
  email         VARCHAR(40)  NOT NULL,
  amministratore INTEGER    NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_utenti_nomeUtente ON UTENTI (nomeUtente);
```

Colonne: `idUtente` PK autoincrement; `nomeUtente` obbligatorio e unico (indice
esplicito `idx_utenti_nomeUtente` + vincolo `UNIQUE` sulla tabella); `password`
dichiara `varchar(100)` perché **contiene sempre un hash BCrypt** (il commento in
testa al file registra la sostituzione del `varchar(20)` originale);
`email` `varchar(40)`; `amministratore` intero 0/1 con default `0`.

## Inizializzazione — `Database.init()` (idempotente)

Invocato all'avvio del contesto da `AppContextListener` (`AppContextListener.java:13-21`).

1. **Creazione**: legge `/schema.sql` dal classpath (`Database.java:113-120`),
   spezza il file per `;` costruendo statement riga per riga ed esegue ciascun
   statement per riga ed esegue ciascun statement, saltando i commenti (`--`) e le
   righe vuote (`:70-86`). Gli statement
   `CREATE … IF NOT EXISTS` rendono l'avvio ripetibile senza errori.
2. **Migrazione** (`:87-97`): se `PRAGMA table_info(UTENTI)` non riporta una colonna
   `amministratore` (`hasColumn`, `:100-111` — confronto case-insensitive):
   - `ALTER TABLE UTENTI ADD COLUMN amministratore INTEGER NOT NULL DEFAULT 0`;
   - `UPDATE UTENTI SET amministratore = 1 WHERE idUtente = (SELECT MIN(idUtente))`
     — promozione dell'utente più anziano per garantire che **resti almeno un admin**
     su database preesistenti (altrimenti l'app sarebbe inaccessibile alle
     funzioni amministrative).

## Connessione

`Database.getConnection()` (`Database.java:50-65`):

- percorsi risolti da `getPath()` con precedenza
  **`-Dapp.db.path` > `APP_DB_PATH` > `${catalina.base}/data/app.db` > `./data/app.db`**
  (`:33-48`);
- creazione della directory del file se assente (`Files.createDirectories`, `:51-58`);
- connessione `jdbc:sqlite:<percorso assoluto>`;
- all'apertura esegue `PRAGMA foreign_keys = ON` e `PRAGMA busy_timeout = 5000`
  (`:60-63`).

La registrazione esplicita del driver avviene in un blocco `static`
(`Class.forName("org.sqlite.JDBC")`, `:19-27`) per il modo di classloading di Tomcat
(ulteriori dettagli in [backend.md](backend.md)).

## Dati esistenti e sequenze di bootstrap

| Evento | Effetto |
|---|---|
| DB nuovo (schema assente o vuoto) | `CREATE TABLE IF NOT EXISTS` crea `UTENTI` già **con** `amministratore`; nessuna promozione dato che la colonna è dall'inizio |
| Prima registrazione | Il primo account riceve `amministratore = 1` perché `dao.count() == 0` in `AuthServlet.register` (`AuthServlet.java:120`) |
| DB antico senza colonna (migrazione) | `init()` aggiunge la colonna e promuove `MIN(idUtente)` indipendentemente da quante righe ci siano |
| Script di recupero manuale (`set-admin.sh`) | aggiunge la colonna anche se manca e porta un utente a 1/0; **rifiuta** `0` sull'ultimo admin (vedi blocchi `:48-75`) |

## Query usate dall'applicazione

Tutte le query sopra `UTENTI` sono in `UtenteDao` (prepared statement, dettagli in
[backend.md](backend.md)): `SELECT idUtente, nomeUtente, password, email,
amministratore` (una riga per chiave o per nomeUtente, oppure lista `ORDER BY
nomeUtente COLLATE NOCASE`), `INSERT` con chiave generata, `UPDATE ` (solo flag o
anagrafica + password), `DELETE`, e due `COUNT(*)` (`UtenteDao.java:76-82`).

## Posizione del file e git

- In sviluppo `data/app.db` nella root del progetto (da `APP_DB_PATH` impostata da
  `scripts/env.sh:23`);
- `data/`, `*.db`, `*.db-wal`, `*.db-shm` sono nella `.gitignore`
  (sezione "Database SQLite"): il file di database non è in versione.

## Informazioni non verificate dal codice

- La "varchar(100) invece del varchar(20) originale" è dichiarata nel commento di
  `database/schema.sql` (storia del progetto): il codice attuale vale per il solo
  schema presente oggi.
