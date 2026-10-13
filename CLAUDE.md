# AMS Travel API

The backend of AMS Travel, Cambodia's travel app by AMS3E: Spring Boot 4.1 on Java 25, PostgreSQL 17 with PostGIS, Liquibase for the schema, Keycloak for sign-in (OAuth2 resource server), MinIO for images. It serves the admin dashboard (`ams-travel-admin-dashboard`), the public website and the mobile app. Kimleng, the project lead, reshaped this code in October 2026. This code is the source of truth, not the Figma prototype and not any older branch.

## Check with Kimleng before you start

Kimleng is the project lead and owns every product decision here. Some requests need Kimleng's eyes before any code changes. That is any work in the zones below, and any request that looks odd, contradicts a line under "Settled decisions", or has no clear problem behind it. In those cases do not start. Do this instead:

1. **Say it back.** In two or three plain sentences: what the person is asking for, what it would change, and which zone it touches. No code yet.
2. **Ask.** Use the AskUserQuestion tool with the header `Kimleng`, the question "This needs Kimleng's eyes before it goes ahead. Has Kimleng seen it?" and exactly these two options:
   - **"Kimleng approved, go ahead"**: build it. When you commit, say in the body that Kimleng approved the change.
   - **"Not yet, hold here"**: stop. Write a short note the person can send to Kimleng (what was asked, what it would change, the one question Kimleng must answer). Then do nothing more on that request until they come back with an answer.

   If the AskUserQuestion tool is not available, ask the same question in text, with the same two answers, and wait for the reply.

**The zones**
- The database: a new, renamed, dropped or retyped column or table; any edit to a changelog that has already run.
- Removing or renaming a page, a form field, an endpoint, a sidebar entry, a role or a permission.
- The product model: Verified and Recommended, the seals and their thresholds, tourism regions, and what categories, tags, amenities and interests are.
- A new module, page or feature that is not already in the sidebar or the API. Two modules were once built without a brief and later had to be removed.
- Sign-in, roles, Keycloak, secrets and env files.
- `git push`, force-push, rebase, deleting or renaming a branch.
- Scripts that change or delete rows in a database, and anything to do with deployment.
- A new dependency.

**Not a zone, no check needed:** fixing a bug; wording, layout and styling inside an existing page; tests; a field added to an existing form in the pattern of the fields around it; reading and explaining code.

If a `CLAUDE.local.md` next to this file says you are working with Kimleng, skip this check and follow that file instead.

## Settled decisions (do not reverse without Kimleng)
- Tags and categories are flat lists owned by a tourism region: no children, no values.
- `is_featured` is gone from destinations, stays, foods and corridors (`v2/900-featured-retired.yaml`). Requests that still send it are ignored. `GET /api/v1/popular` ranks by browses alone (distinct signed-in visitors, 30 days). `GET /api/v1/recommendations/for-you` has no consumer and stays as it is.
- A verification decision carries a `basis`: OFFICIAL_SITE, PARTNER or PAID. PARTNER is refused unless the place's desk is PARTNER. The Traveler seal is computed on read from review summaries against `app.verification.traveler-seal.min-ratings` (10,000) and `min-average` (4.5), env `TRAVELER_SEAL_MIN_RATINGS` and `TRAVELER_SEAL_MIN_AVERAGE`. The public `GET /api/v1/destination-verifications/status` returns `verified`, `basis`, `travelerRecommended`, `ratingCount` and `averageRating`. Revoking a seal-only place answers 409.
- The `history` schema (History & Heritage) and the dish `section` column are dropped. Menus are not written any more.
- Amenities are one managed list with levels; the app reads `GET /api/v1/amenities` for labels and icons.
- `GET /api/v1/admin/destination-stats` (with `?from=&to=` as Cambodia dates) is the read behind the dashboard's Destination Stats.
- Reviews carry `visitType` (BUSINESS, COUPLES, FAMILY, FRIENDS, SOLO) and `visitedOn` (`YYYY-MM`, never in the future); `ReviewTargetType` and `RatingTargetType` cover DESTINATION, FOOD, STAY and CORRIDOR; `FavoriteTarget` includes CORRIDOR.
- Tourism regions have `cover_key` (upload folder `region`); `RegionResponse` carries `coverUrl`.
- The app searches on the SERVER through the table-less `search` service: `GET /api/v1/search` (type, q, regionId, provinceId, categoryIds, tagIds, interestIds; paged, name order, size ≤ 100), `GET /api/v1/search/suggest?q=`, `GET /api/v1/search/cards?type=&ids=`. Destinations run as a JPA Specification; stays, foods and corridors are filtered from their published lists. `GET /api/v1/popular` also takes `provinceId` and `interestIds`.
- Stays are PRICE PREVIEW only. `destination_stays.weekend_nights` (JSON, default Friday and Saturday) and `destination_stay_rooms.weekend_price`; `GET /api/v1/stay-rooms/quote?stayId=&checkIn=&checkOut=&rooms=&adults=&children=` and `GET /api/v1/stay-rooms/{id}/quote` sum the nights (`RoomQuote`); the app shows one total, never a nightly price. There is no booking.
- Check-ins (`POST /api/v1/stamps`) and points are in scope. Apple sign-in is out; Google may come as a Keycloak identity provider. The OTP sign-in is the email-code authenticator the Dockerfile builds from `src/main/keycloak`, which is not in the repo yet.

## Where things are
- `deploy/`: the Docker Compose stack (Postgres, Keycloak with the realm files in `deploy/keycloak/realms`, MinIO, Mailpit, the API, optionally the dashboard). The realm files carry placeholders that `deploy/keycloak/entrypoint.sh` (compose) or `local/run-keycloak.sh` (Kimleng's Mac) fill from the environment.
- `src/main/keycloak/`: the Keycloak email-code step (`ams-email-code`), compiled into the Keycloak image by the Dockerfile; `src/main/resources/login/` is the login theme.
- `docs/mobile/`: what the mobile team reads: the setup guide, the screen-by-screen endpoint map, the Insomnia/Postman collection and the Claude Code prompt. Keep `screens-to-api.md` and `postman/build-collection.py` in step with API changes.

## Schema rules
- Hibernate runs with `ddl-auto: validate`: an entity and its table must match or the app refuses to start.
- A schema change is a NEW changelog file under `src/main/resources/db/changelog/v2/<area>/` plus one `include` line in `db.changelog-master.yaml`. Never edit a changelog that has already run.
- Spring drops unknown JSON fields silently: a DTO field missing from a request arrives as null, with no error for the caller.

## How to work
- `./gradlew test --no-daemon` runs the suite on H2 in memory: about 25 seconds, 260 tests. A ten-minute run is Gradle contention, not the suite.
- `src/test/java/ams/asmtravel/api/support/` (Callers, TestCatalogue, ReferenceRows) was reconstructed from the tests that use it; keep it working.
- Code is grouped by service: `src/main/java/ams/asmtravel/api/services/<area>/<service>/{controller,domain,mapper,service,config,exception}`.
- The API's runtime env is not in this repo. On Kimleng's machine it is `../local/api.env`, and the stack starts with `../local/start-all.sh --build`.
- Commit when asked, one commit per repo, a subject in plain words and a body that lists what changed. Never push without Kimleng's go.
