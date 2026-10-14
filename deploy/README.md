# deploy/

The Docker Compose stack: Postgres with PostGIS, Keycloak with the two realms (`keycloak/realms`, placeholders
filled by `keycloak/entrypoint.sh`), MinIO (built from source, see `minio/Dockerfile`), Mailpit, the API, and
the dashboard as the `cms` profile.

## On your own machine

1. Install **Docker Desktop** (Mac or Windows) and start it.
2. Clone this repo, branch `kimleng`, and open a terminal in this `deploy/` folder.
3. `cp .env.example .env` (Windows PowerShell: `Copy-Item .env.example .env`), open `.env` and fill in the
   passwords at the top with anything you like, 16 characters or more. Leave the rest.
4. `docker compose up -d`. The first run builds the images and takes ten to fifteen minutes: the API is
   compiled, and so is MinIO, because MinIO no longer publishes ready images for its community edition. Watch
   with `docker compose logs -f api` until it says the catalogue was seeded; `docker compose ps` shows every
   service as healthy when it is ready.

You then have:

| What | Address | Notes |
| --- | --- | --- |
| The API | http://localhost:8080/api/v1 | its own docs at http://localhost:8080/swagger-ui.html |
| Keycloak | http://localhost:9090 | admin console with `KEYCLOAK_ADMIN_USER` / `KEYCLOAK_ADMIN_PASSWORD` from `.env` |
| Mailpit, the inbox | http://localhost:8025 | every verification code and password reset lands here |
| MinIO console | http://localhost:9001 | pictures; `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` |
| Postgres | localhost:5432 | database `ams_travel`, user `ams_user`, password `APP_DB_PASSWORD` |

The catalogue starts seeded (published places, tourism regions, provinces, corridors, amenities); stays,
food places and reviews start empty. The traveller `visitor@ams.test` and the staff `admin@ams.test` exist
with the `LOCAL_USER_PASSWORD` of `.env`. Signing in as staff asks for a six-digit code after the password;
Mailpit has it.

Stop with `docker compose down`; start again with `docker compose up -d`. `docker compose down -v`
wipes the databases and the pictures, and the next start seeds everything again.

### From an emulator or a phone

Inside an emulator, `localhost` is the emulator itself, not your computer:

| Where the app runs | API base URL | Keycloak address |
| --- | --- | --- |
| iOS simulator | http://localhost:8080/api/v1 | http://localhost:9090 |
| Android emulator | http://10.0.2.2:8080/api/v1 | http://10.0.2.2:9090 |
| A phone on the same Wi-Fi | http://YOUR-IP:8080/api/v1 | http://YOUR-IP:9090 |

Keycloak must know the address the app uses, because the tokens it issues carry it and the API checks
it. Set `KEYCLOAK_PUBLIC_URL` in `.env` to that address (for Android `http://10.0.2.2:9090`), then
`docker compose up -d` again.

### The dashboard (CMS)

Clone `ams-travel-admin-dashboard` next to this repo (same parent folder) and run
`docker compose --profile cms up -d`. It opens at http://localhost:3001 with `admin@ams.test`.

## On a server

Use `docker-compose.dokploy.yml` with `.env.dokploy.example` and `dashboard.env.example`: the steps are in
`DOKPLOY.md`. The mobile team integrates against that server; their guide is `../docs/mobile/README.md`.
