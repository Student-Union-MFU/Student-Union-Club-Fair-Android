package com.su.clubfair.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.su.clubfair.R

/**
 * Anuphan — the app's one text face, for Thai and Latin alike.
 *
 * It replaced Alan Sans, which had **no Thai glyphs at all**: 494 codepoints
 * mapped and not one in U+0E00–U+0E7F. Every Thai screen was therefore falling
 * back to whatever the phone ships — Noto Sans Thai on Android — so half the
 * users of a deliberately bilingual app were reading a typeface nobody chose,
 * and the two languages did not look like the same product. That is the whole
 * reason for the change; the rest is a bonus.
 *
 * Anuphan is Cadson Demak's, drawn for user interfaces with the two scripts
 * designed together rather than a Latin face with Thai bolted on. Its skeleton
 * is round, which matters here: this app is built out of circles — 54 uses of
 * `CircleShape`, a pill nav bar, circular chips and avatars — and an angular
 * face against that reads as two design systems sharing a screen. What it drops
 * is Alan Sans's *softness*: narrower, with open apertures, so the countdown's
 * big digits stop closing up into blobs at 40sp.
 *
 * Static instances cut from the variable font's 100..700 `wght` axis, not the
 * variable font itself. Variation settings need API 26 and this app's floor is
 * 24, so shipping the variable font would collapse every weight into one
 * instance on Android 7 — survivable for the wordmark (see [Bitcount]) and not
 * for the face the whole UI is set in.
 *
 * **No ExtraBold.** The axis stops at 700, where Alan Sans went to 900. Compose
 * resolves `FontWeight.ExtraBold` to the nearest cut, so the three places that
 * ask for it get Bold. Worth knowing before adding a fourth.
 */
val AppSans = FontFamily(
    // Thin and ExtraLight exist for one thing: the countdown figure on Home,
    // which is large enough to carry a hairline weight. Nothing at body size
    // should reach for either — below about 20sp these go to grey mush on a
    // dark ground.
    Font(R.font.anuphan_thin, FontWeight.Thin),
    Font(R.font.anuphan_extralight, FontWeight.ExtraLight),
    Font(R.font.anuphan_light, FontWeight.Light),
    Font(R.font.anuphan_regular, FontWeight.Normal),
    Font(R.font.anuphan_medium, FontWeight.Medium),
    Font(R.font.anuphan_bold, FontWeight.Bold),
)

/** Perfect Romantic — the display serif the "Club Fair" wordmark used to be set in. */
val PerfectRomantic = FontFamily(
    Font(R.font.perfect_romantic, FontWeight.Normal),
)

/**
 * Bitcount Prop Single — the dot-matrix display face the wordmark is set in now.
 *
 * Shipped as the variable font rather than a static cut, because the axes are the
 * whole point of this family and the instance we want isn't a named one:
 *
 *  - `wght` 300 — each pixel is drawn as a dot, so weight is dot *diameter*, not
 *    stroke thickness. Past about 600 the dots swell until they touch and the
 *    grid closes into blobby continuous strokes, which loses the one thing the
 *    face is for. Lighter goes the other way: the dots shrink and separate, so
 *    the wordmark reads as a sparse matrix rather than a solid word. 300 is
 *    about as far as that can go before the letters stop holding together at
 *    a glance.
 *  - `CRSV` 0 — the family ships defaulting to 0.5, half-way into the cursive
 *    forms. A blend is the wrong default for a wordmark: it gives `a` and `e` a
 *    handwritten slant that fights the rigid pixel grid the face is built on.
 *
 * `ELXP`/`ELSH` (element expansion and shape) stay at 0 — square, unexpanded
 * pixels. Variation settings need API 26; on 24 and 25 this falls back to the
 * font's default instance, which is legible, just lighter and half-cursive.
 */
@OptIn(ExperimentalTextApi::class)
val Bitcount = FontFamily(
    Font(
        resId = R.font.bitcount_prop_single,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(300),
            FontVariation.Setting("CRSV", 0f),
        ),
    ),
)

/**
 * The weight the app's own text is set in.
 *
 * One constant rather than a decision per call site, because "how heavy is this
 * app" is a single design question and it was previously answered 150 times —
 * 60 Normal, 44 Medium, 42 Bold — by whoever wrote each screen. Moving it is now
 * one edit; before, it was a day of grep.
 *
 * It applies to text drawn in a **primary** colour: white, the accent, a zone's
 * own hue. Text in the muted inks — `Ink.Muted`, `Ink.Label`, `Ink.Faint`,
 * `Ink.Placeholder` — is deliberately left alone, because that is what a label
 * is in this app and labels earn their subordination through tone rather than
 * through weight. Making those lighter too would push them past legible.
 *
 * ⚠ Below `Light` this stops being a style choice. Hairline white on the dark
 * glass this app is built on is the first thing to disappear on a dim OLED or a
 * cheap LCD, and body text at 12–14sp has no strokes to spare — which is why it
 * settled here at `Normal`, a weight that holds up on a bad screen in a bright
 * hall and still reads as light against the Bold this app used to be.
 */
val AppTextWeight = FontWeight.Normal

private val d = Typography()

/** Material 3 type scale, restyled onto Alan Sans. */
val Typography = Typography(
    displayLarge = d.displayLarge.copy(fontFamily = AppSans),
    displayMedium = d.displayMedium.copy(fontFamily = AppSans),
    displaySmall = d.displaySmall.copy(fontFamily = AppSans),
    headlineLarge = d.headlineLarge.copy(fontFamily = AppSans),
    headlineMedium = d.headlineMedium.copy(fontFamily = AppSans),
    headlineSmall = d.headlineSmall.copy(fontFamily = AppSans),
    titleLarge = d.titleLarge.copy(fontFamily = AppSans),
    titleMedium = d.titleMedium.copy(fontFamily = AppSans),
    titleSmall = d.titleSmall.copy(fontFamily = AppSans),
    bodyLarge = d.bodyLarge.copy(fontFamily = AppSans),
    bodyMedium = d.bodyMedium.copy(fontFamily = AppSans),
    bodySmall = d.bodySmall.copy(fontFamily = AppSans),
    labelLarge = d.labelLarge.copy(fontFamily = AppSans),
    labelMedium = d.labelMedium.copy(fontFamily = AppSans),
    labelSmall = d.labelSmall.copy(fontFamily = AppSans),
)
