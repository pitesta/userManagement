-- Account di bootstrap per una nuova installazione.
-- Il valore password è un hash BCrypt (costo 10) della password iniziale "admin".
-- I database già popolati non ricevono un account privilegiato inatteso.
INSERT INTO "utenti" ("nome_utente", "password", "email", "amministratore")
SELECT 'admin', '$2a$10$aTSxNvXxQ7YXv3vEzlCVBeTQNviYOD.eln7pkFmMR0Bq4dwREEy/e',
       'admin@example.com', TRUE
WHERE NOT EXISTS (SELECT 1 FROM "utenti");
