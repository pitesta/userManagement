#!/usr/bin/env bash
# Promuove o rimuove un amministratore, utile per il recupero manuale.
# Uso: ./scripts/set-admin.sh <nomeUtente> [1|0]   (default: 1 = promuovi)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/env.sh"

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "Uso: $0 <nomeUtente> [1|0]" >&2
  exit 1
fi

NOME="$1"
FLAG="${2:-1}"

case "$FLAG" in
  0|1) ;;
  *) echo "Flag non valido: usa 1 (amministratore) o 0 (utente)." >&2; exit 1 ;;
esac

if [ ! -f "$APP_DB_PATH" ]; then
  echo "Database non trovato: $APP_DB_PATH" >&2
  echo "Avvia prima l'applicazione (o esegui ./scripts/db-init.sh)." >&2
  exit 1
fi

sqlite() {
  "$SQLITE_HOME/sqlite3" "$APP_DB_PATH" "$@"
}

# Retrocompatibilità: aggiunge la colonna se il DB è più vecchio.
if ! sqlite "PRAGMA table_info(UTENTI);" | grep -q '|amministratore|'; then
  sqlite "ALTER TABLE UTENTI ADD COLUMN amministratore INTEGER NOT NULL DEFAULT 0;"
  echo "Colonna amministratore aggiunta a $APP_DB_PATH"
fi

NOME_SQL="${NOME//\'/\'\'}"
ESISTE="$(sqlite "SELECT COUNT(*) FROM UTENTI WHERE nomeUtente = '$NOME_SQL';")"
if [ "$ESISTE" = "0" ]; then
  echo "Utente non trovato: $NOME" >&2
  exit 1
fi

if [ "$FLAG" = "0" ]; then
  AMMINISTRATORI="$(sqlite "SELECT COUNT(*) FROM UTENTI WHERE amministratore = 1;")"
  GIA_ADMIN="$(sqlite "SELECT COUNT(*) FROM UTENTI WHERE nomeUtente = '$NOME_SQL' AND amministratore = 1;")"
  if [ "$GIA_ADMIN" = "1" ] && [ "$AMMINISTRATORI" -le 1 ]; then
    echo "Operazione rifiutata: deve rimanere almeno un amministratore." >&2
    exit 1
  fi
fi

sqlite "UPDATE UTENTI SET amministratore = $FLAG WHERE nomeUtente = '$NOME_SQL';"

if [ "$FLAG" = "1" ]; then
  echo "Utente '$NOME' impostato come amministratore."
else
  echo "Privilegi di amministratore rimossi all'utente '$NOME'."
fi
sqlite "SELECT idUtente, nomeUtente, amministratore FROM UTENTI ORDER BY nomeUtente COLLATE NOCASE;"
