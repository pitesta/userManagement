# Frontend Angular

Frontend invariato dalla migrazione del backend: Angular 22 continua a usare gli stessi endpoint REST, sessione cookie e contratto JSON. In produzione gli asset sono inclusi nel fat JAR Spring Boot; in sviluppo il proxy Angular inoltra le API al context path backend.

## Fonti di verifica

- `frontend/src/app/app.routes.ts`, `app.config.ts`, `app.ts`, `main.ts`, `index.html`
- `frontend/src/app/core/auth.service.ts`, `auth.guard.ts`, `models.ts` (interi file)
- `frontend/src/app/core/i18n.service.ts` (intero file, 620 righe circa)
- `frontend/src/app/layout/{shell,topbar}.ts|.html`
- `frontend/src/app/pages/{login,home,utenti,nuovo-utente,impostazioni}/*.ts`
- `frontend/package.json` (dipendenze), `frontend/proxy.conf.json`, `frontend/angular.json`
- `frontend/vitest.config.ts`? — **non trovato nel repo**: la config Vitest è dentro
  `angular.json`/builder `@angular/build:application` + devDependencies; vedere [test.md](test.md)

## Stack e progetto

| Voce | Valore verificato | Sorgente |
|---|---|---|
| Framework | Angular `^22.2.0` (core/router/forms/…) | `frontend/package.json:14-20` |
| TypeScript | `~6.0.2` | `frontend/package.json:31` |
| Build/CLI | `@angular/build`/`@angular/cli` `^22.2.5` | `frontend/package.json:27-28` |
| RxJS | `~7.8.0` | `frontend/package.json:21` |
| Test | `vitest ^5.0.0` + `jsdom ^30.0.0` | `frontend/package.json:32-33` |
| PM declarato | `npm@11.20.0` | `frontend/package.json:9` |
| Lingua documento | `<html lang="it">` + `<base href="/">` (già presente nel sorgente, riscritto dal build) | `frontend/src/index.html:2,6` |

Componenti **standalone** (nessun `NgModule`), pagine **lazy** via `loadComponent`,
stato via **signals**.

## Struttura

```
frontend/src/app/
├── main.ts                  bootstrap (bootstrapApplication)
├── index.html               shell HTML, base href, lang="it"
├── styles.css               stili globali
├── app.ts                   App: '<app-topbar /><router-outlet />'
├── app.config.ts            provideRouter(routes) + provideHttpClient() (no interceptor)
├── app.routes.ts            rotte (vedi sotto)
├── core/
│   ├── auth.service.ts      AuthService: signal utente + chiamate HTTP
│   ├── auth.guard.ts        authGuard: funzionale, basato su AuthService.me()
│   ├── i18n.service.ts      I18nService: dizionari it/en/fr, errore→chiave
│   └── models.ts            interfacce (Utente, UtenteResponse, ApiError, ...)
├── layout/
│   ├── shell.{ts,html,css}     menu laterale + zona pagina + "Esci"
│   └── topbar.{ts,html,css}    selettore lingua + titolo fisso
└── pages/
    ├── login/               /login (accesso, registrazione)
    ├── home/                /home (dashboard semplice)
    ├── utenti/              /utenti (lista, ruolo, modifica, elimina)
    ├── nuovo-utente/        /nuovo-utente (creazione account)
    └── impostazioni/        /impostazioni (riepilogo technical stack)
```

## Rotte e protezione rotte

Da `app.routes.ts`:

```
/login                 Login                pubblica
''                     ShellComponent       canActivate: [authGuard]
  ''                   → redirect a 'home'  (pathMatch: 'full')
  'home'               HomeComponent        lazy
  'utenti'             UtentiComponent      lazy
  'nuovo-utente'       NuovoUtenteComponent lazy
  'impostazioni'       ImpostazioniComponent lazy
'**'                   → redirect a ''      (fallback)
```

- `authGuard` (`core/auth.guard.ts:13-21`): funzionale; se il signal dell'utente è
  vuoto chiama `AuthService.me()`; su `401` azzera lo stato e redirige su
  `/login` con `router.createUrlTree(['/login'])`.
- `LoginComponent.ngOnInit` (`pages/login/login.ts:37-41`): se c'è già un utente
  in sessione naviga subito su `/home`.

## Stato e comunicazione HTTP

- `AuthService` (`core/auth.service.ts:13-113`): stato = `signal<Utente | null>`
  (`utenteCorrente`), esposto come `signal` readonly (`auth.utente`).
- Tutte le chiamate HTTP usano **`withCredentials: true`** (necessario per il cookie
  `JSESSIONID` su `localhost:4200` in dev) e URL **relative** (`api/...`,
  `api/admin/...`), risolte contro il `<base href>`: righe `22, 33, 41, 50, 56, 62-66,
  83, 96-99` di `auth.service.ts`.
- Nessun `HttpInterceptor` registrato (`app.config.ts:8` passa `provideHttpClient()`
  senza alcun interceptor).
- Interfacce in `core/models.ts` (`Utente`, `UtenteResponse`, `UtentiResponse`,
  `ApiError`, `ModificaUtenteRequest`, `EliminaUtenteResponse`).

| Metodo AuthService | Endpoint | Note |
|---|---|---|
| `login(nomeUtente, password)` | `POST api/login` | imposta il signal `utente` nel `tap` (`auth.service.ts:20-27`) |
| `register(nomeUtente, password, email)` | `POST api/register` | **non** imposta il signal: autologin non esiste (l'utente passerà al login) — `auth.service.ts:29-37` |
| `me()` | `GET api/me` | `tap` imposta il signal — `auth.service.ts:39-46` |
| `logout()` | `POST api/logout` | azzera il signal nel `tap` — `auth.service.ts:48-52` |
| `utenti()` | `GET api/utenti` | solo il `map` — `auth.service.ts:54-58` |
| `impostaAmministratore(id, flag)` | `POST api/admin/utente` | aggiorna il signal **solo** se l'id corrisponde all'utente in sessione — `auth.service.ts:60-75` |
| `modificaUtente(id, dati)` | `POST api/admin/utente/modifica` | come sopra (`auth.service.ts:77-92`) |
| `eliminaUtente(id)` | `POST api/admin/utente/elimina` | azzera il signal **solo** se `risposta.logout === true` (auto-eliminazione) — `auth.service.ts:94-108` |
| `dimenticaUtente()` | — (solo lato client) | `auth.service.ts:110-112` |

## Messaggi di errore e i18n

- Errori HTTP: `messaggioErrore()` (`auth.service.ts:115-126`) estrae `error.error`
  (`{"error": "…"}` tipizzato come `ApiError`) o un messaggio di fallback (`status 0`
  → "Server non raggiungibile. Verifica che Tomcat sia avviato."). È una stringa
  frontend storica mantenuta per compatibilità; il runtime backend attuale è Spring Boot
  con Tomcat embedded.
- `I18nService` (`core/i18n.service.ts`):
  - signal `linguaCorrente` inizializzato da `localStorage["prgweb.lingua"]`
    tramite `leggiLingua()` (`i18n.service.ts:515,563-574`; fall back su `'it'` se assente/non valido o
    `localStorage` non disponibile — `try/catch` in `563-574`; persistenza in
    `salvaLingua()` `575-581`, fallback "valida per la sessione");
  - dizionari `it`/`en`/`fr` (`i18n.service.ts:21,175,329` e mappa `DIZIONARI:489`);
  - `t(chiave, parametri)` con interpolate `{nome}` (`i18n.service.ts:527-538`);
  - `cambiaLingua()`: imposta il signal, persiste in `localStorage`, aggiorna
    `document.documentElement.lang` (`i18n.service.ts:539-543` e `583-587`);
  - `traduciErrore(messaggio)`: mapping codificato `ERRORI_IT`
    (messaggio italiano del backend → chiave di traduzione), righe `492-511` e
    `546-548`: se il messaggio è sconosciuto, resta invariato;
  - `errore(error)`: combina `messaggioErrore` + `traduciErrore` (`i18n.service.ts:552-560`).
- Tutte le etichette di interfaccia provengono da `i18n.t('chiave')`; **titolo della
  top bar è l'unico testo fisso**: `topbar.html:4` contiene letteralmente
  `Gestione Utenti` (fuori dal dizionario). Selettore lingua: `topbar.ts:13-16`
  (`LINGUE` da `i18n.service.ts:13-17`: Italiano/English/Français).

## Pagine: cosa fa ciascuna pagina (oggi)

- **login** — form Reactive di login e registrazione (tab), articoli `login.*`
  (`pages/login/login.ts:26-35,49-98`).
- **home** — dashboard di 3 card (utenti, nuovo utente, impostazioni) + riquadro
  profilo sessione; nessuna chiamata API fuori da `authGuard` (`pages/home/*.ts/html`).
- **utenti** — crea elenco da `GET api/utenti` (riga `52`), toggle ruolo con
  `impostaAmministratore` (riga `71`), modifica utente con form (~riga `123`),
  elimina con conferma (~riga `164`) — tutte su `pages/utenti/utenti.ts`.
- **nuovo-utente** — form di creazione che usa `auth.register`, cioè
  `POST api/register` (`pages/nuovo-utente/nuovo-utente.ts:41`): la pagina è
  protetta da `authGuard`, ma l'endpoint resta pubblico (da segnalare come
  coerenza inerente al comportamento attuale:
  una persona loggata può creare un account nuovo con un endpoint pubblico).
- **impostazioni** — riepilogo tecnico stampato (frontend, backend, database,
  autenticazione) e lingua in join (`pages/impostazioni/impostazioni.ts:1-14`).

## Informazioni non verificate dal codice

Nessuna: la configurazione dei test è confermata da `.angular.json` (`test` builder
`@angular/build:unit-test`, righe `67-69`) e dall'esecuzione di `ng test --watch=false`
in sessione (Vitest 5.1.0, 2 file/5 test superati — dettagli in [test.md](test.md)).
