# Backend Spring Boot

Il backend è un'applicazione Java 25 con Spring Boot 4.1.2, packaging JAR e Tomcat 11 embedded. Le versioni gestite da Spring Boot BOM non sono ridefinite nel POM. La persistenza usa Spring Data JPA, Hibernate e H2; Flyway possiede le migrazioni dello schema.

## Struttura

```text
backend/src/main/java/it/prgweb/
├── PrgwebApplication.java
├── config/       WebConfig (CORS e fallback SPA)
├── dto/          request/response come record
├── model/        Utente e AppLock (@Entity)
├── repository/   Spring Data JPA e lock con EntityManager
├── service/      AuthService e UtenteService transazionali
├── util/         PasswordUtil (BCrypt)
└── web/          controller REST, ApiException e advice
```

Le request e response sono DTO record; la serializzazione è Jackson 3 fornita dal BOM Boot. I DTO e `UtenteSessione` non includono l'hash password.

## Flusso e responsabilità

I controller mantengono percorsi e forme del contratto F0 e trasferiscono le regole ai service. I service effettuano le operazioni transazionali attraverso i repository. `AppLockRepository` usa `EntityManager` per bloccare la riga permanente `app_lock` con `SELECT ... FOR UPDATE`.

`GlobalExceptionHandler` converte eccezioni di dominio e guasti di persistenza nel formato `{"error":"messaggio in italiano"}`. I dettagli del contratto sono in [api.md](api.md); le invarianti sono descritte in [architettura.md](architettura.md).

## Configurazione e debug

- `backend/src/main/resources/application.yml`: porta, context path, sessione, H2, JPA, Flyway e logging.
- `backend/src/main/resources/db/migration/`: migrazioni SQL versionate.
- `backend/target/prgweb.jar`: JAR eseguibile con dipendenze e frontend.
- `data/prgweb.log`: log del runtime avviato con `scripts/start.sh`.

Per avviare o arrestare usare gli script in `scripts/`; non avviare più processi contro la stessa directory H2. Vedi [ambiente.md](ambiente.md) e [database.md](database.md).
