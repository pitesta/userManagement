#!/usr/bin/env bash
# Dev server Angular con proxy /api -> Tomcat.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/env.sh"

cd "$PROJECT_ROOT/frontend"
if [ ! -d node_modules ]; then
  npm ci --no-audit --no-fund
fi
npm start
