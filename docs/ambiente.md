# Ambiente di sviluppo

*Verificato sul codice: 05/11/2026.*

## Fonti di verifica

- `scripts/install-env.sh`, `scripts/env.sh` (sorgenti consultati)
- `backend/pom.xml` (dipendenze), `frontend/package.json`, `frontend/package-lock.json` (`packageManager`)
- `frontend/proxy.conf.json`
- Verifica effettiva eseguita in data odierna: `source scripts/env.sh && java -version`
  su questa macchina.

## Installazione della toolchain (user-space, senza sudo)

`scripts/install-env.sh` installa tutto sotto `~/.local/opt` (variabile `TOOLS_DIR`
con default `$HOME/.local/opt` — `env.sh:4`). Contenuto attuale della directory
(verificato in un'installazione esistente):

```
~/.local/opt/
├── jdk-25.0.5+8/                 # JDK 25 (selezionato automaticamente da env.sh)
├── jdk  (symlink) + jdk-21.0.13+9
├── apache-maven-3.9.16/
├── apache-tomcat-11.0.27/
├── node-v26.13.0-linux-x64/
├── git/
└── (binari sqlite3 estratti direttamente in TOOLS_DIR — env.sh:13)
```

- Lo script di installazione non è eseguito in questa sessione (toolchain già
  presente): i dettagli di download e installazione vanno letti nel file stesso.

## `scripts/env.sh` — variabili esportate

Da `source scripts/env.sh` (riga per riga, `env.sh:3-26`):

| Variabile | Valore | Righe |
|---|---|---|
| `PROJECT_ROOT` | directory sopra `scripts/` risolta in assoluto | `env.sh:3-4` |
| `TOOLS_DIR` | `${TOOLS_DIR:-$HOME/.local/opt}` | `env.sh:4` |
| `JAVA_HOME` | l'ultimo `jdk-25*` presente in `TOOLS_DIR` (ordinato con `sort -V`), altrimenti `$TOOLS_DIR/jdk` | `env.sh:6-13` |
| `NODE_HOME` | `$TOOLS_DIR/node-v26.13.0-linux-x64` | `env.sh:10` |
| `MAVEN_HOME` | `$TOOLS_DIR/apache-maven-3.9.16` | `env.sh:11` |
| `CATALINA_HOME` | `$TOOLS_DIR/apache-tomcat-11.0.27` | `env.sh:12` |
| `SQLITE_HOME` | `$TOOLS_DIR` (binario `sqlite3` estratto lì) | `env.sh:13-14` |
| `PATH` | `JAVA_HOME/bin : NODE_HOME/bin : MAVEN_HOME/bin : SQLITE_HOME : TOOLS_DIR/npm-global/bin : PATH` | `env.sh:22` |
| `APP_DB_PATH` | `${APP_DB_PATH:-$PROJECT_ROOT/data/app.db}` | `env.sh:23` |
| `APP_CONTEXT` | `${APP_CONTEXT:-prgweb}` — deve coincidere con la directory usata da Tomcat (`bin/setenv.sh`) | `env.sh:25-26` |

Verifica eseguita (05/11/2026, questa macchina):

```console
$ source scripts/env.sh && echo "$JAVA_HOME"
/home/ptesta/.local/opt/jdk-25.0.5+8
$ java -version
openjdk version "25.0.5" 2026-10-20 LTS
```

## Dipendenze dell'applicazione

- **Backend** (`backend/pom.xml`): nella sezione `<dependencies>` — Servlet 6.1
  (`provided`), `sqlite-jdbc` 3.54.1.0, `jbcrypt` 0.4, `jackson-databind` 2.22.4,
  `junit-jupiter` 6.1.3 (test).
- **Frontend** (`frontend/package.json`): Angular `^22.2.0`, RxJS `~7.8.0`,
  TypeScript `~6.0.2`; devDependencies `vitest ^5.0.0`, `jsdom ^30.0.0`,
  `@angular/cli`/`@angular/build` `^22.2.5`; `packageManager: npm@11.20.0`.
- **Proxy dev** (`frontend/proxy.conf.json`): `/api` → `http://localhost:8080`
  con `pathRewrite: {"^/api": "/prgweb/api"}`, `secure: false`,
  `changeOrigin: false`, `logLevel: warn`.

## Nota Java 25 + sqlite-jdbc

Su Java 25, sqlite-jdbc produce warning di *native access*; il flag
`--enable-native-access=ALL-UNNAMED` è configurato in due posti:

- Maven surefire per i test (`backend/pom.xml:78-81`);
- istanza Tomcat condivisa, nel `bin/setenv.sh` generato dall'installazione
  (l'istruzione è citata in AGENTS.md; il file vive in `~/.local/opt/…/bin/setenv.sh`
  fuori dal repository).

## Relazioni tra variabili e componenti (per chiarezza)

```
scripts/env.sh
  ├── APP_CONTEXT ── scripts/build.sh  (ng build --base-href "/$APP_CONTEXT/")
  │                └ scripts/deploy.sh (webapps/$APP_CONTEXT.war)
  ├── APP_DB_PATH ── scripts/db-init.sh e backend (Database.getPath())
  └── CATALINA_HOME → tomcat-start.sh / tomcat-stop.sh / deploy.sh
```

## Informazioni non verificate dal codice

- `scripts/install-env.sh` non eseguito in sessione: dichiarazioni di versione
  del download/refresh dei tool rimandate alla lettura del file.
- `bin/setenv.sh` di Tomcat non è nel repository (fuori repo): la dichiarazione
  viene da `AGENTS.md` e non è raffrontata in questa sessione.
