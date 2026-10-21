# Round 4 for Claude Code (2026-10-22)

Developer: delete the folder where our API docs sit in your app repo and unzip this bundle in its place, then
tell Claude Code `Read CLAUDE-PROMPT-4.md and do what it says.` Everything below the line is for Claude Code.

---

The folder that holds this file was just replaced with the backend team's new bundle (it is written
`api-docs/` below; if yours carries another name, read that folder wherever `api-docs/` appears). Every file
in it is newer than what you read before, so read them again now and trust them over anything you remember or
noted from the old folder. It stays the source of truth.

**If round 3 is not finished yet, do `api-docs/CLAUDE-PROMPT-3.md` first** — everything in it still holds —
then come back here. This round is small: two changes on the corridor screens, nothing else moved.

Read first: `api-docs/screens-to-api.md`, the section "What changed on 2026-10-22" and the Corridors table.
Then re-import both files under `api-docs/postman/` (the corridor requests' answers gained fields).

**Before anything else, one check against the server.** `GET https://api.travel.ams.com.kh/api/v1/corridors`
must answer rows that carry the keys `expectations`, `nameKm`, `shortDescriptionKm` and `descriptionKm`.
Until it does, change nothing and tell me — the server has not been deployed yet.

**No reseed this round: every id you have still stands.**

## The two changes

**1. "What you expect" comes from the corridor itself.** The corridor's `expectations` is a list of short
labels in the order the CMS set ("Sunrise", "Photography", "Historic Site"). Render that list as the
"What you expect" chips on the corridor page: plain text, no icons, CMS order, and hide the whole row while
the list is empty — an empty list is a normal state, not an error. If the app currently builds this row from
the corridor's `interestIds`, remove that: it was a stand-in and no longer matches the product.

**2. Corridors in Khmer.** With the app's language on Khmer, corridors behave like every other kind now:
`?lang=km` on `/search`, `/search/cards` and `/search/suggest` answers corridor cards and suggestions in
Khmer where written (`untranslated` names what is still English — the stop names always are); the corridor
detail (`GET /corridors/{id}`, and the `corridor` inside `/corridors/{id}/itinerary`) carries `nameKm`,
`shortDescriptionKm` and `descriptionKm` beside the English. Fall back to the English field when the Khmer
one is null, exactly as the app already does for attractions.

Show each corridor screen working against the server before moving on, as in every round. Anything the API
lacks goes into `api-docs/API-REQUESTS.md` as a new entry, never a workaround in the app.
