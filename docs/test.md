# Test e verifiche

## Test automatici backend

```bash
source scripts/env.sh
cd backend
mvn test
```

I test usano database H2 temporanei e coprono:

- mappatura Flyway/JPA, CRUD, identità e vincolo univoco;
- service di autenticazione e regole amministrative;
- registrazioni concorrenti e operazioni simultanee sull'ultimo admin;
- lock e mapping degli errori di persistenza;
- contratto MockMvc degli otto endpoint rispetto ai golden F0;
- cookie/sessione, serializzazione dei DTO e assenza della password;
- fallback SPA, asset statici, esclusione API e CORS.

Classi di riferimento: `ApiContractTest`, `ApiExceptionMappingTest`, `MappaturaErroriTest`, `SerializzazioneDtoTest`, test repository e service sotto `backend/src/test/java/it/prgweb`. I test di mapping verificano gli status e i messaggi definiti nel contratto.

## Test frontend

```bash
cd frontend
npx ng test --watch=false
```

Il frontend non è stato modificato dalla migrazione.

## Verifica build e runtime

```bash
./scripts/build.sh
SERVER_PORT=8081 ./scripts/start.sh
./scripts/stop.sh
```

`build.sh` esegue la suite Maven mentre crea il JAR e controlla la presenza della SPA e delle dipendenze nel fat JAR. Per il test operativo backup/ripristino offline, inclusa la verifica HTTP dopo il ripristino, seguire [database.md](database.md).
