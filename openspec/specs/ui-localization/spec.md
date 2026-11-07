# ui-localization Specification

## Purpose

L'interfaccia deve offrire italiano, inglese e francese con testi coerenti in ogni schermata, messaggio ed errore, senza esporre chiavi o fallback in una lingua diversa da quella scelta.

## Requirements

### Requirement: Selettore e dizionari completi
Il selettore SHALL offrire italiano, inglese e francese. Ogni lingua SHALL contenere una traduzione non vuota per tutte le chiavi usate dall'interfaccia e dagli errori.

#### Scenario: Selezione di ciascuna lingua
- **WHEN** l'utente seleziona italiano, inglese o francese
- **THEN** i testi dell'interfaccia e `document.documentElement.lang` corrispondono alla lingua scelta e la scelta viene ricordata

#### Scenario: Copertura delle schermate
- **WHEN** sono renderizzate login/registrazione, home, utenti, nuovo utente e impostazioni in una delle tre lingue
- **THEN** etichette, contenuti descrittivi, titoli e valori localizzabili sono tradotti e nessuna chiave manca nel relativo dizionario

### Requirement: Errori e fallback nella lingua selezionata
Gli errori noti SHALL essere tradotti in tutte le lingue. Una chiave o un messaggio sconosciuto SHALL produrre un messaggio generico nella lingua attiva, mai una chiave grezza o testo di fallback in italiano.

#### Scenario: Errori API in inglese e francese
- **WHEN** l'applicazione riceve uno qualsiasi dei messaggi API italiani noti con inglese o francese selezionato
- **THEN** mostra la traduzione corrispondente nella lingua attiva

#### Scenario: Chiave o errore non riconosciuto
- **WHEN** viene richiesto un testo con una chiave assente o arriva un errore non mappato
- **THEN** viene mostrato il messaggio generico nella lingua attiva e non il nome della chiave o il testo italiano originale
