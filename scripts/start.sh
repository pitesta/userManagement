#!/usr/bin/env bash
# Avvia direttamente il fat JAR Spring Boot già prodotto da build.sh.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/env.sh"

PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
JAR="$PROJECT_ROOT/backend/target/prgweb.jar"
PID_FILE="$PROJECT_ROOT/data/prgweb.pid"
LOG_FILE="$PROJECT_ROOT/data/prgweb.log"

if [ ! -f "$JAR" ]; then
  echo "JAR non trovato: $JAR" >&2
  echo "Esegui prima ./scripts/build.sh." >&2
  exit 1
fi

mkdir -p "$PROJECT_ROOT/data"
export PRGWEB_DB_PATH="${PRGWEB_DB_PATH:-$PROJECT_ROOT/data}"
export SERVER_SERVLET_CONTEXT_PATH="${SERVER_SERVLET_CONTEXT_PATH:-/$APP_CONTEXT}"
case "$SERVER_SERVLET_CONTEXT_PATH" in
  /*) ;;
  *) SERVER_SERVLET_CONTEXT_PATH="/$SERVER_SERVLET_CONTEXT_PATH" ;;
esac
SERVER_SERVLET_CONTEXT_PATH="${SERVER_SERVLET_CONTEXT_PATH%/}"
export SERVER_SERVLET_CONTEXT_PATH
SERVER_PORT="${SERVER_PORT:-8080}"
export SERVER_PORT

if [ -f "$PID_FILE" ]; then
  PID="$(cat "$PID_FILE")"
  if [[ "$PID" =~ ^[0-9]+$ ]] && kill -0 "$PID" 2>/dev/null \
      && ps -p "$PID" -o args= | grep -Fq -- "$JAR"; then
    echo "Spring Boot è già in esecuzione (PID $PID)."
    exit 0
  fi
  rm -f "$PID_FILE"
fi

APP_URL="http://127.0.0.1:$SERVER_PORT$SERVER_SERVLET_CONTEXT_PATH/"
echo "Avvio Spring Boot: $APP_URL"
nohup java -jar "$JAR" >>"$LOG_FILE" 2>&1 &
PID=$!
echo "$PID" > "$PID_FILE"

for _ in $(seq 1 40); do
  if ! kill -0 "$PID" 2>/dev/null; then
    echo "Spring Boot si è arrestato durante l'avvio. Log: $LOG_FILE" >&2
    tail -n 60 "$LOG_FILE" >&2 || true
    rm -f "$PID_FILE"
    exit 1
  fi
  if curl --silent --fail --max-time 1 "$APP_URL" -o /dev/null; then
    echo "Spring Boot pronto (PID $PID): $APP_URL"
    echo "Log: $LOG_FILE"
    exit 0
  fi
  sleep 1
done

echo "Spring Boot non risponde entro 40 secondi. Log: $LOG_FILE" >&2
tail -n 60 "$LOG_FILE" >&2 || true
kill -TERM "$PID" 2>/dev/null || true
rm -f "$PID_FILE"
exit 1
