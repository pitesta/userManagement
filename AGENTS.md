# AGENTS.md

Web app demo: Angular 22 SPA + Java 25 (Jakarta Servlet 6.1) WAR deployed on
Tomcat 11, with SQLite. UI and messages are in Italian.

## Toolchain (user-space, no sudo)

Everything is installed under `~/.local/opt` by `./scripts/install-env.sh`.
Always load the environment before running raw tools:

```bash
source scripts/env.sh
```

This sets `JAVA_HOME` (JDK 25), `PATH` (Node 26, Maven, SQLite, Angular CLI,
Tomcat) and `APP_DB_PATH`. Tomcat is a shared instance: deploy other apps as
separate WARs in `$CATALINA_HOME/webapps`. The SQLite file `data/app.db` is
per-project; change `APP_DB_PATH` to share or relocate it.

## Commands

```bash
./scripts/install-env.sh      # one-time toolchain install
./scripts/start.sh            # db-init + build + deploy + start Tomcat
./scripts/build.sh            # ng build (base-href /prgweb/) + mvn package
./scripts/deploy.sh           # build + copy WAR to Tomcat
./scripts/deploy.sh --no-build
./scripts/dev-frontend.sh     # ng serve :4200 with proxy to :8080/prgweb
./scripts/tomcat-stop.sh
./scripts/set-admin.sh <nomeUtente> [1|0]  # manual admin promote/demote
```

- App URL: http://localhost:8080/prgweb/
- Backend tests: `cd backend && mvn test`
- Single backend test: `cd backend && mvn -Dtest=UtenteDaoTest test`
- Frontend tests: `cd frontend && npx ng test --watch=false` (Vitest, no browser)
- After changing backend code, redeploy with `./scripts/deploy.sh`; Tomcat
  auto-reloads the WAR, no need to stop it.

## Architecture facts that matter

- `database/schema.sql` is the only canonical schema. Maven copies it into the
  WAR classpath; `Database.init()` runs it at startup (idempotent).
- `UtenteDao` uses plain JDBC prepared statements, not an ORM.
- Auth is server-side `HttpSession` + `JSESSIONID`; no JWT. Session user is
  stored without the password hash.
- `UTENTI.amministratore` (INTEGER 0/1) marks administrators. The first
  registered user becomes admin (`AuthServlet.register`); later users default
  to 0. The session stores the flag, so an admin changing their own role has
  the session attribute refreshed by `AdminUtenteServlet`.
- Endpoints: `/api/register`, `/api/login`, `/api/logout`, `/api/me`,
  `/api/utenti` (requires a session) and `/api/admin/utente` (requires an
  admin session, updates the flag), `/api/admin/utente/modifica` (name, email,
  optional password), `/api/admin/utente/elimina` (delete; self-delete returns
  `logout: true`). See `AuthServlet`, `UtentiServlet` and
  `AdminUtenteServlet`.
- The last remaining admin cannot be demoted or deleted: the API returns `409`
  with `Deve rimanere almeno un amministratore.` Use `scripts/set-admin.sh`
  only for manual recovery.
- `SpaFallbackFilter` forwards extensionless GETs (except `/api/*`) to
  `index.html` for Angular HTML5 routing.
- The global top bar lives in `frontend/src/app/layout/topbar.*` and is mounted
  in `app.ts` above the router outlet. Its title is fixed to `Gestione Utenti`
  and must not be translated.
- Translations use the custom signal-based `I18nService`
  (`frontend/src/app/core/i18n.service.ts`) with `it`/`en`/`fr` dictionaries.
  The selected language is persisted in `localStorage` (`prgweb.lingua`);
  Italian is the fallback. Backend error strings stay in Italian and are mapped
  to translation keys by `I18nService.traduciErrore`.
- Angular calls APIs with relative URLs (`api/...`). This relies on
  `<base href>`, which `scripts/build.sh` sets to `/$APP_CONTEXT/`; changing
  `APP_CONTEXT` in `scripts/env.sh` requires a rebuild.
- Dev proxy (`frontend/proxy.conf.json`) rewrites `/api` to `/prgweb/api`.
  The session cookie **must stay `Path=/`** in `WEB-INF/web.xml`, otherwise
  the cookie is not sent by the browser to `localhost:4200`.

## Gotchas (do not regress)

- `password` is `varchar(100)` and stores a BCrypt hash. The original
  `varchar(20)` cannot hold a hash; do not shrink it or store plaintext.
- `Database.init()` migrates old DBs: if `amministratore` is missing it adds
  the column and promotes the oldest user (`MIN(idUtente)`), otherwise the
  app would be left with no administrator. Do not remove this migration.
- Only administrators may call `/api/admin/utente`; the check uses the flag in
  the session, so promote a user and log in again (or let the same admin
  promote themselves through another admin) for the change to apply.
- SQLite JDBC is explicitly loaded with `Class.forName("org.sqlite.JDBC")` in
  `Database`. Removing it breaks Tomcat deployment because `DriverManager` may
  already be initialized by Tomcat's common classloader.
- Java 25 emits native-access warnings for sqlite-jdbc; `--enable-native-access=ALL-UNNAMED`
  is set in Tomcat's generated `bin/setenv.sh` and in surefire.
- DB path precedence: `-Dapp.db.path` > `APP_DB_PATH` > `${catalina.base}/data/app.db`.
- Do not commit `data/`, `backend/target/`, `frontend/dist/`, `frontend/.angular/`,
  or `node_modules/`.

## Conventions

- Java package root: `it.prgweb`; servlets in `web`, DAOs in `dao`, models in
  `model`, utilities in `util`.
- Angular: standalone components, functional guard, lazy-loaded page
  components, signals for state. Pages live in `src/app/pages/*`.
- All visible UI labels go through `I18nService.t('chiave')`; the only fixed
  Italian text is the top bar title `Gestione Utenti`. Add every new key to all
  three dictionaries (`it`, `en`, `fr`).
- Keep API error bodies as `{"error":"messaggio in italiano"}`.
