#!/usr/bin/env bash
# Compila Angular e crea il fat JAR Spring Boot eseguibile con gli asset inclusi.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/env.sh"

echo "==> Build Angular"
cd "$PROJECT_ROOT/frontend"
npm ci --no-audit --no-fund
APP_CONTEXT="${APP_CONTEXT#/}"
APP_CONTEXT="${APP_CONTEXT%/}"
npx ng build --configuration production --base-href "/$APP_CONTEXT/"

FRONTEND_DIST="$PROJECT_ROOT/frontend/dist/frontend/browser"
if [ ! -f "$FRONTEND_DIST/index.html" ]; then
  echo "Build Angular non ha prodotto $FRONTEND_DIST/index.html" >&2
  exit 1
fi
echo "Output Angular verificato: $FRONTEND_DIST"

echo "==> Build fat JAR Spring Boot"
cd "$PROJECT_ROOT/backend"
mvn clean package

# Copia H2 standalone: i tool Shell/Restore non si possono avviare dal fat JAR
# perché H2 è contenuto come dipendenza annidata in BOOT-INF/lib.
H2_DEPENDENCIES="$PROJECT_ROOT/backend/target/h2-dependencies.txt"
mvn -q dependency:list \
  -DincludeGroupIds=com.h2database \
  -DincludeArtifactIds=h2 \
  -DoutputFile="$H2_DEPENDENCIES"
H2_VERSION="$(sed -n 's/^[[:space:]]*com\.h2database:h2:jar:\([^:[:space:]]*\):compile.*/\1/p' "$H2_DEPENDENCIES" | head -n 1)"
if [ -z "$H2_VERSION" ]; then
  echo "Impossibile rilevare la versione H2 risolta dal BOM" >&2
  exit 1
fi
H2_SOURCE="$HOME/.m2/repository/com/h2database/h2/$H2_VERSION/h2-$H2_VERSION.jar"
if [ ! -f "$H2_SOURCE" ]; then
  echo "JAR H2 risolto da Maven non trovato: $H2_SOURCE" >&2
  exit 1
fi
H2_SOURCE_VERSION="$(unzip -p "$H2_SOURCE" META-INF/MANIFEST.MF | sed -n 's/^Implementation-Version: //p' | tr -d '\r')"
if [ "$H2_SOURCE_VERSION" != "$H2_VERSION" ]; then
  echo "Versione H2 del JAR ($H2_SOURCE_VERSION) diversa da Maven ($H2_VERSION)" >&2
  exit 1
fi

H2_STANDALONE="$SCRIPT_DIR/lib/h2.jar"
if [ -f "$H2_STANDALONE" ]; then
  H2_EXISTING_VERSION="$(unzip -p "$H2_STANDALONE" META-INF/MANIFEST.MF | sed -n 's/^Implementation-Version: //p' | tr -d '\r')"
  if [ "$H2_EXISTING_VERSION" != "$H2_VERSION" ]; then
    echo "scripts/lib/h2.jar ($H2_EXISTING_VERSION) non coincide con la dipendenza Maven ($H2_VERSION)" >&2
    exit 1
  fi
fi
mkdir -p "$SCRIPT_DIR/lib"
cp "$H2_SOURCE" "$H2_STANDALONE"
echo "JAR H2 standalone verificato: $H2_STANDALONE ($H2_VERSION)"

JAR="$PROJECT_ROOT/backend/target/prgweb.jar"
if [ ! -f "$JAR" ]; then
  echo "JAR Spring Boot non trovato: $JAR" >&2
  exit 1
fi
if ! jar tf "$JAR" | grep -qx 'BOOT-INF/classes/static/index.html'; then
  echo "Il JAR non contiene BOOT-INF/classes/static/index.html" >&2
  exit 1
fi
if ! jar tf "$JAR" | grep -q '^BOOT-INF/lib/.*\.jar$'; then
  echo "Il JAR non contiene le dipendenze runtime in BOOT-INF/lib" >&2
  exit 1
fi

echo "Fat JAR eseguibile pronto: $JAR"
