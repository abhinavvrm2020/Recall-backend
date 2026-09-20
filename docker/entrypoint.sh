#!/bin/sh
set -eu

# Railway / Heroku-style DATABASE_URL → Spring JDBC URL
if [ -n "${DATABASE_URL:-}" ]; then
  JDBC_URL="$DATABASE_URL"
  case "$JDBC_URL" in
    postgres://*|postgresql://*)
      JDBC_URL=$(printf '%s' "$JDBC_URL" | sed -e 's#^postgres://#jdbc:postgresql://#' -e 's#^postgresql://#jdbc:postgresql://#')
      ;;
  esac
  export SPRING_DATASOURCE_URL="$JDBC_URL"
fi

# Railway sets PORT; Spring Boot uses server.port
export SERVER_PORT="${PORT:-8080}"

exec java ${JAVA_OPTS:-} -Dserver.port="${SERVER_PORT}" -jar /app/app.jar
