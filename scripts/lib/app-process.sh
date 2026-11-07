#!/usr/bin/env bash
# Funzioni condivise per identificare il processo Java che esegue il fat JAR.

prgweb_app_pids() {
  local pid comm args
  while read -r pid comm args; do
    if [[ "${comm##*/}" == java && "$args" == *"$PRGWEB_JAR"* ]]; then
      printf '%s\n' "$pid"
    fi
  done < <(ps -eo pid=,comm=,args=)
}

prgweb_require_stopped() {
  local active_pids pid_file_pid
  active_pids="$(prgweb_app_pids)"
  if [ -n "$active_pids" ]; then
    echo "Spring Boot è ancora attivo (PID: ${active_pids//$'\n'/, }); operazione annullata." >&2
    return 1
  fi

  if [ -f "$PRGWEB_PID_FILE" ]; then
    pid_file_pid="$(cat "$PRGWEB_PID_FILE")"
    if [[ "$pid_file_pid" =~ ^[0-9]+$ ]] && kill -0 "$pid_file_pid" 2>/dev/null; then
      echo "Il PID registrato è ancora attivo ($pid_file_pid); operazione annullata." >&2
      return 1
    fi
    echo "PID file obsoleto dopo la verifica del processo: $PRGWEB_PID_FILE" >&2
    return 1
  fi
}
