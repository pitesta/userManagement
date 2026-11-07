# Golden F0 — API (valori misurati)

Rilevamento eseguito in F0 con `curl` su Tomcat 11.0.27 + WAR attuale, database
SQLite ripartito da zero. Data rilievo: 2026-11-06. Content-Type risposte:
`application/json;charset=UTF-8`.

Verifica incrociata: le stringhe d'errore osservate coincidono esattamente con le 19
chiavi di `ERRORI_IT` in `frontend/src/app/core/i18n.service.ts` (le stringhe riportate
alla fine). Tre di esse (`Si è verificato un errore. Riprova.`,
`Server non raggiungibile. Verifica che Tomcat sia avviato.`,
`Errore interno del database.`) non sono emesse da un endpoint nei casi normali:
le due prime sono testuali lato frontend, la terza è il fallback per guasti DB/lock.

## 1. POST /api/register

| Caso | Status | Corpo |
| --- | --- | --- |
| Corpo assente / non JSON | 400 | `{"error":"Richiesta non valida."}` |
| nome vuoto/nullo | 400 | `{"error":"Il nome utente è obbligatorio."}` |
| nome > 100 caratteri | 400 | `{"error":"Il nome utente non può superare 100 caratteri."}` |
| password < 6 | 400 | `{"error":"La password deve contenere almeno 6 caratteri."}` |
| password > 72 byte | 400 | `{"error":"La password è troppo lunga (massimo 72 byte)."}` |
| email vuota | 400 | `{"error":"L'email è obbligatoria."}` |
| email > 40 caratteri | 400 | `{"error":"L'email non può superare 40 caratteri."}` |
| email malformata | 400 | `{"error":"L'email non è valida."}` |
| nome già presente (pre-check e unique) | 409 | `{"error":"Nome utente già registrato."}` |
| primo utente su DB vuoto | 201 | `{"utente":{"idUtente":1,"nomeUtente":"admin","email":"admin@example.com","amministratore":true}}` |
| utente successivo | 201 | `{"utente":{"idUtente":2,"nomeUtente":"mario","email":"mario@example.com","amministratore":false}}` |

Ordine di validazione osservato (risponde la prima regola violata in questo ordine):
nome vuoto → nome lunghezza → password corta → password lunghezza → email vuota →
email lunghezza → email formato. `nomeUtente` e `email` vengono trimmati.

## 2. POST /api/login

| Caso | Status | Corpo |
| --- | --- | --- |
| body `{}` / campi vuoti | 400 | `{"error":"Inserire nome utente e password."}` |
| credenziali errate | 401 | `{"error":"Credenziali non valide."}` |
| credenziali corrette | 200 | `{"utente":{...}}` + `Set-Cookie` (vedi golden f0-sessione) |

`nomeUtente` trimmato, password non troncata. Corpo utente senza password.

## 3. POST /api/logout

| Caso | Status | Corpo |
| --- | --- | --- |
| qualsiasi (anche senza sessione) | 200 | `{"ok":true}` |

La sessione viene invalidata: `/api/me` col vecchio cookie → 401.

## 4. GET /api/me

| Caso | Status | Corpo |
| --- | --- | --- |
| senza sessione / dopo logout / dopo self-delete | 401 | `{"error":"Non autenticato."}` |
| con sessione valida | 200 | `{"utente":{"idUtente":1,"nomeUtente":"admin","email":"admin@example.com","amministratore":true}}` |

Il corpo riflette il valore di sessione (che può essere stato aggiornato dalle
operazioni amministrative su se stessi).

## 5. GET /api/utenti

| Caso | Status | Corpo |
| --- | --- | --- |
| senza sessione | 401 | `{"error":"Non autenticato."}` |
| con sessione admin | 200 | `{"utenti":[...]}` |
| con sessione non admin | 200 | `{"utenti":[...]}` (nessun check admin) |

Ordinamento osservato (SQLite `COLLATE NOCASE`): `Alpha, beta, mario, wilma, Zeta`.
Elemento = `{"idUtente","nomeUtente","email","amministratore"}` senza password.

## 6. POST /api/admin/utente (`{idUtente, amministratore}`)

| Caso | Status | Corpo |
| --- | --- | --- |
| senza sessione | 401 | `{"error":"Non autenticato."}` |
| sessione non admin | 403 | `{"error":"Operazione riservata agli amministratori."}` |
| body non leggibile | 400 | `{"error":"Richiesta non valida."}` |
| `idUtente <= 0` / assente | 400 | `{"error":"Utente non valido."}` |
| inesistente | 404 | `{"error":"Utente non trovato."}` |
| degrada l'ultimo admin | 409 | `{"error":"Deve rimanere almeno un amministratore."}` |
| self-degradation (sessione) | 200 | l'attributo di sessione si aggiorna: richieste successive della stessa sessione → 401/403. Verifica reale: self-degrade → 200 `{"utente":{...}}` poi `403 Operazione riservata agli amministratori.` |
| successo | 200 | `{"utente":{"idUtente":2,"nomeUtente":"mario","email":"mario@example.com","amministratore":true}}` |

## 7. POST /api/admin/utente/modifica (`{idUtente, nomeUtente, email, password?}`)

| Caso | Status | Corpo |
| --- | --- | --- |
| `idUtente <= 0` | 400 | `{"error":"Utente non valido."}` |
| inesistente | 404 | `{"error":"Utente non trovato."}` |
| anagrafica non valida | 400 | le stringhe di validazione (vedi §1) |
| nome omonimo di altro utente | 409 | `{"error":"Nome utente già registrato."}` |
| successo senza password | 200 | `{"utente":{...}}` — la password precedente resta valida (login verificato) |
| successo con password | 200 | nuova password al login; hash salvato |
| target = utente corrente | 200 | la sessione si aggiorna con i nuovi dati |

Il check "omonimo" precede (nella sequenza osservata): validazione anagrafica →
esistenza target → omonimo unique (applicato via `update` + constraint anche in
caso di race).

## 8. POST /api/admin/utente/elimina (`{idUtente}`)

| Caso | Status | Corpo |
| --- | --- | --- |
| `idUtente <= 0` | 400 | `{"error":"Utente non valido."}` |
| inesistente | 404 | `{"error":"Utente non trovato."}` |
| elimina l'ultimo admin | 409 | `{"error":"Deve rimanere almeno un amministratore."}` |
| self-delete (è l'utente corrente) | 200 | `{"ok":true,"logout":true}` + sessione invalidata (me → 401) |
| successo | 200 | `{"ok":true,"logout":false}` |

L'invariante "almeno un amministratore" vale anche per l'auto-eliminazione
dell'ultimo admin (409).

## Stringhe d'errore (18+1 da ERRORI_IT)

Tutte le stringhe emesse dal backend osservate e coincidenti con `ERRORI_IT`:

1. `Non autenticato.` — 401 (me/utenti/di admin senza sessione)
2. `Operazione riservata agli amministratori.` — 403
3. `Utente non valido.` — 400
4. `Utente non trovato.` — 404
5. `Deve rimanere almeno un amministratore.` — 409
6. `Richiesta non valida.` — 400 (body assente/malformato/illeggibile)
7. `Errore interno del database.` — 500 (mappato su SQLException; non osservabile dal vivo senza guasto; vale come mappa ed è nel contratto)
8. `Inserire nome utente e password.` — 400
9. `Credenziali non valide.` — 401
10. `Il nome utente è obbligatorio.` — 400
11. `Il nome utente non può superare 100 caratteri.` — 400
12. `La password deve contenere almeno 6 caratteri.` — 400
13. `La password è troppo lunga (massimo 72 byte).` — 400
14. `L'email è obbligatoria.` — 400
15. `L'email non può superare 40 caratteri.` — 400
16. `L'email non è valida.` — 400
17. `Nome utente già registrato.` — 409
18. `Si è verificato un errore. Riprova.` — solo frontend (nessuna emissiona backend)
19. `Server non raggiungibile. Verifica che Tomcat sia avviato.` — solo frontend (nessuna emissiona backend)

Nel nuovo backend le stesse 19 stringhe devono essere gli unici possibili corpi
d'errore degli endpoint applicativi; la 7 resta il mapping per guasti tecnici
(schema/mutex/deadlock), testata con test dedicati.

## Comportamenti supplementari osservati (contratto implicito)

- Metodo GET su `/api/login|register|logout` → 405 default Tomcat.
- Percorso API inesistente (es. `/api/qualunque`) → 404 default Tomcat.
- Il rendering della SPA resta fuori dall'API: `GET /prgweb/` e deep link
  (`/prgweb/utenti`) → 200 `index.html`; asset mancante con estensione → 404.
