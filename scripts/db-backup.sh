#!/usr/bin/env bash
# Arresta Spring Boot e crea un backup H2 consistente a database chiuso.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/env.sh"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PRGWEB_JAR="$PROJECT_ROOT/backend/target/prgweb.jar"
PRGWEB_PID_FILE="$PROJECT_ROOT/data/prgweb.pid"
source "$SCRIPT_DIR/lib/app-process.sh"

if [ "$#" -gt 1 ]; then
  echo "Uso: $0 [directory-backup]" >&2
  exit 2
fi

DB_DIR="$(cd "$PRGWEB_DB_PATH" 2>/dev/null && pwd)" || {
  echo "Directory database non trovata: $PRGWEB_DB_PATH" >&2
  exit 1
}
DB_FILE="$DB_DIR/app.mv.db"
H2_JAR="$SCRIPT_DIR/lib/h2.jar"
if [ ! -f "$DB_FILE" ]; then
  echo "Database H2 non trovato: $DB_FILE" >&2
  exit 1
fi
if [ ! -f "$H2_JAR" ]; then
  echo "JAR standalone H2 non trovato: $H2_JAR (esegui ./scripts/build.sh)" >&2
  exit 1
fi

BACKUP_DIR="${1:-${PRGWEB_BACKUP_PATH:-$DB_DIR/backups}}"
mkdir -p "$BACKUP_DIR"
BACKUP_DIR="$(cd "$BACKUP_DIR" && pwd)"

if ! "$SCRIPT_DIR/stop.sh"; then
  echo "Arresto Spring Boot non riuscito; backup annullato." >&2
  exit 1
fi

if ! prgweb_require_stopped; then
  exit 1
fi

STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_FILE="$BACKUP_DIR/prgweb-$STAMP.zip"
if [ -e "$BACKUP_FILE" ]; then
  echo "Il file di backup esiste già: $BACKUP_FILE" >&2
  exit 1
fi
SQL_PATH="${BACKUP_FILE//\'/\'\'}"
echo "Database chiuso; creo backup offline: $BACKUP_FILE"
if ! java -cp "$H2_JAR" org.h2.tools.Shell \
    -url "jdbc:h2:file:$DB_DIR/app;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=FALSE" \
    -user sa -password "" \
    -sql "BACKUP TO '$SQL_PATH'"; then
  echo "H2 Shell non è riuscito a creare il backup. Spring Boot resta fermo." >&2
  exit 1
fi
if [ ! -s "$BACKUP_FILE" ]; then
  echo "H2 Shell non ha prodotto un archivio non vuoto. Spring Boot resta fermo." >&2
  exit 1
fi
echo "Backup H2 completato: $BACKUP_FILE (Spring Boot resta fermo)."
