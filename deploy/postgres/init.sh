#!/bin/bash
# Runs once, when the Postgres volume is empty: the API's role and database with PostGIS,
# and Keycloak's own database. Passwords come from the compose env file.
set -euo pipefail
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<SQL
CREATE ROLE ams_user LOGIN PASSWORD '${APP_DB_PASSWORD}';
CREATE DATABASE ams_travel OWNER ams_user;
CREATE ROLE keycloak LOGIN PASSWORD '${KEYCLOAK_DB_PASSWORD}';
CREATE DATABASE keycloak OWNER keycloak;
SQL
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname ams_travel -c "CREATE EXTENSION IF NOT EXISTS postgis"
