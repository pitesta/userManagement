# Tasks

Riferimenti: `proposal.md`, `design.md` e `specs/{user-data-persistence,ui-localization}/spec.md`.

## 1. Amministratore iniziale

- [x] 1.1 Aggiungere `V2__seed_default_admin.sql` senza modificare V1: inserire `admin` con email `admin@example.com`, ruolo amministratore e hash BCrypt verificato per `admin`, solo se `utenti` è vuota. Aggiungere test Flyway che su database nuovo trovi esattamente un account, verifichi l'hash con `PasswordUtil.check`, e su database con utenti non inserisca un secondo account.
- [x] 1.2 Aggiungere test d'integrazione per login dell'admin seed, cambio della propria password tramite la funzionalità esistente, rifiuto della password iniziale dopo il cambio e nuovo login con la password modificata. Verificare che il DTO/sessione non esponga l'hash.
- [x] 1.3 Rieseguire i test backend per registrazione, modifica admin e invarianti concorrenza/ultimo admin, aggiornando le aspettative di database vuoto solo dove il nuovo seed cambia il presupposto. Verificare che registrazione e mutex continuino a funzionare.

## 2. Localizzazione completa

- [x] 2.1 Cambiare il fallback di `I18nService.t` e degli errori sconosciuti: usare il messaggio generico nella lingua attiva, senza testo italiano o chiavi grezze. Aggiungere test per copertura identica delle chiavi IT/EN/FR, traduzione dei messaggi API noti e fallback sconosciuto in tutte le lingue; eseguire Vitest.
- [x] 2.2 Spostare titolo/sottotitolo top bar e valori stack di Impostazioni nei tre dizionari, aggiornando i riferimenti legacy a Servlet/SQLite con lo stack corrente. Aggiungere test del selettore che verifichi opzioni it/en/fr, aggiornamento del documento e testi localizzati; eseguire Vitest e audit dei template per stringhe visibili non localizzate.

## 3. Ricreazione e verifica finale

- [x] 3.1 Verificare che non ci siano processi Spring Boot attivi, eliminare senza backup `data/app.mv.db` e residui H2 `.lock.db`/`.trace.db`, preservando gli altri file in `data/`. Avviare il JAR e verificare via H2 Shell/Flyway che V1 e V2 siano applicate e che esista esattamente un solo utente `admin` amministratore.
- [x] 3.2 Sul database inizializzato, verificare con HTTP login iniziale, cambio password personale, nuovo login, arresto/riavvio e persistenza dell'account/password modificata. Verificare nuovamente che resti un solo utente e nessuna password in chiaro.
- [x] 3.3 Verificare su un database temporaneo isolato le normali registrazioni e operazioni admin/ruoli, inclusi mutex e protezione dell'ultimo admin. Eseguire build Angular+fat JAR, controllare che il bundle incluso contenga il selettore e marcatori inglesi, avviare il JAR prodotto e confermare il cambio it/en/fr; eseguire `mvn test`, `npx ng test --watch=false`, `openspec validate` e `git diff --check`.
