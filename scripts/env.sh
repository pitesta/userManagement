#!/usr/bin/env bash
# Ambiente di sviluppo locale (user-space). Sorgere con: source scripts/env.sh
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS_DIR="${TOOLS_DIR:-$HOME/.local/opt}"

_jdk_dir="$(find "$TOOLS_DIR" -maxdepth 1 -type d -name 'jdk-25*' 2>/dev/null | sort -V | tail -1)"

export PROJECT_ROOT
export TOOLS_DIR
export NODE_HOME="$TOOLS_DIR/node-v26.13.0-linux-x64"
export MAVEN_HOME="$TOOLS_DIR/apache-maven-3.9.16"

if [ -n "$_jdk_dir" ]; then
  export JAVA_HOME="$_jdk_dir"
else
  export JAVA_HOME="$TOOLS_DIR/jdk"
fi

export PATH="$JAVA_HOME/bin:$NODE_HOME/bin:$MAVEN_HOME/bin:$TOOLS_DIR/npm-global/bin:$PATH"

export APP_CONTEXT="${APP_CONTEXT:-prgweb}"
export PRGWEB_DB_PATH="${PRGWEB_DB_PATH:-$PROJECT_ROOT/data}"
export SERVER_PORT="${SERVER_PORT:-8080}"
export SERVER_SERVLET_CONTEXT_PATH="${SERVER_SERVLET_CONTEXT_PATH:-/$APP_CONTEXT}"
