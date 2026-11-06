# Tasks

> Ogni gruppo identifica le fonti da verificare *prima* di scrivere il documento:
> i report `project-analysis.md` / `auth-analysis.md` sono checklist da ricontrollare
> sul codice, non fonti da copiare. Convenzione "Fonti di verifica" + sezione
> `Informazioni non verificate dal codice` come da `design.md` (decisioni 3 e 4).

## 1. Struttura e convenzioni di `docs/`

- [x] 1.1 Creare la cartella `docs/` e un breve indice `docs/index.md` che elenca i 9
  documenti con un link ciascuno; verificare che i link puntino a file esistenti al termine
  del resto dei gruppi (torna su questo file per completarlo).
- [x] 1.2 Definire la struttura comune: intestazione con data di verifica, blocco
  "Fonti di verifica" (file e righe consultate), sezione `Informazioni non verificate dal
  codice` (presente solo se ci sono punti non derivabili dal sorgente); verificare che ogni
  documento creato dopo usa la stessa struttura.

## 2. Architettura generale

- [x] 2.1 Scrivere `docs/architettura.md`: architettura a due livelli su un unico contesto
  Tomcat (SPA statica + API `/api/*`), ruolo di `SpaFallbackFilter` e `CorsFilter`,
  diagramma della richiesta→risposta; verificare ogni asserzione su
  `backend/src/main/java/it/prgweb/web/{SpaFallbackFilter,CorsFilter}.java` e
  `backend/src/main/webapp/WEB-INF/web.xml` riportando file e righe nel blocco fonti.
- [x] 2.2 Rileggere `project-analysis.md` e marcare ciò che rimane invariato o differisce
  dal codice attuale; verificare la coerenza del documento con il diagramma del report.

## 3. Frontend Angular

- [x] 3.1 Scrivere `docs/frontend.md`: struttura `src/app` (`core`, `layout`, `pages/*`),
  rotte e lazy loading (`app.routes.ts`), `authGuard` (`core/auth.guard.ts`), stato con
  signals (`core/auth.service.ts`, `I18nService` con chiavi `it`/`en`/`fr` in
  `localStorage`), `topbar` sopra l'outlet (`app.ts`); verificare percorsi e righe citate
  su ogni file indicato, inclusi gli esempi di `withCredentials` e `models.ts`.
- [x] 3.2 Rileggere `project-analysis.md` (par. 3, 5) e correggere/riconfermare ogni
  paragrafo; verificare che i nomi di componenti citati corrispondano al codice (es.
  classi esistenti, exports).

## 4. Backend Java e Servlet

- [x] 4.1 Scrivere `docs/backend.md`: layout pacchetti (`web`, `dao`, `db`, `model`, `util`),
  una sezione per servlet (`AuthServlet`, `UtentiServlet`, `AdminUtenteServlet`) con le
  righe chiave (`changeSessionId`, attributi di sessione, controllo ruoli); verificare
  annotazioni `@WebServlet`, mapping path e righe esatte del codice citato.
- [x] 4.2 Rileggere `auth-analysis.md` e incorporare solo i punti *descrittivi* (le
  debolezze si documentano come "stato attuale", senza fix); verificare che nessun
  suggerimento di miglioramento sia entrato nel documento.

## 5. Database SQLite e schema

- [x] 5.1 Scrivere `docs/database.md`: `database/schema.sql` (colonne, vincoli, indice unique),
  inizializzazione idempotente in `db/Database.java` (classpath via `pom.xml` resources,
  `Class.forName("org.sqlite.JDBC")`), la migrazione automatica che aggiunge
  "amministratore" se manca e promuove il primo utente, precedenza dei percorsi DB
  (`-Dapp.db.path` > `APP_DB_PATH` > `${catalina.base}/data/app.db`); verificare ogni punto
  su `database/schema.sql`, `backend/src/main/java/it/prgweb/db/Database.java`,
  `backend/pom.xml`, `scripts/env.sh`.
- [x] 5.2 Rileggere `project-analysis.md` (par. 4) e confermare/correggere; verificare la
  coerenza con `data/app.db` in `.gitignore`/esclusione commit.

## 6. API REST

- [x] 6.1 Scrivere `docs/api.md`: tabella degli 8 endpoint con metodo, path, accesso
  (pubblica/sessione/admin), corpo delle richieste e risposte illustrative, codici di
  stato (200/201/400/401/403/404/409/500) — tratti da `AuthServlet.java`, `UtentiServlet.java`,
  `AdminUtenteServlet.java` (es. 409 "Deve rimanere almeno un amministratore."); verificare
  ogni endpoint tracciando la riga del controller corrispondente e una `curl` reale
  (o caso di test) che confermi il codice di stato.
- [x] 6.2 Rileggere `project-analysis.md` (par. 5) e confermare che la lista degli endpoint
  non difetti rispetto agli `@WebServlet` nel codice; verificare la coerenza del formato
  delle risposte JSON (`{"utente":…}`, `{"utenti":[…]}`, `{"error":"…" in italiano}`).

## 7. Autenticazione e autorizzazioni

- [x] 7.1 Scrivere `docs/auth.md`: sessione server-side `HttpSession` + `JSESSIONID`
  (first-party, niente JWT), config cookie (`web.xml`: `http-only`, `path=/`,
  `tracking-mode=COOKIE`, timeout 30'; `context.xml`: `sameSiteCookies="lax"`), rigenerazione
  ID a login (`changeSessionId()`), invalidazione a logout/autocancellazione, ruolo
  `amministratore` con check nelle servlet, vincolo ultimo admin, refresh del flag in
  sessione; verificare riga per riga su `AuthServlet.java`, `AdminUtenteServlet.java`,
  `web.xml`, `context.xml`.
- [x] 7.2 Rileggere `auth-analysis.md` e incorporare solo i punti di comportamento
  (confermati); verificare che le sezioni di **problemi** di `auth-analysis.md` restino
  fuori da `docs/auth.md` (o almeno solo descritte come stato, non come "to-fix").

## 8. Ambiente, build e deploy

- [x] 8.1 Scrivere `docs/ambiente.md`: `scripts/install-env.sh` (toolchain in
  `~/.local/opt`), `scripts/env.sh` (`JAVA_HOME`, `PATH`, `APP_DB_PATH`), dipendenze
  (`backend/pom.xml`, `frontend/package.json`, `packageManager npm 11`, `proxy.conf.json`);
  verificare riga per riga sui citati script/files e provare `source scripts/env.sh && echo $JAVA_HOME`.
- [x] 8.2 Scrivere `docs/build-deploy.md`: `scripts/build.sh` (ng build con
  `base-href /prgweb/` + `mvn package`), `scripts/deploy.sh` (+ `--no-build`),
  `scripts/start.sh`/`db-init.sh`/`tomcat-start.sh`/`tomcat-stop.sh`, `scripts/set-admin.sh`,
  `scripts/dev-frontend.sh` (ng serve :4200 con proxy); verificare eseguendo
  `./scripts/build.sh` (o ispezionandolo riga per riga se la build è troppo lenta per il
  momento) e citando `scripts/build.sh`/`deploy.sh` riga per riga.
- [x] 8.3 Rileggere `project-analysis.md` (par. 2, 8 e sezione "Comandi") e
  confermare/correggere ogni comando dichiarato; verificare che i comandi riportati
  funzionino come scritto (o siano marcati come non testati).

## 9. Test

- [x] 9.1 Scrivere `docs/test.md`: test backend (`UtenteDaoTest`, `DatabaseMigrationTest`,
  `PasswordUtilTest` — cosa coprono, comandi `cd backend && mvn test`,
  singola con `-Dtest=…`), test frontend (`app.spec.ts`, `i18n.service.spec.ts`,
  comando `npx ng test --watch=false` con Vitest senza browser); verificare eseguendo i comandi
  (o le singole classi) e citando i file di test effettivi.
- [x] 9.2 Rileggere `project-analysis.md` (par. tecnologie, test) e confermare/correggere;
  verificare che il documento elenchi i test *presenti* senza *prescriverne* di nuovi.

## 10. Verifica finale di coerenza

- [x] 10.1 Completare `docs/index.md` (link a tutti i 9 documenti, data di verifica,
  nota "fedele al codice del commit corrente"); verificare con `ls docs/` e apertura
  dei link.
- [x] 10.2 Cross-check finale: per ogni documento, ogni asserzione con riga di codice
  è ancora corretta (rigenerare i valori e confrontare), tutti i documenti hanno il blocco
  "Fonti di verifica", nessuno contiene verb prescrittivi ("deve", "sarebbe meglio", ecc.),
  `git status` mostra `docs/` come unica aggiunta; verificare con `git status --porcelain`
  e revisione campionaria.

## Workflow follow-up

- Archiviare il change dopo la review: `openspec archive add-project-documentation`.
- Se il team vuole, un change successivo potrà riconciliare `AGENTS.md` con `docs/`
  (vedi `design.md`, Non-Goals).
