# Proposal

## Why

Un database nuovo viene creato correttamente da Flyway ma non contiene un account iniziale, quindi l'applicazione non offre le credenziali amministrative richieste all'installazione. Nel sorgente corrente l'inglese è già presente in `LINGUE` e in tutti i 130 messaggi; la sua assenza nell'istanza segnalata non è riproducibile qui, mentre l'interfaccia ha valori fissi non localizzati e fallback che possono mostrare italiano o chiavi grezze. Questa change rende verificabili sia il seed sia la lingua distribuita.

## What Changes

- Aggiungere una migrazione Flyway successiva e immutabile che inserisce un solo account `admin` con hash BCrypt pre-calcolato della password iniziale `admin`; gli avvii seguenti non lo ricreano né ne sovrascrivono la password.
- Mantenere registrazione, modifica profilo, cambio password, ruoli, mutex e protezione dell'ultimo amministratore invariati.
- Conservare le opzioni italiano, inglese e francese; localizzare i testi fissi del top bar e i valori mostrati in Impostazioni, inclusi i dati stack aggiornati a Spring Boot/H2.
- Rendere coerente il fallback di traduzione con la lingua selezionata e aggiungere test di copertura per chiavi, selettore e messaggi d'errore in tutte le lingue.
- Eliminare il database H2 locale `data/app.mv.db` senza backup e ricrearlo con Flyway per verificare il seed iniziale.

## Capabilities

### New Capabilities

- `user-data-persistence`: provisioning iniziale di un solo amministratore con credenziali hashate e migrazione Flyway non ripetitiva.
- `ui-localization`: selezione e copertura coerente di italiano, inglese e francese per testi, errori e fallback dell'interfaccia.

### Modified Capabilities

Nessuna.

## Impact

- Backend: nuova migrazione SQL in `backend/src/main/resources/db/migration/` e test Flyway/API/persistenza.
- Frontend: `I18nService`, top bar e schermata Impostazioni; test Vitest per copertura delle traduzioni e selettore.
- Dati locali: solo `data/app.mv.db` e gli eventuali residui H2 vengono rimossi per il collaudo autorizzato; nessun backup.
- Nessun cambio a dipendenze, API pubbliche, framework o architettura Spring Boot/JPA/H2.
