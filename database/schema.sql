-- Database: SQLite
-- Tabella utenti dell'applicazione.
-- La password contiene sempre un hash BCrypt (mai testo in chiaro):
-- varchar(100) invece del varchar(20) originale, troppo corto per un hash.
CREATE TABLE IF NOT EXISTS UTENTI (
  idUtente      INTEGER      PRIMARY KEY AUTOINCREMENT,
  nomeUtente    VARCHAR(100) NOT NULL UNIQUE,
  password      VARCHAR(100) NOT NULL,
  email         VARCHAR(40)  NOT NULL,
  amministratore INTEGER    NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_utenti_nomeUtente ON UTENTI (nomeUtente);
