# Ambiente

## Toolchain e build

`scripts/install-env.sh` installa in user-space la toolchain di sviluppo, senza sudo. Sorgere `scripts/env.sh` per selezionare Java 25, Node 26 e Maven e configurare i default. Per la sola esecuzione del JAR serve Java/JRE 25; Node e Maven servono per build e test.

| Variabile | Default | Significato |
|---|---|---|
| `PROJECT_ROOT` | directory del repository | radice progetto |
| `PRGWEB_DB_PATH` | `<repo>/data` | directory che contiene `app.mv.db` |
| `SERVER_PORT` | `8080` | porta Tomcat embedded |
| `SERVER_SERVLET_CONTEXT_PATH` | `/prgweb` | context path HTTP |
| `APP_CONTEXT` | `prgweb` | base href Angular impostata durante la build |
| `PRGWEB_BACKUP_PATH` | non impostata | directory opzionale passata allo script backup |

```bash
source scripts/env.sh
java -version
./scripts/build.sh
```

Non sono richiesti né Tomcat esterno né SQLite. Lo script installa solo gli strumenti necessari al progetto; la distribuzione runtime è il JAR Spring Boot.

## Frontend in sviluppo

`./scripts/dev-frontend.sh` avvia Angular su `:4200`. Il proxy inoltra le richieste API a Spring Boot sul context path configurato. Per una porta o un context path diverso, impostare le variabili prima di avviare `scripts/start.sh`; mantenere `APP_CONTEXT` allineato al context path e ricostruire gli asset dopo averlo cambiato.
