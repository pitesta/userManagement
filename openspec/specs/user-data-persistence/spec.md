# user-data-persistence Specification

## Purpose

Persistenza durabile degli account utente su un database H2 Embedded su file, con schema creato da zero e versionato tramite migrazioni Flyway, vincoli di integrità a database e accesso riservato a una sola istanza applicativa.

## Requirements

### Requirement: Schema creato e versionato da migrazioni
Lo schema del database DEVE essere creato alla prima partenza tramite migrazioni versionate applicate automaticamente all'avvio dell'applicazione, senza procedure manuali di inizializzazione né script da eseguire prima dell'accesso; il processo di migrazione SHALL partire automaticamente con l'applicazione. L'applicazione DEVE avviarsi correttamente su un database appena creato: il primo avvio crea tutte le tabelle e la riga-mutex permanente.

#### Scenario: Prima partenza su database inesistente
- **WHEN** l'applicazione viene avviata senza alcun file database esistente
- **THEN** il file database viene creato, le migrazioni vengono applicate in ordine e le API rispondono correttamente dopo l'avvio

#### Scenario: Partenza su database già migrato
- **WHEN** l'applicazione si riavvia su un database con tutte le migrazioni già applicate
- **THEN** nessuna migrazione viene riapplicata e l'applicazione parte senza errori

### Requirement: Persistenza su file
Il database DEVE risiedere su un file persistente nel percorso configurato; l'applicazione SHALL garantire che i dati scritti superino l'arresto e il riavvio dell'applicazione, con checkpoint del file al rilascio secondo l'arresto pulito del processo, senza operazioni manuali.

#### Scenario: Dati superstiti al riavvio
- **WHEN** un utente viene creato e l'applicazione viene arrestata e riavviata
- **THEN** l'utente è ancora presente ed effettua il login con le stesse credenziali

### Requirement: Identificativi univoci e non riutilizzati
Ogni account DEVE avere un identificativo numerico positivo generato automaticamente come chiave primaria identity; il sistema SHALL assegnare identificativi crescenti e non riutilizzare identificativi di account rimossi.

#### Scenario: Identificativi progressivi
- **WHEN** si creano più utenti in sequenza
- **THEN** ogni nuovo utente riceve un identificativo superiore a quelli dei precedenti

### Requirement: Unicità del nome utente
Il nome utente DEVE essere unico, con lettere maiuscole e minuscole distinte; il sistema SHALL riconoscere come conflitto la violazione del vincolo a database e restituire `409 {"error":"Nome utente già registrato."}` sull'endpoint di registrazione, mai come errore interno.

#### Scenario: Violazione simultanea del vincolo unique
- **WHEN** due richieste di registrazione con lo stesso nome utente vengono eseguite in stretta successione
- **THEN** una va a buon fine e l'altra risponde `409` con `{"error":"Nome utente già registrato."}`

### Requirement: Password solo come hash
L'unico dato di credenziale presente a persistenza DEVE essere l'hash BCrypt della password; il sistema SHALL rifiutare la persistenza di qualsiasi password in chiaro e verificare le credenziali unicamente tramite la funzione di confronto BCrypt sugli hash.

#### Scenario: Verifica della presenza dell'hash
- **WHEN** un utente viene registrato o la password aggiornata
- **THEN** nel database è presente un hash BCrypt e nessuna password in chiaro

### Requirement: Accesso al database da una sola istanza
Il database file DEVE essere aperto in scrittura da una sola istanza dell'applicazione per volta; il sistema SHALL rifiutare con errore esplicito un accesso simultaneo da un secondo processo senza corrompere il file (modalità server automatica esclusa, come da decisione).

#### Scenario: Apertura contemporanea di due processi
- **WHEN** un secondo processo tenta di aprire lo stesso file database mentre l'applicazione è in esecuzione
- **THEN** l'accesso viene rifiutato con errore esplicito e il file resta integro

### Requirement: Validazione dello schema all'avvio
All'avvio l'applicazione DEVE verificare che lo schema del database corrisponda al modello; il sistema SHALL arrestare l'avvio con errore esplicito in caso di mancata corrispondenza (migrazioni non allineate o schema alterato a mano), senza proseguire in silenzio.

#### Scenario: Mancata corrispondenza dello schema
- **WHEN** l'applicazione parte con un database il cui schema non corrisponde al modello
- **THEN** l'avvio fallisce con errore esplicito di validazione

### Requirement: Backup offline con il JAR standalone H2
Il sistema SHALL fornire una procedura di backup con il JAR standalone H2 allineato alla versione H2 dell'applicazione. Il backup SHALL collegarsi al database solo dopo la chiusura completa dell'applicazione e SHALL lasciare l'applicazione arrestata al termine.

#### Scenario: Backup con applicazione attiva
- **WHEN** `db-backup.sh` viene invocato mentre Spring Boot è in esecuzione
- **THEN** lo script esegue l'arresto controllato, verifica che il processo sia terminato e che H2 abbia rilasciato il file, poi lancia H2 Shell; lo zip viene prodotto con exit-code positivo, versione H2 coincidente con quella dell'applicazione, ed è una copia consistente del database; l'applicazione resta ferma

#### Scenario: Arresto non riuscito prima del backup
- **WHEN** il processo Spring Boot non termina o il file H2 risulta ancora aperto
- **THEN** lo script non avvia H2 Shell, non dichiara il backup riuscito e restituisce un errore esplicito

#### Scenario: Backup con database chiuso
- **WHEN** l'applicazione è arrestata e `db-backup.sh` esegue il backup
- **THEN** H2 Shell esegue `BACKUP TO '<dir>/prgweb-<timestamp>.zip'`; lo script verifica exit-code positivo e zip non vuoto, usando il JAR H2 allineato all'applicazione

### Requirement: Ripristino con il JAR standalone H2
Il ripristino SHALL essere consentito solo con applicazione arrestata e SHALL usare il JAR standalone H2 per ricostruire il database prima di sostituire il file attivo.

#### Scenario: Ripristino su database eliminato
- **WHEN** un database eliminato viene ricostruito dal backup con `org.h2.tools.Restore` e l'app viene riavviata
- **THEN** l'applicazione parte e l'utente precedentemente registrato effettua il login con le stesse credenziali

#### Scenario: Comando di ripristino
- **WHEN** l'operatore ripristina un archivio creato con `BACKUP TO` mentre l'applicazione è ferma
- **THEN** la procedura esegue `java -cp scripts/lib/h2.jar org.h2.tools.Restore -file <backup>.zip -dir <dir> -db app` e sostituisce il file `app.mv.db`

#### Scenario: Ripristino a applicazione attiva
- **WHEN** il ripristino viene tentato mentre l'applicazione è in esecuzione
- **THEN** il comando fallisce con un errore esplicito e il file del database resta invariato

### Requirement: Amministratore iniziale creato una sola volta
Una nuova installazione del database SHALL contenere un solo utente `admin` con ruolo amministratore e password iniziale `admin` memorizzata esclusivamente come hash BCrypt. Il seed SHALL essere applicato una sola volta dalla gestione versionata dello schema.

#### Scenario: Primo avvio su database nuovo
- **WHEN** Flyway inizializza un database H2 senza schema
- **THEN** è presente esattamente un utente, `admin`, amministratore, con hash BCrypt verificabile per la password iniziale `admin`

#### Scenario: Avvii successivi
- **WHEN** l'applicazione viene riavviata dopo l'inizializzazione o dopo una modifica della password
- **THEN** il database non viene reinizializzato, l'utente non viene duplicato e la password modificata resta valida

#### Scenario: Nessuna credenziale in chiaro
- **WHEN** viene controllata la riga dell'utente iniziale nel database
- **THEN** la colonna password contiene un hash BCrypt verificabile e non il valore `admin` in chiaro

### Requirement: Amministratore iniziale gestito dalle funzionalità esistenti
L'account iniziale SHALL poter usare le normali operazioni di autenticazione e amministrazione, senza privilegi speciali o percorsi alternativi.

#### Scenario: Cambio password e nuovo login
- **WHEN** `admin` accede con la password iniziale e aggiorna la propria password attraverso l'applicazione
- **THEN** la password iniziale non è più valida, la nuova password è memorizzata come hash e consente un nuovo login

#### Scenario: Gestione utenti e registrazioni
- **WHEN** l'amministratore iniziale gestisce utenti o ruoli e un utente si registra
- **THEN** le regole esistenti di autorizzazione, mutex e protezione dell'ultimo amministratore restano applicate e la registrazione continua a creare un account normalmente
