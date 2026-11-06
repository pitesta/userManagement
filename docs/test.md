# Test presenti

*Verificato sul codice: 05/11/2026 — tutte le suite eseguite con esito positivo
in questa sessione (vedi "Verifica eseguita").*

## Fonti di verifica

- `backend/src/test/java/it/prgweb/dao/UtenteDaoTest.java`,
  `backend/src/test/java/it/prgweb/db/DatabaseMigrationTest.java`,
  `backend/src/test/java/it/prgweb/util/PasswordUtilTest.java`
- `frontend/src/app/app.spec.ts`, `frontend/src/app/core/i18n.service.spec.ts`
- `backend/pom.xml` (surefire), `frontend/angular.json` (`test` builder),
  `frontend/package.json` (script `test` + devDependencies)
- Esecuzione reale di `mvn test` e `npx ng test --watch=false` (05/11/2026)

## Backend (JUnit 5/6 tramite Maven Surefire)

Comandi (da AGENTS.md e `pom.xml`, verificati in sessione):

```console
$ source scripts/env.sh && cd backend && mvn test          # tutte
$ cd backend && mvn -Dtest=UtenteDaoTest test              # singola classe
```

| Classe di test | Test | Cosa copre | Sorgente |
|---|---|---|---|
| `UtenteDaoTest` | 7 | CRUD su `UTENTI` (`create`, `findById`, `findByNomeUtente`, `findAll`, `update`, `updateAmministratore`, `deleteById`) con `Database.init()` in `@BeforeEach` su un DB temporaneo | `UtenteDaoTest.java` |
| `DatabaseMigrationTest` | 1 | Migrazione dell'`init()` su un DB preesistente: aggiunge `amministratore` e promuove `MIN(idUtente)` | `DatabaseMigrationTest.java` |
| `PasswordUtilTest` | 2 | Roundtrip `hash/check`; hash malformato → `false` senza exception | `PasswordUtilTest.java` |

### Verifica eseguita (05/11/2026)

```console
$ cd backend && mvn test
[INFO] Tests run: 7  -- in it.prgweb.dao.UtenteDaoTest
[INFO] Tests run: 1  -- in it.prgweb.db.DatabaseMigrationTest
[INFO] Tests run: 2  -- in it.prgweb.util.PasswordUtilTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Frontend (Vitest via builder Angular)

```console
$ cd frontend && npx ng test --watch=false      # run unico, no watch
```

Config: `angular.json:67-69` — `test` con builder `@angular/build:unit-test`
(Angular si appoggia a **Vitest**; senza browser). `npm test` (`package.json:6`)
equivale a `ng test` con watch.

Suite presenti:

| File | Test (dati dal codice) | Cosa copre |
|---|---|---|
| `src/app/app.spec.ts` | 1: "crea il componente root" | montaggio del root `App` |
| `src/app/core/i18n.service.spec.ts` | 4: parte dall'italiano, cambia lingua e la ricorda, interpola i parametri, traduce gli errori IT del back-end | `I18nService` e la mappa `ERRORI_IT` |

### Verifica eseguita (05/11/2026)

```console
$ cd frontend && npx ng test --watch=false
Test Files  2 passed (2)
Tests       5 passed (5)
```

## Copertura del progetto, quale è

- **Testati**: DAO (query), migrazione DB, hashing password, i18n client, monta del
  componente root.
- **Non testati**: servlet HTTP (nessuna suite di integrazione REST, nessun
  `@SpringBootTest`-equivalente — codice servlet senza test), componenti Angular di
  pagina (`home`, `utenti`, `nuovo-utente`, `impostazioni`, `login`), `AuthService`
  e `authGuard` (nessuna spec dedicata), script shell (`scripts/`).

*Nota*: questa sezione elenca le suite di test **oggi esistenti**, senza
prescriverne di nuovi; il codice servlet HTTP è stato verificato nella sessione di
documentazione non tramite test, ma con richieste reali `curl` (vedi [api.md](api.md)).

## Informazioni non verificate dal codice

- Ambiente di esecuzione Vitest: il run in sessione ("environment 56%…" nel
  riepilogo, 2 file passati) conferma che le suite girano senza browser nel
  nodo del processo; la configurazione dichiarata dal builder è quella mostrata
  in `angular.json`.
