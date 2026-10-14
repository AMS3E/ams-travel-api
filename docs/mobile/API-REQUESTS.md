# API requests from the mobile app

This file is the one channel through which the app asks the backend for a change. The app follows the
Figma screens and the API follows its data model, so in places they disagree: a field a screen needs that
no endpoint returns, a value in the wrong shape, a filter a list lacks, an action with no endpoint. The app
never patches around such a gap on the phone. The need is written here instead, Kimleng takes this file
to the backend, and the backend answers each entry in its **Status** line.

## How it works

1. Copy this file once into the root of the app repo and keep it there, under version control.
2. One entry per need, in the shape below, appended under **Entries**, newest last. Fill every line;
   "unknown" is an answer too. Never delete an answered entry.
3. On the screen, leave the element as a `TODO R-nnn` naming the entry, make it degrade gracefully
   (hide it, or show what the API has today), and carry on with the next screen.
4. Send the file to Kimleng as soon as an entry blocks a screen, otherwise at the end of the day. It comes
   back with the Status lines filled in; the app then uses what was built.

## What belongs here

- A field no endpoint returns.
- A value in the wrong shape for the screen: a month where the screen shows a full date, a code where it
  shows a label the app cannot derive, a number without its unit or currency, free text the screen must parse.
- A list that needs a filter, a sort, a count or a page size it does not have.
- An action with no endpoint.
- A screen that needs two or three calls where one answer would do.

## What does not belong here

- Anything `screens-to-api.md` already answers, including the card every list is made of.
- Labels and formatting the app can do itself (`GOLDEN` shown as "Gold", a date shown in Khmer).
- Styling, and the Menu screen, which is out of scope.
- Sign-in, sign-up, the email code and forgot-password: they are Keycloak's pages, not the API.

## Entry shape

```
### R-001 · <screen> · <element>
- **Screen:** the name as in the Figma, and the state (list, detail, empty, signed-in...)
- **Element:** what the user sees, in words
- **Endpoint:** the call the screen already makes, or "none"
- **Today:** what the API gives there: field and type, or nothing
- **Needed:** what the screen needs: field, type, an example value
- **Why:** one sentence, what breaks or looks wrong without it
- **Blocks the screen:** yes or no
- **Status:** requested YYYY-MM-DD
```

The backend writes only the Status line: `built YYYY-MM-DD, <endpoint or field>` or `declined: <reason>`.

## Example

### R-000 · Profile · name fields
- **Screen:** Profile, signed-in, editing
- **Element:** two inputs, First name and Last name
- **Endpoint:** `GET /users/me`, `PUT /users/me`
- **Today:** `fullName` (string), one value: "Dara Chan"
- **Needed:** `firstName` and `lastName` (string each), or the screen keeps one "Full name" input
- **Why:** splitting one string on a space gets Khmer and multi-word names wrong
- **Blocks the screen:** no, one input shows the full name meanwhile
- **Status:** example, not requested

## Entries

