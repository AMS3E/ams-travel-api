# Hosting on Dokploy

The backend (API, Keycloak, MinIO, Postgres) is one Dokploy **Compose** service built from this repo.
The CMS is a separate Dokploy **Application** built from the dashboard repo. Both on branch `kimleng`.
About an hour, most of it the first image build.

## 0. Before you start

- **Git access.** Dokploy → Settings → Git: connect GitHub (the GitHub App for the AMS3E organisation,
  or a token) so both repos can be cloned.
- **Three domain names**, pointed at the Dokploy server in DNS, for example
  `api.amstravel.amskh.co`, `auth.amstravel.amskh.co`, `cms.amstravel.amskh.co`.
- **The mail account**: host, port, user, app password, from address (the values from the old
  `deployment/.env`, proven on 2026-10-13).
- **Server room**: the first deploy compiles the API, MinIO and Keycloak's build step on the server.
  About 4 GB of free memory and 10 GB of disk. If the build dies, see "If something fails" below.

## 0b. How traffic reaches this Dokploy (learned 2026-10-13)

The company's Dokploy sits **behind an nginx gateway** at `167.179.14.44`, the address `deploy.amskh.co`
resolves to. The Dokploy server itself (`167.179.14.34`) is not reachable from the internet, and the
gateway answers 403 or a redirect for any hostname it has not been told about. So for every new domain:

- the DNS record points at the **gateway**, `167.179.14.44`, DNS only;
- the admin adds the hostname on the gateway, forwarded to the Dokploy server, the same way as the existing sites;
- Dokploy's "Validate DNS" keeps complaining that the record should point at `.34`; that is cosmetic.
- **HTTPS stays ON on all three domain cards.** The gateway holds its own certificate (a Let's Encrypt
  wildcard for `*.travel.ams.com.kh`) and forwards to Dokploy's *HTTPS* entrypoint. A card with HTTPS off
  only exists on Dokploy's HTTP entrypoint, so every request for it comes back as a plain
  "404 page not found" (seen 2026-10-14 on the CMS). Switch HTTPS on, save, redeploy, and it answers.
  Keycloak sees the public `https://` address through the forwarded headers (`KC_PROXY_HEADERS=xforwarded`).

## 1. The backend: a Compose service

1. Project "Three E" → environment → **Create Service → Compose**. Name it `ams-travel-backend`.
2. Provider: GitHub, repository `AMS3E/ams-travel-api`, branch `staging` (the branch the testing server runs; `kimleng` is the working branch it is cut from),
   compose path `deploy/docker-compose.dokploy.yml`.
3. **Environment** tab: paste `deploy/.env.dokploy.example` with every value filled in
   (your three domains, long random secrets, the mail account). Save.
4. **Domains** tab, add two. Before the first deploy the service list is empty ("Services not found"), because
   Dokploy has not cloned the repo yet: press **Manual** and type the service name.
   - the API domain → service `api`, container port `8080`, HTTPS on (Let's Encrypt);
   - the Keycloak domain → service `keycloak`, container port `8080`, HTTPS on.
   The Keycloak domain must be exactly the `KEYCLOAK_PUBLIC_URL` of the environment, since tokens carry it.
   The API domain is also the environment's `API_PUBLIC_URL` (added 2026-10-14): an earned badge's share
   link starts with it, so without the variable the links point at localhost.
   `SEED_RESET_TOKEN` (added 2026-10-14) is for the staging catalogue only: set it to a token nobody used
   before, redeploy, and the API wipes everything the seed wrote (staff edits to those rows included) and
   seeds the lively catalogue afresh; the token is remembered, so it never wipes twice.
5. **Deploy**. Watch the logs: `keycloak` imports the two realms once, `api` runs the schema and, with
   `SEED_CATALOG=true`, seeds the catalogue with pictures. Done when both report healthy.
6. Check in a browser:
   - `https://<auth domain>/realms/amstravel/.well-known/openid-configuration` shows the issuer with your domain;
   - `https://<api domain>/api/v1/search?q=angkor` lists places;
   - `https://<auth domain>/` opens the Keycloak admin console (`KEYCLOAK_ADMIN_USER` / `KEYCLOAK_ADMIN_PASSWORD`).

## 2. The CMS: an Application service

1. **Create Service → Application**. Name it `ams-travel-cms`.
2. Provider: GitHub, repository `AMS3E/ams-travel-admin-dashboard`, branch `kimleng`, **Build Path `/`**
   (the folder to build from, not the Dockerfile). Save, then under Build Type pick **Dockerfile** and leave
   its three fields empty (defaults: `Dockerfile`, context `.`, last stage).
3. **Environment**: paste `deploy/dashboard.env.example` filled in: your domains, and
   `KEYCLOAK_CLIENT_SECRET` = the `AMSADMIN_DASHBOARD_SECRET` of the backend.
4. **Domains**: the CMS domain → container port `3001`, HTTPS on. It must equal `DASHBOARD_URL` of the
   backend environment, because Keycloak only sends staff back to that address.
5. **Deploy**, then sign in at the CMS domain with `admin@ams.test` and `LOCAL_USER_PASSWORD`.

## 3. Content

Two choices for the catalogue the hosted CMS starts with:

- **The seed** (default, `SEED_CATALOG=true`): the 61 places, regions, provinces, corridors and
  amenities that ship with the API, with pictures. Simplest.
- **An export of the Mac** (`local/export-content.sh` on Kimleng's machine makes
  `local/exports/<date>/ams_travel.dump` and `ams-travel-images/`): deploy with `SEED_CATALOG=false`,
  then on the Dokploy server, with the export copied over by `scp`:

  ```bash
  # the database: into the compose's postgres container
  docker cp ams_travel.dump $(docker ps -qf name=ams-travel-postgres):/tmp/
  docker exec -it $(docker ps -qf name=ams-travel-postgres) \
    pg_restore -U postgres -d ams_travel --no-owner --no-privileges --clean --if-exists /tmp/ams_travel.dump
  # the pictures: into MinIO over the compose network (replace the two MinIO values)
  docker run --rm --network ams-travel_default -v "$PWD/ams-travel-images:/src:ro" rclone/rclone \
    --s3-provider Minio --s3-endpoint http://minio:9000 --s3-access-key-id MINIO_ROOT_USER --s3-secret-access-key MINIO_ROOT_PASSWORD \
    sync /src :s3:ams-travel-images
  docker restart $(docker ps -qf name=ams-travel-api)
  ```

  The network name is the compose project name plus `_default`; `docker network ls` shows it.

## 4. After the first deploy

- **Accounts.** The realms come with the test accounts (`admin@ams.test`, `bdo@`, `editor@`,
  `contributor@`, `partner@`, `visitor@`) on `LOCAL_USER_PASSWORD`. Create the real staff in the Keycloak
  console (realm `amsadmin`, Users), give them a desk role, then delete the test accounts you do not use.
  Every staff sign-in asks for a six-digit code sent to the account's email address (since 2026-10-14), so
  each real account needs a mailbox its owner reads; the test accounts' `@ams.test` addresses receive nothing.
- **Mail.** Both realms need the mail server: `amstravel` sends the registration codes, `amsadmin` the staff
  sign-in codes. Register a traveller once at `https://<auth domain>/realms/amstravel/account` to see the code
  arrive. A Gmail app password allows about 500 mails a day; move to a mail service before launch.
- **A realm imported before 2026-10-14** still signs staff in with the password alone: Keycloak imports a
  realm once. Run `local/keycloak-staff-code-flow.sh` against the auth domain with the admin login (it creates
  the flow "browser with email code", binds it, switches on brute-force protection and the 30-minute idle
  timeout, and can set the mail server). Or build it in the console: Authentication → Create flow → Cookie
  (Alternative), Identity Provider Redirector (Alternative), a sub-flow (Alternative) holding Username Password
  Form (Required) and Email code (Required) → Bind flow → Browser flow.
- **Backups.** The data lives in the Docker volumes `postgres-data` and `minio-data` of the compose
  service. Dokploy's volume backups, or a nightly `pg_dump` and `rclone sync` of the bucket, cover both.
- **Updates.** Push to `kimleng`, press Deploy on each service. Database changes run by themselves (Liquibase).

## If something fails

- **The build dies on the server** (killed, out of memory): raise the server's memory, or build the images
  elsewhere and let Dokploy pull them. The Dockerfile has three targets (`runtime`, `keycloak`) plus
  `deploy/minio`; a GitHub Actions workflow pushing them to GHCR is the next step if this bites.
- **Keycloak answers but the API says the token's issuer is wrong**: `KEYCLOAK_PUBLIC_URL` and the Keycloak
  domain differ. They must match character for character, `https://` included.
- **The CMS sign-in loops back to the login page**: `DASHBOARD_URL` in the backend environment is not the CMS
  domain, so Keycloak refuses the redirect. Fix the value and redeploy the backend (the realms are imported
  once; change the redirect in the Keycloak console under Clients → amsadmin-dashboard, or reset the volume).
- **Pictures do not load in the CMS**: `API_BASE_URL` of the dashboard must be the public API domain.
