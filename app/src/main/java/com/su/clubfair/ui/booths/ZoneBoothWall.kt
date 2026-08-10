package com.su.clubfair.ui.booths

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.su.clubfair.R
import com.su.clubfair.ui.components.GlassIconButton
import com.su.clubfair.ui.components.ProgressTrack
import com.su.clubfair.ui.components.liquidGlass
import com.su.clubfair.ui.model.Booth
import com.su.clubfair.ui.model.Zone
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.Palette

/**
 * The wall's two column widths, in lanes of twelve.
 *
 * A real masonry wall has no rows in it. Every arrangement this screen has had
 * so far did: a staggered grid whose full-width tiles banded it, then a hand-laid
 * plan whose blocks banded it again. Both were tables with uneven cells, because
 * both decided up front which tile went where — and a plan that says where every
 * tile goes will always produce alignments, since something has to line up for
 * the plan to close.
 *
 * So nothing here says where a tile goes. The tiles are measured, and each one is
 * dropped into whichever column is currently shorter. That is the whole algorithm
 * and it is the actual definition of masonry: no tile knows about any other, and
 * the raggedness is what falls out rather than what was drawn.
 *
 * Two columns, **equal**. They were 7 lanes against 5 for one version, on the idea
 * that unequal columns give the wall a width to vary as well as a height. What it
 * actually gave the wall was a permanent bias: the wide one is always the left
 * one, so every tile on the left was big and every tile on the right was small,
 * which is a rule the eye picks up in about a second.
 *
 * That bias is not a tuning mistake, it is what fixed columns *are*. A tile's
 * column is chosen by which side is shorter, but a column's width never changes,
 * so the two facts can't be made to vary together. Genuinely mixed widths need
 * the packer to re-split the wall at a new lane part way down, and it can only do
 * that where both columns happen to land at exactly the same height — which,
 * with heights coming from text, essentially never happens. Forcing it means
 * either a full-width tile to reset on (a band across the wall, which is what
 * made the last two versions read as blocks) or leaving the odd lane unfilled
 * (a notch of background between two cards, which reads as a bug).
 *
 * So the width is even and the *height* does all the varying — see [TileSize].
 * That is how masonry works everywhere it works: Pinterest's columns are all the
 * same width too.
 *
 * Three columns would be 96dp each, which leaves 72dp of text, and
 * "International" — the longest unbreakable word in the roster — sets to about
 * 82dp at the name's size. Two is what the club names allow.
 */
private const val WallLanes = 12
private val ColumnSpans = intArrayOf(6, 6)

/**
 * How much of a club a tile shows — and so, how tall it comes out.
 *
 * This exists because the wall had nothing to be ragged *with*. Content decides
 * the heights, and the content is 27 blurbs written to the same length: at one
 * column width they all wrap to the same number of lines, so every tile came out
 * within twenty dp of every other and the wall looked like a table again. Two
 * ragged columns of near-identical tiles is a grid that has been nudged.
 *
 * So the tiles differ in what they carry. **The blurb is not what differs** — a
 * version of this tried making the short tiles name-only, and a booth with no
 * line about what the club does is the one thing on this wall that has to be on
 * every card: it is the reason to walk over, and a card without it is a card you
 * can't use. Every tile has the mark, the badge, the name and the blurb.
 *
 * What differs is what comes *under* the blurb, and it is additive rather than
 * subtractive — each step adds a line nobody was missing on the tiles that don't
 * have it:
 *
 * - [Standard] stops at the blurb. About 135dp, 154 with a name that wraps.
 * - [Detail] adds how many members the club has. About 158–177dp.
 * - [Feature] adds when and where it meets on top of that. About 177–196dp.
 *
 * Sixty dp of spread across the wall, which is enough to see, and every card is
 * still a complete card.
 */
private enum class TileSize { Standard, Detail, Feature }

/**
 * Which booth gets which size, by its place in the zone.
 *
 * Nine, so a zone runs the pattern exactly once and no zone repeats a rhythm. It
 * is shuffled rather than cycled — `Standard, Detail, Feature` three times over
 * would put the same shape every third tile, and a repeat at that spacing is
 * visible even when the heights aren't.
 *
 * The packer still decides the sides, so the pattern never lands as a pattern:
 * the same nine sizes come out as a different wall depending on how the tiles
 * above them fell.
 */
private val SizePlan = listOf(
    TileSize.Standard,
    TileSize.Feature,
    TileSize.Standard,
    TileSize.Detail,
    TileSize.Standard,
    TileSize.Feature,
    TileSize.Detail,
    TileSize.Feature,
    TileSize.Detail,
)

/**
 * How many lines of blurb a tile that carries one will show.
 *
 * Three rather than two, so nothing is ellipsised: a blurb runs to about fifty
 * characters and a 6-lane tile holds twenty-five to a line, so two lines clipped
 * the longest of them. Tiles are as tall as what is written on them, so an extra
 * allowance costs nothing on the ones that don't need it.
 */
private const val BlurbLines = 3

/**
 * The club's icon, on its plate, at exactly one size on every tile.
 *
 * This replaced a bare glyph scaled to the tile it stood on — 34dp on a short
 * tile, 64dp on a tall one, on the theory that a tall tile should be a bigger
 * picture. The theory ignored what the icons are. They are a 24dp line set drawn
 * on a 24dp grid with a 2dp stroke, so scaling one to 64dp renders that stroke at
 * 5.3dp and scaling one to 34dp renders it at 2.8dp: the same family, at half
 * again the weight, tile to tile. Nothing else on the wall varies its line weight
 * by position, so the set read as borrowed from four different sets.
 *
 * [GlyphSize] is the icons' own 24dp, which is the only size at which the stroke
 * is the 2dp it was drawn as. It is also exactly what `BoothSheet` draws — so the
 * glyph on the tile and the glyph on the panel it opens are the same object at
 * the same size, rather than one shrinking into the other.
 *
 * The plate does the job the varying size was reaching for. Glyphs have wildly
 * different bounding boxes — `</>` is wide and flat, a music note is tall and
 * narrow — and floating loose on the glass they sit at visibly different optical
 * sizes even when nominally equal. A plate gives all 27 the same square, and it
 * balances the corner badge across the top of the tile: two objects, one at each
 * end, rather than one solid disc and some loose strokes.
 *
 * One step down from the panel's 48dp plate, because a tile is one step down from
 * a panel.
 */
private val PlateSize = 44.dp
private val GlyphSize = 24.dp

/**
 * The tiles' glass.
 *
 * No blur, which is the opposite of what the nav bar does and right for the
 * opposite reason. The bar is one pane over *scrolling* content — text and cards
 * pass under it and have to be softened. These are nine panes over a still mesh
 * that is nothing but soft gradients already, so the blur pass had almost
 * nothing to soften while costing the most of any effect here: with it, four
 * flings through a zone missed 7% of their frames; without it, 2%.
 *
 * What replaced it is the rest of the material — vibrancy under the glass, a
 * cast shadow and an inner shadow at the top edge. That is what a pane of glass
 * laid on a wall actually does, and doing only two of these is what made the
 * first version read as a tinted rectangle.
 *
 * A deeper bend than the bar's, not a shallower one: these panes are small, and
 * a refraction band tuned for a 56dp bar is a hairline on a 200dp tile.
 */
private val TileBlur = 0.dp
private val TileRefractionHeight = 22.dp
private val TileRefractionAmount = 26.dp

/** How far the tiles sit off the wall, as light says it. */
private val TileShadowRadius = 18.dp
private val TileShadowDrop = 8.dp

/**
 * A tile carries two colours, and only two: white for what is read, the area's
 * accent for what is recognised.
 *
 * Everything on the card used to be the accent — the fill, the border, the name,
 * the blurb, the glyph, the badge — separated only by alpha. One hue doing every
 * job is what made the wall look wrong: a pale tinted pane with pale tinted text
 * on it has no contrast anywhere except against the mesh, so the card read as a
 * single smudge of colour rather than as a card with writing on it, and the gold
 * zone in particular went muddy.
 *
 * Split by job instead. Text is white, because white on a dark mesh is the one
 * pairing that stays legible in sunlight at the fair, and because the rest of the
 * app already reads in white. The accent is spent on the two marks that say which
 * area this is — the club's glyph and the corner badge — where it sits on its own
 * against the glass rather than under text.
 */
private fun cardInk(accent: Color): Color = lerp(accent, Color.White, 0.22f)

/**
 * The glass itself: the area's colour breathed into white, not white tinted with
 * it.
 *
 * Lifted much further than it was (0.55 → 0.82) and at lower alphas, so the pane
 * is essentially white glass that happens to lean the zone's way. The hue is
 * still there when nine of them are together — a rainforest wall and an ocean
 * wall are visibly different rooms — but no single tile announces a colour, which
 * is the whole of what "subtle" means here.
 *
 * Lifting toward white rather than dropping alpha keeps the hue: alpha alone,
 * over a near-black mesh, makes a colour darker on its way to disappearing.
 */
private fun cardTint(accent: Color): Color = lerp(accent, Color.White, 0.82f)

/**
 * The white ink, at the two weights a tile uses it.
 *
 * The name is full white and the blurb is a little over half, which is the same
 * step [Ink.Label] and [Ink.Muted] make everywhere else in the app — this is that
 * relationship, stated locally because it sits on glass rather than on the mesh
 * and wants a touch more of both.
 */
private const val InkName = 1f
private const val InkBlurb = 0.66f

/** How much of itself the accent keeps on the glyph — the second colour. */
private const val InkGlyph = 0.90f

/** How much the opened tile lifts off the wall. */
private const val LiftScale = 1.06f

/** What the rest of the wall falls back to while one tile is open. */
private const val DimmedAlpha = 0.35f

/** One duration for the lift and the travel, so they read as one movement. */
private const val LiftMillis = 320

/**
 * Where down the window the opened tile comes to rest — above the panel, which
 * takes something over half the screen.
 */
private const val FocusViewportFraction = 0.26f

/**
 * The booths of one area, as a wall of tiles.
 *
 * This replaced a level track: nine cards zig-zagging down a drawn connector,
 * one per row. The route was the idea — a trail with stops on it — and the
 * trouble with it was that the trail is a fiction. The booths of a zone are not
 * in a line and a student does not walk them in order, so the line was telling
 * them something untrue while spending a whole screen row on each booth, which
 * put six of the nine below the fold.
 *
 * A masonry wall is the honest shape for it: everything in the zone, side by
 * side, no order implied beyond the booth numbers printed on them. The varied
 * tiles are what keep it from reading as a spreadsheet — the eye moves down a
 * staggered wall the way it moves down a page of photographs, in no particular
 * order, which is exactly how someone picks a booth to walk to.
 *
 * The area's name is the first thing in the wall rather than a bar above it, so
 * it scrolls away with the tiles it titles.
 */
@Composable
fun ZoneBoothWall(
    zone: Zone,
    booths: List<Booth>,
    selected: Booth?,
    onSelectBooth: (Booth) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    var viewportPx by remember { mutableIntStateOf(0) }
    var mastheadPx by remember { mutableIntStateOf(0) }
    // Where each tile came to rest, in the wall's own coordinates. Nothing knows
    // this ahead of time now — the packer decides it from measured heights — so
    // the tiles report it, and the focus shift below reads it back.
    val tileCentres = remember { mutableStateMapOf<Int, Float>() }

    // What the tiles refract. It wraps the mesh and only the mesh: a pane that
    // samples a layer it is drawn inside is sampling itself, so the wall has to
    // sit outside this rather than the shell's backdrop being reused.
    val wallpaper = rememberLayerBackdrop()

    // The panel covers the bottom of the screen, so a tile in the lower half
    // opens behind the thing it opened. The wall slides up until it is clear.
    //
    // Sliding rather than scrolling, because the last row is already at the end
    // of the scroll — a scroll target for it clamps and the tile stays under
    // the panel. Padding the wall out far enough to let it scroll would leave a
    // screen of dead space under the last booth for everyone just reading it.
    val shift = remember { Animatable(0f) }
    LaunchedEffect(selected) {
        val index = selected?.let(booths::indexOf) ?: -1
        val tile = tileCentres[index]
        val target = if (index < 0 || viewportPx == 0 || tile == null) {
            0f
        } else {
            // The tile's centre in the window: where it sits in the wall, plus the
            // masthead above it, less however far the wall has been scrolled.
            val centre = mastheadPx + tile - scroll.value
            // Upwards only — a tile already above the line is not hidden by
            // anything, and pushing it down would just open a gap under the
            // area's name.
            (viewportPx * FocusViewportFraction - centre).coerceAtMost(0f)
        }
        shift.animateTo(target, tween(durationMillis = LiftMillis, easing = FastOutSlowInEasing))
    }

    Box(modifier = modifier.fillMaxSize()) {
        // The app's own mesh, the same one Home and the profile sit on. Every
        // backdrop this screen has had that came from somewhere else — three
        // pixel-art paintings, a drawn map, a tile village, two parallax washes
        // of the area's colour — is what kept making the Booths tab look like a
        // screen from a different app.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(wallpaper),
        ) {
            MeshBackground(modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            ZoneTopBar(onBack = onBack)

            // The clip is what keeps the slide honest: the wall moves as one
            // layer, and without a frame to move inside it rides up over the
            // back button and into the status bar.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .onSizeChanged { viewportPx = it.height },
            ) {
                // Scrolled by hand rather than lazily, and that is affordable
                // here in a way it would not be on a list: a zone is nine tiles.
                // Laying all nine out costs less than the bookkeeping a lazy
                // component does to avoid laying them out.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .graphicsLayer { translationY = shift.value }
                        .padding(horizontal = Dimens.ScreenPadding),
                ) {
                    ZoneMasthead(
                        zone = zone,
                        scanned = booths.count { it.scanned },
                        total = booths.size,
                        modifier = Modifier.onSizeChanged { mastheadPx = it.height },
                    )

                    BoothMasonry(
                        booths = booths,
                        accent = zone.accent,
                        selected = selected,
                        backdrop = wallpaper,
                        onSelectBooth = onSelectBooth,
                        onTileCentre = { index, centre -> tileCentres[index] = centre },
                    )

                    Spacer(Modifier.height(Dimens.NavBarClearance))
                }
            }
        }
    }
}

/**
 * The masonry itself: measure each tile, drop it in the shorter column, repeat.
 *
 * A custom `Layout` because the packing needs the measured heights and nothing
 * else does the job — `LazyVerticalStaggeredGrid` packs this way but only over
 * equal columns, and every other container in the toolkit lays out in rows.
 * Fifteen lines of measure policy buys unequal columns and content-driven heights
 * together.
 *
 * The order matters and is the reason this is a `Layout` rather than arithmetic:
 * a tile's column is decided *before* it is measured, because the column is what
 * fixes its width, and its width is what decides how tall its blurb makes it.
 * Measure it against the wrong column and the height that comes back is the
 * height it would have had somewhere else.
 *
 * Booths keep their roster order down the wall, so the badge numbers still run
 * roughly in sequence — the packer only chooses the side, never the order.
 *
 * The lane arithmetic is the standard grid identity: a tile spanning n lanes also
 * swallows the n−1 gaps inside it. The narrow column is pinned to the wall's right
 * edge rather than measured out from the left, so rounding on the lane width can
 * never leave a sliver of background down the outside.
 */
@Composable
private fun BoothMasonry(
    booths: List<Booth>,
    accent: Color,
    selected: Booth?,
    backdrop: Backdrop,
    onSelectBooth: (Booth) -> Unit,
    onTileCentre: (index: Int, centre: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            booths.forEachIndexed { index, booth ->
                BoothTile(
                    booth = booth,
                    accent = accent,
                    size = SizePlan[index % SizePlan.size],
                    modifier = Modifier
                        .tileSurface(
                            accent = accent,
                            scanned = booth.scanned,
                            backdrop = backdrop,
                            opened = selected == booth,
                            dimmed = selected != null && selected != booth,
                            onClick = { onSelectBooth(booth) },
                        )
                        .onGloballyPositioned {
                            onTileCentre(index, it.positionInParent().y + it.size.height / 2f)
                        },
                )
            }
        },
    ) { measurables, constraints ->
        val gap = Dimens.Space.roundToPx()
        val wall = constraints.maxWidth
        val lane = (wall - gap * (WallLanes - 1)).toFloat() / WallLanes
        val widths = ColumnSpans.map { (lane * it + gap * (it - 1)).toInt() }
        val xs = intArrayOf(0, wall - widths[1])
        // How far down each column has been filled. The whole algorithm is this
        // array and the `minIndex` below it.
        val filled = IntArray(ColumnSpans.size)

        val placed = measurables.map { measurable ->
            val column = filled.indices.minBy { filled[it] }
            val placeable = measurable.measure(
                constraints.copy(
                    minWidth = widths[column],
                    maxWidth = widths[column],
                    minHeight = 0,
                    maxHeight = Constraints.Infinity,
                ),
            )
            val y = filled[column]
            filled[column] = y + placeable.height + gap
            Triple(placeable, xs[column], y)
        }

        // Less one gap: the last tile in each column doesn't need one under it.
        val height = (filled.max() - gap).coerceAtLeast(0)
        layout(wall, height) {
            placed.forEach { (placeable, x, y) -> placeable.place(x, y) }
        }
    }
}

/**
 * The pane every tile is cut from.
 *
 * Liquid glass rather than the frosted `glassSurface` the rest of the app uses,
 * and the difference is worth the shader here: a wall is a field of panes laid
 * over one still image, which is the one arrangement where refraction reads as
 * material — each tile bends the mesh's arcs a little differently depending on
 * where it sits, so nine tiles are nine pieces of glass rather than nine copies
 * of the same grey rectangle.
 *
 * The border is not decoration and not a duplicate of the library's highlight.
 * The highlight is a shader, and the shader does not exist below API 33; on
 * `minSdk` 24 a tile without this has no edge at all.
 */
@Composable
private fun Modifier.tileSurface(
    accent: Color,
    scanned: Boolean,
    backdrop: Backdrop,
    opened: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (dimmed) DimmedAlpha else 1f,
        animationSpec = tween(durationMillis = LiftMillis),
        label = "tileAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (opened) LiftScale else 1f,
        animationSpec = tween(durationMillis = LiftMillis, easing = FastOutSlowInEasing),
        label = "tileLift",
    )
    val shape = RoundedCornerShape(Dimens.RadiusLg)

    // No size here: the slot on the wall sets it, and a tile that sized itself
    // would fight the offset it was placed at.
    return this
        .graphicsLayer {
            this.alpha = alpha
            scaleX = scale
            scaleY = scale
        }
        .liquidGlass(
            backdrop = backdrop,
            shape = shape,
            blurRadius = TileBlur,
            refractionHeight = TileRefractionHeight,
            refractionAmount = TileRefractionAmount,
            // The area's colour *is* the surface. It used to be a tinted block
            // over the top half of the tile with clear glass under it, which
            // made one card look like two materials butted together — the seam
            // across the middle was the worst thing on the wall. One even tint
            // over the whole pane, deeper once the booth is done, so a zone half
            // finished reads as half lit from across the room.
            //
            // Down again, to 0.18/0.09. The gap between the two states matters
            // more than either number: scanned has to be visibly the fuller
            // pane, and at these alphas it is exactly twice the fill, which is
            // enough to see down a wall and not enough to see on one card.
            surface = cardTint(accent).copy(alpha = if (scanned) 0.18f else 0.09f),
            // No shader highlight. Both of the library's ramp their brightness
            // around the outline — the point of them — and around a 20dp corner
            // radius at this size that reads as an edge that fades out rather
            // than as light catching a rim. The flat border below is the edge.
            highlight = null,
            // Light from above, which is where the mesh's own blooms are. The
            // shadow is what stops a translucent card reading as a hole cut in
            // the wallpaper.
            shadow = Shadow(
                radius = TileShadowRadius,
                offset = DpOffset(0.dp, TileShadowDrop),
                color = Color.Black,
                alpha = if (scanned) 0.30f else 0.22f,
            ),
            // A lip along the inside of the top edge — the thickness of the
            // glass, seen from slightly below.
            innerShadow = InnerShadow(
                radius = 6.dp,
                offset = DpOffset(0.dp, 2.dp),
                color = Color.White,
                alpha = 0.16f,
            ),
        )
        // One hairline, even the whole way round, at the same alpha on every
        // side — the same rule `Glass.kt` follows for the frosted panes, and for
        // the same reason: at 1dp a ramp doesn't read as lighting, it reads as a
        // border that fades out. It is also the only edge an API 24 phone can
        // draw, so this way every phone gets the same one.
        .border(
            width = 1.dp,
            color = cardTint(accent).copy(alpha = if (scanned) 0.30f else 0.18f),
            shape = shape,
        )
        .clip(shape)
        .clickable(onClick = onClick)
}

/**
 * One booth as a portrait tile.
 *
 * Two inks, split by job: the glyph and the badge are the area's colour, the
 * name and blurb are white. Putting the text in the accent as well made the card
 * one flat wash — the writing had nothing to contrast with except the mesh
 * behind the glass, which is not a contrast the eye can use at a glance.
 *
 * **The tile is as tall as what is on it, and nothing else.** It used to be given
 * a height and pin its plate to the ceiling and its caption to the floor, which
 * meant every dp the content did not need showed up as a gap through the middle
 * — the thing that made the tall tiles look empty, and the thing the old varying
 * glyph size existed to hide. The `weight(1f)` that pushed the caption down is
 * now a fixed gap, and the column wraps.
 *
 * What varies is how much there is to be as tall as — see [TileSize]. Every tile
 * carries the mark, the badge, the name and the blurb; the taller ones add facts
 * under it. Content deciding the height is what masonry actually is, so the
 * content has to be worth differing — and the differing has to happen somewhere
 * that isn't the blurb.
 *
 * The type went up a step and a half — 13sp/11sp to 15sp/12sp — which is the
 * change that made the tile look like a card rather than a caption. 13sp bold is
 * the size a *label* is set at; this line is the name of the thing the tile is
 * for, and it was smaller here than the same club's name is anywhere else in the
 * app.
 *
 * The name is never truncated. Two lines hold 32 characters at 15sp in the wide
 * column and 26 in the narrow one; the longest club in the roster is 27 and
 * breaks after "International", so it fits either way. The blurb is the only
 * thing allowed to trail off, because the panel repeats it in full — at
 * [BlurbLines] almost nothing does.
 */
@Composable
private fun BoothTile(
    booth: Booth,
    accent: Color,
    size: TileSize,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(Dimens.Space)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            BoothGlyph(booth = booth, accent = accent)
            Spacer(Modifier.weight(1f))
            StatusBadge(booth = booth, accent = accent)
        }

        Spacer(Modifier.height(Dimens.Space))
        Text(
            text = booth.name,
            fontFamily = AlanSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            color = Color.White.copy(alpha = InkName),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        // On every tile without exception. It is the line that says why to walk
        // over, and a booth card without one is a card that can't be used.
        // When there is one. `about` is null on all 28 booths until the Student
        // Union writes the copy, and an empty line under every name reads as a
        // card that failed to load — so the tile omits it rather than reserving
        // space for nothing.
        booth.about?.let { blurb ->
            Spacer(Modifier.height(Dimens.SpaceXs))
            Text(
                text = blurb,
                fontFamily = AlanSans,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Color.White.copy(alpha = InkBlurb),
                maxLines = BlurbLines,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (size != TileSize.Standard) {
            Spacer(Modifier.height(Dimens.SpaceSm))
            // The club's English name, where there is one.
            //
            // A member count and a "meets Wed 17:00 · Main Field" line used to sit
            // here. Both are gone rather than ported: `members`, `meets` and
            // `venue` were invented per club when the roster was written, and the
            // Student Union has never supplied any of the three. There is nothing
            // to render, and a fabricated figure on a card a student uses to choose
            // where to walk is worse than a shorter card.
            booth.nameEn?.let { english ->
                Text(
                    text = english,
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = cardInk(accent).copy(alpha = InkGlyph),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The club's icon on its plate — the one shape that is the same on all 27 tiles.
 *
 * The wash is [cardInk] rather than the pane's [cardTint] for the same reason the
 * unscanned badge is: the pane is nearly white glass now, and a white-washed
 * plate on it is not a plate. It sits a little under the badge's wash so that of
 * the two objects across the top, the one carrying the booth's *state* is the one
 * that comes forward.
 */
@Composable
private fun BoothGlyph(booth: Booth, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(PlateSize)
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(cardInk(accent).copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        BoothIcon(
            booth = booth,
            size = GlyphSize,
            tint = cardInk(accent).copy(alpha = InkGlyph),
        )
    }
}

/**
 * How wide the corner disc is — number or tick, the same circle either way.
 *
 * 26, up from 24, so a two-digit booth number sits inside the disc with air round
 * it at the larger type below rather than touching the rim.
 */
private val BadgeSize = 26.dp

/**
 * The one thing on the tile that says whether this booth is done.
 *
 * Solid accent under a dark glyph when it is: [Palette.Ink] on the area's
 * colour is the same pairing the scan button and the pass use, and it is the
 * only fully saturated shape on the wall, which is what makes it findable at
 * arm's length while walking. The dark is not a second colour on the card — it
 * is the absence of one, and a tick has to sit on something.
 *
 * Unscanned it is the booth number, in the same accent on a wash of itself:
 * present, quiet, and the same size, so the wall has one column of discs rather
 * than a tick that appears and disappears. The wash is [cardInk] rather than
 * [cardTint] now that the pane is nearly white glass — a white disc on a white
 * pane is not a disc.
 */
@Composable
private fun StatusBadge(booth: Booth, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(BadgeSize)
            .clip(CircleShape)
            .background(
                if (booth.scanned) cardInk(accent)
                else cardInk(accent).copy(alpha = 0.20f)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (booth.scanned) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.booths_scanned_desc),
                tint = Palette.Ink,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Text(
                text = booth.displayCode,
                fontFamily = AlanSans,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = cardInk(accent).copy(alpha = InkGlyph),
            )
        }
    }
}

/**
 * The way out, and nothing else.
 *
 * It held the area's name and its progress as well, which made it a title bar,
 * and a title bar is a thing that sits over content rather than a thing that
 * belongs to it. The name is on the wall now — see [ZoneMasthead] — and what is
 * left is the one control the row exists for, the same round glass button the
 * profile sheet goes back with.
 */
@Composable
private fun ZoneTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(
            icon = R.drawable.ic_arrow_back,
            contentDescription = stringResource(R.string.booths_back),
            onClick = onBack,
        )
    }
}

/**
 * The area's name at the head of its own wall.
 *
 * It reads as a title for the nine booths below it rather than for the screen,
 * which is what it actually is, and it scrolls away with them.
 *
 * The letter tile is the one off the zone card, unchanged and deliberately so:
 * it is the last thing the student tapped, so arriving to it again is what
 * makes the wall read as the inside of that card rather than as a new screen.
 * No horizontal padding of its own — it is an item in the wall, and the wall
 * sets the gutters.
 */
@Composable
private fun ZoneMasthead(
    zone: Zone,
    scanned: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = Dimens.SpaceSm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(Dimens.RadiusSm))
                    .background(zone.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = zone.letter,
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = zone.accent,
                )
            }

            Spacer(Modifier.width(Dimens.Space))
            Column(Modifier.weight(1f)) {
                Text(
                    text = zone.title,
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Bold,
                    // 24sp, one step up from the 20sp of a card title: this is
                    // the only heading on the screen, so it is allowed to be
                    // the biggest thing on it.
                    fontSize = 24.sp,
                    color = Color.White,
                )
                Text(
                    text = listOfNotNull(zone.titleEn, zone.subtitle)
                        .joinToString("  ·  "),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Ink.Muted,
                )
            }
        }

        Spacer(Modifier.height(Dimens.Space))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressTrack(
                fraction = if (total == 0) 0f else scanned.toFloat() / total,
                modifier = Modifier.weight(1f),
                accent = zone.accent,
                height = 6.dp,
            )
            Spacer(Modifier.width(Dimens.Space))
            Text(
                text = stringResource(R.string.booths_zone_progress, scanned, total),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Color.White,
            )
        }
    }
}
