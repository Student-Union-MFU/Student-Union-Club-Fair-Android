package com.su.clubfair.ui.prizes

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.model.FairProgress
import com.su.clubfair.ui.model.PrizeTier
import com.su.clubfair.ui.theme.AppSans
import com.su.clubfair.ui.theme.AppTextWeight
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.Palette
import kotlin.math.roundToInt

/**
 * Distance down the page between two checkpoints.
 *
 * Generous, and it has to be: with three stops on the whole route, the space
 * between them is where all the progress is shown. A short hop would leave the
 * lit part of the road too small to read a position off.
 */
private val SegmentHeight = 236.dp

/**
 * Deliberately small, and hollow — see [StartStop].
 *
 * There is no prize at the start, and the first version of this drew it as a
 * 44dp accent-filled disc, which is what an *earned* prize looks like one size
 * down. Three lime circles down a road read as three prizes however the sign
 * under one of them is worded.
 */
private val StartSize = 30.dp
private val PrizeSize = 72.dp
private val MarkerSize = 40.dp

/** Room above the first stop and below the last one's signpost. */
private val TopGap = 8.dp
private val PlateReserve = 72.dp

/**
 * How far the road swings from the centre, as a fraction of the room left after
 * the widest stop.
 *
 * Not the full width: a signpost is centred on its stop, so a stop pushed to the
 * edge would take its sign off the screen with it.
 */
private const val SwingFraction = 0.55f

private val LabelWidth = 176.dp
private val CaptionWidth = 120.dp
private val LabelGap = 12.dp

/** How far the marker's caption floats off it. */
private val CaptionGap = 8.dp

/**
 * How close to the stop above it the marker has to be for its caption to move
 * below it instead.
 *
 * Derived rather than picked: a caption above the marker reaches about 50dp up —
 * half the marker, the gap and its own height — and a prize disc hangs 36dp below
 * its centre, so anything under about 86dp of separation collides. The rest is
 * margin, since the marker's exact height on the curve depends on the arc.
 */
private val StopClearance = 104.dp

private val TrackWidth = 6.dp
private val TrackGlowWidth = 16.dp

/**
 * The route through the fair: where you started, and the two stops worth walking
 * to.
 *
 * Three checkpoints, because that is how many the fair has — the start, the prize
 * at fifteen booths and the prize at twenty-eight. An earlier version of this
 * drew one node per booth, twenty-eight of them, on the reasoning that a route
 * wants steps. It was wrong about what the steps were: a student does not walk
 * "booth 9 then booth 10", they walk towards the next prize, and the twenty-five
 * anonymous dots in between were scenery that made the screen four times longer
 * than it needed to be.
 *
 * What replaces them is the road itself. Progress is the **length of lit road**
 * rather than a count of filled dots: at seven of fifteen the first leg is a bit
 * under half lit, and the marker sits at that point on the curve. So the same
 * question — how far am I, how much further — is answered by a position rather
 * than by counting, and it stays answerable when the Student Union moves a
 * threshold, because nothing here knows what fifteen means.
 *
 * Everything is derived from [progress]. The marker is at
 * `visited` booths along, the legs are `0 → 15` and `15 → 28`, and a scan moves
 * the marker by moving one number.
 */
@Composable
fun PrizeRoadmap(
    progress: FairProgress,
    modifier: Modifier = Modifier,
) {
    val stops = remember(progress.prizes, progress.total) { checkpointsOf(progress) }
    // Nothing to walk to. The screen above only draws the route when there are
    // tiers, so this is the belt to that braces.
    if (stops.size < 2) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val width = maxWidth
        val density = LocalDensity.current

        val layout = remember(stops, width) { routeLayout(stops, width) }
        val geometry = remember(layout, density) { with(density) { geometryOf(layout) } }
        val walk = remember(geometry, progress.visited) {
            walkAlong(geometry, stops, progress.visited)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.height),
        ) {
            RouteTrack(geometry = geometry, walk = walk)

            // Stops first, signs second, marker last — each layer draws over the
            // one it has to stay legible against.
            layout.nodes.forEachIndexed { index, node ->
                val tier = stops[index].tier
                if (tier == null) {
                    StartStop(
                        reached = progress.visited > 0,
                        modifier = Modifier.nodeOffset(node, StartSize),
                    )
                } else {
                    PrizeDisc(
                        tier = tier,
                        description = tierDescription(tier, progress.visited),
                        modifier = Modifier.nodeOffset(node, PrizeSize),
                    )
                }
            }

            layout.nodes.forEachIndexed { index, node ->
                val tier = stops[index].tier
                if (tier == null) {
                    StartCaption(node = node, routeWidth = width)
                } else {
                    PrizePlate(
                        tier = tier,
                        remaining = (tier.threshold - progress.visited).coerceAtLeast(0),
                        node = node,
                        routeWidth = width,
                        // Towards the middle of the route, which is the side that
                        // always has room for it.
                        toLeft = node.x > width / 2,
                    )
                }
            }

            // The caption goes above the marker unless the marker has just left a
            // stop, which is where the disc it left is. Two or three booths into a
            // leg is a common place to be standing, so this is not an edge case.
            val markerY = with(density) { walk.marker.y.toDp() }
            HereMarker(
                position = walk.marker,
                captionAbove = layout.nodes.none { node ->
                    markerY - node.y in 0.dp..StopClearance
                },
            )
        }
    }
}

// ---- What the route is made of ------------------------------------------

/**
 * One stop: the booth count it sits at, and the prize there if it is one.
 *
 * The start is a stop with no prize rather than a special case, so the road, the
 * walk and the layout all work over one list instead of over "the start and then
 * the rest".
 */
private data class Checkpoint(val threshold: Int, val tier: PrizeTier?)

private fun checkpointsOf(progress: FairProgress): List<Checkpoint> =
    listOf(Checkpoint(threshold = 0, tier = null)) +
        progress.prizes.sortedBy { it.threshold }.map { Checkpoint(it.threshold, it) }

private data class RouteNode(val x: Dp, val y: Dp)

private data class RouteLayout(val nodes: List<RouteNode>, val height: Dp)

/**
 * Places the stops down the page, alternating either side of the centre.
 *
 * Alternating rather than following a wave: with three stops a sine has nothing
 * to be a wave *of*, and picking sides outright is both simpler to read and
 * simpler to predict when the Student Union adds a fourth tier.
 */
private fun routeLayout(stops: List<Checkpoint>, width: Dp): RouteLayout {
    val centre = width / 2
    val swing = ((width - PrizeSize) / 2) * SwingFraction

    val nodes = stops.indices.map { index ->
        RouteNode(
            // The start sits on the centre line, and the stops after it lean
            // right, left, right — so the first leg always has a bend in it to
            // show progress along, whichever way the route is walked.
            x = when {
                index == 0 -> centre
                index % 2 == 1 -> centre + swing
                else -> centre - swing
            },
            y = TopGap + PrizeSize / 2 + SegmentHeight * index,
        )
    }
    return RouteLayout(
        nodes = nodes,
        height = nodes.last().y + PrizeSize / 2 + LabelGap + PlateReserve,
    )
}

/**
 * The road in pixels: one path per leg, and how long each one is.
 *
 * Split by leg rather than drawn as one path because the walk has to stop
 * part-way along a leg, and "40% of the second leg" is only meaningful against
 * that leg's own length.
 */
private class RouteGeometry(
    val points: List<Offset>,
    val legs: List<Path>,
    val lengths: List<Float>,
)

private fun Density.geometryOf(layout: RouteLayout): RouteGeometry {
    val points = layout.nodes.map { Offset(it.x.toPx(), it.y.toPx()) }
    val legs = points.zipWithNext { from, to -> legBetween(from, to) }
    val measure = PathMeasure()
    val lengths = legs.map { leg ->
        measure.setPath(leg, false)
        measure.length
    }
    return RouteGeometry(points = points, legs = legs, lengths = lengths)
}

/**
 * One leg, as a cubic whose handles sit halfway down.
 *
 * A straight line between two stops that are on opposite sides of the screen
 * reads as a diagonal cut across the page. The flat top and bottom of this curve
 * are what make it look like a road leaving one stop and arriving at the next.
 */
private fun legBetween(from: Offset, to: Offset): Path = Path().apply {
    moveTo(from.x, from.y)
    val midY = (from.y + to.y) / 2f
    cubicTo(from.x, midY, to.x, midY, to.x, to.y)
}

/** How far along the road the student is, per leg and as a point to stand on. */
private class Walk(val fractions: List<Float>, val marker: Offset)

/**
 * Turns a booth count into a position on the road.
 *
 * The legs are not the same length in booths — the first is fifteen and the
 * second thirteen — so progress is worked out inside whichever leg the student is
 * on, against that leg's own span. Fifteen booths of a fifteen-booth leg fills
 * it; the sixteenth starts the next one.
 */
private fun walkAlong(
    geometry: RouteGeometry,
    stops: List<Checkpoint>,
    visited: Int,
): Walk {
    val fractions = stops.zipWithNext { from, to ->
        val span = (to.threshold - from.threshold).toFloat()
        // A pair of tiers at the same threshold would divide by zero. It is a
        // configuration nobody would write on purpose and the server does not
        // forbid, so the leg is simply treated as done.
        if (span <= 0f) 1f else ((visited - from.threshold) / span).coerceIn(0f, 1f)
    }

    val measure = PathMeasure()
    val standingOn = fractions.indexOfFirst { it < 1f }
    val marker = when {
        // Every leg walked: the student is at the end of the road.
        standingOn < 0 -> geometry.points.last()
        else -> {
            measure.setPath(geometry.legs[standingOn], false)
            measure.getPosition(geometry.lengths[standingOn] * fractions[standingOn])
        }
    }
    return Walk(fractions = fractions, marker = marker)
}

// ---- Drawing --------------------------------------------------------------

/**
 * The road: lit behind the marker, dashed ahead of it.
 *
 * Each leg is cut at the point the walk reached, with [PathMeasure.getSegment],
 * rather than by drawing a fraction of a straight line — the road is a curve, so
 * "60% of the way along" is a distance travelled, not a proportion of the gap
 * between two points.
 */
@Composable
private fun RouteTrack(geometry: RouteGeometry, walk: Walk) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val measure = PathMeasure()

        geometry.legs.forEachIndexed { index, leg ->
            val length = geometry.lengths[index]
            val walked = length * walk.fractions[index]

            if (walked < length) {
                val ahead = Path()
                measure.setPath(leg, false)
                measure.getSegment(walked, length, ahead, true)
                drawPath(
                    path = ahead,
                    color = Color.White.copy(alpha = 0.16f),
                    style = Stroke(
                        width = TrackWidth.toPx(),
                        cap = StrokeCap.Round,
                        // Dashes are what make "not yet" legible without a
                        // legend — a fainter solid line reads as the same road in
                        // worse light.
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(10.dp.toPx(), 12.dp.toPx()),
                        ),
                    ),
                )
            }

            if (walked > 0f) {
                val behind = Path()
                measure.setPath(leg, false)
                measure.getSegment(0f, walked, behind, true)
                // A wide, faint pass under the line, so the road glows on the
                // dark backdrop the way the accent does everywhere else.
                drawPath(
                    path = behind,
                    color = Palette.Accent.copy(alpha = 0.16f),
                    style = Stroke(width = TrackGlowWidth.toPx(), cap = StrokeCap.Round),
                )
                drawPath(
                    path = behind,
                    color = Palette.Accent,
                    style = Stroke(width = TrackWidth.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }
}

/** Centres something of [size] on a node. */
private fun Modifier.nodeOffset(node: RouteNode, size: Dp): Modifier = this.offset {
    IntOffset(
        (node.x.toPx() - size.toPx() / 2f).roundToInt(),
        (node.y.toPx() - size.toPx() / 2f).roundToInt(),
    )
}

/**
 * Puts a label beside a stop, level with it.
 *
 * `Modifier.layout` rather than `offset`, because the offset lambda cannot see
 * the size of what it is offsetting and "level with the stop" is a statement
 * about the label's own height. A two-line sign and a three-line sign have to
 * come out on the same centre line as the disc, and guessing a constant put one
 * of them visibly low.
 */
private fun Modifier.besideStop(
    nodeX: Dp,
    nodeY: Dp,
    nodeSize: Dp,
    width: Dp,
    routeWidth: Dp,
    toLeft: Boolean,
): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val left = if (toLeft) {
        nodeX - nodeSize / 2 - LabelGap - width
    } else {
        nodeX + nodeSize / 2 + LabelGap
    }.coerceIn(0.dp, (routeWidth - width).coerceAtLeast(0.dp))

    layout(placeable.width, placeable.height) {
        placeable.place(left.roundToPx(), nodeY.roundToPx() - placeable.height / 2)
    }
}

/**
 * Where the route begins. **Not a prize.**
 *
 * There are two prizes on this route and three stops, and the difference has to
 * be visible at a glance rather than only in the sign underneath: a ring, at less
 * than half the size of a prize, with nothing inside it. A gift disc is big,
 * solid and carries a glyph; this is a mark on the road saying the road starts
 * here.
 *
 * The ring lights once the student has collected anything, which is the only
 * state it has — "have you started", not "have you earned it".
 */
@Composable
private fun StartStop(reached: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(StartSize)
            .clip(CircleShape)
            .background(Palette.Base)
            .border(
                width = 3.dp,
                color = if (reached) Palette.Accent else Color.White.copy(alpha = 0.28f),
                shape = CircleShape,
            ),
    )
}

@Composable
private fun StartCaption(node: RouteNode, routeWidth: Dp) {
    Text(
        text = stringResource(R.string.prizes_start),
        modifier = Modifier
            .besideStop(
                nodeX = node.x,
                nodeY = node.y,
                nodeSize = StartSize,
                width = CaptionWidth,
                routeWidth = routeWidth,
                toLeft = true,
            )
            .width(CaptionWidth),
        fontFamily = AppSans,
        fontWeight = AppTextWeight,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
        color = Ink.Muted,
        textAlign = TextAlign.End,
    )
}

/** The whole stop as one sentence, for anyone who cannot see the route. */
@Composable
private fun tierDescription(tier: PrizeTier, visited: Int): String = pluralStringResource(
    R.plurals.prizes_tier_desc,
    tier.threshold,
    tier.name,
    tier.threshold,
    tierStatus(tier, (tier.threshold - visited).coerceAtLeast(0)),
)

@Composable
private fun tierStatus(tier: PrizeTier, remaining: Int): String = when {
    tier.claimed -> stringResource(R.string.prizes_claimed)
    tier.reached -> stringResource(R.string.prizes_ready)
    else -> pluralStringResource(R.plurals.prizes_remaining, remaining, remaining)
}

/**
 * A prize where it falls on the route.
 *
 * Carries the whole stop as its content description — name, threshold and how far
 * off it is — because the sign below it is marked silent. One stop, one thing to
 * hear, rather than a gift followed by three unattributed sentences.
 */
@Composable
private fun PrizeDisc(
    tier: PrizeTier,
    description: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(PrizeSize)
            .clip(CircleShape)
            .background(if (tier.reached) Palette.Accent else Palette.Panel)
            .border(
                width = 2.dp,
                color = if (tier.reached) Palette.Accent else Color.White.copy(alpha = 0.22f),
                shape = CircleShape,
            )
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        when {
            tier.claimed -> Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = Palette.Ink,
                modifier = Modifier.size(30.dp),
            )
            tier.reached -> Mfu333Mark(tint = Palette.Ink)
            // Shut, and visibly so. A reward you have not earned drawn in the
            // same mark as one you have is the one thing this screen must not
            // say.
            else -> Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

/**
 * The MFU333 wordmark, as the thing sitting in an earned stop.
 *
 * Set as type rather than drawn as a vector: it is a word, and a word rendered
 * from the app's own family stays sharp at any density, follows the theme's ink,
 * and never has to be redrawn if the reward is renamed. A traced outline would
 * be a second copy of the name that no string change can reach.
 *
 * Two lines, not one. Seven characters across a 72dp disc leaves each glyph
 * about four dp wide — legible on a render, not on a phone at arm's length in a
 * hall. Stacked "MFU" over "333" doubles the height available to each line, and
 * the split falls where the name already reads as two parts.
 *
 * Silent to TalkBack: [PrizeDisc] above carries the whole stop as one
 * description, and a wordmark repeating the tier name would be the second half
 * of a stutter.
 */
@Composable
private fun Mfu333Mark(tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clearAndSetSemantics {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "MFU",
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 15.sp,
            lineHeight = 15.sp,
            letterSpacing = (-0.2).sp,
            color = tint,
        )
        Text(
            text = "333",
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 17.sp,
            lineHeight = 17.sp,
            letterSpacing = (-0.2).sp,
            color = tint,
        )
    }
}

/**
 * The signpost for a prize: what it is, and how far off.
 *
 * **Beside the stop, not below it**, and that is the whole reason this looks the
 * way it does. Below is where a sign wants to go and where two other things
 * already are: the road, which leaves every stop heading for the next one, and
 * the marker, which rides that road and therefore sits directly under a stop for
 * the first few booths of every leg. At nineteen of twenty-eight the marker
 * landed on top of "Prize 1 / Claimed" and neither could be read. Level with the
 * disc and towards the middle of the route, both are clear.
 *
 * On a glass plate rather than loose on the backdrop, for the same reason every
 * other card in the app is one — and so the route reads as part of the app
 * rather than as a game pasted into it.
 *
 * Silent to TalkBack: the disc already carries all three lines as one
 * description. See [PrizeDisc].
 */
@Composable
private fun PrizePlate(
    tier: PrizeTier,
    remaining: Int,
    node: RouteNode,
    routeWidth: Dp,
    toLeft: Boolean,
) {
    val status = tierStatus(tier, remaining)

    Column(
        modifier = Modifier
            .besideStop(
                nodeX = node.x,
                nodeY = node.y,
                nodeSize = PrizeSize,
                width = LabelWidth,
                routeWidth = routeWidth,
                toLeft = toLeft,
            )
            .width(LabelWidth)
            .glassSurface(cornerRadius = Dimens.RadiusMd)
            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceSm)
            .clearAndSetSemantics {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = tier.name,
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 14.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = status,
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 12.sp,
            lineHeight = 1.35.em,
            color = if (tier.reached) Palette.Accent else Ink.Muted,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        tier.description?.let { line ->
            Text(
                text = line,
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 11.sp,
                lineHeight = 1.35.em,
                color = Ink.Muted,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Where the student is standing, on the road rather than on a stop.
 *
 * This is what the twenty-eight dots were really for, and it does the job better:
 * between two checkpoints the marker is at the point the walk reached, so being
 * two booths into a thirteen-booth leg looks like being two booths into it.
 *
 * The one animated thing on the screen, and it is a `graphicsLayer` scale on a
 * 40dp disc rather than anything that redraws the backdrop — see the note on
 * `MeshBackground.DriftSteps` for what a full-screen animation costs this app.
 */
@Composable
private fun HereMarker(position: Offset, captionAbove: Boolean) {
    val pulse by rememberInfiniteTransition(label = "here").animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (position.x - MarkerSize.toPx() / 2f).roundToInt(),
                    (position.y - MarkerSize.toPx() / 2f).roundToInt(),
                )
            }
            .size(MarkerSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(MarkerSize)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                    // Fades as it grows, so the ring reads as one breath rather
                    // than as a disc that keeps changing size.
                    alpha = (1.35f - pulse) / 0.35f * 0.5f
                }
                .clip(CircleShape)
                .background(Palette.Accent.copy(alpha = 0.35f)),
        )
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Palette.Accent)
                .border(3.dp, Palette.Base, CircleShape),
        )
    }

    // Above the marker, not beside it. Beside is where the signposts are, and the
    // marker spends the first stretch of every leg level with the stop it has
    // just left. Either way it lands on the road, which is why the caption
    // carries its own ground.
    Text(
        text = stringResource(R.string.prizes_here),
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val edge = position.y + (MarkerSize / 2 + CaptionGap).toPx() *
                    if (captionAbove) -1f else 1f
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = (position.x - placeable.width / 2f).roundToInt(),
                        y = edge.roundToInt() - if (captionAbove) placeable.height else 0,
                    )
                }
            }
            .clip(CircleShape)
            .background(Palette.Base.copy(alpha = 0.72f))
            .padding(horizontal = Dimens.SpaceSm, vertical = 3.dp),
        fontFamily = AppSans,
        fontWeight = AppTextWeight,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp,
        color = Palette.Accent,
        textAlign = TextAlign.Center,
    )
}
