# Proposal

## Why

Il progetto `prgWebAngularJava` non ha una documentazione funzionale e tecnica strutturata:
le conoscenze esistono solo sparse (AGENTS.md orientato agli agenti, i report di analisi
`project-analysis.md` e `auth-analysis.md` generati in esplorazione). Serve una documentazione
fedele allo stato attuale del codice, verificata direttamente nei sorgenti, per facilitare
onboarding, manutenzione e verifiche successive — senza introdurre alcuna funzionalità.

## What Changes

- Creazione di una cartella `docs/` con documentazione in italiano, destinata ai lettori
  umani (sviluppatori, revisori), in aggiunta — e non in sostituzione — di AGENTS.md:
  - `docs/architettura.md`: architettura generale (SPA + WAR singolo contesto, filtri, flusso
    richiesta→risposta).
  - `docs/frontend.md`: struttura frontend Angular 22 (core, layout, pagine, rotte, guardie,
    i18n, stato via signals).
  - `docs/backend.md`: struttura backend Java 25 (servlet, filtri, DAO, Database, util).
  - `docs/database.md`: schema SQLite (`database/schema.sql`), vincoli, migrazione `init()`.
  - `docs/api.md`: API REST esistenti (endpoint, richieste/risposte, codici di stato, accessi).
  - `docs/auth.md`: autenticazione/autorizzazione (sessione `HttpSession`, cookie, ruoli,
    vincolo ultimo admin) — descrizione del solo comportamento attuale, senza criteri di
    miglioramento.
  - `docs/ambiente.md`: configurazione ambiente (env.sh, toolchain in `~/.local/opt`, Tomcat,
    percorsi DB).
  - `docs/build-deploy.md`: build, avvio e deploy (script in `scripts/`).
  - `docs/test.md`: test presenti (JUnit, Vitest) e come eseguirli.
- Ogni affermazione deve essere verificata sul codice del repository; le fonti di supporto
  sono le analisi `project-analysis.md` e `auth-analysis.md`, da ricontrollare prima di usarle.
- Segnalazione esplicita nella documentazione di ogni informazione non verificabile o
  derivata da conoscenza esterna (intestazione "Informazioni non verificate dal codice").
- **Nessuna** modifica al codice sorgente, allo schema del database, alla configurazione o
  agli script esistenti. **Nessuna** correzione dei problemi già noti (quelli documentati
  restano documentati come sono, senza proposte di fix).

## Capabilities

### New Capabilities

<!-- Nessuna capability: il progetto esistente cambia solo esternamente tramite file di
     documentazione che non alterano alcun comportamento del sistema. Per effetto di
     skip_specs: true le delta specs non vengono create. -->
- Nessuno — change di sola documentazione (`skip_specs: true` in `.openspec.yaml`):
  nessun comportamento osservabile del sistema cambia.

### Modified Capabilities

- Nessuno.

## Impact

- **File nuovi**: `docs/*.md` (lista sopra). Nessun file di codice, configurazione, schema o
  script toccato.
- **Nessun impatto su**: runtime, API, database, build, test. La documentazione non modifica
  nulla di ciò che è già descritto nei rapporti `project-analysis.md` e `auth-analysis.md`
  (che restano come materiali di reference; la nuova documentazione li integra, non li sostituisce).
