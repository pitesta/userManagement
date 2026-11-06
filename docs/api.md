# API REST

*Verificato sul codice: 05/11/2026. Codici di stato esercitati con `curl` reale in
sessione: `200`, `201`, `400`, `401`, `403`, `405`, `204` (e SPA index 200);
`404`/`409`/`500` documentati solo dal codice:
vedi "Copertura e limiti della verifica" in coda.*

## Fonti di verifica

- `backend/src/main/java/it/prgweb/web/AuthServlet.java`, `UtentiServlet.java`,
  `AdminUtenteServlet.java` (interi file)
- `backend/src/main/java/it/prgweb/util/Json.java`
- `frontend/src/app/core/auth.service.ts`, `core/models.ts` (corpi di richiesta/risposta)

## Convenzioni generali

- Base path: tutte le risorse sono sotto `/api/…` (contesto `/prgweb`, quindi URL
  completa `http://localhost:8080/prgweb/api/...`).
- Corpo delle risposte: JSON serializzato da `util.Json`
  (`application/json;charset=UTF-8`, `util/Json.java:21`).
- Errori: sempre `{"error": "<messaggio in italiano>"}` (`util/Json.java:25-29`).
- Autenticazione: sessione server-side via cookie `JSESSIONID`; **nessun token**.
  Dettagli in [auth.md](auth.md).
- Metodi: solo `GET` e `POST` — tutte le mutazioni sono `POST`, `PUT`/`DELETE` non
  esistono nel progetto. Il `CorsFilter` risponde `204` alle preflight `OPTIONS`
  (`CorsFilter.java:34-37`).

## Tabella completa degli endpoint (8)

| # | Endpoint | Metodo | Accesso | Funzione |
|---|---|---|---|---|
| 1 | `/api/login` | POST | pubblica | Login, crea/riusa sessione |
| 2 | `/api/register` | POST | pubblica | Registrazione |
| 3 | `/api/logout` | POST | pubblica | Invalidazione della sessione |
| 4 | `/api/me` | GET | pubblica | Utente nella sessione corrente |
| 5 | `/api/utenti` | GET | sessione | Elenco di tutti gli utenti |
| 6 | `/api/admin/utente` | POST | admin | Cambio del flag `amministratore` |
| 7 | `/api/admin/utente/modifica` | POST | admin | Modifica anagrafica + password opzionale |
| 8 | `/api/admin/utente/elimina` | POST | admin | Eliminazione di un utente |

Mapping sorgenti: `AuthServlet.java:23`, `UtentiServlet.java:19`,
`AdminUtenteServlet.java:25-29`. Corrispondenza lato client in `auth.service.ts`:
`login` 20, `register` 29, `me` 39, `logout` 48, `utenti` 54, ruolo 60, modifica 77,
elimina 94.

## Dettaglio per endpoint

### 1. `POST /api/login`

- Richiesta: `{"nomeUtente": "…", "password": "…"}` (`AuthServlet.java:71-80`)
  — la validazione *lato client* del form vale `Validators.required`.
- Comportamento: lookup + BCrypt; fallimento → `401 "Credenziali non valide."`
  (`:83-85`); successo → `200 {"utente": {...}}` con cookie di sessione,
  dopo `changeSessionId()` (`:88-91`).
- Codici di stato osservati: `400` (campi mancanti — o JSON malformato:
  `AuthServlet.java:62-63`), `401`, `500` (errore SQL).

### 2. `POST /api/register`

- Richiesta: `{"nomeUtente": "…", "password": "…", "email": "…"}` (`RegisterRequest`,
  `AuthServlet.java:188`).
- Validazioni lato server (`:147-170`): nome obbligatorio, non più di `100` caratteri;
  password >= 6 caratteri e <= 72 **byte**; email obbligatoria, max `40` caratteri,
  pattern `^[^@\s]+@[^@\s]+\.[^@\s]+$` (riga `28`).
- Primo utente: `amministratore = dao.count() == 0` (`:120`).
- Errori: `409 "Nome utente già registrato."` (esplicito `:110-113` o da UNIQUE
  constraint `:123-129`); `201` a inserimento riuscito con utente senza password
  (`:132-134`).

### 3. `POST /api/logout`

- Richiesta: nessun corpo significativo (il client manda `{}`: `auth.service.ts:50`).
- `session.invalidate()` sempre → `200 {"ok": true}` (`AuthServlet.java:137-145`).

### 4. `GET /api/me`

- Nessun corpo. Sessione valida → `200 {"utente": {...}}`; senza →
  `401 "Non autenticato."` (`AuthServlet.java:37-47`).

### 5. `GET /api/utenti`

- Sessione valida necessaria (`UtentiServlet.java:26-31`, in assenza → `401`).
- Risposta: `200 {"utenti": [<Utente>...]}` — lista completa, nessuna paginazione,
  ordinata per `nomeUtente COLLATE NOCASE` (`UtenteDao.java:44`); ogni elemento non
  ha `password` (hash dello DAO è rimosso via `senzaPassword()`: `UtentiServlet.java:34`).

### 6. `POST /api/admin/utente`

- Richiesta: `{"idUtente": <id>, "amministratore": true|false}` (record
  `Richiesta`, `AdminUtenteServlet.java:71-83` e `:245`).
- Successo: `200 {"utente": {...aggiornato}}` (`:100-105`).
- Errori: `404 "Utente non trovato."` (`:81-83`, `:95-97`); `409 "Deve rimanere
  almeno un amministratore."` quando si declassa l'ultimo admin (`:87-92`).

### 7. `POST /api/admin/utente/modifica`

- Richiesta: `{"idUtente": <id>, "nomeUtente": "…", "email": "…",
  "password": "…"}` con password **opzionale** (record `ModificaRequest`,
  `AdminUtenteServlet.java:248`): se assente oppure solo spazi (`isBlank`, `:120`) la
  password non viene toccata.
- Successo: `200 {"utente": {...}}` (`:162-167`).
- Errori: `400` per validazioni (`:122-126` — stesso set di register), `404`
  (`:128-132`, `:148-151`), `409 "Nome utente già registrato."` per collisione o
  UNIQUE (`:134-138`, `:152-160`).

### 8. `POST /api/admin/utente/elimina`

- Richiesta: `{"idUtente": <id>}` (record `EliminaRequest`, `:251`).
- Successo: `200 {"ok": true, "logout": <bool>}` — `logout` è `true` **solo**
  se l'utente eliminato è l'admin corrente, in quel caso la sessione è già
  invalidata lato server (`:197-204`).
- Errori: `404` (`:179-182`, `:192-195`), `409 "Deve rimanere almeno un
  amministratore."` (`:186-190`).

## Amministratori e autorizzazioni

Per gli endpoint 6/7/8 `AdminUtenteServlet.doPost` verifica
sessione utente **e** flag `amministratore` **prima** del switch:
`401 "Non autenticato."` senza sessione, `403 "Operazione riservata agli
amministratori."` sessione senza ruolo (`AdminUtenteServlet.java:45-53`).
`/api/utenti` richiede solo una sessione qualsiasi (`UtentiServlet.java:28-31`).

## Copertura e limiti della verifica

- **Tutti** i dettagli sopra provengono da lettura diretta dei controller.
- **Verifica con `curl` reale eseguita in sessione** (05/11/2026, Tomcat locale):

```console
$ curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/prgweb/api/me        # 401 (senza sessione)
$ curl -s -w '%{http_code}' -d '{"nomeUtente":"x","password":"y"}' api/login       # 401 "Credenziali non valide."
$ curl -s -o /dev/null -w '%{http_code}' -d 'not json' api/login                   # 400
$ curl -s -o /dev/null -w '%{http_code}' -X OPTIONS ... -H 'Origin: http://localhost:4200'  # 204
$ curl -s -w '%{http_code}' -d '{...}' api/register                                # 201 (utente creato)
$ curl -s -o /dev/null -w '%{http_code}' -d '{...}' -H 'Content-Type: ...' api/login  # 200 (+ cookie)
$ curl -s -o /dev/null -w '%{http_code}' -b cookie api/me                          # 200
$ curl -s -o /dev/null -w '%{http_code}' -b cookie api/utenti                      # 200
$ curl -s -w '%{http_code}' -b cookie -d '{...}' api/admin/utente                  # 403 (sessione non admin)
$ curl -s -w '%{http_code}' -b cookie -d '{}' api/logout                           # 200 {"ok":true}
$ curl -s -o /dev/null -w '%{http_code}' -b cookie api/me                          # 401 (dopo logout)
$ curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/prgweb/api/logout   # 405 (GET su endpoint POST)
$ curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/prgweb/             # 200 (index.html, SPA)
```

- **Non esercitati con `curl` in questa sessione** (descrizione dal codice): i codici
  `404 "Utente non trovato."`, `409` del vincolo ultimo admin e il ramo
  `logout: true` dell'endpoint 6/7/8; per esercitarli sarebbe servito un utente
  amministratore già esistente noto, non disponibile in quest'ambiente. I codici
  `400/401/403/204/200/201/405` sono verificati con richiesta reale (vedi sopra).

## Informazioni non verificate dal codice

- Vedere la sezione "Copertura e limiti della verifica": i codici `404`, `409` e `500`
  non sono esercitati con richiesta reale in questa sessione (comportamento
  derivato dal codice sorgente).

## Nota di coerenza con il frontend

Il client accetta le risposte nei tipi di `core/models.ts`: `UtenteResponse`
(`{"utente": ...}`), `UtentiResponse` (`{"utenti": [...]}`), `EliminaUtenteResponse`
(`{"ok", "logout"}`), `ApiError` (`{"error"}`) — coincide con ciò che le servlet
scrivono via `util.Json`.

