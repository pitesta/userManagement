# Design

## Context

V1 creates the H2 schema and mutex row but no user. The active H2 file is at `data/app.mv.db`, has Flyway V1 applied and currently contains zero user rows. `PasswordUtil.hash` and `PasswordUtil.check` use the existing BCrypt dependency. The Angular source and locally built JAR already contain the `it`, `en`, and `fr` dictionaries with 130 matching keys; the selector includes English. The current translation helper falls back to Italian and then the raw key, while the top bar and settings stack values include fixed or pre-migration text.

## Goals / Non-Goals

**Goals:** add the default account without changing schema ownership, authentication, or administrator locking; make every visible translatable string and fallback consistent with the selected locale; verify the produced JAR and real database lifecycle.

**Non-Goals:** change API routes, DTOs, session behavior, JPA/H2 architecture, user registration rules, password policy, or other Angular behavior.

## Decisions

### Add an append-only Flyway migration

Add V2 after V1. It conditionally inserts the initial row only when `utenti` is empty, using `admin`, `admin@example.com`, `amministratore = TRUE`, and a precomputed BCrypt hash of `admin` produced with the existing BCrypt implementation. A conditional insert avoids adding a default privileged account to databases that already contain users. Flyway's schema history ensures V2 is not rerun on later startups; V1 remains unchanged.

The production database is recreated by removing only the H2 file and H2 residuals after implementation, then starting Spring Boot so Flyway applies V1 and V2. No backup is created, per the user's explicit instruction. This does not delete the unrelated legacy SQLite file if one is present.

### Reuse the existing account-management path

The seed is an ordinary `Utente` row. Password modification, role operations, and last-admin/mutex rules continue through the existing controllers and services. No special endpoint, startup re-seed, or plaintext credential handling is introduced.

### Make locale selection fail closed to a localized message

Keep the existing three language options and dictionaries. Change missing-key and unknown-error fallback to the selected dictionary's generic error message; never fall back to Italian or return a key. Tests compare the three dictionaries' key sets and verify all mapped API messages in English and French.

Move the top-bar title/subtitle and settings stack values into translation dictionaries. Update the stack values to the current Angular 22, Spring Boot 4.1.2, embedded Tomcat 11, and H2 Embedded runtime. The user request explicitly requires all UI elements to follow locale selection, superseding the previous convention that kept the title Italian.

### Verify the shipped frontend, not just its source

Run the frontend unit suite and `scripts/build.sh`, which rebuilds Angular before packaging. Inspect the final JAR's static bundle for the English selector and translated markers, then start that exact JAR and switch among all three options in the locale component test. The source-level audit found no missing dictionary keys; this guards against a stale or incomplete packaged frontend.

## Risks / Trade-offs

- [The fixed initial password is widely known] → It is only stored as BCrypt; document the initial login and verify the owner can change it immediately through the existing self-edit flow.
- [A user interface deployment may still serve an older JAR] → Rebuild and verify the packaged static bundle, then test the exact executable JAR.
- [Deleting `data/app.mv.db` is irreversible] → The user explicitly authorized deletion without backup; verify no application process is active and remove only the H2 database/residual files, preserving logs and unrelated files.

## Migration Plan

1. Add V2 and tests without changing the active database.
2. Run backend/frontend tests and rebuild the fat JAR.
3. Stop/confirm the application is stopped; remove `data/app.mv.db` and H2 lock/trace remnants without backup.
4. Start the built JAR; verify Flyway V1/V2, exactly one initial admin, BCrypt login, password change, new login, restart persistence and user-management behavior.
5. Verify the packaged UI language selector and translations in Italian, English and French.

## Open Questions

The user-reported English loss is not reproducible from this checkout or its current JAR. The exact external process or browser bundle that displayed only two languages is not available for inspection; the change therefore verifies and rebuilds the local deployable artifact and adds regression coverage.
