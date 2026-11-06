# Build, avvio e deploy

*Verificato sul codice: 05/11/2026. Build, deploy e avvio Tomcat eseguiti con
esito positivo in questa sessione (sezione "Verifica eseguita").*

## Fonti di verifica

- `scripts/build.sh`, `scripts/deploy.sh`, `scripts/start.sh`, `scripts/db-init.sh`,
  `scripts/dev-frontend.sh`, `scripts/tomcat-start.sh`, `scripts/tomcat-stop.sh`,
  `scripts/set-admin.sh` (interi file)
- Esecuzione reale di `./scripts/build.sh` (05/11/2026) su questa macchina

## Panorama

```
start.sh ──> db-init.sh ──> deploy.sh ──> tomcat-start.sh
                        (deploy chiama build.sh se non --no-build)

Sequenza tipica di sviluppo:
  ./scripts/start.sh            # completo: db + build + deploy + Tomcat
  ./scripts/dev-frontend.sh     # opzionale: ng serve :4200 + proxy (sviluppo UI)
```

## `scripts/build.sh` (fronte 1: ng build; fronte 2: mvn package)

Rif. righe per riga:

1. `set -euo pipefail`; risolve `SCRIPT_DIR` e inietta `env.sh` (`build.sh:2-5`).
2. `npm ci --no-audit --no-fund` in `frontend/` (`build.sh:9`).
3. `npx ng build --configuration production --base-href "/$APP_CONTEXT/"`
   (`build.sh:10`) — la `base-href` dipende da `APP_CONTEXT` (default `prgweb`,
   `env.sh:25-26`); cambiare il context richiede una nuova build.
4. `mvn -q clean package` in `backend/` (`build.sh:13-15`); `<finalName>prgweb</finalName>`
   dà `backend/target/prgweb.war` (`pom.xml:54`).

### Verifica eseguita (output reale, abbreviato)

```console
$ ./scripts/build.sh
==> Build Angular
[npm ci ...]
Application bundle generation complete.
  main-*.js 103.75 kB (26.56 kB gz) · styles-*.css 462 B · initial 291.21 kB
  lazy chunk files (5): utenti 19.30 kB · login 11.96 kB ·
     nuovo-utente 6.39 kB · home 4.63 kB · impostazioni 3.63 kB
Output location: /home/ptesta/sviluppo/opencode/prgWebAngularJava/frontend/dist/frontend
==> Build WAR
WAR pronto: /home/ptesta/sviluppo/opencode/prgWebAngularJava/backend/target/prgweb.war
$ ./scripts/deploy.sh --no-build
Deploy completato: /home/ptesta/.local/opt/apache-tomcat-11.0.27/webapps/prgweb.war
$ ./scripts/tomcat-start.sh
Tomcat pronto: http://localhost:8080/prgweb/
```

Confermato: i *lazy chunk* corrispondono 1:1 alle 5 pagine con `loadComponent`
(dettaglio già in [frontend.md](frontend.md)). Il catalogo completo dei codici di
stato verificati con `curl` è in [api.md](api.md).

## `scripts/db-init.sh`

Il database è preparato con il client `sqlite3` di `SQLITE_HOME`:
`mkdir -p` della directory di `APP_DB_PATH`, poi
`sqlite3 "$APP_DB_PATH" < database/schema.sql` (`db-init.sh:9-13`), quindi stampa
di `.schema UTENTI`. Idempotente: `schema.sql` usa `CREATE TABLE/IF NOT EXISTS` e
`CREATE INDEX IF NOT EXISTS` (vedi [database.md](database.md)).

## `scripts/deploy.sh`

- Senza `--no-build` invoca `build.sh` (`deploy.sh:7-9`); con `--no-build` usa il
  WAR esistente.
- Controlla `backend/target/prgweb.war` e abortisce se assente (`:11-16`).
- Se Tomcat è in esecuzione (`pgrep -f catalina.base=$CATALINA_HOME`) lo ferma con
  `tomcat-stop.sh` e aspetta fino a 20 secondi (`:18-26`).
- Copia il WAR in `$CATALINA_HOME/webapps/$APP_CONTEXT.war` (rimozione
  preventiva della copia precedente, `:29-30`).
- Se Tomcat era in esecuzione, lo riavvia (`:32-34`).

Grazie al classloader per applicazione di Tomcat, il nuovo WAR e il reload
subiscono un deploy "freddo" qui: non c'è support hot-swap (il cambiamento
del codice Java richiede un nuovo deploy per essere attivo).

## Avvio / stop di Tomcat

- `tomcat-start.sh`: controlla il processo con lo stesso `pgrep`; se non gira
  avvia `$CATALINA_HOME/bin/startup.sh`; poi **sonda** `http://localhost:8080/`
  con `wget --spider` fino a 40 volte, 1 s ciascuna (`tomcat-start.sh:9-23`);
  in caso di fallimento segnala `logs/catalina.out`.
- `tomcat-stop.sh`: `$CATALINA_HOME/bin/shutdown.sh`, `|| true` per non fallire se
  già fermo (`tomcat-stop.sh:8-9`).

## `scripts/start.sh`

Tre step in sequenza rigida con `set -euo pipefail`: `db-init.sh` → `deploy.sh`
(che fa build) → `tomcat-start.sh` (`start.sh:7-9`).

## Sviluppo frontend: `scripts/dev-frontend.sh`

- `source env.sh`; se `node_modules` manca esegue `npm ci` (`dev-frontend.sh:9-12`);
- `npm start` → `ng serve --proxy-config proxy.conf.json` (script `start` di
  `frontend/package.json:4`) → `http://localhost:4200` con le chiamate `/api`
  reinviate a `:8080/prgweb/api` (dettagli in [architettura.md](architettura.md)).

## `scripts/set-admin.sh` (recupero manuale)

`./scripts/set-admin.sh <nomeUtente> [1|0]` (`:6`):

- apre `APP_DB_PATH` con `sqlite3` (`:15-20`); se il DB non esiste invita a
  `db-init.sh` (`:26-29`);
- aggiunge la colonna `amministratore` se manca (`:31-34`);
- escape dell'apostrofo del nome (`NOME_SQL`, `:38`);
- rifiuta di rimuovere il flag all'**ultimo amministratore** (`:48-57`);
- esegue `UPDATE` e stampa la lista aggiornata (`:59-69`).

## URL conclusivi

| Scenario | URL |
|---|---|
| App in Tomcat | `http://localhost:8080/prgweb/` |
| Dev server | `http://localhost:4200` (proxy `/api` verso `:8080/prgweb/api`) |
| Tomcat shell | `http://localhost:8080/` (solo ping di `tomcat-start.sh`) |

## Informazioni non verificate dal codice

- Nessuna: build, deploy, avvio e stop di Tomcat sono stati eseguiti con esito
  positivo in questa sessione.
