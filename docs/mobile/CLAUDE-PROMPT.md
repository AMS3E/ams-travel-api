# Prompt for Claude Code (paste as the first message, from the mobile app's repo)

I am building the AMS Travel mobile app against the AMS Travel API. The backend repo is cloned at
`../ams-travel-api` (branch `kimleng`) and runs on my machine with Docker, as described in
`../ams-travel-api/docs/mobile/README.md`.

Before you write any code, read these three files in the backend repo and keep them as the source of truth:
1. `docs/mobile/README.md`: how the backend runs here, the addresses for the emulator, how sign-in works
   (Keycloak, authorization code with PKCE, client `amstravel-mobile`, redirect `amstravel://callback`),
   and the test accounts.
2. `docs/mobile/screens-to-api.md`: every screen of the app and the exact endpoint, parameters and fields
   behind each element. Follow it; do not invent endpoints or fields.
3. `docs/mobile/postman/AMS Travel Mobile.postman_collection.json`: a request example for every call.

Rules for this integration:
- Guest mode first: every read is public and needs no token. Signed-in calls send the bearer token.
- Lists are made of the "card" that `GET /api/v1/search` and `GET /api/v1/search/cards` return; a row never
  needs a second call. Paged answers are `{content, page}`.
- Never filter the catalogue on the phone: search, filters and suggestions are server calls.
- Stays show one total for the chosen nights from `GET /api/v1/stay-rooms/quote`, never a per-night price.
- A review is one `POST /api/v1/reviews` with rating, title, content, visitType, visitedOn and photo keys;
  photos are uploaded first with `POST /api/v1/media/files` (type `review`).
- The Menu screen of the design is out of scope. The OTP screen is Keycloak's page, not ours.
- If something in the design has no endpoint in `screens-to-api.md`, say so and ask Kimleng before building
  a workaround; do not change the backend repo.

Start by checking the backend is up (`GET http://localhost:8080/api/v1/search?q=angkor`, or the
emulator address from the README), then set up the API client and the sign-in flow in this app.
