# Build e avvio autonomo

Il backend si distribuisce come fat JAR Spring Boot 4.1.2 con Tomcat 11 embedded e H2 Embedded. Per il runtime serve Java/JRE 25; per la build servono anche Node 26/npm e Maven.

## Build

```bash
source scripts/env.sh
./scripts/build.sh
```

Lo script esegue `npm ci`, compila Angular con base href `/$APP_CONTEXT/`, verifica `frontend/dist/frontend/browser/index.html`, esegue `mvn clean package`, copia la versione Maven di H2 in `scripts/lib/h2.jar` e controlla il contenuto finale.

Artefatti: `backend/target/prgweb.jar` (fat JAR con `BOOT-INF/classes/static/index.html`) e `scripts/lib/h2.jar` (tool H2 standalone per Shell/Restore).

## Avvio e arresto

```bash
./scripts/start.sh
./scripts/stop.sh
```

Lo start avvia il JAR esistente con `java -jar`, scrive PID e log sotto `data/` e attende la risposta HTTP. Lo stop invia SIGTERM e attende la chiusura di Spring, Hikari e H2.

URL predefinito: `http://localhost:8080/prgweb/`. Nessun Tomcat esterno è necessario.

## Configurazione

| Variabile | Default | Uso |
|---|---|---|
| `PRGWEB_DB_PATH` | `<repo>/data` | directory H2 |
| `SERVER_PORT` | `8080` | porta embedded |
| `SERVER_SERVLET_CONTEXT_PATH` | `/prgweb` | context path |
| `APP_CONTEXT` | `prgweb` | base href frontend |

Per avviare su una porta dedicata: `SERVER_PORT=8081 ./scripts/start.sh`. Se si modifica il context path, mantenerlo allineato a `APP_CONTEXT` e ricostruire gli asset.

## Backup e ripristino

La procedura operativa è offline: `db-backup.sh` arresta e verifica l'app prima di H2 Shell, poi lascia il servizio fermo. `db-restore.sh` richiede il processo fermo e resta fermo dopo il ripristino, salvo `--start`. I comandi e le verifiche end-to-end sono in [database.md](database.md).
