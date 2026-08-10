package com.su.clubfair.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The app's one colour scheme.
 *
 * This replaced a set of four per-tab "ecosystem" palettes — a green rainforest,
 * an orange savannah, a blue ocean and a darker abyss — that swapped as you
 * swiped. Four backdrops meant four contrast situations, four accents and four
 * versions of every chip to keep looking right, and the app read as four apps
 * sharing a nav bar. One palette, one backdrop: the surface is the constant and
 * the content is what changes.
 *
 * The scheme is a single deep-green ground lit by soft blooms (see
 * `MeshBackground`) under a bright lime accent. Everything is built to sit under
 * white text and the white-alpha glass in `Glass.kt`, which only reads as frost
 * over something dark — so keep [Base] and [Floor] near-black if they ever move.
 */
object Palette {
    /** Top of the vertical wash. Near-black, barely green. */
    val Base = Color(0xFF0B120E)

    /**
     * Bottom of the wash, and the contrast floor the whole UI stands on. Also
     * what Material paints for any surface it insists on filling.
     */
    val Floor = Color(0xFF050806)

    /** The lime bloom — the brightest thing in the backdrop, and the only warm one. */
    val GlowLime = Color(0xFFA8E065)

    /** The emerald bloom: the wide, mid-value fill that gives the ground its hue. */
    val GlowEmerald = Color(0xFF1E7A4E)

    /** The coldest bloom, sitting deepest. Keeps the green from going monotone. */
    val GlowDeep = Color(0xFF0D5A55)

    /** The interactive accent: CTAs, the scan ring, ticks, progress. */
    val Accent = Color(0xFFC6F16C)

    /** Dark ink for text sitting on [Accent] or [Paper]. */
    val Ink = Color(0xFF0A1408)

    /** Near-white for the pass. Kept close to white — the pass gets scanned. */
    val Paper = Color(0xFFF1F6EA)

    /**
     * The one opaque panel tone, for surfaces that genuinely can't be glass —
     * the register form's dropdown opens over its own fields, so a translucent
     * menu would show the field it is covering straight through its options.
     */
    val Panel = Color(0xFF11201A)

    /**
     * Something is wrong: a rejected field, a code that isn't a booth's.
     *
     * Coral rather than a signal red. The app stands on a near-black green under
     * a lime accent, and a saturated red on that ground is the loudest thing on
     * any screen it appears on — which is the wrong weight for "check this
     * field". Warm enough to read as a warning against green, light enough to
     * hold its own contrast on [Base], and far enough from [Accent] in hue that
     * a lit chip and a rejected field are never confusable.
     */
    val Alert = Color(0xFFFF9E80)
}

/**
 * The three areas of the fair floor: Rainforest, Savannah, Deep Ocean.
 *
 * These were three steps along the app's own green, on the principle that one
 * palette keeps the app looking like one app. The areas are themed habitats now
 * — named after them, drawn as them — and a habitat that isn't its own colour
 * isn't a habitat. A savannah rendered in the same green as a rainforest is a
 * label, not a place.
 *
 * The rule the old comment was protecting still holds, and is why this is a
 * short list rather than a free-for-all: these three appear on the zone cards
 * and inside a zone's map, and nowhere else. Everything outside the Booths tab
 * stays on [Palette.Accent].
 */
object ZoneAccent {
    /** Canopy green — the closest of the three to the app's own accent. */
    val Rainforest = Color(0xFF6FD98F)

    /** Dry-grass gold. The one warm colour in the app, and it earns it here. */
    val Savannah = Color(0xFFE0A94A)

    /** Shallow-water cyan, not navy: it has to hold white text on a dark ground. */
    val DeepOcean = Color(0xFF4CC1E6)
}

/**
 * The accent in force for the surrounding subtree.
 *
 * [Palette.Accent] everywhere except inside a zone's booth cards, where the
 * booths list re-provides the zone's own step. Providing it rather than
 * threading a colour through means an icon tile, number chip or tick nested in a
 * card picks it up for free and can't forget to.
 *
 * Static rather than dynamic: it changes per section of a list, not per frame,
 * and static means the subtree under the provider invalidates as one.
 */
val LocalAccent = staticCompositionLocalOf { Palette.Accent }

/**
 * White at the alphas the UI leans on.
 *
 * These were redeclared privately in eight screens, which is how three of them
 * ended up with slightly different "muted" greys. They stay pure white rather
 * than becoming palette-tinted: tinting body text with the backdrop's hue is
 * what makes a themed app look like a colour filter was dropped over it.
 */
object Ink {
    /** Section labels and secondary headings. */
    val Label = Color(0xCCFFFFFF)

    /** Supporting copy — captions, counts, meta lines. */
    val Muted = Color(0x8FFFFFFF)

    /** Dividers, empty checkpoint cells, disabled glyphs. */
    val Faint = Color(0x59FFFFFF)

    /** Input placeholders. */
    val Placeholder = Color(0xB3FFFFFF)
}
