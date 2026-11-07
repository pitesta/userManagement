# Documentazione del progetto

Applicazione demo “Gestione Utenti”: frontend Angular 22 e backend Java 25 con Spring Boot 4.1.2, Tomcat embedded e H2 Embedded su file. UI e messaggi sono in italiano.

## Indice

| Documento | Contenuto |
|---|---|
| [architettura.md](architettura.md) | Componenti e flussi SPA/API |
| [backend.md](backend.md) | MVC, service, repository e persistenza |
| [database.md](database.md) | Schema Flyway, H2 e backup/ripristino offline |
| [api.md](api.md) | Endpoint REST, payload e messaggi |
| [auth.md](auth.md) | Sessione HTTP, cookie e ruoli |
| [ambiente.md](ambiente.md) | Toolchain e variabili di configurazione |
| [build-deploy.md](build-deploy.md) | Fat JAR, build, avvio e arresto |
| [test.md](test.md) | Test automatici e verifiche operative |
| [frontend.md](frontend.md) | Angular, routing, stato e i18n |

Le decisioni e i golden di migrazione sono nella change OpenSpec `migrate-backend-to-spring-boot-jpa-h2`. Gli script di progetto e `AGENTS.md` descrivono i comandi rapidi.
