# Report di analisi esplorativa — prgWebAngularJava

*Data: 05/11/2026 · Analisi di solo lettura, nessun codice modificato.*

---

## 1. Architettura generale

Applicazione web demo **"Gestione Utenti"** (interfaccia in italiano) a due livelli quadranti
sullo stesso processo Tomcat:

```
[Browser] --HTTP--> [Tomcat 11]
                       |
                       |-- /prgweb/            --> Angular SPA (statici, build AOT)
                       |-- /api/*              --> Jakarta Servlet 6.1 (WAR "prgweb")
                       |        |
                       |        `-- JDBC ----> [SQLite: data/app.db]
```

- **Backend**: WAR Java 25, servlet classiche (nessun framework), persistenza con JDBC puro.
  L'angolo frontend è servito dallo stesso contesto: `SpaFallbackFilter` inoltra le GET senza
  estensione (escluso `/api/*`) a `index.html`, così l'HTML5 routing funziona senza server separato.
- **Frontend**: Angular 22 standalone,deployato nel WAR; in sviluppo può girare su `:4200`
  (`ng serve` con proxy `/api` → `:8080/prgweb/api`).
- **Autenticazione**: sessione server-side (`HttpSession` + cookie `JSESSIONID`, `Path=/`).
  Nessun JWT. L'utente in sessione è memorizzato senza l'hash della password.
- **Ruoli**: colonna `UTENTI.amministratore` (0/1). Il primo utente registrato diventa
  automaticamente amministratore; i successivi nascono non-admin. L'ultima amministratrice
  rimanente non può essere declassata né eliminata (API → `409`).

## 2. Tecnologie utilizzate

| Livello | Tecnologia |
|---|---|
| Frontend | Angular 22 (standalone, lazy `loadComponent`, signals), TypeScript 6, RxJS 7 |
| Test frontend | Vitest 5 (run senza browser, via `jsdom`) |
| Backend | Java 25, Jakarta Servlet API 6.1 (`provided`), packaging WAR Maven |
| Persistenza | SQLite (driver `sqlite-jdbc` 3.54), JDBC puro, nessun ORM |
| Sicurezza | BCrypt (`jbcrypt` 0.4) per gli hash delle password |
| Serializzazione | Jackson 2.22 (`jackson-databind`) |
| Test backend | JUnit Jupiter 6 |
| Toolchain | Node 26, Maven, Angular CLI, Tomcat 11 — installati in `~/.local/opt` da `scripts/install-env.sh`, attivati con `source scripts/env.sh` |

## 3. Struttura principale

```
frontend/src/app/
├── core/                 ← AuthService, authGuard, I18nService (it/en/fr), models.ts
├── layout/               ← shell + topbar (titolo fisso "Gestione Utenti")
└── pages/
    ├── login/            ← /login
    ├── home/             ← /home (protetta)
    ├── utenti/           ← /utenti (protetta)
    ├── nuovo-utente/     ← /nuovo-utente (protetta)
    └── impostazioni/     ← /impostazioni (protetta)

backend/src/main/java/it/prgweb/
├── web/                  ← AuthServlet, UtentiServlet, AdminUtenteServlet,
│                           CorsFilter, SpaFallbackFilter, AppContextListener
├── dao/                  ← UtenteDao (prepared statement, nessun ORM)
├── db/                   ← Database.init(): crea/migra lo schema all'avvio
├── model/                ← Utente
└── util/                 ← Json, PasswordUtil (BCrypt)
backend/src/test/java/    ← UtenteDaoTest, DatabaseMigrationTest, PasswordUtilTest

database/schema.sql       ← unica fonte dello schema (copiata nel classpath dal build)
scripts/                  ← install-env, env, start, build, deploy, db-init,
                            dev-frontend, tomcat-start/stop, set-admin
```

Convenzioni chiave: servlet in `web`, DAO in `dao`, modelli in `model`, utility in `util`;
componenti Angular standalone con stato via signals; tutte le etichette di interfaccia passano
da `I18nService.t('chiave')` (unico testo fisso: il titolo della top bar).

## 4. Database e comunicazione

- **Schema**: un'unica tabella `UTENTI` (`idUtente` PK autoincrement, `nomeUtente` unico,
  `password` varchar(100) con hash BCrypt, `email`, `amministratore` 0/1). `schema.sql` è
  l'unico schema canonico: Maven lo copia nel WAR e `Database.init()` lo esegue all'avvio
  in modo idempotente.
- **Migrazione automatica**: su DB preesistenti senza `amministratore`, `init()` aggiunge la
  colonna e promuove l'utente più anziano (`MIN(idUtente)`) per non lasciare l'app senza admin.
- **Comunicazione frontend ↔ backend**: `fetch`/HttpClient con **URL relativi** (`api/...`),
  resi validi dal `<base href>/$APP_CONTEXT/`; il dev proxy riscrive `/api` su `/prgweb/api`.
  Il cookie di sessione deve restare `Path=/` perché arrivi anche al dev server `:4200`.
- **Errori**: il backend restituisce sempre `{"error":"messaggio in italiano"}`,
  tradotto lato client da `I18nService.traduciErrore`.
- **Percorso DB**: precedenza `-Dapp.db.path` > `APP_DB_PATH` > `${catalina.base}/data/app.db`.

## 5. Funzionalità implementate

| API | Funzione | Accesso |
|---|---|---|
| `POST /api/register` | Registrazione; il primo utente diventa admin | pubblica |
| `POST /api/login` | Login (sessione) | pubblica |
| `POST /api/logout` | Logout | pubblica |
| `GET /api/me` | Utente corrente (ripristino sessione) | pubblica |
| `GET /api/utenti` | Elenco utenti | sessione |
| `POST /api/admin/utente` | Cambio flag amministratore | admin |
| `POST /api/admin/utente/modifica` | Modifica nome/email/eventuale password | admin |
| `POST /api/admin/utente/elimina` | Eliminazione utente (auto-eliminazione → `logout: true`) | admin |

Frontend: login/registrazione, guardia funzionale su tutte le rotte protette,
gestione utenti (lista, creazione, modifica ruolo, modifica profilo, eliminazione),
pagina impostazioni con cambio lingua (it/en/fr persistito in `localStorage`,
chiave `prgweb.lingua`). Sincronizzazione della sessione via `AuthService` + `authGuard`
(`GET /api/me` come fallback all'avvio).

## 6. Criticità e possibili miglioramenti

**Criticità attuali (da tenere d'occhio, non necessariamente difetti per una demo):**

1. **SQLite in produzione Tomcat** — file singolo senza pool di connessioni: sufficiente per
   una demo, ma un collo di bottiglia sotto concorrenza di scrittura (lock dell'intero file).
2. **Servlet "a switch"** — `AuthServlet` e `AdminUtenteServlet` instradano con `case` sulla
   path: cresce male; a superiore maturità converrebbe un piccolo framework di routing (o Spring/JAX-RS).
3. **L'admin può modificare se stesso** — gestito correttamente (la sessione viene
   aggiornata da `AdminUtenteServlet`), ma resta un flusso delicato: un admin può
   autocambiare ruolo. Il vincolo "almeno un admin" protegge il caso peggiore.
4. **Nessuna login-page di recupero password** — non è prevista reset via email.
5. **`CorsFilter` + `SpaFallbackFilter`** mappati genericamente: da verificare che non espongano
   l'API in modo più largo del necessario.
6. **Email varchar(40)** — limite severo per indirizzi realistici.
7. **Nessuna paginazione** su `/api/utenti`: con relativamente pochi utenti è OK, crescita futura.
8. **Rigenerazione della sessione**: non è chiaro (da verificare) se la `JSESSIONID` venga
   rigenerata dopo login/logout — buona difesa contro session fixation in produzione.

**Estensioni candidate (idea di backlog):**

- Gestione ruoli più ricca (es. ruolo "moderatore"), audit log delle azioni admin.
- Reset password / verifica email.
- Paginazione + riordini nella lista utenti.
- Refresh token non necessario (sessione cookie), ma potrebbe servire un timeout configurabile.
- Sul frontend: test di integrazione delle pagine oltre agli unit dei servizi core.

---

*Rapporto generato a scopo documentativo. Nessun file di codice è stato modificato;
questo documento è l'unico artefatto creato dall'analisi.*
