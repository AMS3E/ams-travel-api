#!/bin/bash
# Fills the realm files' placeholders from the environment, then hands over to Keycloak.
# The same two realm files serve a laptop, the compose stack and a hosted deployment; only
# the addresses, the SMTP server and the secrets differ, and those come from the env file.
set -euo pipefail
# A value with |, & or \ in it would break sed; escape those.
esc() { printf '%s' "$1" | sed -e 's/[|&\\]/\\&/g'; }
mkdir -p /opt/keycloak/data/import
for realm in /opt/keycloak/realm-templates/*.json; do
  sed -e "s|__SMTP_HOST__|${SMTP_HOST:-mailpit}|g" -e "s|__SMTP_PORT__|${SMTP_PORT:-1025}|g" \
      -e "s|__SMTP_FROM__|$(esc "${SMTP_FROM:-noreply@amstravel.local}")|g" -e "s|__SMTP_FROM_NAME__|$(esc "${SMTP_FROM_NAME:-AMS Travel}")|g" \
      -e "s|__SMTP_AUTH__|${SMTP_AUTH:-false}|g" -e "s|__SMTP_USER__|$(esc "${SMTP_USER:-}")|g" -e "s|__SMTP_PASSWORD__|$(esc "${SMTP_PASSWORD:-}")|g" \
      -e "s|__SMTP_SSL__|${SMTP_SSL:-false}|g" -e "s|__SMTP_STARTTLS__|${SMTP_STARTTLS:-false}|g" \
      -e "s|__WEBSITE_URL__|${WEBSITE_URL:-http://localhost:3000}|g" -e "s|__DASHBOARD_URL__|${DASHBOARD_URL:-http://localhost:3001}|g" \
      -e "s|__SSL_REQUIRED__|${SSL_REQUIRED:-none}|g" \
      -e "s|__AMSTRAVEL_API_SECRET__|$(esc "${AMSTRAVEL_API_SECRET:?set AMSTRAVEL_API_SECRET}")|g" \
      -e "s|__AMSADMIN_API_SECRET__|$(esc "${AMSADMIN_API_SECRET:?set AMSADMIN_API_SECRET}")|g" \
      -e "s|__AMSADMIN_DASHBOARD_SECRET__|$(esc "${AMSADMIN_DASHBOARD_SECRET:?set AMSADMIN_DASHBOARD_SECRET}")|g" \
      -e "s|__LOCAL_USER_PASSWORD__|$(esc "${LOCAL_USER_PASSWORD:?set LOCAL_USER_PASSWORD}")|g" \
      "$realm" > "/opt/keycloak/data/import/$(basename "$realm")"
done
exec /opt/keycloak/bin/kc.sh "$@"
