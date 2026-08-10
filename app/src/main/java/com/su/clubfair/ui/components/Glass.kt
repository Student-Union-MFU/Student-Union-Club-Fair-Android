package com.su.clubfair.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * A frosted pane — glassmorphism.
 *
 * The material is deliberately flat: a blurred backdrop, one even translucent
 * fill, one hairline. Nothing else.
 *
 * The fill was briefly a diagonal gradient — brighter at the top-left corner,
 * falling off to the bottom-right — to match a reference mockup. It looked worse
 * on real screens than it did in the mockup, and it is not coming back: a pane
 * whose own face has a tonal ramp competes with the backdrop showing *through*
 * it, which is the only thing a pane this translucent is supposed to be showing.
 * One even alpha lets what is behind the glass do the work.
 *
 * It used to carry a stack of painted edge lighting too — an inner lip, a
 * three-stop rim, a diagonal specular. That is the vocabulary of Apple's Liquid
 * Glass, which describes a *thick slab* with light bending around its edges.
 * Glassmorphism is the opposite idea: a thin, even sheet of frost that is
 * interesting only because of what sits blurred behind it. Painting highlights
 * onto the front doesn't make it glassier, it makes it read as an imitation of
 * the other material.
 *
 * There is no backdrop blur here at all any more. It used to take a Haze state,
 * and only the nav bar ever passed one; that bar now uses the liquid glass
 * material in `GlassNavBar.kt` instead, which refracts as well as blurs. Every
 * remaining caller is a card or a field, where a blur pass per pane on a
 * scrolling grid of 27 buys nothing the frost fill isn't already doing.
 *
 * [intensity] ramps 0..1 for focus or selection, lifting the frost and the edge
 * together, the way more light falling on real frosted glass would.
 */
@Composable
fun Modifier.glassSurface(
    cornerRadius: Dp,
    intensity: Float = 0f,
): Modifier = glassSurface(
    shape = RoundedCornerShape(cornerRadius),
    intensity = intensity,
)

/** [glassSurface] over an arbitrary outline — the ticket's notched shape needs this. */
@Composable
fun Modifier.glassSurface(
    shape: Shape,
    intensity: Float = 0f,
): Modifier {
    val fill = lerp(0.10f, 0.17f, intensity)
    val edge = lerp(0.22f, 0.38f, intensity)

    return this
        .background(color = Color.White.copy(alpha = fill), shape = shape)
        // One hairline, even the whole way round. It used to ramp from bright at
        // the top-left to dim at the bottom-right, on the theory that a light
        // would catch it there — but at 1dp a gradient doesn't read as lighting,
        // it just reads as a border that fades out, which looks like a rendering
        // fault rather than a material.
        .border(width = 1.dp, color = Color.White.copy(alpha = edge), shape = shape)
    // No Modifier.shadow: Android renders elevation shadows assuming an opaque
    // surface, so under a translucent fill the shadow reads straight through the
    // glass as a dark inner rectangle.
}
