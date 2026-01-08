#!/usr/bin/env bash
set -euo pipefail

DB_CONTAINER="local-db"
DB_NAME="ecommerce"
DB_USER="postgres"

EMAIL="master@example.com"
NICKNAME="master"
PHONE="010-1234-5678"
ROLE="MASTER"
PASSWORD_BCRYPT='$2a$12$Z.ubcT3NlI.0i5HIXaspgOL80XiMldT6izrtmlJHuVTQ9E/zDhbHq'

docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" <<SQL
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO p_user (user_id, email, password, nickname, phone_number, role, status, created_at, updated_at)
VALUES (
  gen_random_uuid(),
  '${EMAIL}',
  '${PASSWORD_BCRYPT}',
  '${NICKNAME}',
  '${PHONE}',
  '${ROLE}',
  'ACTIVE',
  NOW(),
  NOW()
);
SQL

echo "✅ Created MASTER user: ${EMAIL}"
