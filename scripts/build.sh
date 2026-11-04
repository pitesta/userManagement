#!/usr/bin/env bash
# Compila il frontend Angular e crea il WAR con le API Java + static files.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/env.sh"

echo "==> Build Angular"
cd "$PROJECT_ROOT/frontend"
npm ci --no-audit --no-fund
npx ng build --configuration production --base-href "/$APP_CONTEXT/"

echo "==> Build WAR"
cd "$PROJECT_ROOT/backend"
mvn -q clean package

echo "WAR pronto: $PROJECT_ROOT/backend/target/prgweb.war"
