# Documentazione del progetto prgWebAngularJava

Applicazione demo **"Gestione Utenti"**: frontend Angular 22 e backend Java 25
(Jakarta Servlet 6.1, WAR su Tomcat 11) con database SQLite, in un unico contesto
`/prgweb/`. Interfaccia e documentazione in italiano.

**Stato della documentazione** — verificata sul codice: **05/11/2026**.
Ogni documento indica nel blocco "Fonti di verifica" i file e le righe consultati;
le informazioni che non derivano direttamente dai sorgenti sono raccolte nella
sezione `Informazioni non verificate dal codice` del singolo documento.

Fedele al codice del commit corrente: la documentazione descrive **solo** ciò che
esiste; nessun miglioramento, nessun comportamento prescrittivo.

## Indice

| Documento | Contenuto |
|---|---|
| [architettura.md](architettura.md) | Architettura generale: SPA + WAR sullo stesso contesto, filtri, flusso richiesta→risposta |
| [frontend.md](frontend.md) | Struttura frontend Angular (core, layout, pagine, rotte, i18n, signals) |
| [backend.md](backend.md) | Struttura backend Java (servlet, filtri, DAO, Database, util) |
| [database.md](database.md) | Database SQLite: schema, inizializzazione e migrazione |
| [api.md](api.md) | API REST: endpoint, corpi, codici di stato, accessi |
| [auth.md](auth.md) | Autenticazione e autorizzazioni (sessione, cookie, ruoli) |
| [ambiente.md](ambiente.md) | Configurazione dell'ambiente (toolchain, variabili) |
| [build-deploy.md](build-deploy.md) | Build, avvio e deploy (script `scripts/`) |
| [test.md](test.md) | Test presenti ed esecuzione |

## Documenti di riferimento fuori da `docs/`

- `../project-analysis.md` e `../auth-analysis.md`: report di analisi del 05/11/2026
  usati come checklist di verifica; la documentazione in `docs/` è costituita da
  una revisione del codice indipendente e non li rimpiazza.
- `../AGENTS.md`: istruzioni operative per agenti (comandi, convenzioni); resta
  separato da questa documentazione.
