# user-management Specification

## Purpose

Gestione globale degli utenti dell'applicazione: registrazione, autenticazione con sessione HTTP, elenco utenti e operazioni amministrative (ruolo, anagrafica, eliminazione), con le invarianti amministrative protette da serializzazione mediante mutex applicativo.

## Requirements

### Requirement: Registrazione account
Il sistema SHALL esporre `POST /api/register`: a input valido risponde `201` con `{"utente":{"idUtente","nomeUtente","email","amministratore"}}` senza password. Il primo account creato su database vuoto DEVE diventare amministratore (`amministratore: true`), gli account successivi nascono non amministratori. Nome utente duplicato → `409 {"error":"Nome utente già registrato."}`.

#### Scenario: Prima registrazione su database vuoto
- **WHEN** viene registrato il primo utente dopo la creazione del database
- **THEN** la risposta è `201` con `amministratore: true` e senza password nel corpo

#### Scenario: Registrazione successiva
- **WHEN** un utente si registra quando esiste già almeno un account
- **THEN** la risposta è `201` con `amministratore: false`

#### Scenario: Nome utente già presente
- **WHEN** si registra un account con nome utente esistente
- **THEN** la risposta è `409` con `{"error":"Nome utente già registrato."}`

### Requirement: Validazione dei dati di registrazione
Il sistema SHALL rifiutare la registrazione con `400` e messaggio italiano quando il `nomeUtente` supera 100 caratteri o è vuoto, l'`email` supera 40 caratteri, non rispetta il formato email o è vuota, oppure la `password` è più corta di 6 caratteri o supera 72 byte.

#### Scenario: Input non valido
- **WHEN** la registrazione contiene un campo vuoto, troppo lungo, un'email senza formato o una password sotto la soglia
- **THEN** la risposta è `400` con `{"error":"<messaggio in italiano>"}` corrispondente alla regola violata

### Requirement: Autenticazione e utente corrente
Il sistema SHALL esporre `POST /api/login`: credenziali valide → `200 {"utente":{...}}` (senza password) con una nuova sessione server-side, con rigenerazione dell'identificatore di sessione al login; credenziali errate → `401 {"error":"Credenziali non valide."}`; campi mancanti → `400 {"error":"Inserire nome utente e password."}`. `GET /api/me` risponde `200 {"utente":{...}}` con i dati della sessione corrente e `401 {"error":"Non autenticato."}` in assenza di sessione valida.

#### Scenario: Login valido
- **WHEN** il login ha successo con credenziali corrette
- **THEN** la risposta è `200` con l'utente senza password e il session ID della sessione cambia rispetto alla richiesta precedente

#### Scenario: Credenziali errate
- **WHEN** si fornisce una combinazione nome utente/password errata
- **THEN** la risposta è `401` con `{"error":"Credenziali non valide."}`

#### Scenario: Utente corrente senza sessione
- **WHEN** `GET /api/me` viene chiamata senza un cookie di sessione valido
- **THEN** la risposta è `401` con `{"error":"Non autenticato."}`

### Requirement: Sessione HTTP via cookie
L'applicazione SHALL identificare la sessione tramite il cookie `JSESSIONID` con gli attributi riscontrati nel contract test F0 dell'applicazione attuale: `Path` sulla radice del contesto, `HttpOnly`, `SameSite=Lax`, scadenza a 30 minuti e tracking a solo cookie. I valori effettivi rilevati in F0 DEVONO sempre prevalere sui valori indicati come default nel design: in qualunque divergenza il valore misurato DEVE fare fede.

#### Scenario: Cookie di sessione emesso al primo accesso autenticato
- **WHEN** si effettua un login riuscito che emette il cookie di sessione
- **THEN** il cookie presenta gli attributi (Path, HttpOnly, SameSite, durata) misurati nel golden F0

#### Scenario: Sessione valida dal dev server
- **WHEN** il frontend gira sul dev server Angular e inoltra le API tramite proxy
- **THEN** il cookie è presente nelle chiamate successive e la sessione resta valida

### Requirement: Logout
Il sistema SHALL esporre `POST /api/logout`: risponde `200 {"ok":true}` e invalida la sessione; ogni richiesta successiva con quel cookie è trattata come non autenticata (`GET /api/me` → `401`).

#### Scenario: Logout rende non valida la sessione
- **WHEN** si esegue il logout e subito dopo si chiama `GET /api/me` con lo stesso cookie
- **THEN** la risposta è `401` con `{"error":"Non autenticato."}`

### Requirement: Elenco utenti protetto da sessione
`GET /api/utenti` SHALL rispondere `200 {"utenti":[{idUtente,nomeUtente,email,amministratore}...]}` ordinando per nome utente senza distinzione tra maiuscole e minuscole, solo per una sessione valida; senza sessione → `401 {"error":"Non autenticato."}`. Nessuna password appare mai in alcun elemento.

#### Scenario: Elenco per utente autenticato
- **WHEN** un utente autenticato richiede l'elenco
- **THEN** la risposta è `200` con la lista completa senza password e con i veri valori del flag di amministrazione

#### Scenario: Elenco senza sessione
- **WHEN** `GET /api/utenti` viene chiamato senza cookie di sessione valido
- **THEN** la risposta è `401` con `{"error":"Non autenticato."}`

### Requirement: Operazioni amministrative protette
Le operazioni amministrative (`POST /api/admin/utente` cambio ruolo, `POST /api/admin/utente/modifica`, `POST /api/admin/utente/elimina`) SHALL rispondere `403 {"error":"Operazione riservata agli amministratori."}` per una sessione autenticata non amministratore e `401 {"error":"Non autenticato."}` senza sessione. Con `idUtente` non positivo → `400 {"error":"Utente non valido."}`; per un utente inesistente → `404 {"error":"Utente non trovato."}`.

#### Scenario: Cambio ruolo da un utente non amministratore
- **WHEN** un utente senza ruolo di amministratore chiede il cambio di ruolo di un altro utente
- **THEN** la risposta è `403` con `{"error":"Operazione riservata agli amministratori."}`

#### Scenario: Target inesistente
- **WHEN** un amministratore chiede un'operazione su un `idUtente` non esistente
- **THEN** la risposta è `404` con `{"error":"Utente non trovato."}`

### Requirement: Cambio ruolo amministratore
`POST /api/admin/utente` con `{idUtente, amministratore}` SHALL aggiornare il flag di amministrazione del target e rispondere `200 {"utente":{...}}`. Se il target è l'utente corrente, l'attributo di sessione `amministratore` DEVE essere aggiornato. La degradazione dell'ultimo amministratore rimanente SHALL fallire con `409 {"error":"Deve rimanere almeno un amministratore."}` ed essere bloccata dalla serializzazione sul mutex.

#### Scenario: Promozione a amministratore
- **WHEN** un amministratore imposta `amministratore: true` per un utente non amministratore
- **THEN** la risposta è `200` e l'utente risulta amministratore nell'elenco

#### Scenario: Degradazione dell'ultimo admin
- **WHEN** si degrada l'unico amministratore rimanente
- **THEN** la risposta è `409` con `{"error":"Deve rimanere almeno un amministratore."}` e il ruolo resta invariato

#### Scenario: Auto-aggiornamento del ruolo in sessione
- **WHEN** un amministratore modifica il proprio ruolo
- **THEN** la sessione corrente della stessa richiesta riflette il nuovo valore

### Requirement: Modifica anagrafica da amministratore
`POST /api/admin/utente/modifica` con `{idUtente, nomeUtente, email, password?}` SHALL aggiornare nome e email e, se `password` è presente e non vuota, sostituire l'hash della password; la password fornita DEVE essere salvata solo in forma di hash. Nome utente appartenente a un altro utente → `409 {"error":"Nome utente già registrato."}`. Se il target è l'utente corrente, la sessione DEVE riflettere i dati aggiornati.

#### Scenario: Aggiornamento senza cambio password
- **WHEN** l'amministratore aggiorna nome e email senza fornire password
- **THEN** la risposta è `200` con l'utente aggiornato e le credenziali precedenti ancora valide

#### Scenario: Rinomina a nome già esistente
- **WHEN** l'amministratore modifica un utente con un nome già usato da un altro utente
- **THEN** la risposta è `409` con `{"error":"Nome utente già registrato."}`

### Requirement: Eliminazione utente da amministratore
`POST /api/admin/utente/elimina` con `{idUtente}` SHALL rispondere `200 {"ok":true}` e rimuovere l'account, tranne per l'ultimo amministratore rimanente (`409 {"error":"Deve rimanere almeno un amministratore."}`). Se l'eliminato è l'utente corrente, la risposta include `"logout": true` e la sessione viene invalidata.

#### Scenario: Auto-eliminazione
- **WHEN** un amministratore elimina se stesso
- **THEN** la risposta è `200` con `{"ok":true,"logout":true}` e ogni richiesta successiva è non autenticata

#### Scenario: Eliminazione dell'ultimo admin
- **WHEN** l'unico amministratore rimanente viene eliminato
- **THEN** la risposta è `409` con `{"error":"Deve rimanere almeno un amministratore."}` e l'account resta attivo

### Requirement: Messaggi di errore in corpo standard
Ogni errore dell'API SHALL rispondere con corpo `{"error":"<messaggio in italiano>"}`; l'interpretazione lato frontend tramite la mappa `ERRORI_IT` DEVE continuare a funzionare con le stringhe invariate. Il formato di errore predefinito del framework non DEVE mai comparire nelle risposte degli endpoint applicativi.

#### Scenario: Errore di richiesta malformata
- **WHEN** arriva un corpo JSON non valido o non leggibile su un endpoint delle API
- **THEN** la risposta è `400` con `{"error":"Richiesta non valida."}`

#### Scenario: Errore interno del database
- **WHEN** un'operazione al database fallisce per un motivo tecnico (lock, I/O)
- **THEN** la risposta è `500` con `{"error":"Errore interno del database."}`

### Requirement: Serializzazione tramite mutex applicativo
Il sistema SHALL serializzare su una riga-mutex in tabella tecnica la registrazione, la degradazione dell'ultimo amministratore e l'eliminazione dell'ultimo amministratore. Il lock DEVE essere acquisito con `SELECT ... FOR UPDATE` dentro la stessa transazione che verifica le invarianti e scrive. L'esito di due registrazioni contemporanee con nomi utente differenti su database vuoto DEVE includere esattamente un amministratore e il test DEVE verificarlo concretamente su più run.

#### Scenario: Due registrazioni simultanee con nomi utente differenti su database vuoto
- **WHEN** due registrazioni contemporanee con nomi utente differenti partono su un database senza utenti
- **THEN** entrambe rispondono `201` con l'account creato; esattamente una riceve `amministratore: true` come primo utente e l'altra riceve `amministratore: false`, indipendentemente dall'ordine di completamento; non esiste alcun esito con zero o due amministratori

#### Scenario: Degradazione simultanea su due utenti diversi
- **WHEN** due amministratori degradano contemporaneamente due utenti diversi portando il conteggio a zero
- **THEN** solo una delle operazioni va a buon fine e l'altra risponde `409` con `{"error":"Deve rimanere almeno un amministratore."}`

#### Scenario: Eliminazione dell'ultimo admin contemporanea alla degradazione
- **WHEN** la degradazione e l'eliminazione dell'ultimo admin vengono richieste contemporaneamente
- **THEN** le operazioni sono serializzate e l'invariante "almeno un amministratore" resta vera

### Requirement: Esiti delle attese sul mutex
Il timeout o il deadlock di un'attesa sul lock applicativo SHALL produrre `500 {"error":"Errore interno del database."}`, mai un comportamento silenzioso, un'esecuzione parziale o un'attesa indefinita.

#### Scenario: Timeout del lock
- **WHEN** un'attesa sul mutex supera il timeout di lock
- **THEN** la risposta è `500` con `{"error":"Errore interno del database."}` e nessuna scrittura parziale rimane applicata

### Requirement: Password mai esposta dalle API
Nessuna password, né in chiaro né come hash, SHALL essere esposta tramite le API; la verifica delle credenziali usa la funzione BCrypt senza esposizione dell'hash.

#### Scenario: Password assente in ogni risposta
- **WHEN** il frontend chiama uno qualsiasi degli endpoint di lettura utenti
- **THEN** il corpo non contiene alcun campo password
