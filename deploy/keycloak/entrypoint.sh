#!/bin/bash
# Fills the realm files' placeholders from the environment, then hands over to Keycloak.
# The same two realm files serve a laptop, the compose stack and a hosted deployment; only
# the addresses, the SMTP server and the secrets differ, and those come from the env file.
set -euo pipefail
mkdir -p /opt/keycloak/data/import
for realm in /opt/keycloak/realm-templates/*.json; do
  sed -e "s|__SMTP_HOST__|${SMTP_HOST:-mailpit}|g" -e "s|__SMTP_PORT__|${SMTP_PORT:-1025}|g" \
      -e "s|__WEBSITE_URL__|${WEBSITE_URL:-http://localhost:3000}|g" -e "s|__DASHBOARD_URL__|${DASHBOARD_URL:-http://localhost:3001}|g" \
      -e "s|__SSL_REQUIRED__|${SSL_REQUIRED:-none}|g" \
      -e "s|__AMSTRAVEL_API_SECRET__|${AMSTRAVEL_API_SECRET:?set AMSTRAVEL_API_SECRET}|g" \
      -e "s|__AMSADMIN_API_SECRET__|${AMSADMIN_API_SECRET:?set AMSADMIN_API_SECRET}|g" \
      -e "s|__AMSADMIN_DASHBOARD_SECRET__|${AMSADMIN_DASHBOARD_SECRET:?set AMSADMIN_DASHBOARD_SECRET}|g" \
      -e "s|__LOCAL_USER_PASSWORD__|${LOCAL_USER_PASSWORD:?set LOCAL_USER_PASSWORD}|g" \
      "$realm" > "/opt/keycloak/data/import/$(basename "$realm")"
done
exec /opt/keycloak/bin/kc.sh "$@"
