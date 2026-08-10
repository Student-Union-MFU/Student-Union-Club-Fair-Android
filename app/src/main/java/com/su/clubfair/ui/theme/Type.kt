package com.su.clubfair.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.su.clubfair.R

/**
 * Alan Sans — static instances cut from the variable font's 300..900 weight axis.
 * Files live in res/font/.
 */
val AlanSans = FontFamily(
    Font(R.font.alan_sans_light, FontWeight.Light),
    Font(R.font.alan_sans_regular, FontWeight.Normal),
    Font(R.font.alan_sans_medium, FontWeight.Medium),
    Font(R.font.alan_sans_bold, FontWeight.Bold),
    Font(R.font.alan_sans_extrabold, FontWeight.ExtraBold),
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

private val d = Typography()

/** Material 3 type scale, restyled onto Alan Sans. */
val Typography = Typography(
    displayLarge = d.displayLarge.copy(fontFamily = AlanSans),
    displayMedium = d.displayMedium.copy(fontFamily = AlanSans),
    displaySmall = d.displaySmall.copy(fontFamily = AlanSans),
    headlineLarge = d.headlineLarge.copy(fontFamily = AlanSans),
    headlineMedium = d.headlineMedium.copy(fontFamily = AlanSans),
    headlineSmall = d.headlineSmall.copy(fontFamily = AlanSans),
    titleLarge = d.titleLarge.copy(fontFamily = AlanSans),
    titleMedium = d.titleMedium.copy(fontFamily = AlanSans),
    titleSmall = d.titleSmall.copy(fontFamily = AlanSans),
    bodyLarge = d.bodyLarge.copy(fontFamily = AlanSans),
    bodyMedium = d.bodyMedium.copy(fontFamily = AlanSans),
    bodySmall = d.bodySmall.copy(fontFamily = AlanSans),
    labelLarge = d.labelLarge.copy(fontFamily = AlanSans),
    labelMedium = d.labelMedium.copy(fontFamily = AlanSans),
    labelSmall = d.labelSmall.copy(fontFamily = AlanSans),
)
