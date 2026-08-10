# SU Club Fair

Android app written in Kotlin with Jetpack Compose.

## Toolchain

| | |
|---|---|
| Gradle | 9.6.1 (via `./gradlew`) |
| Android Gradle Plugin | 9.3.1 |
| Kotlin | built into AGP 9 (Compose compiler plugin 2.4.10) |
| Compose BOM | 2026.06.01 |
| compileSdk / targetSdk | 37 |
| minSdk | 24 |
| Java / JVM target | 21 |

Dependency versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Build

```bash
./gradlew assembleDebug     # debug APK -> app/build/outputs/apk/debug/
./gradlew assembleRelease   # minified release APK (unsigned)
./gradlew test              # JVM unit tests
./gradlew connectedAndroidTest   # instrumented tests (needs a device/emulator)
./gradlew lintDebug         # Android lint
./gradlew installDebug      # build + install on a connected device
```

The SDK location is read from `local.properties` (gitignored); set `ANDROID_HOME`
instead if you prefer.

## Layout

```
app/src/main/java/com/su/clubfair/
    MainActivity.kt          # entry point, hosts HomeScreen
    ui/theme/                # the palette, Material scheme, typography, scale
    ui/scene/                # the generated backdrop — palette to pixels
app/src/main/res/            # strings, themes, launcher icon
app/src/test/                # JVM unit tests
app/src/androidTest/         # Compose UI / instrumented tests
```

## Where to put assets

| What | Folder | Used from Compose as |
|---|---|---|
| Fonts (`.ttf` / `.otf`) | `app/src/main/res/font/` | `Font(R.font.inter_regular)` |
| Icons / logos (vector `.xml`) | `app/src/main/res/drawable/` | `painterResource(R.drawable.logo)` |
| Photos / bitmaps (`.png`, `.webp`, `.jpg`) | `app/src/main/res/drawable-xxhdpi/` (+ other densities) | `painterResource(R.drawable.hero)` |
| Images that must not be rescaled | `app/src/main/res/drawable-nodpi/` | `painterResource(...)` |
| Anything else (JSON, Lottie, big binaries) | `app/src/main/assets/` | `context.assets.open("file.json")` |
| Unprocessed source art (not packaged) | `art/` | — import into `res/` first |

`art/` holds the originals as handed over; nothing there ships in the APK. What
each one became:

| `art/` original | Imported as |
|---|---|
| `onboard.png` | `drawable-nodpi/onboard_hero.webp` (hue-shifted, see below) |
| `mfu.png` | `drawable-nodpi/logo_mfu.png` |
| `su.png` | `drawable-nodpi/logo_su.png` |
| `PerfectRomantic-Regular.otf` | `font/perfect_romantic.otf` (unchanged) |

`login_bg.png`, `welcome_bg.png`, `onboard_bg.png` and `home_bg.png` are the
mesh gradients the app used to sit on. Nothing imports them any more — the
backdrop is generated (see [Colour and the backdrop](#colour-and-the-backdrop))
— and the three `.webp` copies have been deleted from `res/`. The sources are
kept in `art/` as a record of the original hand-over.

`onboard_hero.webp` is `art/onboard.png` with its violet body rotated to amber.
The render was built for the old purple theme and was the one thing on screen
that couldn't be re-tinted from a palette. Hues in 215–315° are remapped into
30–50° — a quarter of the original spread, so the render's shading survives as
shading rather than smearing into a rainbow — leaving the lime accent alone.
The script is in the commit that introduced it; re-run it against `art/` rather
than editing the `.webp` if the palette moves again.

**Filename rules for anything under `res/`:** lowercase letters, digits and
underscores only — no spaces, dashes or capitals, and it can't start with a
digit. `Club-Logo 2.png` must become `club_logo_2.png`, or the build fails.
Files under `assets/` have no such restriction.

Density buckets for bitmaps: `mdpi` = 1x, `hdpi` = 1.5x, `xhdpi` = 2x,
`xxhdpi` = 3x, `xxxhdpi` = 4x. If you only supply one size, put it in
`drawable-xxhdpi/` and Android will scale it for other screens. Prefer vector
XML for icons so no scaling is needed at all — Android Studio's
*File > New > Vector Asset* converts SVGs.

### Fonts in use

**Alan Sans** ([Google Fonts](https://fonts.google.com/specimen/Alan+Sans), OFL).
Upstream ships only a variable font with a 300–900 weight axis, so the five
static weights in `res/font/` (`light`, `regular`, `medium`, `bold`,
`extrabold`) were cut from it with `fonttools`:

```python
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer
f = TTFont("AlanSans[wght].ttf")
instancer.instantiateVariableFont(f, {"wght": 300}, inplace=True, updateFontNames=True)
f.save("alan_sans_light.ttf")
```

They're wired into the Material 3 type scale in
[`ui/theme/Type.kt`](app/src/main/java/com/su/clubfair/ui/theme/Type.kt) as
`AlanSans`. Note the axis floor is 300 — there is no ExtraLight/Thin.

**Playfair** ([Google Fonts](https://fonts.google.com/specimen/Playfair), OFL) —
the high-contrast Didone used only for the "Club Fair" wordmark, exposed as
`PlayfairDisplay`. Cut from the variable font at `wght=300, opsz=1200,
wdth=87.5` (thin + display optical size + condensed), then subset to Latin,
which took it from 408KB to 47KB:

```bash
pyftsubset Playfair-instance.ttf --output-file=playfair_light.ttf \
  --unicodes="U+0020-007E,U+00A0-00FF,U+2018-201D,U+2013-2014,U+2026" \
  --layout-features='kern,liga,calt'
```

### Colour and the backdrop

One palette, one backdrop, every screen. Everything visual — the ground, the
accents, the chips, the pill CTA — comes from
[`ui/theme/Palette.kt`](app/src/main/java/com/su/clubfair/ui/theme/Palette.kt),
and the backdrop that carries it is
[`ui/scene/MeshBackground.kt`](app/src/main/java/com/su/clubfair/ui/scene/MeshBackground.kt).

| Role | Value | Where it shows |
|---|---|---|
| `Base` / `Floor` | `#0B120E` → `#050806` | the vertical ground, top to bottom |
| `GlowLime` | `#A8E065` | the bright bloom, high and right |
| `GlowEmerald` | `#1E7A4E` | the wide fill bloom, left |
| `GlowDeep` | `#0D5A55` | the cold bloom, bottom |
| `Accent` | `#C6F16C` | CTAs, scan ring, ticks, progress |
| `Ink` / `Paper` | `#0A1408` / `#F1F6EA` | text on the accent; the pass panel |
| `Panel` | `#11201A` | the one opaque surface (the Register dropdown) |

**This replaced four per-tab ecosystems.** The app used to be themed as four
worlds — a rainforest, a savannah, a deep ocean and the abyss the scanner sat in
— each with its own hue, silhouettes, light shafts and drifting motes, panning
continuously as you swiped tabs. It was the best-looking thing in the repo and
it was the wrong idea: four backdrops meant four contrast situations to keep
white text legible against, four accents for every chip to look right in, and an
app whose colour depended on which tab you happened to be on. The surface should
be the constant and the content the thing that changes.

What replaced it is a mesh gradient: a near-black green ground with three soft
blooms lit onto it with `BlendMode.Screen`, dithered, and knocked back toward
`Floor` under a vignette. The blooms drift on slow circles (42s for a full
circuit) — a static radial gradient on a phone reads as a printed image behind
glass; one that has moved a few percent by the time you come back to a screen
reads as light.

Three rules the scheme assumes, and that a change to it has to keep:

- **Stay dark.** The glass in `Glass.kt` is pure white at 10–17% alpha. That is
  frost over this ground and a smear over a white page. `MeshBackground` also
  lays a flat 18% wash of `Floor` plus a vignette over the finished mesh: a
  bloom is bright enough at its centre to take body copy under 4.5:1 on its own,
  and knocking the whole thing back toward the scheme's own darkest tone costs
  almost nothing visually while guaranteeing the entire UI a backdrop.
- **The accent doubles as the progress colour.** Filled checkpoints, scanned
  ticks and the scan ring are all `Accent`. There is no separate "success"
  green — in a green app it would say nothing.
- **`Ink` is white, not tinted.** Labels, captions and hairlines are pure white
  at fixed alphas. Tinting body text with the backdrop's hue is what makes a
  themed app look like a colour filter was dropped over it.

The blooms are deliberately lopsided and their alphas deliberately low. Spread
them evenly or turn them up and each floods far enough to meet the next, the
dark lanes of base between them close, and the screen goes back to being one
flat tint — which is the failure mode the four-world version was built to avoid
and this one has to avoid too. If the ground ever looks washed out, move a bloom
or shrink its radius before raising its alpha.

`ZoneAccent` is the one place colour still varies: three steps along the same
green, one per zone of the fair floor, used only inside the booths list. They
were three unrelated hues under the old scheme. See [Booths](#booths).

The one deliberate exception to all of it is the QR modules on the pass, which
stay a neutral near-black — see [Student pass](#student-pass).

Material You dynamic colour used to be switched on in `Theme.kt`. It's off, and
should stay off: it hands the entire scheme to whatever the user's wallpaper
happens to be, which for an event with a fixed identity means the branding is
decided by someone's lock screen.

### Welcome screen

[`ui/welcome/WelcomeScreen.kt`](app/src/main/java/com/su/clubfair/ui/welcome/WelcomeScreen.kt)
is full-bleed behind edge-to-edge system bars, with `logo_mfu` / `logo_su` in
the footer row. The
title auto-shrinks via `BasicText(autoSize = …)` so it never clips on narrow
screens.

### Auth screens

[`ui/auth/`](app/src/main/java/com/su/clubfair/ui/auth/) holds `LoginScreen` and
`RegisterScreen`. Both share the control kit in `AuthComponents.kt`:

| Piece | Notes |
|---|---|
| `AuthBackground` | full-bleed `MeshBackground`, edge-to-edge insets + `imePadding()` |
| `AuthTextField` | glass pane via `glassField`, 14dp radius |
| `AuthDropdownField` | read-only `ExposedDropdownMenuBox` styled to match, for School / Major |

`glassField` just delegates to `glassSurface` at a 14dp radius, with a focus ramp
that brightens the pane over 220ms — see [Glass](#glass) for the material itself.
It passes no `HazeState`: the panes sit on a smooth gradient, where blurring
hands back very nearly the same gradient, and a blur pass per field on a form of
eight is the wrong place to spend it anyway — at field size the frost fill
separates them on its own. The nav bar is the one surface that earns real blur —
see [Glass](#glass).

One thing that looks like an oversight but isn't: the outline is drawn by
`glassSurface` as part of the rim, so Material's own border colours are set
transparent to stop a second, flatter edge landing on top of it.

The pill CTA lives in
[`ui/components/PillButton.kt`](app/src/main/java/com/su/clubfair/ui/components/PillButton.kt)
because onboarding uses it too. It is the accent, filled — it used to be a pale
near-white, on the reasoning that being the only *opaque* thing on a screen of
frosted panes is what marks the primary action. That still holds, but with one
accent instead of four there is now a colour to spend on it.

The School / Major option lists in `RegisterScreen.kt` are placeholders — swap
them for the real faculty data when it lands. Neither screen talks to a backend
yet; `onLogin` / `onCreateAccount` are hoisted callbacks.

### Onboarding screen

[`ui/onboarding/OnboardingScreen.kt`](app/src/main/java/com/su/clubfair/ui/onboarding/OnboardingScreen.kt)
shows the `onboard_hero` illustration, an auto-sizing headline and the
`PillButton` CTA.

The hero sits in a `weight(1f)` box, so it grows into whatever vertical space
the headline and CTA leave over (capped at 460dp for tablets) instead of being
pinned to a fixed size. It drifts ±10dp vertically on a 2.6s ease-in-out loop
that reverses forever. The drift is skipped outright when
`ValueAnimator.areAnimatorsEnabled()` is false, which respects the system
"remove animations" setting — and matters for tests, since an endless animation
stops the Compose clock ever reaching idle. `OnboardingScreenTest` pins
`mainClock.autoAdvance = false` for the same reason.

### Home screen

[`ui/home/HomeScreen.kt`](app/src/main/java/com/su/clubfair/ui/home/HomeScreen.kt)
scrolls: a greeting header with a profile button, the headline, a Checkpoints card (count, progress track and a 9-wide grid with one
cell per booth, filled once scanned), a rank / prizes / time-left stat row, the
Clubs and Prizes tiles, and a "next up" card for the concert. The percentage
truncates rather than rounds, so 26/27 can't read as "100 % complete".

The stat row is one pane split by hairlines, not three separate chips. The chips
came first and read as clutter: at a third of the screen width each was cramped,
and their 16dp icons were noise next to the cards above and below.

The nav bar lives in [`ui/AppShell.kt`](app/src/main/java/com/su/clubfair/ui/AppShell.kt),
**not** in any tab — it's a sibling pinned to the bottom, so content slides
underneath it. That's deliberate: it's what gives the bar's backdrop blur
something real to soften (see [Glass](#glass)). Each tab ends with a spacer of
the bar's height plus margin so its last card can clear it.

### Scan

[`ui/scan/ScanScreen.kt`](app/src/main/java/com/su/clubfair/ui/scan/ScanScreen.kt)
reads a booth's QR code: CameraX preview, a scrim with the window punched through
it by `BlendMode.Clear` (not four rectangles around a gap — those seam at the
corners once there's a radius), accent corner brackets and a sweep band.

Decoding is [`QrDecoder`](app/src/main/java/com/su/clubfair/ui/scan/QrDecoder.kt),
which takes a packed luminance buffer and no Android types at all. That's what
lets `QrDecoderTest` drive the real decode path on the JVM with a generated code
— the alternative is pointing a phone at a poster, which isn't repeatable. It
tries the frame inverted as well as as-is, because ZXing will not read a
light-on-dark code and booth printouts aren't guaranteed either way.

`QrAnalyzer` compacts plane 0 before handing it over: camera rows are padded to a
hardware stride, and passing that in unpacked skews every row by the padding so
nothing ever decodes. Rotation is ignored on purpose — a QR carries its own
orientation markers.

ZXing was already a dependency for rendering the pass, so the scanner adds no
second barcode library. Nothing is persisted; wiring a checkpoint to a scan needs
a backend, like the rest of the placeholder data.

### Student pass

[`ui/qr/QrTicketScreen.kt`](app/src/main/java/com/su/clubfair/ui/qr/QrTicketScreen.kt)
is the student's own ID as a QR code, styled as a concert ticket —
a notched outline, a dashed perforation and a stub carrying the ID, major and
school. It is **display only**; nothing scans, so there's no camera permission
and it does not use the camera permission that Scan added.

It is **no longer a tab**. Scan took that slot, and the pass now opens as a sheet
from a row on Profile, dismissed by the button or by back. The two are different
jobs — the pass is something a booth scans off the student, Scan is the student
scanning a booth — and the pass is a thing you fetch and put away rather than a
place you browse to.

The notches are a real `Shape` (a rounded rect with two circles subtracted via
`PathOperation.Difference`), not circles painted on top. They have to be, because
the pane is translucent — anything drawn over a notch would still show the glass
fill behind it rather than the background.

The code itself is encoded with ZXing but rendered here, module by module, so it
can carry rounded dots and rounded finder patterns. Error correction is level H
(~30% recoverable) because the SU logo in the middle covers real modules.

**The QR is dark-on-light, and that is not a style choice.** The mockup draws it
light-on-dark straight onto the card. That version was built, then tested by
decoding a screenshot of the running app: as rendered it **failed to decode**,
and the same image inverted read back the ID first time. ZXing — what most
Android scanning apps are built on — will not read an inverted code, and a stamp
rally across 27 booths is a bad place to discover which scanners cope. Swapping
`QrInk` and `QrPaper` restores the original look if the booth-side scanner is
known to handle inversion.

To re-run that check: screenshot the pass, crop the code, and decode it with
`MultiFormatReader` and `TRY_HARDER` — both as-is and inverted.

The greeting name, rank, prize count and event details are placeholders, like the
School / Major lists — swap them when there's a backend.

The pass paints its own `MeshBackground`: it opens as a sheet over whatever tab
you were on, so it isn't standing on the shell's copy.

Icons are Lucide (ISC), vendored as vector drawables in `res/drawable/ic_*.xml`
rather than pulled in as a dependency — `com.composables:icons-lucide-android`
is resource-only and would merge all 1,665 drawables into the APK for the
handful actually used. To add another, pull it out of that artifact rather than
redrawing it by hand, and recolour the stroke to `#FFFFFFFF`.

### Spacing and radii

[`ui/theme/Dimens.kt`](app/src/main/java/com/su/clubfair/ui/theme/Dimens.kt) is
the app's 4dp scale — gaps, screen gutter, card padding and corner radii. Use it
rather than a literal `.dp` in a screen; if something genuinely needs a new step,
add it there so the next screen can reach for the same one.

It exists because the screens had drifted badly. An audit turned up corner radii
of 12, 13, 14, 16, 18, 20, 22 and 24; card padding of 13, 14, 18 and 22; and gaps
of 10, 12, 20, 22 and 28. No single value was wrong, but side by side the
surfaces stopped reading as one app — cards in the same column had different
corners, and identical-looking blocks sat at different distances. Now every
top-level card is `RadiusLg` with `CardPadding`, every stack of cards is spaced
by `Space`, and every tab uses `ScreenPadding`.

Also worth knowing: `BoothCard` reserves its name and blurb lines with
`minLines`. Grid cells size to the tallest item in their row and are *not*
stretched to match, so cards whose text runs short would otherwise leave the
divider and detail block sitting at a different height in every cell. An earlier
attempt used a fixed card height instead, which had the same alignment effect but
silently clipped the longest cards ("Art & Illustration Club" over three lines of
blurb) — the reserved-lines version can't.

### Glass

`ui/components/Glass.kt` holds `glassSurface`, the frosted pane used by the auth
fields, the cards and the tiles. `ui/components/GlassNavBar.kt` holds the *other*
material — liquid glass — used by the nav bar alone. They are different things
and the difference is worth being precise about.

**The frosted pane** is a translucent fill plus one hairline, and nothing else.
The fill is one even 10% white (17% on focus or selection); the hairline is an
even 22% (38%). Both were briefly gradients — a diagonal ramp on the fill, a
top-left-to-bottom-right ramp on the border — and both were reverted. A pane with
a tonal ramp on its own face competes with the backdrop showing *through* it, and
at 1dp a gradient border doesn't read as a light catching an edge, it reads as a
border that fades out.

It went through a bigger wrong turn before that. It used to paint an inner lip, a
three-stop rim and a diagonal specular onto every pane — the vocabulary of
Apple's Liquid Glass, a *thick slab* with light bending round its edges — and it
read as a cheap imitation. Glassmorphism is the opposite idea: a thin, even sheet
of frost that is interesting only because of what sits blurred behind it.

**The liquid glass** on the nav bar is the real version of what those painted
highlights were imitating, and it is a third-party library:
[`io.github.kyant0:backdrop`](https://central.sonatype.com/artifact/io.github.kyant0/backdrop)
([AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)). The
difference from frost is **refraction**: frost blurs what is behind it, this
*bends* it, so the backdrop's arcs and blooms stretch and curve as they pass
under the bar's edge, and a booth card scrolling underneath visibly distorts
through it.

How it is wired:

- `AppShell` holds a `rememberLayerBackdrop()` and puts `Modifier.layerBackdrop`
  on the box containing the mesh and the tab content. That layer is what the bar
  samples, so it has to wrap everything the bar should see through itself — and
  nothing it shouldn't. **The bar is deliberately outside it**, or it would
  refract itself.
- The bar applies `Modifier.drawBackdrop(...)` with a `blur` and a `lens`, plus
  the library's `Highlight`, which is computed from the shape's own signed
  distance field rather than painted round the outside as a border.
- **API gates, and they matter here.** The blur needs API 31 and the refraction
  needs API 33 (`RuntimeShader`); the library no-ops each below those rather than
  throwing, so on an older phone the bar degrades to a plain translucent pill and
  nothing crashes. This app ships to 24, so that path is real, not theoretical.
- Only the nav bar gets it. Refraction earns its cost on the one surface floating
  over moving content; running a per-pane shader over a scrolling grid of 27
  booth cards would not.

This **replaced Haze**, which used to provide a backdrop blur for the same bar
and was the only thing in the app using it. `glassSurface` no longer takes a
`HazeState` and the dependency is gone. If a blur-without-refraction is ever
wanted again, note what Haze needed, because all three failed *silently* — the
bar simply rendered unblurred: `HazePositionStrategy.Local` rather than the
default `Auto` (which tracks screen coordinates and lands outside the window
during a slide transition), `hazeSource` placed where the backdrop is actually
drawn (a draw modifier only records what comes *after* it in the chain), and
Haze's own `blurredEdgeTreatment` rather than `Modifier.clip`.

### Nav bar behaviour

Two things beyond tapping, both taken from Telegram:

- **Slide across it.** A horizontal drag anywhere on the bar moves the selection
  as the finger passes each tab. The detector is on the *container*, not the
  items: a drag that started on Home has to keep being delivered while the finger
  is over Booths, and a per-item detector only ever sees its own bounds. The
  items keep their own `clickable` for taps — `clickable` releases movement once
  it passes touch slop, so the two coexist.
- **The gesture reads `selected` through `rememberUpdatedState`,** and that is
  load-bearing. `pointerInput` is keyed on the slot width, so its coroutine is
  not restarted when the selection changes — anything it closes over stays frozen
  at the value it had when the finger went down. The first version compared
  against a captured `selected`, so a drag starting on Home compared 0 against a
  stale 0 on the way back and swallowed the call: every tab was reachable except
  the one you started from. A gesture that outlives the state it reads has to
  read it indirectly.
- **A pill tracks the finger while you drag.** The first version switched tabs
  correctly and showed *nothing* until the finger crossed a slot boundary, which
  reads as the gesture not being picked up at all. The indicator is an
  `Animatable` for a reason: the drag `snapTo`s it to the finger position
  continuously, and letting go `animateTo`s it to the selected slot on a low-
  bouncy spring — one value, two ways of being moved. A plain `animateFloatAsState`
  can only do the second.
- **The icon bounces.** A short tween up to 1.28× then a spring back that
  overshoots. Two stages rather than one spring from rest, because a spring eases
  *out* of the tap — it is slowest exactly when the finger lands, which is when
  the feedback has to arrive. It fires on every tap including the tab you are
  already on: the bounce is the acknowledgement that the tap landed, and
  swallowing it when nothing changes is what makes a bar feel dead. It is a
  `graphicsLayer` scale, not a size change, so it can't shove its neighbours
  around mid-bounce.

### Booths

[`ui/booths/BoothsScreen.kt`](app/src/main/java/com/su/clubfair/ui/booths/BoothsScreen.kt)
lists all 27 clubs in a two-column grid, grouped into three zones, with All /
Scanned / Remaining filters. Each card carries its own Lucide icon, the booth
number (with a tick folded into it once scanned), the club name, a line on what
it does, and the practical details — when it meets, where, and how many members.

The page is **three sections, one per zone**. `ZoneHeader` makes what the area
*is* the headline and its signage code the subtitle — a student is looking for
"Sports & Wellness" and will find it under "Zone A" — over a rule that fades from
the zone's accent into nothing, with that zone's own scanned count on the right.
The cards below it are wrapped in a `CompositionLocalProvider` that re-provides
`LocalAccent` for the section, so every card, icon tile, number chip and tick
picks up the zone's accent without `BoothCard` knowing anything about zones, and
a new accent-using part of a card can't forget to.

The three `ZoneAccent` steps are close together on purpose — lime, mint, aqua,
all within the app's green. They used to be three unrelated hues, one per
ecosystem, which worked only because each zone had a matching world behind it.
With one backdrop, three competing hues on one scrolling page would read as three
different apps' cards shuffled together. Close enough to belong, far enough apart
to tell a section boundary at a glance.

Those details are the point of the page. The first version was hairline-separated
rows: a number badge and a club name, nine to a zone card. Tidy, and useless — 27
near-identical lines told a student nothing about which booth was worth queueing
for.

Inside a card, the icon tile and the booth-number chip share one `BadgeHeight`
so their top and bottom edges line up — a chip sized to hug its own text sat
visibly short of the tile next to it.

Two things the grid forced:

- **`BoothCard` has a fixed height,** with the meta block pushed down by a
  weighted spacer. Grid cells size to the tallest item in their row, so without
  it the dividers and detail lines would sit at a different height in every card
  and the grid would look ragged.
- **The header item is wrapped in an explicit `Column`.** `LazyColumn` lays an
  item's several root composables out down the main axis, but `LazyVerticalGrid`
  places them at the same offset — without the wrapper the title, the progress
  line and the filter chips silently draw on top of each other.

The filter chips reuse `glassSurface`'s `intensity` ramp for their selected
state — the same 0..1 the auth fields use for focus, so selection reads as more
light on the same material rather than a different one.

The 27 club icons are Lucide, vendored as `res/drawable/ic_club_*.xml` the same
way as the rest (see [Home screen](#home-screen)).

### Profile

[`ui/profile/ProfileScreen.kt`](app/src/main/java/com/su/clubfair/ui/profile/ProfileScreen.kt)
is the Profile tab: an identity card (initial avatar, name, student ID, a "pass
active" chip), the same `StatPane` Home uses, the details captured at
registration (phone, school, major), sign-out, and the MFU / SU marks
from the welcome footer. Sign-out is a ghost surface rather than the accent
`PillButton` — it's the one destructive action on the page and shouldn't outshout
the content. Nothing is editable; there's no backend to write to.

### Where the student data lives

[`ui/model/Student.kt`](app/src/main/java/com/su/clubfair/ui/model/Student.kt)
holds the signed-in student — the fields `RegisterScreen` collects (name, phone,
school, major) plus what the fair tracks (booths visited, rank, prizes).
`PlaceholderStudent` stands in until sign-in returns a real one.

[`ui/model/Booth.kt`](app/src/main/java/com/su/clubfair/ui/model/Booth.kt) holds
the 27-club roster. Which booths count as scanned is *derived* from
`Student.visited` rather than stored per booth, so the list can never disagree
with the "19 / 27" on Home and the profile. Once scanning is real that flips
round: the booths become the source of truth and the count is derived from them.

Home, the pass and the profile all read that one holder rather than their own
constants. They used to each carry their own copy of the name, ID, major and
school in `strings.xml`, which is three places for the same person to drift apart
the moment one of them changed.

### Window background

`themes.xml` sets `android:windowBackground` to a dark colour. The app inherits
`android:Theme.Material.NoActionBar`, whose default background is light, and the
app is dark end to end — so that default only ever showed up as a **white flash**
between screens, or through anything that fades. It is what forced both the
sign-in flow's and the tab bar's transitions to be slide-only with no crossfade;
the dark colour is the backstop for anything that still slips through.

### Navigation

`MainActivity` routes Welcome → Login → Register → Onboarding with a
`rememberSaveable` enum plus a `BackHandler` (back rewinds sign-in only;
onboarding and Home are one-way). Move to Navigation Compose once there's a
real post-login graph.

Screens swap through an `AnimatedContent` that slides horizontally over 380ms.
Direction comes from the `Screen` enum's ordinals — a higher ordinal is "deeper"
and slides in from the right, a lower one slides back from the left — so keep
the constants declared in flow order.

The sign-in flow has no crossfade: every screen carries the same backdrop, so the
outgoing and incoming ones tile exactly while they slide — fading either would
only let the window background flash through.

Tabs inside `AppShell` slide horizontally over 380ms on Material 3's emphasized
curve, exactly one screen width each way, with a half-length crossfade. The
backdrop is a sibling underneath the whole `AnimatedContent` rather than
something each tab paints, so it stays put while the content moves over it and
nothing shows through the moment both tabs are on screen.

This used to be a five-layer parallax: the backdrop's own depth planes panned at
0.10 / 0.28 / 0.62 while the content overshot all of them by 1.15×. It went with
the ecosystems — there are no depth planes left to move at different rates, and
overshooting a static backdrop is just an overshoot.

Tabs are Home, Booths, Profile — and Scan, which sits at index 3 but **outside**
the bar as its own round button. It's the one thing a student does rather than
looks at; mixing an action in with the destinations made it read as a fourth
page.

### The mesh background

[`ui/scene/MeshBackground.kt`](app/src/main/java/com/su/clubfair/ui/scene/MeshBackground.kt)
draws the whole backdrop, takes no parameters, and is the same on every screen —
Welcome, both auth forms, all four tabs, the pass and the scanner. That is the
point of it: there is no per-screen knob to set wrong, and no screen can end up
rendered against a ground the rest of the app doesn't use.

It is five passes, in order:

1. A vertical wash, `Base` at the top to `Floor` at the bottom.
2. Three (well, four) blooms — soft three-stop radial gradients drawn with
   `BlendMode.Screen`, each drifting on its own slow circle. Screen rather than
   the default source-over because these are *lights*: light on a dark surface
   lifts it without dragging it toward grey the way a translucent overlay does.
3. The arcs.
4. The grain.
5. The trim — a flat wash of `Floor`, a vignette, and bands at the top and bottom
   where the status bar and the floating nav bar sit.

**The arcs** are eight of them, in three sweeps of two or three, cropped into
three corners. They are circles whose **centres sit outside the screen**, which
is why their coordinates look wrong (`-0.22`, `1.34`): put the centre on screen
and you get a ring, and a ring is a shape the eye reads as a *thing*; keep it off
and you get a curve that reads as the edge of something much larger.

Within a sweep the innermost arc is the lead — heaviest and brightest — and the
one or two behind it fall away, so a sweep reads as one edge with highlights
coming off it rather than as three lines at equal intervals.

**The trap, which cost a build:** because the centre is off screen, a radius that
sounds generous can leave the whole circle outside the viewport, and the arc
silently doesn't render — no error, nothing drawn. The radius has to exceed the
distance from the centre to the nearest visible pixel, which on a tall phone is
easily more than one shorter-side; two of the three sweeps have radii above 1 for
that reason alone. If an arc doesn't show up, check its geometry before touching
its alpha.

This layer was rebuilt three times and every dead end is worth not repeating:

1. **Parallel sine waves.** Three families of near-parallel hairlines drifting
   across the screen. This is the default mesh-gradient decoration and it read as
   exactly that.
2. **Topographic contours.** Two nests of closed elevation rings around the
   blooms, so a bloom read as a hill and the rings as its slope. It genuinely
   looked good and it was far too loud — it turned the backdrop into the thing
   you looked at.
3. **Four-point sparkles.** Added alongside the arcs from the reference art.
   Cut on request; the note worth keeping is that a solid shape at a real alpha
   has to clear the opaque controls on *every* screen, because one backdrop sits
   behind eight different layouts and half a shape poking out from behind a
   button reads as breakage. Behind glass is fine — the fill is translucent, so
   it shows through, which is the point of the material.

A backdrop should be interesting when you go looking for it and invisible when
you aren't, and the arcs are the version that lands there.

Nothing in the layer is animated. The blooms drift and the lines sit still on
purpose: the read is light moving over something that isn't. Animate both and
they just slide past each other, which looks like a screensaver.

They are stroked with a radial gradient centred on the screen rather than a flat
colour, so an arc dissolves toward the edges instead of running clean off them; a
hairline that hits the screen edge at full strength reads as a UI divider someone
forgot to inset. Their alphas are higher at the bottom of the screen than the top
for the same apparent weight, because `drawTrim` runs after them and lays 44% of
`Floor` over the bottom edge against 34% at the top. Tune them against a
screenshot, not against each other.

Two things in there are worth not re-discovering:

- **The grain layer is load-bearing, not texture.** A full-screen gradient across
  a narrow, very dark range bands badly on 8-bit panels — this ground is about a
  dozen steps of green top to bottom, and without dithering it arrives as a dozen
  visible stripes. A tiled 64×64 noise bitmap at 3.5% alpha is the fix, and it
  has to be drawn *before* the trim, since it is there to break up everything
  above it. `RuntimeShader` would be cleaner and needs API 33; this app ships
  to 24.
- **The drift phase is read inside the draw block, not in composition.** It
  changes every frame. Unwrapped with `by` at the top of the composable it would
  recompose this node sixty times a second for what is only ever a redraw; held
  as a `State` and read in `onDrawBehind`, a full drift cycle costs redraws and
  nothing else.

Bloom positions, radii and sway are all fractions of the screen, so the
composition holds its shape from a small phone to a tablet rather than bunching
into a corner. Two of the four are centred off-screen on purpose — a light with
no visible source reads as depth; one whose centre is on screen reads as a lens
flare stuck to the glass.

The previous backdrop generated three parallax depth planes of silhouettes per
world — leaflets, acacias, kelp fronds, coral — from a single lens primitive, and
tiled them seamlessly at the screen width. It is gone along with the ecosystems.
If something like it comes back, the two constraints that made it work were:
horizon lines using whole numbers of cycles across the width (so both ends of a
layer meet at the same height), and seeded generators (so a given screen size
always produces the same scenery — art that reshuffles on rotation reads as a
bug).
