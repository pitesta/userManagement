# Autenticazione e autorizzazioni

*Verificato sul codice: 05/11/2026.*

## Fonti di verifica

- `backend/src/main/java/it/prgweb/web/AuthServlet.java` (intero file)
- `backend/src/main/java/it/prgweb/web/AdminUtenteServlet.java` (intero file)
- `backend/src/main/java/it/prgweb/web/UtentiServlet.java` (intero file)
- `backend/src/main/java/it/prgweb/util/PasswordUtil.java`
- `backend/src/main/webapp/WEB-INF/web.xml`, `META-INF/context.xml`
- `frontend/src/app/core/auth.service.ts`, `auth.guard.ts`, `layout/shell.ts`

## Modello d'accesso attuale

Autenticazione **server-side** con `HttpSession` + cookie `JSESSIONID`; non esistono
JWT, token in header o `Authenticator` di librerie. L'identità corrente vive come
attributo `"utente"` (costante `AuthServlet.SESSION_USER`, `AuthServlet.java:26`)
nella sessione; non esistono entità separate di "token" o "refresh".

```
[login.ts] --POST api/login--> [AuthServlet.login]
   |                               |  findByNomeUtente + BCrypt.check  (righe 82-84)
   |                               |  session = getSession(true)        (89)
   |                               |  req.changeSessionId()             (90)
   |                               |  session.setAttribute("utente", senzaPassword()) (91)
   |<-- Set-Cookie: JSESSIONID (nuovo ID, Path=/, HttpOnly, SameSite=Lax) --|
[shell.ts logout 32-37] --POST api/logout--> [AuthServlet.logout] --> invalidate() (140)
[auth.guard ] --GET api/me--> stato ripristinato quando il signal è vuoto
```

## Configurazione del cookie di sessione

| Proprietà | Valore | Sorgente |
|---|---|---|
| Nome | `JSESSIONID` (standard Tomcat) | convenzione servlet |
| `HttpOnly` | `true` (`<http-only>true</http-only>`) | `web.xml:12` |
| `Path` | `/` (commento nel file: "Necessario anche in dev: il proxy gira su localhost:4200") | `web.xml:13-14` |
| Tracking solo via cookie | `<tracking-mode>COOKIE</tracking-mode>` — niente URL rewriting | `web.xml:16` |
| `SameSite` | `lax` (`<CookieProcessor sameSiteCookies="lax"/>`) | `context.xml:3` |
| Timeout sessione | 30 minuti (`<session-timeout>30</session-timeout>`) | `web.xml:10` |
| `Secure` | **non impostato**: la sessione viene trasmessa anche su HTTP | assenza nel `cookie-config` `web.xml:9-17` |

## Ciclo di vita della sessione

- **Login** (`AuthServlet.java:71-96`): dopo il controllo delle credenziali
  (`findByNomeUtente` + `BCrypt.check`; fallimento → `401 "Credenziali non valide."`),
  la sessione viene recuperata o creata e **l'ID viene rigenerato** con
  `req.changeSessionId()` *prima* di scrivere l'attributo utente (`:89-91`).
- **Registrazione** (`:98-135`): **non** effettua l'autologin; non tocca la sessione.
  L'utente registrato si autentica poi con `login`.
- **Ripristino**: `GET /api/me` legge l'attributo `utente` (`:37-47`); il frontend lo
  usa quando il proprio signal è vuoto (dopo un refresh della pagina,
  `auth.guard.ts:14-17`).
- **Logout** (`:137-145`): `getSession(false)` + `session.invalidate()`; risposta
  sempre `{"ok": true}`. Il client azzera il proprio signal nel `tap`
  (`auth.service.ts:48-52`) e la shell naviga su `/login` (`shell.ts:32-37`).
- **Auto-eliminazione** (`AdminUtenteServlet.java:197-204`): se l'eliminato è l'admin
  corrente, la sessione viene invalidata e la risposta porta `{"logout": true}`; il
  frontend azzera il signal (`auth.service.ts:102-105`).

## Rigenerazione dell'ID (difesa dalla session fixation)

`changeSessionId()` è invocato a ogni login riuscito, subito dopo la validazione
delle credenziali e prima che l'attributo utente sia scritto: un eventuale valore
di ID visibile nella sessione pre-autenticazione non vale più dopo. Il nuovo ID
arriva al browser nel cookie `JSESSIONID` restituito dalla risposta di login
(`AuthServlet.java:89-91` — pattern `getSession(true)` + `changeSessionId()`).

## Password

- Hash BCrypt con `COST = 10` (`PasswordUtil.java:8,13-15`); verifica con
  `BCrypt.checkpw`, che restituisce `false` (senza exception) per input malformati
  (`:17-26`).
- Fallimento credenziali a livello di controller → `401 "Credenziali non valide."`
  (`AuthServlet.java:83-85`).
- L'hash della password **non viaggia**: `@JsonIgnore` sul getter (`Utente.java:32-35`),
  `senzaPassword()` per sessione e risposte (`Utente.java:58-65`).
- Limiti della password lato server: minimo 6 caratteri, massimo 72 **byte** (limite
  di BCrypt; `AuthServlet.java:31,154-159`).

## Autorizzazioni e ruoli

| Endpoint | Requisito | Sorgente |
|---|---|---|
| `/api/login`, `/api/register`, `/api/logout`, `/api/me` | nessun requisito di sessione | — |
| `/api/utenti` | qualsiasi utente autenticato | `UtentiServlet.java:26-31` |
| `/api/admin/utente`, `/api/admin/utente/modifica`, `/api/admin/utente/elimina` | sessione **e** flag `amministratore` | `AdminUtenteServlet.java:41-53` (401 senza sessione/utente, `403 "Operazione riservata agli amministratori."` senza flag) |

Amministratori:

- Il **primo** account registrato riceve `amministratore = 1` (`AuthServlet.java:120`);
  gli account seguenti nascono con `0`.
- Il vincolo "serve almeno un amministratore" è applicato dal
  `dao.countAmministratori() <= 1` nel declassamento (`cambiaRuolo`) e nell'eliminazione
  di un admin (`AdminUtenteServlet.java:87-92,186-190`) → `409 "Deve rimanere
  almeno un amministratore."`; la stessa protezione esiste nello script manuale
  `set-admin.sh:48-75`.
- Se un admin cambia il **proprio** profilo o ruolo, il nuovo stato è riportato
  immediatamente nella sessione corrente con `session.setAttribute`
  (`AdminUtenteServlet.java:101-103,163-165`).

## Coerenza frontend ↔ backend

- Tutte le chiamate Angular usano `withCredentials: true` (`auth.service.ts:22,33,41,
  50,56,65,83,99`): il cookie arriva anche al dev server `localhost:4200` attraverso
  il proxy (dettagli in [frontend.md](frontend.md) e [architettura.md](architettura.md)).
- `authGuard` considera "autenticato" solo chi risponde `200` da `api/me`
  (`auth.guard.ts:14-17`): azzera il signal e redirige a `/login` in caso di 401.
- Un `401` di un'API interna durante l'uso normale (es. sessione scaduta con la
  SPA aperta) **non** reindirizza: non esiste un interceptor globale, l'errore resta
  visibile nella pagina corrente (`app.config.ts:8`) — comportamento attuale, citato
  anche in [frontend.md](frontend.md).
- Nessun token è leggibile da JavaScript: il cookie di sessione è `HttpOnly`.

## Limitazioni del modello attuale (solo descrizione del comportamento esistente)

Posizioni verificate nel codice, riportate come **stato attuale**; nessuna proposta
di rimedio in questo change:

- **Nessun rate limit / lockout** su `POST /api/login` (`AuthServlet.java:71-96`).
- **Nessun token CSRF**: la protezione attuale è il cookie `SameSite=Lax`
  (`context.xml:3`) e il CORS di `localhost:4200` (`CorsFilter.java:18,26-31`).
- **Cookie senza `Secure`** — vedi tabella di configurazione sopra.
- **Cambio password non revoca le sessioni già attive** dell'utente bersaglio:
  `AdminUtenteServlet.modifica` aggiorna la riga DB (`:143-165`) e non esiste
  alcun registro di sessioni per utente nel progetto.
- **Credenziali in chiaro su HTTP** nell'allestimento attuale (niente `Secure`, niente
  TLS locale): il comportamento attuale di `startup.sh`/`deploy.sh` espone `:8080` su HTTP.

## Informazioni non verificate dal codice

- Comportamento del cookie di sessione al logout (eventuale `Set-Cookie` di
  scadenza emesso da Tomcat dopo `invalidate()`): non desumibile dai sorgenti,
  richiede verifiche manuali con strumenti del browser.

