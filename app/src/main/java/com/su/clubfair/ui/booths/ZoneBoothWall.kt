package com.su.clubfair.ui.booths

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
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
import com.su.clubfair.ui.model.blurb
import com.su.clubfair.ui.model.categoryName
import com.su.clubfair.ui.model.displayName
import com.su.clubfair.ui.model.displayTitle
import com.su.clubfair.ui.model.zoneIntent
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.Palette

/**
 * Two columns, equal width, and the club names are what set the number.
 *
 * Three columns would be 96dp each, which leaves 72dp of text, and
 * "International" — the longest unbreakable word in the roster — sets to about
 * 82dp at the name's size. Two is what the club names allow.
 */
private const val WallColumns = 2

/**
 * Every tile is the same size, and the wall is ragged because the columns are
 * **offset** rather than because the tiles differ.
 *
 * This is the third arrangement the wall has had and the first one that is both
 * ragged and lazy, which turns out to be the same problem twice.
 *
 * The version before this packed measured tiles into whichever column was
 * shorter — real masonry, heights coming from the text. It read well and it had
 * two costs. Content-driven heights cannot be known without measuring, so the
 * whole wall had to be laid out to place any of it; and the height a tile came
 * out at was the length of a blurb, which is a fact about the Student Union's
 * copy rather than about the club, so the raggedness was arbitrary in a way
 * nobody could act on.
 *
 * Fixing the size gives up nothing that was being used and buys the thing that
 * matters: a slot whose position is arithmetic rather than a measurement, which
 * is what lets the wall be lazy. The stagger below is what keeps it from being a
 * table — see [rememberColumnStagger].
 *
 * [TileChrome] is the part that does not scale with type — the two paddings, the
 * plate row and the gaps around it. [TileText] is the part that does: [TextLines]
 * shared between the name and the blurb, plus one of category, at a 1× font
 * scale. It holds either way the budget is spent — a three-line name over a
 * two-line blurb is three dp taller than two over three. They are split so
 * that a student running the system font at 1.6× gets a taller tile rather than a
 * clipped one, and every tile grows by the same amount so the stagger holds.
 */
private val TileChrome = 80.dp
private val TileText = 112.dp

/** How far the type is allowed to push the tile before it starts clipping instead. */
private const val MaxTypeScale = 1.6f

/** One tile's height at the reader's font scale. Every tile on the wall gets this. */
@Composable
private fun rememberTileHeight(): Dp {
    val fontScale = LocalDensity.current.fontScale
    return TileChrome + TileText * fontScale.coerceIn(1f, MaxTypeScale)
}

/**
 * How far the second column hangs below the first — the whole of the effect.
 *
 * Exactly half a tile plus half a gap, so the two columns are as far out of phase
 * as they can get: every tile's midpoint sits level with the seam between its two
 * neighbours, and no horizontal line anywhere on the wall touches more than one
 * tile. Anything less than half reads as a grid that failed to line up; half
 * reads as brickwork.
 *
 * It is applied to exactly one tile — the first one to land in the second column —
 * and then it holds for the whole wall on its own. Uniform heights mean the
 * packer alternates strictly from that point, so one offset at the top propagates
 * all the way down without anything else having to know about it.
 */
@Composable
private fun rememberColumnStagger(): Dp = (rememberTileHeight() + Dimens.Space) / 2

/**
 * The blurb is clipped to what the tile holds, which is three lines.
 *
 * Its own note because this has been both ways. The cap came off when tiles were
 * content-sized: a wall where one card ends in "…" and the card beside it does
 * not reads as the first one having failed to load, not as it having more to say,
 * and with heights coming from the text there was no reason to cap it.
 *
 * A fixed tile brings the cap back by definition — something has to give when the
 * copy is longer than the card — and the "…" objection goes with it, because now
 * *every* long blurb ends the same way at the same line. The ragged-vs-uniform
 * complaint only ever applied to a wall where the cards were different sizes.
 *
 * Three lines is what the placeholder copy runs to at this column width; `about`
 * is empty on all 28 booths today, so nothing in the fair currently reaches it.
 */
private const val BlurbLines = 3

/**
 * How many lines the club's name may run to, and what it costs.
 *
 * Two was wrong, and "Community Development Volunteer Club" is the proof: it set
 * as "Community / Development Volu…" and stopped, so the one line on the card
 * that has to be complete — the name of the thing the tile is *for* — was the one
 * being cut. A blurb that trails off still tells you what the club does. A name
 * that trails off is a club you cannot identify, on a card whose entire job is to
 * be identified from across a hall.
 *
 * So the name takes a third line when it needs one. What it does *not* do is make
 * the tile taller: uniform heights are what the stagger is built on, and one card
 * growing to fit its title would put a notch in the column and break the phase for
 * everything under it.
 *
 * The room comes out of the blurb instead. [TextLines] is the budget the two of
 * them share, and it is spent name-first — the name takes what it needs and the
 * blurb gets the remainder, so a three-line name is paid for by a two-line blurb.
 * That is the right way round: the blurb is placeholder copy on all 28 booths
 * today, and losing its last line costs a fragment of a sentence, while gaining a
 * third name line is the difference between a club being named and not.
 *
 * A short name earns no bonus — one line of name still leaves the blurb at
 * [BlurbLines] rather than stretching it to four. The slack goes to the spacer
 * above the category, where it is a margin instead of a card that looks
 * differently laid out from its neighbours.
 */
private const val NameLinesTypical = 2
private const val NameLinesMax = 3
private const val TextLines = NameLinesTypical + BlurbLines

/**
 * What the blurb gets once the name has taken its share.
 *
 * Never less than one line: a card with a name and nothing under it is the
 * name-only tile an earlier version tried and the reason the blurb is on every
 * card in the first place.
 */
private fun blurbLinesFor(nameLines: Int): Int =
    (TextLines - nameLines.coerceAtLeast(NameLinesTypical)).coerceIn(1, BlurbLines)

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
 * pass under it and have to be softened. These are a screenful of panes over a
 * still mesh that is nothing but soft gradients already, so the blur pass had
 * almost nothing to soften while costing the most of any effect here: with it,
 * four flings through a zone missed 7% of their frames; without it, 2%.
 *
 * Those numbers were taken on a nine-tile wall laid out all at once, which is
 * neither the layout nor the zone the app actually has — Savannah is 16. They are
 * the right order of magnitude and the wrong absolute figures; worth re-taking
 * against the lazy grid before anything is tuned on them.
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
 * still there when a wall of them is together — a rainforest wall and an ocean
 * wall are visibly different rooms — but no single tile announces a colour, which
 * is the whole of what "subtle" means here.
 *
 * Lifting toward white rather than dropping alpha keeps the hue: alpha alone,
 * over a near-black mesh, makes a colour darker on its way to disappearing.
 *
 * Not private any more: the zone picker's three cards are made of this too, so
 * the card you tap and the wall of tiles behind it are one material rather than
 * two that happen to sit next to each other. One recipe, stated once — copying
 * `0.82` into the other file is how the two drift apart the next time it moves.
 */
internal fun cardTint(accent: Color): Color = lerp(accent, Color.White, 0.82f)

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
 * How much wall is composed below the fold while a booth is open.
 *
 * The focus shift slides the wall up as a layer, and a lazy grid has no idea that
 * happened — it composes for its own bounds, so whatever the slide uncovers at the
 * bottom of the screen would be empty. This is how much taller the grid is told it
 * is while a tile is selected, and it is exactly the furthest the shift can travel:
 * a tile at the very bottom of the window has to climb all but
 * [FocusViewportFraction] of it.
 *
 * Only while one is selected. During a fling — the case that has to hold its frame
 * budget — it is zero and the grid composes exactly what is on screen.
 */
private const val FocusHeadroom = 1f - FocusViewportFraction

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
    val grid = rememberLazyStaggeredGridState()
    var viewportPx by remember { mutableIntStateOf(0) }

    val tileHeight = rememberTileHeight()
    val stagger = rememberColumnStagger()
    val tileHeightPx = with(LocalDensity.current) { tileHeight.toPx() }

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
        // The grid reports where it actually put things, already relative to the
        // window and already net of the scroll — which is the whole of what the
        // hand-rolled masonry needed a map of reported tile centres to work out.
        // The masthead is item 0, so a booth's row is one past its place in the list.
        val tile = grid.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index + 1 }
        val target = if (index < 0 || viewportPx == 0 || tile == null) {
            0f
        } else {
            // Measured from the bottom of the slot rather than the middle of it:
            // the one staggered tile carries its offset as padding above the
            // glass, so its slot is taller than the pane inside it and only the
            // bottom edges of the two coincide.
            val centre = tile.offset.y + tile.size.height - tileHeightPx / 2f
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
                // Lazy, and it has to be. This was a hand-scrolled Column on the
                // grounds that "a zone is nine tiles" — which was never true of
                // the zone that matters. The fair's 28 booths fall 7 / 16 / 5
                // across the three areas, so Savannah is more than twice the wall
                // the arithmetic assumed, and every tile on it is a full pane of
                // liquid glass: a vibrancy pass and a lens pass per tile, over a
                // backdrop the drifting mesh invalidates every frame. Laying all
                // of them out was affordable at nine and was most of why the
                // Savannah wall dropped frames the other two did not.
                //
                // The tiles are a fixed size now, so nothing has to be measured to
                // know where it goes and the grid can place a slot from its index.
                // That is what makes the laziness available at all.
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(WallColumns),
                    state = grid,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusHeadroom(active = selected != null)
                        .graphicsLayer { translationY = shift.value },
                    contentPadding = PaddingValues(
                        start = Dimens.ScreenPadding,
                        end = Dimens.ScreenPadding,
                        bottom = Dimens.NavBarClearance,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Space),
                    verticalItemSpacing = Dimens.Space,
                ) {
                    item(span = StaggeredGridItemSpan.FullLine, key = "masthead") {
                        ZoneMasthead(
                            zone = zone,
                            booths = booths,
                            scanned = booths.count { it.scanned },
                            total = booths.size,
                        )
                    }

                    itemsIndexed(booths, key = { _, booth -> booth.id }) { index, booth ->
                        BoothTile(
                            booth = booth,
                            accent = zone.accent,
                            modifier = Modifier
                                // The offset that makes the wall a wall. It goes
                                // on one tile only — the first to land in the
                                // second column, which with the masthead spanning
                                // the full line above it is booth number two — and
                                // uniform heights carry it down the rest on their
                                // own. See [rememberColumnStagger].
                                //
                                // Outside the glass rather than inside it: this
                                // moves the pane down the wall, it does not pad the
                                // card's contents away from its own top edge.
                                .padding(top = if (index == 1) stagger else 0.dp)
                                .height(tileHeight)
                                .tileSurface(
                                    accent = zone.accent,
                                    scanned = booth.scanned,
                                    backdrop = wallpaper,
                                    opened = selected == booth,
                                    dimmed = selected != null && selected != booth,
                                    onClick = { onSelectBooth(booth) },
                                ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tells the grid it is taller than its slot, so the focus shift has something to
 * slide into.
 *
 * The shift moves the wall as a graphics layer. That is deliberate — see the note
 * at its call site for why it is not a scroll — but a lazy component composes for
 * the bounds it was measured at and knows nothing about a translation applied to
 * its output. Slide it up 70% of the window and the bottom 70% of the screen is
 * whatever was already below the fold, which is nothing.
 *
 * So it is measured against a taller window and placed in the real one, with the
 * parent's `clipToBounds` cutting off the overhang. The grid fills the extra
 * height with the rows that come next, which are exactly the rows the slide is
 * about to reveal.
 *
 * Only while a booth is open. [FocusHeadroom] worth of extra tiles is real work,
 * and the moment it is being paid for is the one moment nothing is moving — the
 * wall has stopped, the other tiles are dimming to [DimmedAlpha], and the panel is
 * on its way up. A fling gets the plain viewport.
 */
private fun Modifier.focusHeadroom(active: Boolean): Modifier =
    if (!active) this else layout { measurable, constraints ->
        val extra = (constraints.maxHeight * FocusHeadroom).roundToInt()
        val placeable = measurable.measure(
            constraints.copy(
                minHeight = constraints.maxHeight + extra,
                maxHeight = constraints.maxHeight + extra,
            ),
        )
        layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, 0) }
    }

/**
 * The pane every tile is cut from.
 *
 * Liquid glass rather than the frosted `glassSurface` the rest of the app uses,
 * and the difference is worth the shader here: a wall is a field of panes laid
 * over one still image, which is the one arrangement where refraction reads as
 * material — each tile bends the mesh's arcs a little differently depending on
 * where it sits, so a wall of tiles is a wall of pieces of glass rather than one
 * grey rectangle repeated.
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

    // No size here: the caller sets the height, above this in the chain, so that
    // the staggered tile's offset lands outside the glass rather than inside it.
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
 * **Every tile is the same height**, set by the wall rather than by the tile —
 * see the note on [TileChrome]. So the card has slack in it, and where the slack
 * goes is the one thing worth being careful about here.
 *
 * It goes into a single gap above the category line, which is pinned to the
 * floor. Not spread through the card: an earlier fixed-height version pinned the
 * plate to the ceiling *and* the caption to the floor and let the middle stretch,
 * which opened a hole between the blurb and the name on every tile whose copy ran
 * short. One gap in one place, always the same place, reads as a margin. Two gaps
 * that grow and shrink read as a layout that has come apart.
 *
 * The type went up a step and a half — 13sp/11sp to 15sp/12sp — which is the
 * change that made the tile look like a card rather than a caption. 13sp bold is
 * the size a *label* is set at; this line is the name of the thing the tile is
 * for, and it was smaller here than the same club's name is anywhere else in the
 * app.
 *
 * The name takes up to three lines and the blurb gets what is left of
 * [TextLines] — see [NameLinesTypical] for why the name is the one that wins the
 * argument. A line holds about 16 characters at 15sp in a column of this width,
 * so three lines clear the longest club in the roster with room over.
 */
@Composable
private fun BoothTile(
    booth: Booth,
    accent: Color,
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

        // How many lines the name actually took, once it has been set. The blurb
        // below reads it back to decide how much room is left — see [TextLines].
        var nameLines by remember(booth.id) { mutableIntStateOf(NameLinesTypical) }

        Spacer(Modifier.height(Dimens.Space))
        Text(
            text = booth.displayName(),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            color = Color.White.copy(alpha = InkName),
            maxLines = NameLinesMax,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { nameLines = it.lineCount },
        )

        // On every tile without exception, again. It is the line that says why
        // to walk over, and a booth card without one is a card that can't be
        // used — which is why the tile stopped omitting it: `Booth.blurb` falls
        // back to placeholder copy for the club's category while `about` is
        // empty, so there is always something here and the tiles stop varying
        // in height for no reason a student can see.
        Spacer(Modifier.height(Dimens.SpaceXs))
        Text(
            text = booth.blurb(),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = Color.White.copy(alpha = InkBlurb),
            maxLines = blurbLinesFor(nameLines),
            overflow = TextOverflow.Ellipsis,
        )

        // The card's slack, all of it, in one place.
        Spacer(Modifier.weight(1f))

        // What kind of club it is. On every tile now rather than on the taller
        // two thirds of them — with one height there is no such thing as a tile
        // that has room for it and one that doesn't, and a line that appears on
        // some cards and not others reads as missing data on the rest.
        //
        // It held the club's name in the other language until that turned out to
        // mean a Thai line on every tile of an English screen.
        //
        // A member count and a "meets Wed 17:00 · Main Field" line used to sit
        // here. Both are gone rather than ported: `members`, `meets` and `venue`
        // were invented per club when the roster was written, and the Student
        // Union has never supplied any of the three. There is nothing to render,
        // and a fabricated figure on a card a student uses to choose where to walk
        // is worse than a shorter card.
        booth.categoryName()?.let { category ->
            Text(
                text = category,
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
 * It reads as a title for the booths below it rather than for the screen,
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
    booths: List<Booth>,
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
                    text = zone.displayTitle(),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Bold,
                    // 24sp, one step up from the 20sp of a card title: this is
                    // the only heading on the screen, so it is allowed to be
                    // the biggest thing on it.
                    fontSize = 24.sp,
                    color = Color.White,
                )
                Text(
                    text = zoneIntent(zone, booths).orEmpty(),
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
