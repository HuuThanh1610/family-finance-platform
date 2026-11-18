#!/usr/bin/env bash
# Apply Flyway-style SQL migrations via psql (dev verification without Spring Boot).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
HOST="${POSTGRES_HOST:-localhost}"
PORT="${POSTGRES_PORT:-5432}"
SUPERUSER="${POSTGRES_SUPERUSER:-postgres}"
SUPERPASSWORD="${POSTGRES_SUPERPASSWORD:-changeme_postgres_super}"

export PGPASSWORD="$SUPERPASSWORD"

apply_db() {
  local db="$1"
  local dir="$2"
  echo "==> Migrating $db from $dir"
  for file in $(ls "$dir"/V*.sql | sort -V); do
    echo "    -> $(basename "$file")"
    psql -h "$HOST" -p "$PORT" -U "$SUPERUSER" -d "$db" -v ON_ERROR_STOP=1 -f "$file" >/dev/null
  done
}

apply_db profile_db "$ROOT/services/profile-service/src/main/resources/db/migration"
apply_db family_db "$ROOT/services/family-service/src/main/resources/db/migration"
apply_db wallet_db "$ROOT/services/wallet-service/src/main/resources/db/migration"
apply_db payment_db "$ROOT/services/payment-service/src/main/resources/db/migration"
apply_db budget_db "$ROOT/services/budget-service/src/main/resources/db/migration"
apply_db expense_db "$ROOT/services/expense-service/src/main/resources/db/migration"
apply_db household_db "$ROOT/services/household-service/src/main/resources/db/migration"
apply_db notification_db "$ROOT/services/notification-service/src/main/resources/db/migration"
apply_db analytics_db "$ROOT/services/analytics-service/src/main/resources/db/migration"

echo "All migrations applied."
