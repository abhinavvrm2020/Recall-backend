#!/bin/sh
set -eu

# Railway DATABASE_URL is libpq-style:
#   postgresql://user:pass@host:port/db
# Postgres JDBC does NOT accept user:pass@ in the authority — parse it into
# spring.datasource.url + username + password instead.

if [ -n "${DATABASE_URL:-}" ]; then
  RAW="$DATABASE_URL"

  # Strip scheme
  NO_SCHEME=$(printf '%s' "$RAW" | sed -E 's#^(postgres|postgresql)://##')

  # user:pass@host:port/db  OR  host:port/db
  case "$NO_SCHEME" in
    *@*)
      USERINFO=${NO_SCHEME%%@*}
      HOSTPART=${NO_SCHEME#*@}
      DB_USER=${USERINFO%%:*}
      DB_PASS=${USERINFO#*:}
      ;;
    *)
      HOSTPART=$NO_SCHEME
      DB_USER=${PGUSER:-${POSTGRES_USER:-}}
      DB_PASS=${PGPASSWORD:-${POSTGRES_PASSWORD:-}}
      ;;
  esac

  HOSTPORT=${HOSTPART%%/*}
  DB_NAME=${HOSTPART#*/}
  DB_NAME=${DB_NAME%%\?*}

  case "$HOSTPORT" in
    *:*)
      DB_HOST=${HOSTPORT%%:*}
      DB_PORT=${HOSTPORT##*:}
      ;;
    *)
      DB_HOST=$HOSTPORT
      DB_PORT=5432
      ;;
  esac

  export SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
  if [ -n "${DB_USER}" ]; then
    export SPRING_DATASOURCE_USERNAME="$DB_USER"
  fi
  if [ -n "${DB_PASS}" ]; then
    export SPRING_DATASOURCE_PASSWORD="$DB_PASS"
  fi

  echo "Datasource configured for host=${DB_HOST} port=${DB_PORT} db=${DB_NAME} user=${DB_USER}"
fi

export SERVER_PORT="${PORT:-8080}"

exec java ${JAVA_OPTS:-} -Dserver.port="${SERVER_PORT}" -jar /app/app.jar
