# SU Club Fair — context for building the website

**Copy this file into the website project as `CLAUDE.md`.** It is written for an
assistant starting with an empty folder and no access to the Android app or the
server source. Everything in it was read out of those two repos rather than
remembered, but they are the source of truth — if this file and the code
disagree, the code is right and this file is stale.

Companion repos, if they happen to be on the same machine:

- `su-clubfair-mobile` — the Android app (Kotlin, Compose)
- `su-server` — the Go backend that both the app and this website talk to

---

## 1. What the product is

Mae Fah Luang University Student Union's **Club Fair**: a two-day event where
students walk a hall of **28 club booths**, scan a QR at each one, and collect
stamps toward prizes.

- **Dates:** 22 Aug 2026 09:00 → 23 Aug 2026 17:00, `Asia/Bangkok`.
  (`FairSchedule` in the app; the website should not hardcode these separately
  if it can avoid it.)
- **Audience:** MFU students, intakes 66–69, signing in with a
  `@lamduan.mfu.ac.th` address.
- **Languages:** Thai and English, both first-class. The API returns both
  (`name` / `name_en`), and the app ships `values/` and `values-th/` strings.
  Assume a bilingual site.

### The three zones

The hall is divided into three themed areas, and they are real rows in the
database (`clubfair_zone`), not app constants:

| Code | Thai | English | What is in it | Accent |
|---|---|---|---|---|
| A | โซนป่าดิบชื้น | Rainforest Zone | Religion, culture, volunteering | `#6FD98F` |
| B | โซนทุ่งหญ้าสะวันนา | Savannah Zone | Sport, student relations | `#E0A94A` |
| C | โซนมหาสมุทรลึก | Deep Ocean Zone | Academic | `#4CC1E6` |

The letters are painted on the physical signage, so they are the thing a student
matches against the hall — always show the letter, not just the name.

### Prizes

Two tiers, and the **server owns them** (`clubfair_prize_tier`):

- **Prize 1** — 15 booths
- **Prize 2** — 28 booths (every booth)

Never hardcode these. They are rows precisely so they can be moved mid-fair
without a release. Render whatever `/clubfair/progress` returns, including the
count. The names are deliberately neutral — the actual prizes were not confirmed
when this was written, and a name that sounds like a promise is a problem at the
prize table.

---

## 2. What the website might be

Three surfaces are plausible and they are very different jobs. Decide which one
you are building before writing code.

1. **Public info site** — what the fair is, when, the booth directory, the map.
   No login needed: `/clubfair/booths` and `/clubfair/zones` are deliberately
   public so a student deciding whether to come does not have to sign in.
2. **Booth display** — ⚠ **this does not exist and the fair does not work
   without it.** Every booth needs a screen showing a QR that rotates every 30
   seconds. The server endpoint is built and nothing calls it. See §6.
3. **Staff console** — post announcements, claim prizes at the prize table.
   Both endpoints exist and are staff-gated.

---

## 3. Design system

The app has one visual identity and the website should be recognisably the same
product. The rules below were each arrived at by trying the opposite first;
they are worth respecting even when they feel restrictive.

### Palette

```
Base       #0B120E   top of the vertical wash — near-black, barely green
Floor      #050806   bottom of the wash, and the contrast floor everything stands on
Accent     #C6F16C   lime. CTAs, ticks, progress, the scan ring
Ink        #0A1408   dark text, for use ON Accent or Paper
Paper      #F1F6EA   near-white, for the pass (it gets scanned, so keep it bright)
Panel      #11201A   the one opaque panel tone, for surfaces that cannot be glass
Alert      #FF9E80   coral, not red — see below

Backdrop glows (not UI colours):
GlowLime    #A8E065      GlowEmerald #1E7A4E      GlowDeep #0D5A55

Zone accents: Rainforest #6FD98F · Savannah #E0A94A · DeepOcean #4CC1E6
```

White at fixed alphas does the text work — `#CCFFFFFF` labels, `#8FFFFFFF`
supporting copy, `#59FFFFFF` dividers, `#B3FFFFFF` placeholders. Body text is
pure white, never tinted with the backdrop's hue; tinting type with the
background is what makes a themed app look like a colour filter was applied.

**Accent discipline is the single most important rule.** Lime means *something
happened* — a scanned checkpoint, a filled progress bar, a call to action. It is
not a decorative colour. An early version of the app painted zone cards with a
34%→10% accent wash and it made that screen look like it came from a different
product.

**Alert is coral rather than signal red** because on a near-black green ground
under a lime accent, a saturated red is the loudest thing on any screen it
appears on — the wrong weight for "check this field".

### Typography

- **Alan Sans** — everything. Weights 300/400/500/700/800.
- **Bitcount Prop Single** — the "Club Fair" wordmark only. A dot-matrix face;
  set it at `wght` 300 and `CRSV` 0. Weight is dot *diameter* here, not stroke
  thickness: past ~600 the dots touch and the grid closes into blobs, which
  loses the only thing the face is for.
- **Perfect Romantic** — a serif the wordmark used to use. Retired; do not
  reintroduce it.

### Material

The app is glassmorphism on a dark mesh backdrop:

- **Frosted panes.** One even translucent fill (10% white, 17% when emphasised)
  plus one hairline (22%, 38% emphasised). Nothing else. No gradients across a
  pane's own face — a pane with a tonal ramp competes with what shows *through*
  it, which is the only thing a translucent surface is supposed to be showing.
- **No shadows under glass.** Elevation shadows assume an opaque surface; under
  a translucent fill they read as a dark inner rectangle.
- **One backdrop, everywhere.** A dark green vertical wash lit by four soft
  radial blooms, crossed by long arcs whose centres sit *off-screen* (so you see
  an edge of something much larger, never a ring). Over that: soft dark leaf
  shadows cropped into the top corners, and pools of dappled light. Four
  per-section palettes were tried and reverted — the app read as four apps
  sharing a nav bar.

If the website needs a hero background, reproduce that recipe rather than
inventing one: vertical wash `#0B120E → #050806`, a few large low-alpha radial
glows in the three glow colours, and cropped arcs.

### Icons and illustration

- White line work. Two weights only: full white for the shape, `#8AFFFFFF`
  (54%) for secondary detail.
- **Never fill illustrations with palette colours.** Tried three times; solid
  colour carries far more weight than the frosted glass around it and reads as
  clip-art pasted onto the page. An outline weighs what an icon weighs.
- Rounded caps and joins throughout.
- Marks are drawn at the size they will be used. A 24dp glyph enlarged to 100dp
  looks unfinished — that exact mistake is why the zone marks were redrawn.

---

## 4. Vocabulary

Use these words; the app and the signage do.

| Use | Not |
|---|---|
| booth | stall, stand, exhibitor |
| zone (and its letter) | area, section |
| checkpoint / stamp | point, token |
| scan | check in *(as a student-facing verb)* |
| pass | ticket, badge |
| Prize 1 / Prize 2 | tier 1, halfway, full sweep |

---

## 5. API

**Base URL:** `https://api.studentunion.social`
Local development: `http://localhost:8080`.

⚠ **CORS.** The server allows `http://localhost:3000`, `http://localhost:3001`
and `https://*.trycloudflare.com` by default. **Any other origin — including the
deployed website — must be added to `CORS_ALLOWED_ORIGINS`** (comma-separated)
in the server's environment. It is added to the defaults, so no rebuild is
needed, but the site will fail with opaque CORS errors until someone does it.

### Auth

Club Fair has **its own** JWT, separate from the SU and Walk-Bike-Week tokens,
with its own secret and its own claim names (`cf_uid`, `cf_role`) and a required
`clubfair` audience. Do not attempt to reuse a token from another SU product.

- `POST /clubfair/auth/login` — email + password → `{ token, user }`
- `POST /clubfair/auth/google` — Google sign-in
- `POST /clubfair/auth/register`
- Send it as `Authorization: Bearer <token>`.
- Tokens last **30 days**. A 401 means the session is over — sign the user out
  and say why, do not retry silently.

### Public — no token

- `GET /clubfair/booths` → booth directory
- `GET /clubfair/zones` → the three zones

### Student — token required

- `GET /clubfair/me` · `PATCH /clubfair/me` · `PUT /clubfair/me/password`
- `GET /clubfair/progress` → visited count, total, visited booth ids, rank, prize tiers
- `GET /clubfair/checkins` · `POST /clubfair/checkins`
- `GET /clubfair/announcements` · `POST /clubfair/announcements/{id}/reactions`

### Staff — token with a staff role

- `GET /clubfair/booths/{id}/checkin-code` — what a booth display polls
- `POST /clubfair/prizes/claim`
- `POST /clubfair/announcements` · `DELETE /clubfair/announcements/{id}`

### Shapes

JSON is `snake_case`. The fields the app relies on:

```jsonc
// GET /clubfair/booths → [ ... ]
{ "id": 1, "name": "…", "name_en": "…", "category": "…", "zone": "A",
  "booth_code": "A4", "about": "…", "icon": "football", "event_id": null }

// GET /clubfair/zones → [ ... ]
{ "code": "A", "name": "โซนป่าดิบชื้น", "name_en": "Rainforest Zone",
  "intent": "…", "sort_order": 1 }

// GET /clubfair/progress
{ "visited": 7, "total": 28, "visited_booth_ids": [1,2,5],
  "rank": 42,
  "prizes": [ { "id": 1, "threshold": 15, "name": "Prize 1",
                "description": "15 booths visited",
                "reached": false, "claimed": false } ] }
```

`icon` is a neutral token (`football`, `photo`), not a filename and not a URL —
each client maps it to its own asset. Six of the 28 booths have `null`; fall
back to a neutral glyph rather than showing the booth code where a picture goes.

---

## 6. The check-in scheme (read this before building the booth display)

Each booth has a 32-byte secret that **never leaves the server**. A display at
the booth shows a QR encoding:

```
clubfair://checkin?b=<booth id>&w=<window>&c=<code>

window = unix_seconds / 30
code   = HMAC-SHA256(booth_secret, "<boothId>:<window>"), first 12 hex chars
```

- The QR **rotates every 30 seconds** (`CheckInWindow`).
- The server accepts a code up to **3 minutes old** (`CheckInMaxAge`, tunable
  via `CLUBFAIR_CHECKIN_MAX_AGE_SECONDS`). The gap between the two is deliberate
  — it absorbs scan latency and clock skew. Do not "sync" them.
- **The accepted age is the screenshot-sharing window.** Anything a booth
  displays can be photographed; this scheme stops a student *inventing* a code
  for a booth they never visited, which is the failure it was built to fix. It
  cannot stop sharing, so the age is deliberately short.

A booth display therefore needs to: authenticate as staff, poll
`GET /clubfair/booths/{id}/checkin-code` (every ~10s is plenty), render the
returned payload as a QR, and be readable across a metre of crowded hall —
large, high contrast, screen kept awake. It should degrade visibly when polling
fails rather than showing a stale code that no longer scans.

The students' app posts the scanned payload **verbatim**; it never parses it and
cannot, since only the server holds the secret.

---

## 7. State of things

**Built and working:** student app (auth, booth directory, zones, scanning,
progress, prizes, announcements), the whole server API, rotating code
generation and verification.

**Not built:** the booth display (§2.2). The scanner in the student app has
nothing to scan until it exists.

**Known issue worth not repeating in web code:** a scan taken offline is queued
on the phone and can silently expire before it uploads, because the server only
accepts a code for 3 minutes. If the website ever queues writes, tell the user
when one is dropped.

---

## 8. House style

The two existing repos are heavily commented, and the comments explain **why**,
including what was tried and rejected. Match that. A comment that says what the
line does is noise; one that says why it is 30 seconds and not 30 minutes saves
the next person a day.

Specifically worth carrying over:

- Record dead ends. Half the design decisions above are "we tried the obvious
  thing and here is how it failed".
- Prefer server-owned configuration over constants in the client. Thresholds,
  zone names and booth data are rows for a reason: the fair is one weekend and
  a redeploy mid-event is not a plan.
- Bilingual from the start, not retrofitted.
