# Design

## Context

Vedi `proposal.md` — Why. Stato di partenza rilevante per l'approccio:

- Il repo contiene già due report di analisi verificati sul codice:
  `project-analysis.md` (architettura, stack, struttura, API, funzionalità, criticità)
  e `auth-analysis.md` (flusso autenticazione, problema per riga sorgente).
- La documentazione "nativa" esistente è `AGENTS.md`, scritta per agenti operativi
  (comandi, gotcha, convenzioni) e non adatta come documento di lettura umana.
- Codice sorgente di riferimento: `frontend/src/app/**`, `backend/src/main/java/it/prgweb/**`,
  `backend/src/main/webapp/**`, `database/schema.sql`, `scripts/*.sh`, `frontend/*.{json,ts}`,
  `backend/pom.xml`.
- Vincoli di progetto: interfaccia e messaggi in italiano; nessuna modifica a codice,
  architettura o config durante il change.

## Goals / Non-Goals

**Goals:**

- Produrre 9 documenti in `docs/`, tutti in italiano, fedeli al codice attuale.
- Ogni punto citato (file, righe, endpoint, chiavi) verificato direttamente sul repository
  prima di essere scritto; fonti `project-analysis.md` e `auth-analysis.md` usate come
  punto di partenza da ricontrollare, non copiate alla cieca.
- Sezione dedicata e marcata (`Informazioni non verificate dal codice`) in ogni documento
  che contiene almeno un'informazione non derivabile dai sorgenti.

**Non-Goals:**

- Nessuna evoluzione della documentazione automatizzata (niente generatori, niente CI).
- Nessun giudizio di qualità sul codice: i problemi noti vengono riportati solo come
  "stato attuale documentato", senza proposte di fix.
- Nessun aggiornamento a `AGENTS.md` (resta orientato agli agenti; docs e AGENTS.md
  potranno essere riconciliati in un change futuro).

## Decisions

1. **Destinazione `docs/` (9 file tematici) invece di un monolite `README` esteso.**
   Ogni area della richiesta (architettura, frontend, backend, DB, API, auth, ambiente,
   build, test) ha un proprio file: lettori mirati, diff leggibili, manutenzione differenziata.
   *Alternativa scartata*: un unico `docs/documentazione.md` lungo ~10 pagine — più difficile
   da consultare e aggiornare a pezzi.
2. **Stile descrittivo, non prescrittivo.** La documentazione descrive *cosa è* ("il cookie
   di sessione è HttpOnly e SameSite=Lax, senza flag Secure — vedi `web.xml`"), non cosa
   dovrebbe essere: coerenza con il vincolo "nessun miglioramento".
   *Alternativa scartata*: consigli operativi inline — violerebbe la direttiva del change.
3. **Convenzione di marcatura delle fonti.** Ogni documento comincia con un blocco
   "Fonti di verifica" (file consultati con riga/righe); le uniche frasi senza riferimento
   diretto al codice finiscono nella sezione `Informazioni non verificate dal codice`.
   *Alternativa scartata*: note di riga capillari su ogni singola frase — rumore eccessivo
   e più oneroso da mantenere.
4. **Ricava e ricontrolla, non riassumi.** I due report di analisi sono usati come
   checklist di cose da verificare (i rapporti sono datati 05/11/2026 e contengono già la marca
   "da approfondire"): ogni punto va *ri-verificato* nei file indicati prima di entrare nei
   documenti — l'autore del report e il lettore del report possono sbagliare.
   *Alternativa scartata*: copia-adattamento dei due `*-analysis.md` — vanificherebbe
   l'obiettivo di fedeltà attuale.
5. **Diagrammi ASCII inline** per architettura, flusso richieste e flusso auth
   (stesso stile dei due report, zero dipendenze da tooling di rendering).
   *Alternativa scartata*: Mermaid — richiede reader con rendering e introduce una dipendenza di formato.

## Risks / Trade-offs

- [Aumento di superficie di documentazione da mantenere] → Convenzione "Fonti di verifica"
  che rende immediato capire quando un documento va rivisto rispetto a un cambiamento
  di codice (fonte non più verificata dopo il cambiamento).
- [Report *-analysis.md possibili incoerenze con il codice aggiornato] → la verifica
  indipendente in `tasks` è obbligatoria: niente copia, niente "sostituzione".
- [Sovrapposizione con AGENTS.md] → per lo scope di questo change: divergenze ammesse
  (differenza di pubblico e di scopo); la riconciliazione è out-of-scope ed esplicitata.
- [Documenti in italiano per un team potenzialmente internazionale] → scelta coerente con
  l'intero progetto (UI in italiano); decisione registrata, cambiabile in un change futuro.

## Open Questions

- Nessuna: le decisioni in mancanza di risposte del codice sono tutte coperte dalla
  convenzione di marcatura "Informazioni non verificate dal codice" (es. strategie di
  servizio HTTP esterne, comportamenti di tooling fuori repo, ecc.).
