#!/usr/bin/env bash
# Arresta il fat JAR Spring Boot con SIGTERM per chiudere Hikari e fare il checkpoint H2.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/env.sh"

PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
JAR="$PROJECT_ROOT/backend/target/prgweb.jar"
PID_FILE="$PROJECT_ROOT/data/prgweb.pid"

if [ ! -f "$PID_FILE" ]; then
  echo "Spring Boot non risulta avviato (PID file assente)."
  exit 0
fi

PID="$(cat "$PID_FILE")"
if [[ ! "$PID" =~ ^[0-9]+$ ]] || ! kill -0 "$PID" 2>/dev/null \
    || ! ps -p "$PID" -o args= | grep -Fq -- "$JAR"; then
  rm -f "$PID_FILE"
  echo "Rimosso PID file obsoleto; nessuna applicazione Spring Boot terminata."
  exit 0
fi

echo "Arresto Spring Boot (PID $PID)..."
kill -TERM "$PID"
for _ in $(seq 1 40); do
  if ! kill -0 "$PID" 2>/dev/null; then
    rm -f "$PID_FILE"
    echo "Spring Boot arrestato; H2 ha chiuso il database."
    exit 0
  fi
  sleep 1
done

echo "Spring Boot non si è arrestato entro 40 secondi; PID $PID ancora attivo." >&2
exit 1
