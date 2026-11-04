#!/usr/bin/env bash
# Ferma Tomcat.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/env.sh"

"$CATALINA_HOME/bin/shutdown.sh" 2>/dev/null || true
echo "Tomcat fermato."
