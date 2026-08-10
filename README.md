# SU Club Fair

Android app written in Kotlin with Jetpack Compose.

## Toolchain

| | |
|---|---|
| Gradle | 9.6.1 (via `./gradlew`) |
| Android Gradle Plugin | 9.2.1 |
| Kotlin | built into AGP 9 (Compose compiler plugin 2.4.10) |
| Compose BOM | 2026.06.01 |
| compileSdk / targetSdk | 37 |
| minSdk | 24 |
| Java / JVM target | 21 |

Dependency versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Build

```bash
./gradlew assembleDebug     # debug APK -> app/build/outputs/apk/debug/
./gradlew assembleRelease   # minified release APK
./gradlew test              # JVM unit tests
./gradlew connectedAndroidTest   # instrumented tests (needs a device/emulator)
./gradlew lintDebug         # Android lint — aborts on error
./gradlew installDebug      # build + install on a connected device
```

`make` wraps the common loop — `make run` boots the emulator, installs and
launches; `make help` lists the rest.

The SDK location is read from `local.properties` (gitignored); set `ANDROID_HOME`
instead if you prefer.

### Variants and signing

The debug build carries `applicationIdSuffix = ".debug"`, so it installs
**alongside** a release build instead of replacing it — useful when one phone is
being demoed from and another is being developed against. Its package is
therefore `com.su.clubfair.debug`, which is what the `Makefile` targets.

Release signing reads an untracked `keystore.properties` at the repo root:

```properties
storeFile=/path/to/clubfair.jks
storePassword=…
keyAlias=clubfair
keyPassword=…
```

CI can supply the same four as `CLUBFAIR_STORE_FILE`, `CLUBFAIR_STORE_PASSWORD`,
`CLUBFAIR_KEY_ALIAS` and `CLUBFAIR_KEY_PASSWORD`. With neither present
`assembleRelease` still succeeds and produces an **unsigned** APK, so a checkout
without the keystore is not a broken checkout. The keystore and
`keystore.properties` are gitignored; keep them that way.

## Layout

```
app/src/main/java/com/su/clubfair/
    MainActivity.kt          # entry point and the signed-in / signed-out gate
    ClubFairApplication.kt   # the object graph: one repository, process-wide
    data/                    # storage, the repository, and the fair's own rules
    ui/                      # AppShell + FairViewModel, then one package per screen
    ui/theme/                # the palette, Material scheme, typography, scale
    ui/scene/                # the generated backdrop — palette to pixels
app/src/main/res/            # strings (en + th), themes, launcher icon
app/src/test/                # JVM unit tests
app/src/androidTest/         # Compose UI / instrumented tests
```

Compose reads the fair through `data/FairRepository`; nothing in `ui/` touches
storage directly. See [Where the student data lives](#where-the-student-data-lives).

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

**Nothing else from the earlier designs is still in `res/`.** These were all
verified unreferenced by any Kotlin or XML source and deleted, ~2.3 MB in total:

| Removed | Was |
|---|---|
| `bg_fairmap.png` (1.6 MB) | the fair-map backdrop |
| `bg_ocean.jpg`, `bg_savannah.jpg`, `bg_rainforest.jpg` | the per-zone "ecosystem" backdrops the generated mesh replaced |
| `assets/models/fox.glb`, `booth_stall.glb` | went with the SceneView renderer, along with its catalog entry |
| root-level `ocean.jpg`, `savanna.jpg`, `tempbg.jpg` | byte-identical copies of the three above |

`shrinkResources` would have stripped them from a release APK, but they still cost
the repo, every clone and every debug install. `lintDebug` reports `UnusedResources`
and now aborts on error, so the next batch shows up before it accumulates.

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

Both forms are **driven by state, not by their own `remember`**. `LoginForm` and
`RegisterForm` live in
[`AuthViewModel`](app/src/main/java/com/su/clubfair/ui/auth/AuthViewModel.kt) and
compute their own per-field errors, so nothing is lost to a rotation and the rules
are unit-testable without a device. Both screens previously accepted anything at
all — including empty fields — and `onLogin` discarded the credentials verbatim.

Two details in the validation that are easy to get backwards:

- **`error` is what a field thinks; `showErrors` is whether the form is ready to
  say so.** Validating as someone types tells them their password is too short
  when they have entered one character of it, which is true, useless, and reads as
  the form shouting.
- **A rejected field names the rule it broke** (`FieldError` → a string resource
  in `AuthComponents.kt`, so the ViewModel never holds a `Context`). "Invalid"
  makes someone guess which of three rules they missed. The message is attached
  with `semantics { error(…) }` as well as drawn, so TalkBack announces the reason
  on focus.

`RegisterScreen` asks for what the app then actually renders: name (the greeting,
the pass initials), **student id** (the pass QR encodes it), phone (the identifier
sign-in matches against), school and major (the profile). It used to ask for a name
and a password on the reasoning that the Google step establishes the rest — which
it doesn't, since that button is a stub, so everything the app showed had to be
invented and was.

The School list in
[`data/Campus.kt`](app/src/main/java/com/su/clubfair/data/Campus.kt) **needs
checking against the registrar before release**: a student whose school is missing
cannot finish signing up, which is the worst way for a stale list to fail. Major is
deliberately a text field, not a list — programmes change every intake and a
dropdown one year out of date forces a student to pick something they are not on.

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
second barcode library.

A scan is **recorded** now rather than shown and discarded.
[`ScanOutcome`](app/src/main/java/com/su/clubfair/data/FairRepository.kt) has
three cases and the screen says which, because three different things need saying
to someone standing at a booth holding a phone up: it worked, you already did this
one, or that is not a fair code. Re-scanning a collected booth used to produce a
card identical to a fresh checkpoint — telling a student they had gained something
they had not.

The tab is no longer a dead end when the camera is unavailable. Four things used
to end here and all four now fall through to typing the number in
([`ScanControls.kt`](app/src/main/java/com/su/clubfair/ui/scan/ScanControls.kt)):
permission refused, no camera on the device, a code too damaged or glared to read,
and a booth whose printed sign has gone missing by the afternoon. The third is the
most common and the only one with no error state to land on, which is why the way
in sits on the running camera screen too.

A typed number is *exactly* as trustworthy as a scanned one, because `BoothCode`
accepts a bare number and nothing on the device can certify that anyone stood
anywhere. This adds no new hole — it makes the existing one visible, and both
paths close together when a server signs the codes.

There is a torch, drawn only when `cameraInfo.hasFlashUnit()` says there is one: a
fair hall is lit for people, not cameras, and a booth's code is usually on a table
under someone's shadow. Driving it lives in its own `LaunchedEffect` so toggling
the light doesn't tear down and rebuild the whole camera session.

A successful scan buzzes (Settings can turn that off). A booth read at arm's
length is a phone nobody is looking at — held up at a sign while the student
watches the sign — so the haptic is what says "done" without needing eyes on the
display. It stays quiet for a code that simply wasn't ours; a buzz for every stray
QR the camera swept past would make the phone feel broken.

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

> **Note** — the rest of this section describes the two-column grid with All /
> Scanned / Remaining filters that this tab *used* to be, and is out of date.
> `BoothsScreen` now shows **three area cards, then the wall of whichever one you
> pick** (`ZoneBoothWall`), for the reasons in that file's own header: the filter
> answered a question the number at the top of the page already answered and made
> two thirds of the fair vanish to do it, and a grid of 27 identical squares is a
> database listing. Everything below about zone accents, `LocalAccent` and the
> booth panel still holds.

[`ui/booths/BoothsScreen.kt`](app/src/main/java/com/su/clubfair/ui/booths/BoothsScreen.kt)
takes the ticked roster and groups it by zone. Each card carries its own Lucide
icon, the booth number (with a tick folded into it once scanned), the club name
and a line on what it does.

#### Search

[`ui/booths/BoothSearch.kt`](app/src/main/java/com/su/clubfair/ui/booths/BoothSearch.kt)
is a layer over both the area picker and a zone's wall, reached from the magnifier
beside the title.

Browsing by area is right for a student wandering and useless for one who has been
told "come find the Robotics Club" — the areas are habitats, so there is no way to
reason from a club's name to which of the three it sits in. Results carry the zone
for that reason: the answer to "where is Robotics" is a place on the floor.

Matching is a plain substring over the name **and the blurb**, which is why "hack"
finds the Coding Club. It stays a substring because that is forgiving in the
direction that matters — half-way through typing "badmin" you should already see
the club — and the cost is breadth: "art" also matches "St**art**up Club", because
it genuinely is in there. So results are *ordered* rather than filtered harder, and
a club with a word actually beginning with the term comes first. Losing a real hit
is a worse failure than showing a loose one below it. Ties fall back to booth order
so the list doesn't reshuffle as a query is typed.

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
registration, a way through to Settings, sign-out, and the MFU / SU marks from
the welcome footer. Sign-out is a ghost surface rather than the accent
`PillButton` — it's the one destructive action on the page and shouldn't outshout
the content. Nothing is editable; there's no backend to write to.

The details card shows what sign-up actually collected and says "Not set" for
anything it didn't, rather than rendering a blank row that reads as a failure to
load. Email is the one routinely unset: it comes from the Google step, which is
still a stub.

The two policy names in the footer are links now. They were styled text going
nowhere, on a screen whose sign-up counterpart makes a student agree to both by
tapping a button — see [`data/Links.kt`](app/src/main/java/com/su/clubfair/data/Links.kt)
for the pages that **have to exist before release**. Google Play also requires a
reachable privacy policy URL on the store listing, so this is a shipping blocker
rather than a nicety.

### Settings

[`ui/settings/SettingsScreen.kt`](app/src/main/java/com/su/clubfair/ui/settings/SettingsScreen.kt)
opens over Profile as a second sheet, so closing it returns to the page it was
opened from without re-animating that page.

Three groups: the one preference worth having (vibrate on a scan), an honest
statement of where the student's checkpoints actually are, and the version number
every bug report starts with. The storage card is not a footnote — progress is
held on this phone and nowhere else, and saying so is what makes the current
behaviour safe to rely on. "Erase everything" is the only destructive action in
the app and the only thing behind a confirmation dialog, because there is nothing
to undo *to*.

### Where the student data lives

Four layers, and the boundary between the second and third is the one that
matters: **nothing in `ui/` touches storage.**

| | |
|---|---|
| [`data/ClubFairStore.kt`](app/src/main/java/com/su/clubfair/data/ClubFairStore.kt) | One DataStore. The account, the scans, the student's own reactions, their preferences. Every read is a `Flow`. |
| [`data/FairRepository.kt`](app/src/main/java/com/su/clubfair/data/FairRepository.kt) | The single seam the UI reads the fair through, and where the server boundary is marked. |
| [`ui/FairViewModel.kt`](app/src/main/java/com/su/clubfair/ui/FairViewModel.kt) | `SessionState` and `FairUiState`, held across configuration changes. |
| `ui/**/…Screen.kt` | Stateless. Every screen takes its state and its callbacks, and keeps a `@Preview` default. |

[`ui/model/Student.kt`](app/src/main/java/com/su/clubfair/ui/model/Student.kt) is
the signed-in student. `visited` counts what this device has actually scanned and
`prizes` derives from it through
[`data/FairRules.kt`](app/src/main/java/com/su/clubfair/data/FairRules.kt).
`rank` is **nullable and always null**, because no phone can rank a student
against every other student — everything that renders it shows an em dash.
`PreviewStudent` is for `@Preview` only; nothing in the running app falls back to
it, and a screen with no session shows its signed-out state instead.

[`ui/model/Booth.kt`](app/src/main/java/com/su/clubfair/ui/model/Booth.kt) holds
the 27-club roster, and `boothRoster(scanned: Set<Int>)` ticks it against the
booth numbers actually collected. The flip the old design predicted has happened:
the booths are the source of truth and every count derives from them. Previously
the roster took a `visited` count and marked the first N booths scanned, so
scanning booth 23 first would have lit up booth 1 — invisible while the count was
a hardcoded 19, and a card showing the wrong booths the moment scanning was real.

Home, the pass and the profile all read one holder rather than their own
constants. They used to each carry their own copy of the name, ID, major and
school in `strings.xml`, which is three places for the same person to drift apart
the moment one of them changed.

### What this app keeps, and what it cannot

Everything is device-local. There is no server, and three things are answered on
the phone that a server will have to answer instead. Each is marked at its own
definition as well as here:

- **Sign-in** ([`data/PasswordHasher.kt`](app/src/main/java/com/su/clubfair/data/PasswordHasher.kt))
  checks the password against a PBKDF2 salt and hash held on the device — a
  random 128-bit salt, 120k iterations, and the password itself never stored.
  That is the right way to do the wrong thing: it proves the phone was told the
  correct password, not that the person holding it is who they say they are.
- **Checkpoints** ([`data/BoothCode.kt`](app/src/main/java/com/su/clubfair/data/BoothCode.kt))
  are asserted by the student's own device. The parser is strict about *format*
  now, which stops the app crediting a booth because the camera swept past a
  price tag — but every accepted form is guessable from a booth's number, so a
  student can still mint a code for a booth they never visited. Only a server
  signing each booth's code and recording who redeemed it fixes that.
- **The announcements channel** is a fixed seed list. There is nothing to read
  from and nothing to post to; the composer is behind an `isAdmin` flag no path
  currently sets.

`FairRepository.unsyncedScans` is the seam for the first of those: every scan
this device holds, oldest first, which is exactly the backlog to POST once there
is somewhere to POST it. Settings shows the count, because a student who assumes
their afternoon is safely recorded somewhere will find out otherwise by losing it.

A reinstall loses everything. Sign-out does not — the account and its scans
survive so signing back in works; registering a *different* student replaces the
account and clears the previous one's progress, which is the only shared-phone
case there is one account to handle.

### Localisation

`values/` is English, `values-th/` is Thai, and `resourceConfigurations` pins the
APK to those two so it stops carrying every locale AndroidX ships strings for.

**The Thai is a first pass and needs a native review before release.** Every
string is translated and the plurals and format arguments are correct, but the
register and tone have not been read by a Thai speaker — and this app's audience
reads Thai first. Two conventions worth keeping if the copy is rewritten: no
spaces between words (Thai does not word-space, and doing so is the fastest way
for a translation to read as machine output), and `<plurals>` carrying only an
`other` item, since Thai has no grammatical plural and supplying `one` would imply
a distinction the language does not draw.

`app_name` is deliberately `translatable="false"` — it is the name of the event,
not a description of it.

Dates and times are never pre-formatted into the model. `Announcement` carries an
instant and the channel formats it at draw time through `DateUtils`, which is what
makes a post readable in Thai, in a 24-hour locale, and still correct after
midnight. It used to be the string `"Today at 12:30"` baked into the data.

### Window background

`themes.xml` sets `android:windowBackground` to a dark colour. The app inherits
`android:Theme.Material.NoActionBar`, whose default background is light, and the
app is dark end to end — so that default only ever showed up as a **white flash**
between screens, or through anything that fades. It is what forced both the
sign-in flow's and the tab bar's transitions to be slide-only with no crossfade;
the dark colour is the backstop for anything that still slips through.

### Navigation

There is exactly one gate: **is anyone signed in?** `MainActivity` crossfades
between four phases — restoring, signed out, onboarding, the shell — and the
signed-out flow keeps its own `rememberSaveable` `AuthStep` enum plus a
`BackHandler` for Welcome → Login → RegisterGoogle → Register.

Routing used to be a single `Screen` enum that a tap moved through, which meant
"signed in" was a position in an animation rather than a fact: closing the app on
Home reopened it on Welcome, and there was nothing for a session to be restored
*into*.

Two things about that gate are easy to get wrong and both have bitten:

- **`SessionState.Restoring` is not a nicety.** Reading DataStore is
  asynchronous, so for the first frames after launch the app does not know
  whether anyone is signed in. Treating "not yet known" as "signed out" makes a
  returning student watch the Welcome screen flash past on every cold start. The
  bare backdrop is shown instead — it is what every screen stands on anyway, so
  the arrival reads as one continuous surface.
- **The crossfade keys on the phase, not on the session value.** Keying it on
  `SessionState` directly looks equivalent and is not: `SignedIn` carries the
  student, so every scan produces a new instance, `Crossfade` reads that as a
  different screen, and the whole signed-in tree is torn down and rebuilt — which
  in practice threw you back to the Home tab every time you collected a booth.

Screens swap through an `AnimatedContent` that slides horizontally over 380ms.
Direction comes from the `AuthStep` enum's ordinals — a higher ordinal is "deeper"
and slides in from the right, a lower one slides back from the left — so keep
the constants declared in flow order.

Navigation Compose is still not in use, and that is a deliberate hold rather than
an oversight: its two real wins here are deep links and a back stack, the app has
no web presence to link into, and the back behaviour is already explicit. What it
*would* cost is re-expressing every hand-tuned transition in this file as
per-destination `enterTransition`s. Revisit it when something outside the app needs
to point at a screen inside it.

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

## Accessibility

Not a full audit, and worth being clear about that — no TalkBack pass has been
sat through end to end, and the type scale has not been checked at 200% font
size. What has been fixed is the class of problem where a control is *invisible*
to a screen reader rather than merely awkward:

- **The checkpoint grid** was 27 undecorated boxes, so 27 stops that each said
  nothing. It is one node now describing the number it is a picture of.
- **The pass QR** is a `Canvas` with a decorative logo over it, which made the
  largest element on the screen completely silent. It names the id it carries, so
  a student using TalkBack can confirm they are holding up their own pass.
- **Reaction chips** were an emoji and a number read as two unrelated nodes.
  They are one `toggleable` announcing "👍, 63" with a state, so lit-or-not is
  carried by something other than a colour.
- **Detail rows** on the profile merge, so "Email, Not set" is one announcement.
- **Form errors** attach with `semantics { error(…) }` as well as being drawn, and
  the sign-in failure banner is an assertive live region — it arrives after a
  submit, when focus is nowhere near it. So is the scan result card, which appears
  while the phone is held up at a sign.
- **Touch targets**: the password reveal, the search clear/close, the torch and
  the legal links are all 48dp regardless of the size of the glyph or label inside
  them. Rows in Settings and Profile are `heightIn(min = 56.dp)`.
- **Headings** are marked on sheet titles and section labels, so heading
  navigation works instead of reading from the top each time.

Known gaps: no TalkBack walkthrough, no large-font-scale pass (several cards use
fixed `dp` heights that will clip before 200%), and no RTL check despite
`supportsRtl="true"`.

## Tests

```bash
./gradlew test              # 43 JVM unit tests
./gradlew connectedAndroidTest   # Compose UI tests, needs a device
```

The unit tests cover the logic that can be got wrong silently, which is most of
`data/`:

| | |
|---|---|
| `BoothCodeTest` | the strict parse, and specifically the payloads the old trailing-digit regex credited a booth for — a phone number, a room sign, a Wi-Fi QR |
| `ValidationTest` | phone normalisation across `+66`/spaces/dashes, the password rules and *which* rule failed, student ids |
| `FairDataTest` | prize milestones, the schedule's three phases, scan record encoding, roster ticking, search matching and ranking |
| `QrDecoderTest` | drives the real decode path on the JVM against a generated code |

This replaced `assertEquals(4, 2 + 2)`. The instrumented `AuthScreenTest` was also
asserting against fields the register screen had dropped long ago and passed
anyway, because nothing ran it — it drives the screens through their state objects
now, which is the only way the error paths are reachable.

Two habits worth keeping: `FairRepository` takes its clock as a parameter so tests
can hold time still, and `BoothCode.parse` takes the booth count rather than
reaching for a global, so a test does not depend on the roster's current length.
