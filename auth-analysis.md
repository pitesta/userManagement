# Analisi del sistema di autenticazione — auth-analysis.md

*Data: 05/11/2026 · Revisione di sola lettura: nessun file di codice modificato, nessuna correzione effettuata.*

---

## 1. Flusso complessivo

```
[login.ts] --POST api/login--> [AuthServlet.login:71-96]
      |                             |  findByNomeUtente + BCrypt check
      |                             |  session = req.getSession(true)     (riga 89)
      |                             |  req.changeSessionId()              (riga 90)  <- rigenerazione ID
      |                             |  session.setAttribute("utente", utente.senzaPassword())
      |<-- Set-Cookie JSESSIONID (Path=/, HttpOnly, sameSite="lax") --|
[shell.ts:32-37] --POST api/logout--> [AuthServlet.logout:137-145] -> session.invalidate()
[auth.guard.ts] --GET api/me--> fallback all'avvio per ripristinare lo stato client
```

Lo stato lato client è un signal in `AuthService` (`frontend/src/app/core/auth.service.ts`);
il server resta la fonte di verità e il guard interroga `/api/me` quando il signal è vuoto.

## 2. Punti di forza verificati

| # | Aspetto | File e righe |
|---|---|---|
| 1 | **Rigenerazione ID di sessione al login** (`changeSessionId()` dopo la validazione delle credenziali e prima di scrivere l'attributo utente): pattern raccomandato contro la *session fixation*. | `backend/src/main/java/it/prgweb/web/AuthServlet.java:89-91` |
| 2 | **Invalidazione completa al logout**: `getSession(false)` + `invalidate()`; nessuna generazione di nuovo ID necessaria. | `AuthServlet.java:138-141` |
| 3 | **Password mai in sessione né nelle risposte**: l'utente memorizzato/risposto passa sempre da `senzaPassword()`. | `AuthServlet.java:88,133` · `AdminUtenteServlet.java:100,162` |
| 4 | **Cookie HttpOnly** (`<http-only>true</http-only>`) e session tracking **solo via cookie** (`<tracking-mode>COOKIE</tracking-mode>` → niente ID nelle URL). | `backend/src/main/webapp/WEB-INF/web.xml:12,16` |
| 5 | **SameSite=Lax** sul cookie di sessione (mitiga i POST cross-site). | `backend/src/main/webapp/META-INF/context.xml:3` |
| 6 | **Controllo ruoli sistematico** nelle operazioni admin: 401 se nessuna sessione, 403 se non amministratore. | `backend/src/main/java/it/prgweb/web/AdminUtenteServlet.java:41-53` · `UtentiServlet.java:26-31` |
| 7 | **Aggiornamento del flag in sessione** quando un admin declassa se stesso (effetto immediato, nessuna ricognizione necessaria); autocancellazione → `session.invalidate()` + `logout: true`. | `AdminUtenteServlet.java:101-103,197-204` |
| 8 | **Coerenza frontend/backend**: tutte le chiamate usano `withCredentials: true` (richiesto dal dev proxy `:4200`); signup senza autologin e UI che si riporta al login; auto-eliminazione azzera il signal. | `auth.service.ts:20-52,102-105` · `login.ts:81-91` · `auth.guard.ts:14-17` |

## 3. Problemi reali (verificati nel codice)

| # | Problema | File e righe interessate |
|---|---|---|
| 1 | **Cookie di sessione senza flag `Secure`**. Non è impostato nel `<cookie-config>` né accanto al `CookieProcessor`. Corretto nello sviluppo locale via HTTP, ma dietro HTTPS (es. Tomcat in produzione) il cookie sarebbe trasmissibile anche in chiaro sottodomini/DNS. | `web.xml:9-17` · `context.xml:3` |
| 2 | **Nessuna protezione da brute force** su `/api/login`: né rate-limiting, né lockout, né CAPTCHA o ritardi. Con "register" aperto è lo scenario d'uso tipico di una demo, ma è il difetto più notevole in caso di esercizio reale. | `AuthServlet.java:71-96` |
| 3 | **Enumerazione nome utente via timing**: `trovato.isEmpty() || !PasswordUtil.check(...)`. Con un nome utente inesistente il controllo BCrypt viene saltato; la risposta è misurabilmente più veloce → distinzione tra "nome utente non esiste" e "password sbagliata" sulla sola latenza. | `AuthServlet.java:82-86` |
| 4 | **Nessun token CSRF**: le mutazioni sono tutti POST autenticabili via cookie; la mitigazione attuale è solo `SameSite=Lax` (`context.xml:3`) + CORS ristretto con `Allow-Credentials: true` per `Origin` esatto `http://localhost:4200` (`CorsFilter.java:18,26-31`). In produzione con altri origin la via è più porosa. | `web.xml` (nessun token CSRF configurato) · `CorsFilter.java` |
| 5 | **Cambio password di un utente non revoca le sue sessioni già attive**: un admin che modifica la password di un altro utente non invalida le sessioni dell'utente target — nessun registro di sessioni per utente nel progetto. Il difetto è strutturale, non un bug locale. | `AdminUtenteServlet.java:143-165` |
| 6 | **Nessun interceptor 401 globale nel frontend** (`provideHttpClient()` nudo): se la sessione scade (timeout 30') con la SPA aperta, un `401` da qualsiasi API interna mostra il messaggio di errore nella pagina, senza reindirizzare al login; il redirect avviene solo al cambio di rotta via guard. | `frontend/src/app/app.config.ts:9` · `auth.guard.ts` (attivo solo in navigazione) |
| 7 | **Sessione non revocabile dopo una compromissione del cookie**: non esistono meccanismi di rivocazione (2FA, IP-change check, ecc.): sessione" (fino a timeout o logout manuale). Limite inerente all'architettura `HttpSession`;
segnalato come limite di design. | limite strutturale; mitigato dal timeout `web.xml:10` |

## 4. Dubbi da approfondire (non accertati)

| # | Dubbio | Fondamento |
|---|---|---|
| 1 | **Comportamento del cookie al logout**: Tomcat con `invalidate()` non emette `Set-Cookie` con `Max-Age=0`; il cookie resta nel browser finché la sessione del browser non è chiusa. Con `getSession(false)` ovunque il cookie stale è innocuo, ma è necessario un **test manuale** (DevTools: `POST /api/logout` → disconnettersi → verificare lo stato del cookie) per escludere riattacchi tipo re-iniezione. | `AuthServlet.java:137-145`; nessun header di clearing nel codice. |
| 2 | **Timeout di sessione e UX**: `<session-timeout>30</session-timeout>` è in **minuti**? In `web.xml` è espresso in minuti standard; ma riesaminare il valore percepito dall'utente con la SPA aperta e con l'interceptor assente (problema reale n. 6). | `web.xml:10` |
| 3 | **Session fixation al primo accesso anonimo**: `changeSessionId()` viene invocato al login; se un utente anonimo ha già ricevuto un ID di sessione prima dell'autenticazione, quell'ID viene ruotato al login. Da verificare con traffic observing i cookie assegnati *prima* del login per escludere correlazioni residue con `tracking-mode=COOKIE`. | design generale; nessun riscontro diretto. |

## 5. Conclusione

L'implementazione è di livello buono per essere una demo: il punto di massima sicurezza è
**la rigenerazione del session id al login** (raro da vedere implementato), da cui dipende
della difesa contro la session fixation. Le debolezze verificate sono invece
mancanze puntuali: il flag `Secure` (punto 3.1), il rate limiting del login (3.2), il
token CSRF (3.4) e un interceptor globale per il 401 lato frontend (3.6). Nessuno richiede
un redesign: sono tutti migliorabili senza stravolgere l'architettura né la
struttura dati o il flusso di autenticazione.

---

*Nessuna modifica al codice sorgente; questo documento è l'unico artefatto creato dall'analisi.*
