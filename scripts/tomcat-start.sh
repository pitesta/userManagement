#!/usr/bin/env bash
# Avvia Tomcat in user-space.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/env.sh"

if pgrep -f "catalina.base=$CATALINA_HOME" >/dev/null 2>&1; then
  echo "Tomcat è già in esecuzione."
else
  echo "Avvio Tomcat..."
  "$CATALINA_HOME/bin/startup.sh"
fi

for _ in $(seq 1 40); do
  if wget -q --spider --timeout=1 "http://localhost:8080/" 2>/dev/null; then
    echo "Tomcat pronto: http://localhost:8080/$APP_CONTEXT/"
    exit 0
  fi
  sleep 1
done

echo "Tomcat non risponde. Controlla $CATALINA_HOME/logs/catalina.out" >&2
exit 1
