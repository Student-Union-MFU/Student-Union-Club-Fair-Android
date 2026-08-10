package com.su.clubfair.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/**
 * How far a pane bends what is behind it, and over what depth.
 *
 * [DefaultRefractionHeight] is the thickness of the glass at its edge — how wide
 * the band is where the image bends — and [DefaultRefractionAmount] is how hard
 * it bends.
 *
 * These are Dp and used to be raw pixels, which was a bug rather than a
 * simplification: `BackdropEffectScope` is a `Density`, so the shader takes
 * whatever it is handed and a "40" tuned on a 420dpi phone is a third of the
 * bend on a 160dpi tablet and twice it on a 560dpi flagship. The numbers below
 * are the old pixel values at the density they were tuned on.
 */
val DefaultRefractionHeight = 15.dp
val DefaultRefractionAmount = 18.dp

/** Softening behind a pane. Wider than the refraction band, so the two read as one material. */
val DefaultGlassBlur = 4.5.dp

/**
 * The liquid glass material, from `io.github.kyant0:backdrop`.
 *
 * This is a genuinely different thing from the frosted pane in `Glass.kt`, and
 * the difference is refraction: the frost *blurs* what is behind it, while this
 * *bends* it, so the backdrop's arcs and blooms stretch and curve as they pass
 * under a pane's edge.
 *
 * All five layers of it come from the library, and using fewer is what made an
 * early version of the booth wall look like tinted rectangles rather than glass:
 *
 * - [vibrancy] lifts the saturation of what shows through, so the mesh's colour
 *   survives being seen through a translucent surface instead of going grey.
 * - [blur] softens it. Optional, and genuinely worth skipping over a still
 *   backdrop — it is the most expensive effect here.
 * - [lens] is the refraction, and the reason for the whole library.
 * - [highlight] is edge lighting computed from the shape's own signed distance
 *   field rather than a border painted round the outside. [Highlight.Default] is
 *   a crisp rim; [Highlight.Ambient] a softer, top-lit one. Both ramp round the
 *   outline by design — pass `null` and draw a plain border instead where an
 *   even edge is wanted, which is also the only edge an API 24 phone can show.
 * - [shadow] and [innerShadow] are what make a pane sit *on* something. A pane
 *   with no shadow is a hole in the screen the same colour as the screen, which
 *   is exactly what a glass card with no depth looks like.
 *
 * Everything shader-backed here needs `RuntimeShader`, so API 33+; the library
 * no-ops `lens` and the highlight below that and the blur below API 31. On this
 * app's `minSdk` a caller that needs a visible edge on old phones has to draw
 * one itself — see the booth wall.
 *
 * [backdrop] must be a layer that contains what the pane should see through
 * itself and **nothing it shouldn't** — a pane sampling a layer it is drawn
 * inside is sampling itself. In practice that means one `layerBackdrop` around
 * the wallpaper, with the panes outside it.
 */
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    shape: Shape,
    blurRadius: Dp = DefaultGlassBlur,
    refractionHeight: Dp = DefaultRefractionHeight,
    refractionAmount: Dp = DefaultRefractionAmount,
    surface: Color = Color.White.copy(alpha = 0.06f),
    highlight: Highlight? = Highlight.Default,
    shadow: Shadow? = null,
    innerShadow: InnerShadow? = null,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        vibrancy()
        // Skipped entirely at zero rather than passed a zero radius: the blur is
        // a sampling pass over the backdrop whether or not it moves anything.
        if (blurRadius > 0.dp) blur(blurRadius.toPx())
        lens(
            refractionHeight = refractionHeight.toPx(),
            refractionAmount = refractionAmount.toPx(),
            depthEffect = true,
        )
    },
    highlight = { highlight },
    shadow = { shadow },
    innerShadow = { innerShadow },
    onDrawSurface = { drawRect(surface) },
)
