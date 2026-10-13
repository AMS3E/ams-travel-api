# deploy/

The Docker Compose stack: Postgres with PostGIS, Keycloak with the two realms (`keycloak/realms`, placeholders
filled by `keycloak/entrypoint.sh`), MinIO (built from source, see `minio/Dockerfile`), Mailpit, the API, and
the dashboard as the `cms` profile.

```bash
cp .env.example .env     # fill in the passwords
docker compose up -d
```

The full guide, written for the mobile team, is `../docs/mobile/README.md`. For a server, use
`docker-compose.dokploy.yml` with `.env.dokploy.example` and `dashboard.env.example`: the steps are in `DOKPLOY.md`.
