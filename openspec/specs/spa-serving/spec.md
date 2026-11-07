# spa-serving Specification

## Purpose

Servire il frontend Angular (SPA) dallo stesso backend: assets statici, fallback HTML5 per il routing Angular e CORS per l'origin del server di sviluppo, senza modifiche al codice frontend.

## Requirements

### Requirement: Fallback SPA per il routing HTML5
Una richiesta GET il cui percorso non contiene un punto (senza estensione) e non appartiene alle API SHALL essere soddisfatta con la pagina `index.html` del frontend, così che i deep link del router Angular (per esempio `/utenti`) possano essere aperti direttamente dal browser e ricaricati.

#### Scenario: Deep link ricaricato
- **WHEN** il browser richiede una GET su un percorso del router senza estensione, per esempio `/utenti`
- **THEN** la risposta è `200` con il contenuto di `index.html`

#### Scenario: Esclusione delle API dal fallback
- **WHEN** una richiesta GET appartiene al percorso delle API (prefisso `/api/`)
- **THEN** il fallback non viene applicato e la richiesta raggiunge il controller delle API

### Requirement: Serving degli assets statici
Gli assets costruiti del frontend (HTML, JavaScript, CSS, font, favicon) SHALL essere serviti direttamente per le richieste con estensione, con i corretti content type, dallo stesso backend; una richiesta GET di un asset mancante risponde `404` e non viene reindirizzata su `index.html`.

#### Scenario: Asset presente
- **WHEN** il browser richiede un file JavaScript o CSS della build del frontend
- **THEN** la risposta è `200` con il contenuto e il content type adeguato

#### Scenario: Asset mancante
- **WHEN** viene richiesto un percorso con estensione che non corrisponde a nessun asset
- **THEN** la risposta è `404` e non viene restituito `index.html`

### Requirement: CORS per l'origin di sviluppo
Per l'origin del server di sviluppo Angular (`http://localhost:4200`) sugli endpoint delle API, il backend SHALL accettare le richieste con credenziali (cookie di sessione), rispondendo con l'allow di quell'origin incluse le credenziali e con i metodi e gli header usati dall'applicazione. In produzione (frontend e API same-origin) le intestazioni CORS non sono necessarie e non alterano il comportamento.

#### Scenario: Preflight dal dev server
- **WHEN** una richiesta preflight OPTIONS con origin `http://localhost:4200` arriva su un endpoint delle API
- **THEN** la risposta dichiara l'allow di quell'origin con credenziali e la lista dei metodi e degli header usati

#### Scenario: Richiesta con cookie dal dev server
- **WHEN** una chiamata API reale con cookie di sessione viene inoltrata dal dev server tramite proxy
- **THEN** la sessione è riconosciuta e la risposta è quella del controller

#### Scenario: Origine non concessa
- **WHEN** una richiesta API ha un origin diverso da quello di sviluppo configurato
- **THEN** le intestazioni CORS non vengono emesse per quella richiesta

### Requirement: Radice del contesto
La radice del frontend SHALL essere servita allo stesso contesto delle API (invariato `/prgweb` per compatibilità con la build Angolare e i proxy); una GET della radice restituisce la pagina iniziale dell'applicazione.

#### Scenario: Apertura della home
- **WHEN** il browser apre la radice del contesto dell'applicazione
- **THEN** la risposta è `200` con `index.html` e l'applicazione Angular si avvia
