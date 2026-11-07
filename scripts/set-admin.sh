#!/usr/bin/env bash
# Recupera il ruolo amministratore su H2, lasciando l'applicazione arrestata.
# Uso: ./scripts/set-admin.sh <nomeUtente> [1|0] (default: 1)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/env.sh"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PRGWEB_JAR="$PROJECT_ROOT/backend/target/prgweb.jar"
PRGWEB_PID_FILE="$PROJECT_ROOT/data/prgweb.pid"
source "$SCRIPT_DIR/lib/app-process.sh"

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "Uso: $0 <nomeUtente> [1|0]" >&2
  exit 2
fi
USERNAME="$1"
ADMIN_FLAG="${2:-1}"
case "$ADMIN_FLAG" in
  0|1) ;;
  *) echo "Flag non valido: usa 1 (amministratore) o 0 (utente)." >&2; exit 2 ;;
esac
if [[ "$USERNAME" == *$'\n'* || "$USERNAME" == *$'\r'* ]]; then
  echo "Nome utente non valido." >&2
  exit 2
fi

if ! "$SCRIPT_DIR/stop.sh"; then
  echo "Arresto Spring Boot non riuscito; modifica annullata." >&2
  exit 1
fi
if ! prgweb_require_stopped; then exit 1; fi
DB_DIR="$(cd "$PRGWEB_DB_PATH" 2>/dev/null && pwd)" || {
  echo "Directory database non trovata: $PRGWEB_DB_PATH" >&2
  exit 1
}
if [ ! -s "$DB_DIR/app.mv.db" ] || [ ! -f "$SCRIPT_DIR/lib/h2.jar" ]; then
  echo "Database o JAR standalone H2 non trovato." >&2
  exit 1
fi

SQL_USER="${USERNAME//\'/\'\'}"
H2_URL="jdbc:h2:file:$DB_DIR/app;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=FALSE"
run_sql() {
  java -cp "$SCRIPT_DIR/lib/h2.jar" org.h2.tools.Shell \
    -url "$H2_URL" -user sa -password "" -sql "$1"
}
query_value() {
  local output
  output="$(run_sql "$1")" || return 1
  printf '%s\n' "$output" | awk -F'|' 'NR == 2 { gsub(/^[[:space:]]+|[[:space:]]+$/, "", $NF); print $NF }'
}

USER_COUNT="$(query_value "SELECT COUNT(*) FROM \"utenti\" WHERE \"nome_utente\" = '$SQL_USER'")"
if [ "$USER_COUNT" != "1" ]; then
  echo "Utente non trovato o nome non univoco: $USERNAME" >&2
  exit 1
fi
if [ "$ADMIN_FLAG" = "0" ]; then
  ADMIN_COUNTS="$(run_sql "SELECT COUNT(*) AS totale, MAX(CASE WHEN \"nome_utente\" = '$SQL_USER' THEN 1 ELSE 0 END) AS target FROM \"utenti\" WHERE \"amministratore\"")"
  read -r ADMIN_TOTAL TARGET_ADMIN < <(printf '%s\n' "$ADMIN_COUNTS" | awk -F'|' 'NR == 2 { gsub(/^[[:space:]]+|[[:space:]]+$/, "", $1); gsub(/^[[:space:]]+|[[:space:]]+$/, "", $2); print $1, $2 }')
  if [ "${TARGET_ADMIN:-0}" = "1" ] && [ "${ADMIN_TOTAL:-0}" -le 1 ]; then
    echo "Operazione rifiutata: deve rimanere almeno un amministratore." >&2
    exit 1
  fi
fi

run_sql "UPDATE \"utenti\" SET \"amministratore\" = $ADMIN_FLAG WHERE \"nome_utente\" = '$SQL_USER'" >/dev/null
if [ "$ADMIN_FLAG" = "1" ]; then
  echo "Utente '$USERNAME' impostato come amministratore. Spring Boot resta fermo."
else
  echo "Privilegi di amministratore rimossi all'utente '$USERNAME'. Spring Boot resta fermo."
fi
run_sql 'SELECT "id_utente", "nome_utente", "amministratore" FROM "utenti" ORDER BY LOWER("nome_utente")'
