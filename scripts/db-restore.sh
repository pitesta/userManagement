#!/usr/bin/env bash
# Ripristina un archivio H2 solo a database chiuso; resta fermo per default.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/env.sh"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PRGWEB_JAR="$PROJECT_ROOT/backend/target/prgweb.jar"
PRGWEB_PID_FILE="$PROJECT_ROOT/data/prgweb.pid"
source "$SCRIPT_DIR/lib/app-process.sh"

START_AFTER=false
BACKUP_FILE=""
for arg in "$@"; do
  case "$arg" in
    --start) START_AFTER=true ;;
    -*) echo "Opzione non riconosciuta: $arg" >&2; echo "Uso: $0 <backup.zip> [--start]" >&2; exit 2 ;;
    *)
      if [ -n "$BACKUP_FILE" ]; then
        echo "Uso: $0 <backup.zip> [--start]" >&2
        exit 2
      fi
      BACKUP_FILE="$arg"
      ;;
  esac
done
if [ -z "$BACKUP_FILE" ]; then
  echo "Uso: $0 <backup.zip> [--start]" >&2
  exit 2
fi
if [ ! -f "$BACKUP_FILE" ] || [ ! -s "$BACKUP_FILE" ]; then
  echo "Archivio di backup non trovato o vuoto: $BACKUP_FILE" >&2
  exit 1
fi
if [ ! -f "$SCRIPT_DIR/lib/h2.jar" ]; then
  echo "JAR standalone H2 non trovato: $SCRIPT_DIR/lib/h2.jar" >&2
  exit 1
fi
prgweb_require_stopped

mkdir -p "$PRGWEB_DB_PATH"
DB_DIR="$(cd "$PRGWEB_DB_PATH" 2>/dev/null && pwd)" || {
  echo "Directory database non trovata: $PRGWEB_DB_PATH" >&2
  exit 1
}
RESTORE_DIR="$(mktemp -d "$DB_DIR/.restore.XXXXXX")"
cleanup() {
  if [ -n "${RESTORE_DIR:-}" ] && [ -d "$RESTORE_DIR" ]; then
    rm -rf -- "$RESTORE_DIR"
  fi
}
trap cleanup EXIT

if ! java -cp "$SCRIPT_DIR/lib/h2.jar" org.h2.tools.Restore \
    -file "$(cd "$(dirname "$BACKUP_FILE")" && pwd)/$(basename "$BACKUP_FILE")" \
    -dir "$RESTORE_DIR" -db app; then
  echo "H2 Restore non riuscito; il database attivo non è stato sostituito." >&2
  exit 1
fi
RESTORED_DB="$RESTORE_DIR/app.mv.db"
if [ ! -s "$RESTORED_DB" ]; then
  echo "H2 Restore non ha prodotto un file app.mv.db valido; database attivo invariato." >&2
  exit 1
fi

CURRENT_DB="$DB_DIR/app.mv.db"
OLD_DB="$RESTORE_DIR/app.previous.mv.db"
if [ -e "$CURRENT_DB" ]; then
  mv -- "$CURRENT_DB" "$OLD_DB"
fi
if ! mv -- "$RESTORED_DB" "$CURRENT_DB"; then
  if [ -e "$OLD_DB" ]; then mv -- "$OLD_DB" "$CURRENT_DB"; fi
  echo "Sostituzione del database fallita; ripristinato il file precedente." >&2
  exit 1
fi
rm -f -- "$DB_DIR/app.lock.db" "$DB_DIR/app.trace.db"
if [ -e "$OLD_DB" ]; then rm -f -- "$OLD_DB"; fi

echo "Database ripristinato da $BACKUP_FILE. Spring Boot resta fermo."
if [ "$START_AFTER" = true ]; then
  if ! "$SCRIPT_DIR/start.sh"; then
    echo "Database ripristinato, ma l'avvio controllato non è riuscito." >&2
    exit 1
  fi
fi
