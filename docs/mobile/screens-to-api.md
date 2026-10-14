# Mobile app screens → API

Written 2026-10-12 after the server search, the price preview, the region cover and the review visit fields were built; updated 2026-10-14 for the hosted API. The seed for the Postman collection (`postman/`). Source: the Figma "High Fidelity Version 3" screens (ask Kimleng for them), checked against the API on branch `kimleng`. Base URL `https://api.travel.ams.com.kh/api/v1`; the paths below are relative to it. "Public" means no token. Everything else needs a Keycloak access token from the `amstravel` realm (public client `amstravel-mobile`, PKCE; see `README.md`). The Menu screen is out of scope by Kimleng's decision.

Paged answers look like `{"content": [...], "page": {"size": 20, "number": 0, "totalElements": 61, "totalPages": 4}}`.

Legend: ✅ exists and fits · ⚠️ exists with a gap · ❌ nothing behind it

## Sign-in (Keycloak, not this API)

| Screen | Behind it | State |
| --- | --- | --- |
| Register, Log in, Forgot password | Keycloak `amstravel` realm: registration on, login with email on, reset on | ✅ through Keycloak's pages |
| Continue with Google | a Keycloak identity provider, once a Google OAuth client exists | ⚠️ not configured yet |
| Continue with Apple | dropped | out of scope |
| Verify OTP (6-digit email code) | Keycloak's registration flow ends with the email-code step (`src/main/keycloak`): the code is emailed to the address typed, entered on Keycloak's page; resend after 60 s, 5 tries, valid 5 minutes | ✅ through Keycloak's pages |
| Explore as Guest | every read marked public below | ✅ |
| After sign-in | `GET /users/me`, `GET /preferences/me` | ✅ |

## The one card every list is made of

`GET /search`, `GET /search/cards` and the filtered lists all return the same **card**: `type`, `id`, `name`, `slug`, `shortDescription`, `coverUrl`, `provinceId`, `provinceName`, `latitude`, `longitude`, `categoryIds`, `categoryNames`, `interestIds`, `tourismRegionIds`, `averageRating`, `ratingCount`, `reviewCount`, `verified`, `basis`, `travelerRecommended`, `priceFrom`, `currency`, `durationDays`. A row in the app needs nothing else. Fields a kind does not have are null (a stay has no slug, a corridor no pin or province, only a stay or food a price, only a corridor a duration).

## Explore

| Element | Endpoint | State |
| --- | --- | --- |
| Chip row: Attraction Site, Stays, Food, Water, Activities, Corridors | `GET /interests` (public) grouped by `groupName`: the six groups are exactly these | ✅ |
| Explore by Tourism Region cards | `GET /regions?kind=TOURISM` (public) → `name`, `coverUrl`, `description` | ✅ |
| "Recommend by Traveler" on a region card | nothing: the seal is per destination | design filler |
| Explore by Popular | `GET /popular?limit=10` (public; `regionId`, `provinceId`, `interestIds` optional) → ids, then `GET /search/cards?ids=` for the rows | ✅ |
| Recording a browse (feeds Popular) | `POST /popular/events` `{destinationId, eventType, sessionId}` (public, counts when signed in); or `PUT /badges/me/explorations/{destinationId}` when signed in (also the Browser badge) | ✅ |
| Location, notification bell | device location; `GET /notifications?unreadOnly=true&limit=1` | ✅ |

## Search

| Element | Endpoint | State |
| --- | --- | --- |
| Keyword search | `GET /search?q=angkor&page=0&size=20` (public): published places only, name order, size ≤ 100 | ✅ |
| Kind chips (All, Stays, Food, Corridors) | `GET /search?type=DESTINATION|STAY|FOOD|CORRIDOR` | ✅ |
| Filters: Tourism Regions, Provinces, Interests, Categories, Tags | `GET /search?regionId=&provinceId=&interestIds=a,b&categoryIds=&tagIds=`; the chips come from `GET /regions?kind=TOURISM`, `GET /provinces`, `GET /interests`, `GET /categories`, `GET /tags` (all public) | ✅ |
| Typing suggestions ("Angkor Wat", "Battambang") | `GET /search/suggest?q=ang&limit=8` (public) → `kind` (DESTINATION, STAY, FOOD, CORRIDOR, PROVINCE, REGION), `id`, `name`, `slug`; names starting with the text first | ✅ |
| Recent keywords | on the device | ✅ |
| Explore by Nearby | `GET /destinations/nearby?latitude=&longitude=&radiusKm=25&limit=20` (public) → ids, then `GET /search/cards?ids=` | ✅ |
| Recommend Corridors | `GET /search?type=CORRIDOR` (public) | ✅ (the seal and review count on a corridor are filler until corridors have reviews) |

## Tourism Regions

| Element | Endpoint | State |
| --- | --- | --- |
| Region list with three places each | `GET /regions?kind=TOURISM` + `GET /search?regionId=&size=3` per region | ✅ |
| Region detail: banner, name, overview | `GET /regions/{id}` → `coverUrl`, `name`, `description` (HTML) | ✅ |
| Historical Map | the cards' `latitude`/`longitude` from `GET /search?regionId=&size=100` | ✅ |
| Categories and Tags chips | `GET /regions/{id}` → `categoryIds`, `tagIds` resolved against `GET /categories`, `GET /tags` | ✅ flat lists, no values |
| Destination list with category line and seal | `GET /search?regionId=` → `categoryNames`, `verified`, `travelerRecommended` on every card | ✅ |

## Destination detail (attraction)

| Element | Endpoint | State |
| --- | --- | --- |
| Cover and gallery | `GET /destinations/{id}` → `coverUrl`; `GET /media?ownerType=DESTINATION&ownerId=` (public) | ✅ |
| Name, categories, slogan quote and its author, description, address, map pin | `GET /destinations/{id}` → `name`, `categoryIds`, `slogan`, `sloganBy`, `description`, `address`, `latitude`, `longitude` | ✅ |
| Rating, review count, seal | `GET /search/cards?ids={id}` gives all three in one call; or `GET /ratings/summary?targetType=DESTINATION&targetIds=`, `GET /reviews/summary?...`, `GET /destination-verifications/status?destinationId=` | ✅ |
| What Travelers Say, Show all reviews | `GET /reviews?targetType=DESTINATION&targetId=&page=&size=` (public) → author, rating, title, content, `visitType`, `visitedOn`, photos | ✅ |
| Recommend Nearby | `GET /destinations/nearby?latitude=&longitude=` with the place's own coordinates, then `GET /search/cards?ids=` | ✅ |
| Heart (save), Share | `POST /favorites` `{targetType: DESTINATION, targetId}`, `DELETE /favorites/DESTINATION/{id}` (signed in); share = app link | ✅ |
| How to get there (route, time, distance) | device maps SDK from the user's location to the pin | ✅ client side |
| Highlights / About sheet | the same `description` | design filler |
| Write a Review | `POST /reviews` `{targetType, targetId, rating 1–5, title ≤200, content ≤5000, visitType, visitedOn "YYYY-MM", photoKeys[]}`; photos first via `POST /media/files` (type `review`) | ✅ |
| Check in ("I'm here", the Adventure badge) | `POST /stamps` with the device position; the API answers with the distance and the radius it allows | ✅ needs a screen in the design |

## Popular page

| Element | Endpoint | State |
| --- | --- | --- |
| Long list filtered by province chips and kind chips | `GET /popular?provinceId=&interestIds=&limit=50` → ids, then `GET /search/cards?ids=`; `interestIds` of the chosen group give the kind chips | ✅ |
| Results state (typing) | `GET /search/suggest` and `GET /search?q=` | ✅ |

## Interests, Stays and Food

| Element | Endpoint | State |
| --- | --- | --- |
| Interest chips and grid | `GET /interests` (public); `GET /search?interestIds=` | ✅ |
| Stay detail: cover, gallery, name, star rating, slogan, description, policies, check-in/out, phone, website, amenities | `GET /stays/{id}` (public) → `starRating`, `slogan`, `description`, `policies`, `checkInFrom`, `checkOutUntil`, `phone`, `website`, `amenities` (slugs → `GET /amenities` for label, icon, group), `weekendNights`; gallery `GET /media?ownerType=STAY&ownerId=` | ✅ |
| "Open 24h" on a stay | nothing: stays have check-in hours, not opening hours | design filler |
| Stays near a destination | `GET /stays?near={destinationId}` (public) | ✅ |
| Rooms list and Room Details (capacity, size, breakfast, amenities by group, photos) | `GET /stay-rooms?stayId=` (public) → `maxAdults`, `maxChildren`, `sizeSqm`, `breakfastIncluded`, `amenities`, `photoUrls`, `description`, `pricePerNight`, `weekendPrice`; groups from `GET /amenities` | ✅ |
| Dates, rooms, adults, children picker and the price | **Price preview, no booking.** `GET /stay-rooms/quote?stayId=&checkIn=2026-10-16&checkOut=2026-10-18&rooms=1&adults=2&children=0` (public; dates default to tonight) → `nights`, `currency`, `fromTotal`, and per room `total`, `weekendNightCount`, `perNight[]`, `fitsGuests`, `enoughRooms`, `meetsMinNights`, `bookable`. The app prints "US$300 total for 2 nights" and never a nightly price. One room: `GET /stay-rooms/{id}/quote?...` | ✅ |
| Stay rating and "What Travelers Say" | `GET /ratings/summary?targetType=STAY`, `GET /reviews?targetType=STAY&targetId=` | ✅ |
| Food detail: cover, name, price range, hours (open/closed), cuisines, meals served, phone, website, slogan, description, map, nearby | `GET /foods/{id}` (public) → `priceRange`, `openingHours` (map day → hours), `cuisines`, `mealsServed`, `phone`, `website`, `slogan`, `description`, `latitude`, `longitude`, `averageRating`, `reviewCount` | ✅ |
| Food near a destination | `GET /foods?near={destinationId}` (public) | ✅ |
| Menu | out of scope | skip |

## Provinces

| Element | Endpoint | State |
| --- | --- | --- |
| Province list, "Show all 25 provinces" | `GET /provinces` (public), 25 rows | ✅ |
| Places in a province, kind and interest chips | `GET /search?provinceId=&interestIds=` | ✅ |

## Map

| Element | Endpoint | State |
| --- | --- | --- |
| Pins around the user, Explore by Nearby | `GET /destinations/nearby` then `GET /search/cards?ids=` | ✅ |
| Filter chips (regions, provinces, interests, corridors) | `GET /search?...&size=100` and read the cards' pins | ✅ |
| Corridor lines on the map | `GET /corridors/{id}/itinerary` → stops with `routePolyline`; the corridor's `encodedPolyline` (Google encoded) | ✅ |
| Route to a place, Start Route | device maps SDK | ✅ client side |

## Corridors

| Element | Endpoint | State |
| --- | --- | --- |
| List: name, duration, cover | `GET /search?type=CORRIDOR` → `name`, `durationDays`, `coverUrl`, ratings | ✅ |
| "What you expect" chips | `interestIds` → `GET /interests` | ✅ |
| About, Route Overview (stops with description), Corridors Map | `GET /corridors/{id}/itinerary` → days, stops (sequence, destination, title, description, arrival, departure, distance, travel minutes, polyline) | ✅ |
| Gallery | `GET /media?ownerType=CORRIDOR&ownerId=` | ✅ |
| Rating, reviews and the Traveler seal on a corridor | `GET /reviews?targetType=CORRIDOR&targetId=`, `POST /reviews` with `targetType: CORRIDOR`; the seal stays a destination thing | ✅ reviews; seal is filler |
| Heart on a corridor | `POST /favorites` `{targetType: CORRIDOR, targetId}` | ✅ |

## Saves

| Element | Endpoint | State |
| --- | --- | --- |
| All saved places | `GET /favorites` (signed in) → `targetType`, `targetId`, then `GET /search/cards?type=&ids=` per type for the rows | ✅ |
| Tabs Regions / Provinces / Interests | group the cards by `tourismRegionIds`, `provinceId`, `interestIds` | ✅ |

## Notifications

| Element | Endpoint | State |
| --- | --- | --- |
| List, All / Unread, mark read | `GET /notifications?unreadOnly=&limit=`, `PUT /notifications/{id}/read`, `PUT /notifications/read-all`, `DELETE /notifications/{id}` (signed in) | ✅ |
| "New places added in Siem Reap", "Complete your profile", "New information updated" | nothing produces them yet: the only types are PUBLISH_APPROVED / REJECTED / CHANGES_REQUESTED, which are staff events | ❌ parked |
| Push delivery | no FCM / APNs integration; `notificationPush` is only stored | ❌ parked |

## Profile and Account

| Element | Endpoint | State |
| --- | --- | --- |
| Name, photo, phone, joined date | `GET /users/me` → `fullName`, `avatarUrl`, `phone`, `createdAt`; `PUT /users/me` `{fullName, phone, avatarKey}`; photo via `POST /media/files` (type `avatar`) | ✅ |
| First name / Last name split, home province, "80% profile completed" | nothing: one `fullName`, no home province, no completion score | ❌ |
| Email and password change | Keycloak account, not this API | ⚠️ decide the flow |
| Review (4) tile | `GET /reviews/me` (signed in) | ✅ |
| Check-ins and points | `GET /stamps/me` (collected stamps), `GET /points/me` (signed in) | ✅ need a place in the Profile design |
| Sign out | token revocation on the device | ✅ |

## Badges

| Element | Endpoint | State |
| --- | --- | --- |
| Badge overview, current status, progress bar, "this season" | `GET /badges/me/progress?seasonId=` (signed in) → tracks, badges, earned; `GET /seasons/current` (public) | ✅ |
| Badge categories Browser / Adventure / Reviewer | `GET /badges` (public): BROWSER, REVIEWER, ADVENTURE and POINTS, each SILVER / GOLDEN / PLATINUM | ⚠️ design says Gold / Silver / Bronze; the API has Silver, Golden, Platinum and a Points track |
| Tier thresholds ("300 browses required") | each badge's `threshold` | ✅ |
| Adventure badge | counts GPS check-ins: `POST /stamps`, `GET /stamps/me` | ✅ needs the check-in screen |
| Achievement, Share to Facebook, View badge history | `GET /badges/me/earned`; `GET /badges/shared/{userBadgeId}` (public page data); `GET /seasons/me/history` | ✅ |
| "Top 5% of active members" | nothing | ❌ |

## Reviews (mine)

| Element | Endpoint | State |
| --- | --- | --- |
| Reviews written, average rating, list with sort | `GET /reviews/me?page=&size=&sort=` (signed in); average from the rows | ✅ |
| Review detail: stars, date, kind of visit, visit month, title, body, photos | `GET /reviews/{id}` → `rating`, `createdAt`, `visitType`, `visitedOn`, `title`, `content`, `photos` | ✅ |
| Edit, delete | `PUT /reviews/{id}`, `DELETE /reviews/{id}` | ✅ |
| Search for a place to review | `GET /search/suggest?q=` | ✅ |

## Preferences

| Element | Endpoint | State |
| --- | --- | --- |
| Language (EN / KH), Currency (KHR / USD), Notification on/off | `GET /preferences/me`, `PUT /preferences/me` `{preferredLanguage, preferredCurrency, preferredRegionId, notificationEmail, notificationPush}` | ✅ |
| Units (km / mi) | nothing | ❌ (theme and preferred region exist instead, unused by the design) |

## Help & Support, Terms & Conditions

Static text and links. No endpoint; ship them in the app, or add a small settings read later if they must change without a release.

## In the API, absent from the design (decided 2026-10-12)

- Check-ins (`POST /stamps`) and points (`GET /points/me`) are IN: the designer adds a check-in action on the destination page and a stamps and points area in Profile.
- `GET /recommendations/for-you` (personal recommendations): no screen; left as is.
- Seasons (`GET /seasons`, `/current`, `/me/progress`, `/me/history`): referenced by the badge screen only.
- Food: 40+ fields served, about 10 read by the design. Stay: 20 policy fields served, the design shows one policies box plus check-in/out. Room: 50 fields served, about 10 read. The Postman collection documents what the app reads.
- Preferences `theme`, `preferredRegionId`.

## Content on the server today (2026-10-14)

59 published destinations with covers, 5 corridors, 25 provinces, 14 tourism regions, 33 interests in 6 groups, 65 categories, 53 tags, 72 amenities, 12 badge definitions, 4 seasons. Zero stays, zero foods, zero reviews, zero ratings. Every stay, food, review and seal screen is empty until the staff add that content in the CMS.
