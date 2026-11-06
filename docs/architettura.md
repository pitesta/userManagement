# Architettura generale

*Verificato sul codice: 05/11/2026.*

## Fonti di verifica

- `backend/src/main/webapp/WEB-INF/web.xml` (intero file)
- `backend/src/main/webapp/META-INF/context.xml` (intero file)
- `backend/src/main/java/it/prgweb/web/SpaFallbackFilter.java` (intero file)
- `backend/src/main/java/it/prgweb/web/CorsFilter.java` (intero file)
- `backend/src/main/web.xml` — non esiste; la configurazione è tutta in
  `backend/src/main/webapp/WEB-INF/web.xml`
- `frontend/proxy.conf.json` (intero file)
- `frontend/src/app/app.ts` (righe `2,6,8`), `frontend/src/app/app.config.ts` (intero file)
- `backend/pom.xml` (sezione `<resources>`: righe `56-65`; riga `54`: `<finalName>prgweb</finalName>`)
- `scripts/build.sh` (riga `10` `--base-href`), `scripts/deploy.sh` (riga `37`: copia WAR in `$CATALINA_HOME/webapps`)
- `backend/src/main/java/it/prgweb/web/AppContextListener.java` (intero file)

## Un contesto, due livelli

Frontend e backend convivono **sullo stesso processo e nello stesso contesto web**:
il WAR `prgweb` pubblicato in `$CATALINA_HOME/webapps/prgweb.war` contiene sia le
classi Java (servlet) sia il bundle Angular precompilato. Il browser parla con un
solo origin; non esiste un server frontend separato in produzione.

```
                          [Browser]
                              |
                              v  HTTP :8080
+--------------------------------------------------------------+
|  Tomcat 11  (contesto /prgweb)                               |
|                                                              |
|  Richiesta                                                   |
|    v                                                         |
|  [SpaFallbackFilter  /*   @WebFilter annotato]                |
|    |  GET senza estensione e fuori /api/*  --> index.html     |
|    |  altrimenti chain.doFilter                              |
|    v                                                         |
|  [CorsFilter        /api/*  @WebFilter]                      |
|    |  regole CORS senza catena: solo header + short-circuit  |
|    |  su OPTIONS                                             |
|    v                                                         |
|  +---------------------------+     +----------------------+  |
|  | Statici (bundle Angular)  |     | Servlet /api/*       |  |
|  | index.html, JS, CSS       |     | AuthServlet          |  |
|  +---------------------------+     | UtentiServlet        |  |
|                                    | AdminUtenteServlet   |  |
|                                    +----------+-----------+  |
|                                               v             |
|                                    [UtenteDao -> SQLite]     |
|                                    data/app.db (file)        |
+--------------------------------------------------------------+
```

## Componenti di confine

| Componente | Mappatura | Ruolo | Sorgente |
|---|---|---|---|
| `SpaFallbackFilter` | `@WebFilter(urlPatterns = "/*")` | `forward` di `index.html` per le URL senza estensione e fuori `/api/*`, per supportare l'HTML5 routing di Angular | `SpaFallbackFilter.java:18,27-36` |
| `CorsFilter` | `@WebFilter(urlPatterns = "/api/*")` | Header CORS **solo** se `Origin` combacia esattamente con `http://localhost:4200` (`Access-Control-Allow-Origin`, `Allow-Credentials`, `Allow-Methods`, `Allow-Headers`, `Vary: Origin`); su `OPTIONS` risponde `204` senza passare alle servlet | `CorsFilter.java:15,26-37` |
| `AuthServlet` | `/api/login`, `/api/register`, `/api/logout`, `/api/me` | Autenticazione | `AuthServlet.java:23` |
| `UtentiServlet` | `GET /api/utenti` | Elenco utenti | `UtentiServlet.java:19` |
| `AdminUtenteServlet` | `/api/admin/utente`, `/api/admin/utente/modifica`, `/api/admin/utente/elimina` | Operazioni admin | `AdminUtenteServlet.java:25-29` |
| `AppContextListener` | `@WebListener` | All'avvio del contesto esegue `Database.init()` (schema + migrazione) | `AppContextListener.java:9-21` |
| Cookie di sessione | `<cookie-config>`, `<tracking-mode>COOKIE</tracking-mode>` | `HttpOnly`, `Path=/`, sessione solo via cookie | `web.xml:9-17` |
| Cookie SameSite | `<CookieProcessor sameSiteCookies="lax"/>` | `Lax` sul cookie di sessione | `context.xml:3` |

## Flusso di una tipica richiesta POST `/api/login`

1. `SpaFallbackFilter` lascia passare la richiesta (path `/api/…`, POST).
2. `CorsFilter` valuta `Origin`: come origine in produzione (nessun dev server) **non**
   viene aggiunto alcun header CORS e la richiesta prosegue normalmente.
3. `AuthServlet.login()` valida credenziali con BCrypt, crea/riusa la sessione e
   rigenera l'ID (`changeSessionId()`) — dettagli in [auth.md](auth.md).
4. Risposta JSON `{"utente":…}` con cookie `JSESSIONID` (`Path=/`, `HttpOnly`,
   `SameSite=Lax`).

## Sviluppo: due processi, un solo origin del browser

In sviluppo serve un secondo processo (`ng serve` su `:4200`) con proxy che
risolve la differenza di origin mantenendo same-origin **dal punto di vista del
browser** (`frontend/proxy.conf.json:1-12`):

- il browser chiama `localhost:4200/api/...`;
- il proxy riporta `/api` su `http://localhost:8080/prgweb/api`
  (`pathRewrite: {"^/api": "/prgweb/api"}`);
- il cookie di sessione ha `Path=/` appositamente per questo scenario (`web.xml:14`),
  così resta
  visibile su entrambi i porti di localhost.

## Build e packaging (a livello architetturale)

- `database/schema.sql` è l'unica fonte dello schema ed è copiato nel classpath del
  WAR come risorsa Maven (`backend/pom.xml` sezione `<resources>` riga `44-53`).
- `scripts/build.sh` compila Angular (`ng build --configuration production
  --base-href "/$APP_CONTEXT/"`) e poi il WAR (`mvn -q clean package`, `<finalName>prgweb</finalName>`).
- `scripts/deploy.sh` copia `backend/target/prgweb.war` in
  `$CATALINA_HOME/webapps/prgweb.war` e riavvia Tomcat se era in esecuzione
  (`deploy.sh:18-43`).

Dettagli operativi: [build-deploy.md](build-deploy.md).

## Informazioni non verificate dal codice

Nessuna: questo documento descrive solo comportamenti verificati nei sorgenti citati
in "Fonti di verifica".
