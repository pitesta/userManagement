# AGENTS.md

Demo web app: Angular 22 SPA + Java 25 Spring Boot 4.1.2 backend, packaged as
an executable fat JAR with embedded Tomcat 11 and H2 Embedded on disk. UI and
messages are in Italian.

## Toolchain (user-space, no sudo)

Everything is installed under `~/.local/opt` by `./scripts/install-env.sh`.
Load the environment before running raw tools:

```bash
source scripts/env.sh
```

This sets Java 25, Node 26, Maven, Angular CLI, `PROJECT_ROOT`, and defaults
for `PRGWEB_DB_PATH`, `SERVER_PORT`, and `SERVER_SERVLET_CONTEXT_PATH`.
No external Tomcat or database server is used.

## Commands

```bash
./scripts/install-env.sh      # one-time toolchain install
./scripts/build.sh            # Angular build + executable backend/target/prgweb.jar
./scripts/start.sh            # start the built JAR (does not build)
./scripts/stop.sh             # SIGTERM and graceful H2 shutdown
./scripts/db-backup.sh        # stops the app, creates offline H2 backup, leaves it stopped
./scripts/db-restore.sh <zip> # restores with the app stopped; optional --start
./scripts/db-console.sh       # H2 Shell, refuses while app is active
./scripts/set-admin.sh <user> [1|0] # stops app, updates role, leaves it stopped
./scripts/dev-frontend.sh     # ng serve :4200 with API proxy to :8080/prgweb
```

- App URL: `http://localhost:${SERVER_PORT:-8080}${SERVER_SERVLET_CONTEXT_PATH:-/prgweb}/`
- H2 directory: `PRGWEB_DB_PATH` (default `<repo>/data`, file `app.mv.db`)
- Backend tests: `cd backend && mvn test`
- Single backend test: `cd backend && mvn -Dtest=UtenteRepositoryTest test`
- Frontend tests: `cd frontend && npx ng test --watch=false` (Vitest, no browser)
- To include Angular in the JAR, use `./scripts/build.sh`; it builds Angular first
  and verifies `BOOT-INF/classes/static/index.html` in the final JAR.

## Architecture facts that matter

- `backend/src/main/resources/db/migration/V1__create_utenti.sql` is the
  initial schema. Flyway applies migrations at startup; Hibernate uses
  `ddl-auto=validate` and globally quoted identifiers to match the lowercase,
  quoted names in V1.
- Persistence is Spring Data JPA + Hibernate. `UtenteRepository` handles
  account access; `AppLockRepository` locks the permanent `app_lock` row for
  registration and last-administrator invariants.
- Domain operations live in `service/`; HTTP endpoints live in REST
  controllers under `web/`. Auth uses server-side `HttpSession` and
  `JSESSIONID`, not JWT. Session data excludes the password hash.
- The first registered user is an administrator. The last remaining admin
  cannot be demoted or deleted; the API returns `409` with
  `Deve rimanere almeno un amministratore.`
- Endpoints: `/api/register`, `/api/login`, `/api/logout`, `/api/me`,
  `/api/utenti`, `/api/admin/utente`, `/api/admin/utente/modifica`,
  `/api/admin/utente/elimina`.
- `WebConfig` registers the SPA fallback for extensionless GETs outside `/api/**` to
  `index.html`. CORS permits the Angular dev origin `http://localhost:4200`.
- The Angular build uses base href `/$APP_CONTEXT/` (default `/prgweb/`). Keep
  it aligned with `SERVER_SERVLET_CONTEXT_PATH` when changing the context.
- The top bar title is fixed to `Gestione Utenti`. Translations use the custom
  signal-based `I18nService` with `it`/`en`/`fr` dictionaries.

## Gotchas (do not regress)

- `password` stores BCrypt hashes, never plaintext. BCrypt accepts at most 72
  input bytes; retain the existing validation.
- Keep `hibernate.globally_quoted_identifiers=true`: Flyway V1 creates quoted
  lowercase table and column names. Without it Hibernate schema validation
  fails at startup.
- Keep `PRGWEB_DB_PATH` as a directory and use the H2 URL configured in
  `application.yml`; do not point it at the old SQLite file.
- H2 is single-instance (`AUTO_SERVER=FALSE`). Do not run two app processes
  against the same database directory.
- H2 Shell and Restore may open the database only after Spring Boot has stopped.
  `db-backup.sh` performs a controlled stop and leaves the app stopped; restore
  and `set-admin.sh` also leave it stopped unless restore is called with `--start`.
- Login rotates the session ID. Cookie settings are `Path=/`, `HttpOnly`,
  `SameSite=Lax`, with a 30-minute inactivity timeout.
- Do not commit `data/`, `backend/target/`, `frontend/dist/`,
  `frontend/.angular/`, or `node_modules/`.

## Conventions

- Java package root: `it.prgweb`; controllers and advice in `web`, repositories
  in `repository`, services in `service`, models in `model`.
- Angular: standalone components, functional guard, lazy-loaded page
  components, signals for state. Pages live in `frontend/src/app/pages/*`.
- All visible UI labels go through `I18nService.t('chiave')`; the only fixed
  Italian text is the top bar title. Add every new key to all three language
  dictionaries.
- Keep API error bodies as `{"error":"messaggio in italiano"}`.
