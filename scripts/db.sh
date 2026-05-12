#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-postgresql}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-postgres}"
DB_NAME="${DB_NAME:-kotlin_spring_development}"
PGPASSWORD="${PGPASSWORD:-${DB_PASSWORD:-postgres}}"

export PGPASSWORD

usage() {
  cat <<'USAGE'
Usage:
  scripts/db.sh create   Create the development database if it does not exist
  scripts/db.sh drop     Drop the development database
  scripts/db.sh reset    Drop and create the development database
  scripts/db.sh migrate  Run Flyway migrations by starting Spring Boot without the web server
  scripts/db.sh psql     Open psql for the development database

Environment variables:
  DB_HOST       default: postgresql
  DB_PORT       default: 5432
  DB_USER       default: postgres
  DB_NAME       default: kotlin_spring_development
  DB_PASSWORD   default: postgres
USAGE
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    echo "Install postgresql-client in the devcontainer and try again." >&2
    exit 1
  fi
}

psql_postgres() {
  psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres "$@"
}

database_exists() {
  local exists
  exists="$(
    psql_postgres -tAc "select 1 from pg_database where datname = '$DB_NAME'" 2>/dev/null || true
  )"
  [[ "$exists" == "1" ]]
}

create_db() {
  require_command createdb
  require_command psql

  if database_exists; then
    echo "Database already exists: $DB_NAME"
    return
  fi

  createdb -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$DB_NAME"
  echo "Created database: $DB_NAME"
}

drop_db() {
  require_command dropdb
  require_command psql

  if ! database_exists; then
    echo "Database does not exist: $DB_NAME"
    return
  fi

  psql_postgres -v ON_ERROR_STOP=1 -c \
    "select pg_terminate_backend(pid) from pg_stat_activity where datname = '$DB_NAME' and pid <> pg_backend_pid();" \
    >/dev/null
  dropdb --if-exists -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$DB_NAME"
  echo "Dropped database: $DB_NAME"
}

reset_db() {
  drop_db
  create_db
  echo "Run './scripts/migrate-db.sh' or './gradlew bootRun' to apply Flyway migrations."
}

migrate_db() {
  ./gradlew bootRun --args='--spring.main.web-application-type=none --spring.main.banner-mode=off'
}

open_psql() {
  require_command psql
  psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME"
}

case "${1:-}" in
  create)
    create_db
    ;;
  drop)
    drop_db
    ;;
  reset)
    reset_db
    ;;
  migrate)
    migrate_db
    ;;
  psql)
    open_psql
    ;;
  help|-h|--help|"")
    usage
    ;;
  *)
    echo "Unknown command: $1" >&2
    usage >&2
    exit 1
    ;;
esac
