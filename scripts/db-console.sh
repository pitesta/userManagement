#!/usr/bin/env bash
# Apre la console H2 standalone solo quando Spring Boot ha chiuso il database.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/env.sh"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PRGWEB_JAR="$PROJECT_ROOT/backend/target/prgweb.jar"
PRGWEB_PID_FILE="$PROJECT_ROOT/data/prgweb.pid"
source "$SCRIPT_DIR/lib/app-process.sh"

if [ "$#" -ne 0 ]; then
  echo "Uso: $0" >&2
  exit 2
fi
if ! prgweb_require_stopped; then exit 1; fi
DB_DIR="$(cd "$PRGWEB_DB_PATH" 2>/dev/null && pwd)" || {
  echo "Directory database non trovata: $PRGWEB_DB_PATH" >&2
  exit 1
}
if [ ! -s "$DB_DIR/app.mv.db" ]; then
  echo "Database H2 non trovato: $DB_DIR/app.mv.db" >&2
  exit 1
fi
if [ ! -f "$SCRIPT_DIR/lib/h2.jar" ]; then
  echo "JAR standalone H2 non trovato: $SCRIPT_DIR/lib/h2.jar" >&2
  exit 1
fi

exec java -cp "$SCRIPT_DIR/lib/h2.jar" org.h2.tools.Shell \
  -url "jdbc:h2:file:$DB_DIR/app;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=FALSE" \
  -user sa -password ""
