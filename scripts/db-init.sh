#!/usr/bin/env bash
# Crea/aggiorna il database SQLite applicando database/schema.sql (idempotente).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/env.sh"

mkdir -p "$(dirname "$APP_DB_PATH")"
"$SQLITE_HOME/sqlite3" "$APP_DB_PATH" < "$PROJECT_ROOT/database/schema.sql"

echo "Database inizializzato: $APP_DB_PATH"
"$SQLITE_HOME/sqlite3" "$APP_DB_PATH" ".schema UTENTI"
