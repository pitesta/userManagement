# Spec Delta

## Purpose

La persistenza su H2 deve rendere disponibile un amministratore alla prima inizializzazione senza esporre credenziali in chiaro né ripetere il seed agli avvii successivi.

## ADDED Requirements

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
