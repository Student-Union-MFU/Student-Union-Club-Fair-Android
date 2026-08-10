package com.su.clubfair.ui.scene

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.su.clubfair.ui.theme.Palette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * One full circuit of the blooms, in milliseconds.
 *
 * Very long on purpose. The backdrop is behind every screen in the app, so it
 * has to be something you notice once and then stop seeing; at anything under
 * half a minute the movement starts competing with the content for attention.
 */
private const val DriftMillis = 42_000

/**
 * The app's backdrop: a dark green ground lit by a handful of soft blooms.
 *
 * The same on every screen — welcome, the auth forms, all four tabs, the pass and
 * the scanner. That is the whole point of it. This used to be four illustrated
 * ecosystems that panned as you swiped tabs, each with its own hue, silhouettes,
 * light shafts and drifting motes; every screen therefore had to work over four
 * different grounds, and the app changed colour under you as you moved through it.
 *
 * A mesh gradient does the job a backdrop actually has to do here: give the glass
 * something with structure to blur, keep the contrast floor under white text
 * predictable, and look like one place. Nothing in it is keyed to a screen, so
 * there is no per-screen parameter to get wrong — it is `MeshBackground()`
 * everywhere or nothing.
 *
 * The blooms drift on their own slow circles rather than sitting still. Static
 * radial gradients on a phone read as a printed image behind glass; a bloom that
 * has moved a few percent by the time you come back to a screen reads as light.
 */
@Composable
fun MeshBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mesh")
    // Held as a State and read inside the draw block rather than unwrapped with
    // `by`: this changes every frame, and reading it in composition would
    // recompose this node sixty times a second for what is only ever a redraw.
    val drift: State<Float> = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(DriftMillis, easing = LinearEasing),
        ),
        label = "drift",
    )

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                // Built once per size, not per frame: nothing in here moves, so
                // there is nothing a redraw could change.
                val arcs = arcPaths(size)

                onDrawBehind {
                    drawGround()
                    drawBlooms(drift.value)
                    drawArcs(arcs)
                    // Dither before the trim, not after: it is there to break up
                    // the banding in everything above it.
                    drawRect(brush = Grain, alpha = 0.035f)
                    drawTrim()
                }
            },
    )
}

// -------------------------------------------------------------------- ground

/** The base wash every bloom is lit onto. */
private fun DrawScope.drawGround() {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Palette.Base,
            0.58f to Color(0xFF080E0B),
            1f to Palette.Floor,
        ),
    )
}

// -------------------------------------------------------------------- blooms

/**
 * One soft light in the mesh.
 *
 * [at] and [sway] are fractions of the screen, [radius] a fraction of its
 * longest side — so the composition holds its shape from a small phone to a
 * tablet instead of the blooms bunching into one corner.
 */
private class Bloom(
    val color: Color,
    val at: Offset,
    val radius: Float,
    val alpha: Float,
    val sway: Offset,
    val phase: Float,
)

/**
 * The composition, back to front.
 *
 * It is deliberately lopsided: the bright lime sits high and to the right, the
 * wide emerald fills the left, and the cold deep-green anchors the bottom. An
 * evenly spread set of blobs averages out into a flat tint — the whole reason
 * this reads as light rather than as a colour filter is that there are dark
 * lanes of the base left visible between the blooms.
 *
 * The alphas are the thing to be careful with. Turned up, each bloom floods far
 * enough to meet the next and the screen ends up one hue; kept here, white text
 * clears 4.5:1 anywhere on the screen once `drawTrim` has run.
 */
private val Blooms = listOf(
    // Off the right edge by design: a light with no visible source reads as
    // depth, one whose centre is on screen reads as a lens flare stuck to the
    // glass.
    Bloom(Palette.GlowLime, Offset(0.92f, 0.06f), 0.60f, 0.30f, Offset(0.045f, 0.035f), 0.00f),
    Bloom(Palette.GlowEmerald, Offset(0.06f, 0.34f), 0.82f, 0.44f, Offset(0.055f, 0.045f), 0.34f),
    Bloom(Palette.GlowDeep, Offset(0.66f, 0.84f), 0.72f, 0.38f, Offset(0.050f, 0.055f), 0.62f),
    // A last touch of lime under the nav bar, so the bottom of the screen isn't
    // dead ground on every tab.
    Bloom(Palette.GlowLime, Offset(0.18f, 1.04f), 0.44f, 0.20f, Offset(0.040f, 0.030f), 0.85f),
)

private fun DrawScope.drawBlooms(drift: Float) {
    val longest = maxOf(size.width, size.height)

    Blooms.forEach { bloom ->
        val angle = (drift + bloom.phase) * 2f * PI.toFloat()
        val center = Offset(
            (bloom.at.x + cos(angle) * bloom.sway.x) * size.width,
            (bloom.at.y + sin(angle) * bloom.sway.y) * size.height,
        )

        // Screen rather than the default source-over: these are lights, and light
        // falling on a dark surface lifts it without washing it toward grey the
        // way a translucent overlay does.
        //
        // Three stops, not two. A linear falloff to transparent leaves a visible
        // rim where the gradient ends; pulling the midpoint down to roughly a
        // third is what makes the edge disappear into the ground.
        drawRect(
            brush = Brush.radialGradient(
                0f to bloom.color.copy(alpha = bloom.alpha),
                0.45f to bloom.color.copy(alpha = bloom.alpha * 0.34f),
                1f to Color.Transparent,
                center = center,
                radius = bloom.radius * longest,
            ),
            blendMode = BlendMode.Screen,
        )
    }
}

// ---------------------------------------------------------------------- arcs

/**
 * A single long arc, cropped by the screen.
 *
 * These are circles whose centres sit **outside** the screen, so all you ever see
 * is one shallow curve grazing a corner. That is the whole trick, and it is why
 * the centres below have coordinates like `-0.2` and `1.3`: put the centre on
 * screen and you get a ring, which is a shape the eye reads as a *thing*. Put it
 * off screen and you get a curve the eye reads as an edge of something much
 * larger, which is the note the reference hits.
 *
 * [at] is a fraction of the screen; [radius] a fraction of its shorter side, so
 * the arcs keep their curvature rather than stretching on a tall phone.
 *
 * **The trap:** because the centre is off screen, a radius that sounds generous
 * can still leave the whole circle outside the viewport, and the arc silently
 * doesn't render — no error, just nothing. [radius] has to exceed the distance
 * from [at] to the nearest visible pixel, which on a tall phone is easily more
 * than one shorter-side. Two of the three sweeps below were invisible for exactly
 * this reason on the first pass. If an arc doesn't show up, check that before
 * touching its alpha.
 *
 * This layer has been rebuilt twice and both dead ends are worth recording. It
 * started as families of parallel sine waves — the default mesh-gradient
 * decoration, and it read as exactly that. It then became two nests of
 * topographic contour rings around the blooms, which was a genuinely good-looking
 * landform and far too loud: it turned the backdrop into the thing you looked at.
 * A backdrop's job is to be interesting when you look for it and invisible when
 * you don't.
 */
private class Arc(
    val at: Offset,
    val radius: Float,
    val alpha: Float,
    /** Lead arcs are heavier than the ones trailing them. */
    val width: Dp,
    val color: Color,
)

/** The lead arc of a pair, and the ones trailing it. */
private val LeadWidth = 1.6.dp
private val TrailWidth = 1.dp

/**
 * Three sweeps of three, cropped into three corners.
 *
 * Grouped rather than evenly spaced: arcs close together read as one edge with
 * highlights falling off it, which is more interesting than seven lines at equal
 * intervals and costs nothing but paths. Within a sweep the innermost is the lead
 * — heaviest and brightest — and the two behind it fall away.
 *
 * The bottom-left sweep carries higher alphas than the top-right one for the same
 * apparent weight: `drawTrim` runs after this and lays 44% of `Floor` over the
 * bottom edge against 34% at the top, over ground that is already darker down
 * there. Tune these against a screenshot, not against each other.
 */
private val Arcs = listOf(
    // Bottom left. The centre is nearly a full screen below the bottom edge, so
    // these radii have to be large — see the note above about the trap.
    Arc(Offset(-0.22f, 1.30f), 1.05f, 0.30f, LeadWidth, Color(0xFFA9DFC2)),
    Arc(Offset(-0.22f, 1.30f), 1.13f, 0.19f, TrailWidth, Color(0xFFA9DFC2)),
    Arc(Offset(-0.22f, 1.30f), 1.21f, 0.11f, TrailWidth, Color(0xFFA9DFC2)),
    // Top right, under the lime bloom. Its centre sits closest to the screen, so
    // it is the one sweep whose radii are under 1.
    Arc(Offset(1.34f, -0.16f), 0.68f, 0.26f, LeadWidth, Color(0xFFD8F2AE)),
    Arc(Offset(1.34f, -0.16f), 0.77f, 0.16f, TrailWidth, Color(0xFFD8F2AE)),
    Arc(Offset(1.34f, -0.16f), 0.87f, 0.10f, TrailWidth, Color(0xFFD8F2AE)),
    // Upper left, so the composition isn't two matching corners and nothing else.
    Arc(Offset(-0.30f, -0.10f), 1.02f, 0.22f, LeadWidth, Color(0xFFBCE7B4)),
    Arc(Offset(-0.30f, -0.10f), 1.10f, 0.13f, TrailWidth, Color(0xFFBCE7B4)),
)

/** Every arc, resolved to a path once per size. */
private fun arcPaths(size: Size): List<Pair<Path, Arc>> {
    val unit = minOf(size.width, size.height)

    return Arcs.map { arc ->
        val path = Path().apply {
            addOval(
                Rect(
                    center = Offset(arc.at.x * size.width, arc.at.y * size.height),
                    radius = arc.radius * unit,
                ),
            )
        }
        path to arc
    }
}
/**
 * The arcs, static under the moving light.
 *
 * Deliberately not animated. The blooms drift, and the read of the layer is that
 * light is moving over something that isn't — give the lines their own motion and
 * the two just slide past each other, which looks like a screensaver.
 *
 * Stroked with a radial gradient centred on the screen rather than a flat colour,
 * so an arc dissolves toward the edges instead of running clean off them; a
 * hairline that hits the screen edge at full strength reads as a UI divider
 * someone forgot to inset.
 *
 * The lead arc of each pair is drawn a shade heavier than a hairline. At 1dp and
 * these alphas the pairs were disappearing on the darker half of the screen, and
 * lifting the alpha alone just made them look like brighter hairlines rather than
 * like an edge with weight to it.
 */
private fun DrawScope.drawArcs(arcs: List<Pair<Path, Arc>>) {
    val longest = maxOf(size.width, size.height)

    arcs.forEach { (path, arc) ->
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                0.00f to arc.color.copy(alpha = arc.alpha),
                0.60f to arc.color.copy(alpha = arc.alpha),
                1.00f to Color.Transparent,
                center = Offset(size.width * 0.5f, size.height * 0.45f),
                radius = longest * 0.66f,
            ),
            style = Stroke(width = arc.width.toPx()),
        )
    }
}

// ---------------------------------------------------------------------- trim

/**
 * Vignette and the contrast floor under the UI.
 *
 * The flat wash is the important half. Every screen puts white text and 10%
 * white glass over this, and a bloom is bright enough at its centre to take body
 * copy under 4.5:1 on its own. Knocking the mesh back toward [Palette.Floor]
 * costs almost nothing visually — it is the scheme's own darkest tone — and buys
 * the entire UI a guaranteed backdrop.
 *
 * The top and bottom bands are doing a second job: the status bar and the
 * floating nav bar both sit over whatever the mesh happens to be doing there.
 */
private fun DrawScope.drawTrim() {
    drawRect(color = Palette.Floor.copy(alpha = 0.18f))
    drawRect(
        brush = Brush.radialGradient(
            0.55f to Color.Transparent,
            1f to Palette.Floor.copy(alpha = 0.48f),
            center = Offset(size.width * 0.5f, size.height * 0.42f),
            radius = maxOf(size.width, size.height) * 0.78f,
        ),
    )
    drawRect(
        brush = Brush.verticalGradient(
            0f to Palette.Floor.copy(alpha = 0.34f),
            0.22f to Color.Transparent,
            0.72f to Color.Transparent,
            1f to Palette.Floor.copy(alpha = 0.44f),
        ),
    )
}

// --------------------------------------------------------------------- grain

/**
 * A tiled noise brush, at the alpha where you feel it and don't see it.
 *
 * Full-screen gradients over a narrow, very dark range band badly on 8-bit
 * panels — the ground here is barely a dozen steps of green from top to bottom,
 * and without this it arrives as a dozen visible stripes. Dithering with noise
 * is the standard fix and the only one available here: `RuntimeShader` would be
 * cleaner but it needs API 33 and this app ships to 24.
 *
 * Cached in a top-level `by lazy` because it is 64×64 and identical everywhere.
 */
private val Grain: ShaderBrush by lazy {
    val edge = 64
    val rng = Random(7)
    val pixels = IntArray(edge * edge) {
        val v = rng.nextInt(256)
        (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    val bitmap = Bitmap.createBitmap(edge, edge, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, edge, 0, 0, edge, edge)
    ShaderBrush(ImageShader(bitmap.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
}
