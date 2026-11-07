#!/usr/bin/env bash
# Installa in user-space (nessun sudo) JDK 25, Node 26, Maven e Angular CLI.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/env.sh"

mkdir -p "$TOOLS_DIR" "$PROJECT_ROOT/data"

download() {
  local url="$1" out="$2"
  if [ -f "$out" ]; then
    echo "[skip] $out"
  else
    echo "[download] $url"
    wget -q --show-progress -O "$out.part" "$url"
    mv "$out.part" "$out"
  fi
}

# --- JDK 25 (Temurin LTS) ---
if ! find "$TOOLS_DIR" -maxdepth 1 -type d -name 'jdk-25*' | grep -q .; then
  download "https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jdk/hotspot/normal/eclipse" "$TOOLS_DIR/jdk25.tar.gz"
  tar -xzf "$TOOLS_DIR/jdk25.tar.gz" -C "$TOOLS_DIR"
  rm -f "$TOOLS_DIR/jdk25.tar.gz"
else
  echo "[skip] JDK 25"
fi

# --- Node.js 26 LTS ---
if [ ! -d "$NODE_HOME" ]; then
  download "https://nodejs.org/dist/v26.13.0/node-v26.13.0-linux-x64.tar.xz" "$TOOLS_DIR/node26.tar.xz"
  tar -xJf "$TOOLS_DIR/node26.tar.xz" -C "$TOOLS_DIR"
  rm -f "$TOOLS_DIR/node26.tar.xz"
else
  echo "[skip] Node 26"
fi

# --- Maven 3.9.16 ---
if [ ! -d "$MAVEN_HOME" ]; then
  download "https://dlcdn.apache.org/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.tar.gz" "$TOOLS_DIR/maven.tar.gz"
  tar -xzf "$TOOLS_DIR/maven.tar.gz" -C "$TOOLS_DIR"
  rm -f "$TOOLS_DIR/maven.tar.gz"
else
  echo "[skip] Maven"
fi

# --- Angular CLI (prefix user-space) ---
mkdir -p "$TOOLS_DIR/npm-global"
"$NODE_HOME/bin/npm" config set prefix "$TOOLS_DIR/npm-global" >/dev/null
if [ ! -x "$TOOLS_DIR/npm-global/bin/ng" ]; then
  echo "[install] Angular CLI 22.2.5"
  "$NODE_HOME/bin/npm" install -g @angular/cli@22.2.5
else
  echo "[skip] Angular CLI"
fi

# Ricarica env.sh ora che JDK 25 è presente.
source "$SCRIPT_DIR/env.sh"

# Ricarica env.sh dopo l'installazione (JAVA_HOME definitivo).
source "$SCRIPT_DIR/env.sh"

echo
echo "Installazione completata:"
echo "  JAVA_HOME    = $JAVA_HOME"
echo "  Node         = $("$NODE_HOME/bin/node" -v)"
echo "  npm          = $("$NODE_HOME/bin/npm" -v)"
echo "  Maven        = $("$MAVEN_HOME/bin/mvn" -v | head -1)"
echo "  Angular CLI  = $("$TOOLS_DIR/npm-global/bin/ng" version 2>/dev/null | grep -i 'Angular CLI' | head -1 | sed 's/^[[:space:]]*//')"
echo "  H2 DB        = $PRGWEB_DB_PATH/app.mv.db"
