#!/usr/bin/env bash
# Avvio completo: DB + build + deploy + Tomcat.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

"$SCRIPT_DIR/db-init.sh"
"$SCRIPT_DIR/deploy.sh"
"$SCRIPT_DIR/tomcat-start.sh"
