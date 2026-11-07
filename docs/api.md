# API REST

Il contratto mantiene gli otto endpoint osservati nel golden F0. Le risposte sono JSON con Content-Type `application/json;charset=UTF-8`; gli oggetti utente espongono `idUtente`, `nomeUtente`, `email`, `amministratore`, mai la password o il suo hash.

| Metodo e percorso | Accesso | Esito positivo |
|---|---|---|
| `POST /api/register` | pubblico | `201 {"utente":...}`; primo account amministratore |
| `POST /api/login` | pubblico | `200 {"utente":...}` e cookie `JSESSIONID` |
| `POST /api/logout` | pubblico | `200 {"ok":true}`, invalida l'eventuale sessione |
| `GET /api/me` | sessione | `200 {"utente":...}` |
| `GET /api/utenti` | sessione | `200 {"utenti":[...]}`; non richiede ruolo admin |
| `POST /api/admin/utente` | admin | `200 {"utente":...}`; aggiorna il ruolo |
| `POST /api/admin/utente/modifica` | admin | `200 {"utente":...}`; password opzionale |
| `POST /api/admin/utente/elimina` | admin | `200 {"ok":true,"logout":false}`; self-delete imposta `logout:true` |

Le request usano i campi camelCase mostrati dal contratto Angular: registrazione/login `nomeUtente`, `password`, registrazione anche `email`; cambio ruolo `idUtente`, `amministratore`; modifica `idUtente`, `nomeUtente`, `email`, `password?`; elimina `idUtente`.

## Errori

Le API rispondono con `{"error":"messaggio in italiano"}`. Status e messaggi golden:

| Status | Messaggi |
|---|---|
| 400 | `Richiesta non valida.`; `Inserire nome utente e password.`; `Il nome utente è obbligatorio.`; `Il nome utente non può superare 100 caratteri.`; `La password deve contenere almeno 6 caratteri.`; `La password è troppo lunga (massimo 72 byte).`; `L'email è obbligatoria.`; `L'email non può superare 40 caratteri.`; `L'email non è valida.`; `Utente non valido.` |
| 401 | `Non autenticato.`; `Credenziali non valide.` |
| 403 | `Operazione riservata agli amministratori.` |
| 404 | `Utente non trovato.` |
| 409 | `Nome utente già registrato.`; `Deve rimanere almeno un amministratore.` |
| 500 | `Errore interno del database.` |

Gli altri due messaggi in `ERRORI_IT` — `Si è verificato un errore. Riprova.` e `Server non raggiungibile. Verifica che Tomcat sia avviato.` — sono fallback frontend, non errori emessi dall'API; il testo storico viene mantenuto per non modificare Angular.

Dettagli di sessione in [auth.md](auth.md). I golden e i test contrattuali sono in `openspec/changes/migrate-backend-to-spring-boot-jpa-h2/golden/f0-api.md` e `backend/src/test/java/it/prgweb/web/`.

