# deploy/

The Docker Compose stack: Postgres with PostGIS, Keycloak with the two realms (`keycloak/realms`, placeholders
filled by `keycloak/entrypoint.sh`), MinIO (built from source, see `minio/Dockerfile`), Mailpit, the API, and
the dashboard as the `cms` profile.

```bash
cp .env.example .env     # fill in the passwords
docker compose up -d
```

The full guide, written for the mobile team, is `../docs/mobile/README.md`. The same files serve a hosted
deployment: set `KEYCLOAK_PUBLIC_URL`, `WEBSITE_URL`, `DASHBOARD_URL` to the public addresses and
`KEYCLOAK_SSL_REQUIRED=external`, and replace Mailpit's SMTP settings in the realm files with a real mail server.
