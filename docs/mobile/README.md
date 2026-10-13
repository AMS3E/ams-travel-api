# AMS Travel: the backend on your machine, for the mobile app

Everything the app talks to runs on your own computer from this repo: the API, Keycloak (sign-in),
Postgres, MinIO (pictures) and Mailpit (a local inbox that catches every email). Nothing needs an
account anywhere. Read `screens-to-api.md` for what each screen calls, and `postman/` for a ready
collection to try the calls in Insomnia or Postman.

## 1. Set up, once

1. Install **Docker Desktop** (Mac or Windows) and start it.
2. Clone this repo, branch `kimleng`, and open a terminal in its `deploy/` folder.
3. `cp .env.example .env` (Windows PowerShell: `Copy-Item .env.example .env`), open `.env` and fill in the
   passwords at the top with anything you like, 16 characters or more. Leave the rest.
4. `docker compose up -d`. The first run builds the images and takes ten to fifteen minutes: the API is
   compiled, and so is MinIO, because MinIO no longer publishes ready images for its community edition. Watch with
   `docker compose logs -f api` until it says the catalogue was seeded; `docker compose ps` shows every
   service as healthy when it is ready.

You then have, on your machine:

| What | Address | Notes |
| --- | --- | --- |
| The API | http://localhost:8080/api/v1 | its own docs at http://localhost:8080/swagger-ui.html |
| Keycloak | http://localhost:9090 | admin console with `KEYCLOAK_ADMIN_USER` / `KEYCLOAK_ADMIN_PASSWORD` from `.env` |
| Mailpit, the inbox | http://localhost:8025 | every verification code and password reset lands here |
| MinIO console | http://localhost:9001 | pictures; `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` |
| Postgres | localhost:5432 | database `ams_travel`, user `ams_user`, password `APP_DB_PASSWORD` |

The catalogue starts with 61 published places, 9 tourism regions, 25 provinces, 5 corridors and the
amenities. Stays, food places and reviews start empty.

Stop with `docker compose down`; start again with `docker compose up -d`. `docker compose down -v`
wipes the databases and the pictures, and the next start seeds everything again.

## 2. Reaching it from the emulator or a phone

Inside an emulator, `localhost` is the emulator itself, not your computer:

| Where the app runs | API base URL | Keycloak address |
| --- | --- | --- |
| iOS simulator | http://localhost:8080/api/v1 | http://localhost:9090 |
| Android emulator | http://10.0.2.2:8080/api/v1 | http://10.0.2.2:9090 |
| A phone on the same Wi-Fi | http://YOUR-IP:8080/api/v1 | http://YOUR-IP:9090 |

Keycloak must know the address the app uses, because the tokens it issues carry it and the API checks
it. Set `KEYCLOAK_PUBLIC_URL` in `deploy/.env` to that address (for Android `http://10.0.2.2:9090`),
then `docker compose up -d` again. Picture links in API answers are relative paths; the app puts its
base URL in front of them.

## 3. Guest and signed-in

A guest needs no token: every read the app makes is public (search, places, stays, food, corridors,
regions, provinces, interests, popular, nearby, the price quote, galleries). Browses can be reported
without signing in too.

A signed-in traveller sends `Authorization: Bearer <access token>` on every request. The first request
with a new token creates the traveller's row in the API by itself; nothing to register on the API side.

### Signing in from the app

The app signs in through Keycloak's own pages with the standard **authorization code + PKCE** flow,
using the system browser. Use AppAuth (`react-native-app-auth`, `flutter_appauth`, AppAuth-iOS,
AppAuth-Android) with these values:

| Setting | Value |
| --- | --- |
| Issuer / discovery | `<Keycloak address>/realms/amstravel` (its `.well-known/openid-configuration` is served) |
| Client id | `amstravel-mobile` |
| Redirect URI | `amstravel://callback` (register the `amstravel` URL scheme in the app) |
| Scopes | `openid profile email offline_access` |
| PKCE | S256, required |

Keycloak's pages handle sign-in, **registration**, forgot-password, and the **six-digit email code**
after registration (the app's Verify OTP screen): the code arrives in Mailpit at http://localhost:8025.
The pages carry the AMS Travel look. To open registration directly, send the user to the issuer's
`/protocol/openid-connect/registrations` endpoint with the same parameters as the authorize call.

Refresh tokens come back with the token; refresh at `<issuer>/protocol/openid-connect/token`
(`grant_type=refresh_token`). Sign out with the `end_session_endpoint` from the discovery document.

### Test accounts

`visitor@ams.test` is a traveller; the password is the `LOCAL_USER_PASSWORD` you set in `.env`.
Registering new accounts works the same as in the real app. The staff accounts (`admin@ams.test` and
others) belong to the back-office realm `amsadmin` and are not for the app.

### Trying calls without the app

Insomnia or Postman: import `postman/` (see its README). Its "Get a visitor token" request uses a
development shortcut, the password grant on the `local-cli` client, so you can test signed-in calls
without the app's sign-in screen.

## 4. The dashboard (CMS), optional

To add or edit content locally, clone `ams-travel-admin-dashboard` next to this repo (same parent
folder) and run `docker compose --profile cms up -d`. It opens at http://localhost:3001 with the
staff account `admin@ams.test` and the same `LOCAL_USER_PASSWORD`.

## 5. Working with Claude Code

This repo has a `CLAUDE.md` with the rules of the code. For the app work, paste the prompt in
`CLAUDE-PROMPT.md` into Claude Code as your first message, from your app's own repo folder.
