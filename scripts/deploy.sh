#!/usr/bin/env bash
# Pubblica il WAR su Tomcat. Uso: deploy.sh [--no-build]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/env.sh"

if [ "${1:-}" != "--no-build" ]; then
  "$SCRIPT_DIR/build.sh"
fi

WAR="$PROJECT_ROOT/backend/target/prgweb.war"
if [ ! -f "$WAR" ]; then
  echo "WAR non trovato: $WAR" >&2
  exit 1
fi

RUNNING=0
if pgrep -f "catalina.base=$CATALINA_HOME" >/dev/null 2>&1; then
  RUNNING=1
  "$SCRIPT_DIR/tomcat-stop.sh"
  for _ in $(seq 1 20); do
    pgrep -f "catalina.base=$CATALINA_HOME" >/dev/null 2>&1 || break
    sleep 1
  done
fi

mkdir -p "$CATALINA_HOME/webapps"
rm -rf "$CATALINA_HOME/webapps/$APP_CONTEXT" "$CATALINA_HOME/webapps/$APP_CONTEXT.war"
cp "$WAR" "$CATALINA_HOME/webapps/$APP_CONTEXT.war"

if [ "$RUNNING" = "1" ]; then
  "$SCRIPT_DIR/tomcat-start.sh"
fi

echo "Deploy completato: $CATALINA_HOME/webapps/$APP_CONTEXT.war"
echo "URL: http://localhost:8080/$APP_CONTEXT/"
