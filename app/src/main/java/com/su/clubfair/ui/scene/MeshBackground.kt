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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.su.clubfair.ui.theme.Palette
import kotlin.math.PI
import kotlin.math.floor
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
 * How many positions the drift is allowed to stop at over one circuit.
 *
 * This exists because the drift was costing the whole app its frame budget, and
 * the cost had nothing to do with how far anything moved.
 *
 * [drift] changes on every animation frame. Read straight into the draw block it
 * invalidates a full-screen redraw sixty times a second — and because this mesh
 * is what every pane of liquid glass in the app samples through `layerBackdrop`,
 * each of those redraws also re-runs the vibrancy and lens shaders on every pane
 * on screen, against a source that is never still long enough to cache. Measured
 * on a Pixel 7: every screen in the app rendered 302 frames per five idle
 * seconds at 21–27ms a frame, doing nothing, on a backdrop whose blooms take
 * forty-two seconds to go round once. Freezing the drift took the same five
 * seconds to zero frames.
 *
 * So the value is stepped and read through a `derivedStateOf`, which recomputes
 * whenever [drift] moves but only wakes the draw when the *step* changes. At 256
 * steps over 42 seconds that is a redraw about every 164ms rather than every
 * 16ms — a tenth of the work for movement nobody can see is missing, since one
 * step shifts a bloom by under three pixels and it is a soft radial gradient
 * being shifted.
 *
 * Turn this up and the app pays for it linearly, in battery as much as in frames.
 */
private const val DriftSteps = 256

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

    // Moving the read out of composition stopped this recomposing every frame; it
    // did not stop it *redrawing* every frame, which on a full-screen backdrop
    // that every glass pane samples is the expensive half. The step is what fixes
    // that — see [DriftSteps]. `derivedStateOf` is the whole mechanism: it is read
    // by the draw block below, and it only notifies that reader when the stepped
    // value actually differs from the last one.
    val stepped: State<Float> = remember(drift) {
        derivedStateOf { floor(drift.value * DriftSteps) / DriftSteps }
    }

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                // Built once per size, not per frame: nothing in here moves, so
                // there is nothing a redraw could change.
                val arcs = arcPaths(size)
                val canopy = canopyLeaves(size)

                onDrawBehind {
                    drawGround()
                    drawBlooms(stepped.value)
                    // Between the blooms and the arcs: the pools are light, so
                    // they belong with the other lights, and the arcs read as an
                    // edge in front of all of it.
                    drawDapple()
                    drawArcs(arcs)
                    // Over the arcs, because a leaf passing in front of a distant
                    // edge is the whole point of having both.
                    drawCanopy(canopy)
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

// -------------------------------------------------------------------- dapple

/**
 * Light broken into pools, the way it arrives through a canopy.
 *
 * The blooms are four big even washes, which is what made the backdrop read as a
 * nice gradient rather than as a place. Sun through leaves is not even — it is
 * pools, most of them small, clustered where the gap in the canopy is and
 * thinning away from it. Same primitive as a bloom, an order of magnitude
 * smaller and more of them.
 *
 * Static, unlike the blooms. They drift because a light with a source off the
 * screen can plausibly move; these are the shadow of a canopy that is nailed to
 * the top of the screen, so moving them would be the one thing that gave the
 * effect away. Being static also means they cost nothing the blooms are not
 * already costing — see [DriftSteps] for why that matters here.
 *
 * All lime, all screen-blended. A pool in the emerald or the deep green would be
 * a coloured light with no source, which is a stain rather than a shaft.
 */
private val Dapple = listOf(
    // The gap: a loose cluster under the lime bloom, top right.
    Triple(Offset(0.78f, 0.09f), 0.085f, 0.16f),
    Triple(Offset(0.64f, 0.04f), 0.060f, 0.16f),
    Triple(Offset(0.90f, 0.17f), 0.070f, 0.16f),
    Triple(Offset(0.70f, 0.19f), 0.045f, 0.16f),
    Triple(Offset(0.52f, 0.12f), 0.040f, 0.16f),
    // What gets through further down, sparser and smaller.
    Triple(Offset(0.28f, 0.50f), 0.055f, 0.16f),
    Triple(Offset(0.44f, 0.42f), 0.038f, 0.16f),
    Triple(Offset(0.86f, 0.38f), 0.050f, 0.16f),
    Triple(Offset(0.18f, 0.70f), 0.045f, 0.16f),
    Triple(Offset(0.60f, 0.63f), 0.034f, 0.16f),
    Triple(Offset(0.36f, 0.28f), 0.030f, 0.16f),
    Triple(Offset(0.94f, 0.55f), 0.040f, 0.16f),
)

private fun DrawScope.drawDapple() {
    val longest = maxOf(size.width, size.height)

    Dapple.forEach { (at, radius, alpha) ->
        drawRect(
            brush = Brush.radialGradient(
                0f to Palette.GlowLime.copy(alpha = alpha),
                0.45f to Palette.GlowLime.copy(alpha = alpha * 0.34f),
                1f to Color.Transparent,
                center = Offset(at.x * size.width, at.y * size.height),
                radius = radius * longest,
            ),
            blendMode = BlendMode.Screen,
        )
    }
}

// -------------------------------------------------------------------- canopy

/**
 * Big soft leaf shadows hanging into the top corners.
 *
 * Same trick as the arcs below: the branch point sits *outside* the viewport, so
 * what you see is foliage passing overhead rather than a plant standing on the
 * screen. Put the origin in view and it stops being a canopy.
 *
 * **Shadow, not drawing.** This went through a line-work version — pale outlined
 * leaves on curved stems, botanically correct and much more detailed — and it was
 * wrong for the same reason the mesh has no illustration in it: the backdrop's job
 * is to be interesting when you look for it and invisible when you don't, and
 * legible foliage is a thing you look at. A soft dark mass overhead is weather.
 *
 * **Nothing here has an edge.** An earlier build outlined the near leaves with a
 * hairline and a midrib, which is what made them read as unfinished: on the dark
 * top band the fills barely registered and all that survived was the outline, so
 * the canopy came out as wireframe. A shadow has no rim. Take the strokes away and
 * the same shapes read as shade.
 *
 * The softness is a stack of concentric copies rather than a blur, because
 * `RenderEffect` needs API 31 and this app ships to 24 — the backdrop should not
 * be a different design on an older phone. Six steps, not three: three was visible
 * *as* three, each copy's outline showing as its own hard edge, and the leaf came
 * out faceted like stacked paper cut-outs.
 *
 * The stack is a **feather on the edge, not a fade across the whole leaf.** A
 * version that grew each copy by half its own width again spread the falloff over
 * sixty-odd pixels and dissolved the silhouette entirely — a stain where a leaf
 * should be. The copies now grow by a fifth at most, and the core carries most of
 * the weight on its own.
 *
 * Confined to the top corners. The greeting, the profile button and the first card
 * all live in the upper middle, and decoration must never sit behind type.
 */
private class LeafFan(
    /** Branch point, in screen fractions. Outside the viewport by design. */
    val at: Offset,
    /** Degrees clockwise from three o'clock, at the middle of the fan. */
    val facing: Float,
    val count: Int,
    val spread: Float,
    /** Fractions of the shorter side, so a leaf keeps its proportions on any screen. */
    val length: Float,
    val width: Float,
)

/** The back plane: bigger, softer, and about half the weight. */
private val Far = listOf(
    LeafFan(Offset(-0.136f, -0.033f), 48f, 4, 46f, 0.53f, 0.148f),
    LeafFan(Offset(1.123f, -0.044f), 128f, 4, 44f, 0.58f, 0.158f),
)

/** The front plane: shorter, denser, and darker where it overlaps. */
private val Near = listOf(
    LeafFan(Offset(-0.049f, -0.067f), 58f, 3, 36f, 0.41f, 0.099f),
    LeafFan(Offset(1.057f, -0.073f), 120f, 3, 34f, 0.43f, 0.104f),
)

/**
 * One leaf, as a stack of outlines from widest to actual size.
 *
 * The first attempt softened each leaf with a radial gradient from its stem,
 * which looked right in the geometry and rendered as nothing: the stem is off
 * screen by design, so the fade was at full strength over the only part anyone
 * can see.
 */
private class Leaf(val layers: List<Path>)

/**
 * Deterministic jitter in `-1..1`.
 *
 * An exactly even spread reads as a rendered fan rather than as leaves — evenness
 * is the giveaway. Hashed rather than [Random] so a phone draws the same canopy
 * every launch: this is scenery, and scenery that reshuffles on rotation is a
 * distraction.
 */
private fun jitter(seed: Int): Float {
    val x = sin(seed * 12.9898f) * 43758.547f
    return (x - floor(x)) * 2f - 1f
}

/** How far each copy is grown, as (length, width), widest first. */
private val FarLayers = List(6) { i ->
    val t = 1f - i / 5f
    (1f + 0.10f * t) to (1f + 0.28f * t)
}
private val NearLayers = List(6) { i ->
    val t = 1f - i / 5f
    (1f + 0.08f * t) to (1f + 0.22f * t)
}

/**
 * The alpha each copy carries — small values that accumulate into a falloff
 * instead of a staircase. The last entry is the core.
 */
private val FarAlphas = listOf(0.020f, 0.025f, 0.030f, 0.040f, 0.050f, 0.140f)
private val NearAlphas = listOf(0.030f, 0.035f, 0.045f, 0.060f, 0.080f, 0.260f)

private fun canopyLeaves(size: Size): List<Pair<Leaf, Boolean>> {
    val unit = minOf(size.width, size.height)

    fun fan(fans: List<LeafFan>, near: Boolean) = fans.flatMapIndexed { fanIndex, f ->
        val from = Offset(f.at.x * size.width, f.at.y * size.height)
        val steps = if (near) NearLayers else FarLayers
        (0 until f.count).map { i ->
            val seed = fanIndex * 37 + i + if (near) 11 else 0
            val angle = f.facing - f.spread / 2f +
                f.spread * i / (f.count - 1f) +
                4f * jitter(seed)
            // Alternating, jittered lengths, so no two neighbours match.
            val length = f.length * unit *
                (if (i % 2 == 0) 0.85f else 1f) * (1f + 0.06f * jitter(seed + 5))
            val width = f.width * unit * (1f + 0.08f * jitter(seed + 9))
            Leaf(steps.map { (growLength, growWidth) ->
                leafPath(from, angle, length * growLength, width * growWidth)
            }) to near
        }
    }

    return fan(Far, near = false) + fan(Near, near = true)
}

/**
 * One leaf: two curves from the stem out to the tip.
 *
 * Cubic rather than quadratic, and that is not a detail. A single control point
 * puts the widest part dead centre and runs straight from there to the tip, which
 * renders as a shard of glass — an early build of this looked like broken bottle.
 * Two control points let the leaf be broad near the stem and taper late, which is
 * the shape of an actual leaf.
 *
 * No lobes or notches. At these alphas an interior cut breaks the silhouette into
 * pieces that read as separate marks, and the silhouette is the entire effect.
 */
private fun leafPath(from: Offset, angle: Float, length: Float, width: Float): Path {
    val a = angle * PI.toFloat() / 180f
    fun at(forward: Float, side: Float) = Offset(
        from.x + forward * cos(a) - side * sin(a),
        from.y + forward * sin(a) + side * cos(a),
    )

    val tip = at(length, 0f)
    return Path().apply {
        moveTo(from.x, from.y)
        cubicTo(
            at(length * 0.14f, -width).x, at(length * 0.14f, -width).y,
            at(length * 0.70f, -width * 0.86f).x, at(length * 0.70f, -width * 0.86f).y,
            tip.x, tip.y,
        )
        cubicTo(
            at(length * 0.70f, width * 0.86f).x, at(length * 0.70f, width * 0.86f).y,
            at(length * 0.14f, width).x, at(length * 0.14f, width).y,
            from.x, from.y,
        )
        close()
    }
}

/** Dark enough to read as shade against the lit top of the screen, still in the scheme. */
private val LeafInk = Color(0xFF04120A)

private fun DrawScope.drawCanopy(leaves: List<Pair<Leaf, Boolean>>) {
    leaves.forEach { (leaf, near) ->
        val alphas = if (near) NearAlphas else FarAlphas
        leaf.layers.forEachIndexed { i, path ->
            drawPath(path, color = LeafInk.copy(alpha = alphas[i]))
        }
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
