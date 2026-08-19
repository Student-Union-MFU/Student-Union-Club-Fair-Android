package com.su.clubfair.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.data.FairSchedule
import com.su.clubfair.data.FairStatus
import com.su.clubfair.ui.components.GlassIconButton
import com.su.clubfair.ui.components.ProgressTrack
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.model.FairProgress
import com.su.clubfair.ui.model.PreviewProgress
import com.su.clubfair.ui.model.PreviewStudent
import com.su.clubfair.ui.model.Student
import com.su.clubfair.ui.model.ProgramEntry
import com.su.clubfair.ui.model.stepsAt
import com.su.clubfair.ui.model.running
import com.su.clubfair.ui.model.upNext
import com.su.clubfair.ui.model.displayTitle
import com.su.clubfair.ui.program.timeRange
import com.su.clubfair.ui.program.whereLine
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AppSans
import com.su.clubfair.ui.theme.AppTextWeight
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.LocalAccent
import com.su.clubfair.ui.theme.Palette
import com.su.clubfair.ui.theme.SUClubFairTheme
import kotlinx.coroutines.delay

private val CardRadius = Dimens.RadiusLg

/**
 * Booths per row in the checkpoint grid.
 *
 * **Fourteen, not seven.** Both divide 28 exactly, and that was the only test
 * the old seven was chosen against — but a cell stretches to the column width,
 * so seven columns made every cell a fifth of the screen wide and the block four
 * of those tall. The card came out around 300dp: taller than everything else on
 * Home put together, for a picture of a number the line above it already gives.
 * With the countdown card added above it, that pushed the greeting off the top
 * of the screen.
 *
 * Fourteen halves the cell and halves the rows — the same 28 stamps in about a
 * quarter of the height, which is the size a stamp card should be. Small is not
 * a compromise here: a cell is a tick, not a target, and nothing is ever tapped.
 */
private const val GridColumns = 14



// Pull-to-refresh is still behind an opt-in in Material 3. Annotated at the one
// screen that uses it rather than switched on for the module, so the day it
// changes shape there is a single call site to fix.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    student: Student = PreviewStudent,
    /**
     * How far round the fair, from `GET /clubfair/progress`.
     *
     * Separate from [student] because it changes on every scan while the identity
     * does not — folding them together meant each scan produced a "new student"
     * and rebuilt the whole shell.
     */
    progress: FairProgress = PreviewProgress,
    /** The last refresh could not reach the server; what is shown may be stale. */
    offline: Boolean = false,
    /** A refresh is in flight — the pull indicator stays out while it is. */
    refreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onOpenClubs: () -> Unit = {},
    onOpenPrizes: () -> Unit = {},
    onOpenProgram: () -> Unit = {},
    program: List<ProgramEntry> = emptyList(),
    onOpenProfile: () -> Unit = {},
) {
    val scroll = rememberScrollState()

    // No backdrop here: AppShell paints one behind every tab, so a tab that
    // painted its own would stack a second full-screen gradient on it.
    //
    // `BoxWithConstraints` + `heightIn(min = maxHeight)` is what lets the content
    // sit on the bottom edge. `Arrangement.Bottom` alone does nothing inside a
    // `verticalScroll`, because the scroll hands its child an infinite height and
    // a column that is exactly as tall as its content has no spare room to
    // arrange within. Forcing a minimum of one viewport gives it that room back
    // while still letting the column grow and scroll on a short screen.
    //
    // The profile button is not in here — it is pinned to the top of the box
    // below, so only the body travels to the bottom.
    // The gesture goes on the box that is already here rather than wrapping the
    // screen in a `PullToRefreshBox`, which is the same thing plus a Box and would
    // have re-indented the entire screen to add one modifier. The indicator is a
    // child of it, below.
    //
    // Before `safeDrawingPadding`, so the pull is caught anywhere on the screen
    // including the inset strip under the status bar, where a downward drag
    // starting at the top edge naturally begins.
    val pullState = rememberPullToRefreshState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pullToRefresh(
                isRefreshing = refreshing,
                state = pullState,
                onRefresh = onRefresh,
            )
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(scroll)
                .padding(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.Bottom,
        ) {
            // The column packs to the bottom, so anything taller than the
            // viewport pushes the greeting off the top rather than scrolling.
            // This is the floor under the status bar and the profile button.
            Spacer(Modifier.height(Dimens.SpaceXl))
            HomeHeader(name = student.name)

            // The one line of supporting type under the name.
            //
            // "Find your club this weekend" sat here first and went because it
            // was doing nothing this line does not: it named the event to a
            // student already inside the event's own app, in copy that read the
            // same on the Tuesday three weeks out as on the morning of the fair.
            // The countdown says what today is and changes while you look at it,
            // and two muted lines under a greeting is one more than the header
            // can carry before it stops being a greeting.
            Spacer(Modifier.height(Dimens.SpaceSm))
            FairStatusLine()

            // Said once, at the top, and only when it is true. A stale-data notice
            // repeated per card would be four times the noise for one fact.
            if (offline) {
                Spacer(Modifier.height(Dimens.Space))
                OfflineNotice()
            }

            // Checkpoints leads the card stack, and is now the only thing that
            // could.
            //
            // The two used to swap places: the countdown was promoted before the
            // fair opened, on the argument that there is no progress to report
            // yet and the one live fact is that it opens in ten days. Defensible,
            // and it made the page rearrange itself on a date — a student who
            // learned where their booth count lives came back on the 22nd to find
            // it had moved. A home screen that keeps still is worth more than one
            // that is optimally ordered on any given morning.
            //
            // That argument is spent now that the countdown is a caption up in
            // the header: nothing is competing for this slot, and Checkpoints
            // holds it because it is the only card on the page about *this
            // student*. The countdown is the same on two thousand phones.
            Spacer(Modifier.height(Dimens.SpaceLg))
            CheckpointsCard(progress = progress)

            Spacer(Modifier.height(Dimens.Space))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space)) {
                ShortcutTile(
                    art = R.drawable.art_clubs,
                    label = R.string.home_clubs,
                    onClick = onOpenClubs,
                    modifier = Modifier.weight(1f),
                )
                ShortcutTile(
                    art = R.drawable.art_prizes,
                    label = R.string.home_prizes,
                    onClick = onOpenPrizes,
                    modifier = Modifier.weight(1f),
                )
            }

            // Under the pair rather than beside them, and a different shape on
            // purpose. Three equal tiles across would have cut each one to a
            // third of the width, and the illustration is what makes these read
            // as somewhere to go rather than as buttons — at that size it stops
            // being an illustration. A wide row also suits what this one is:
            // Clubs and MFU333 are places, the programme is a question with a
            // time in the answer, so it gets a line to say so.
            Spacer(Modifier.height(Dimens.Space))
            ProgramCard(program = program, onClick = onOpenProgram)


            // Clearance so the last card can scroll out from behind the nav bar.
            Spacer(Modifier.height(Dimens.NavBarClearance))
        }

        // Pinned to the top rather than scrolled with the content, so it stays
        // put while the block below sits on the bottom edge.
        //
        // Announcements used to have a bell up here beside it. Events is a tab in
        // the nav bar now, so the bell would be the same glyph twice on one
        // screen, three inches apart, going to the same place. Profile is the
        // opposite case: it left the nav bar, so this is its only way in.
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space),
            horizontalArrangement = Arrangement.End,
        ) {
            GlassIconButton(
                icon = R.drawable.ic_user,
                contentDescription = stringResource(R.string.home_profile),
                onClick = onOpenProfile,
            )
        }

        // Last child, so it draws over the header rather than under it. Painted
        // in the app's own panel and accent instead of Material's surface
        // colours, which on this backdrop arrive as a light grey disc.
        PullToRefreshDefaults.Indicator(
            state = pullState,
            isRefreshing = refreshing,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = Palette.Panel,
            color = LocalAccent.current,
        )
    }
}

/**
 * "Welcome back," over the name, with the name carrying the weight.
 *
 * The name gets a line to itself: it is the only part of the headline that is
 * about *this* student, and on one line a long one either shrank the whole
 * greeting or pushed itself into a ragged second line broken mid-name. The
 * emphasis still comes from weight and colour rather than size — bold white
 * against a normal-weight muted greeting — because two sizes stacked this
 * closely would read as two headings instead of one.
 *
 * Built by splitting the template on its placeholder rather than appending the
 * name to a prefix, so a translation is free to put the name first — which
 * several languages do — without the styling, or the line break, landing on the
 * wrong half.
 */
@Composable
private fun greetingWith(name: String): AnnotatedString {
    val template = stringResource(R.string.home_greeting, NamePlaceholder)
    // The name carries the line on tone alone now: white against the prefix's
    // muted ink, at the same weight as everything else. It was Bold, which is
    // the one thing on the page that would still have been if the sweep had
    // missed it — an AnnotatedString span is not a `fontWeight =` on a Text.
    val emphasis = SpanStyle(fontWeight = AppTextWeight, color = Color.White)

    return buildAnnotatedString {
        pushStyle(SpanStyle(fontWeight = AppTextWeight, color = Ink.Muted))
        val at = template.indexOf(NamePlaceholder)
        if (at < 0) {
            // The placeholder was dropped from a translation. Show the greeting
            // and the name rather than silently losing one of them.
            append(template)
            append('\n')
            withStyle(emphasis) { append(name) }
            return@buildAnnotatedString
        }

        // The break goes next to the name, not at a fixed position in the
        // string: whichever side of it the greeting sits on, the name ends up
        // alone on its own line. Trimmed on the joining side only, so the
        // template's own punctuation — the comma after "Welcome back" — stays
        // where the translator put it.
        val before = template.substring(0, at).trimEnd()
        val after = template.substring(at + NamePlaceholder.length)
        if (before.isEmpty()) {
            withStyle(emphasis) { append(name) }
            append('\n')
            append(after.trimStart())
        } else {
            append(before)
            append('\n')
            withStyle(emphasis) { append(name) }
            append(after)
        }
    }
}

/**
 * Stands in for the name while the template is formatted, so the surrounding copy
 * can be recovered either side of it. Anything a name can't contain will do.
 */
private const val NamePlaceholder = "\u0000"

/**
 * "You may be looking at old numbers."
 *
 * A line, not an error card. The cached data is still on screen and still mostly
 * right, so this qualifies it rather than replacing it — an error screen here
 * would hide a booth count that is very probably correct.
 */
@Composable
private fun OfflineNotice(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = Dimens.Space, vertical = Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_alert_circle),
            contentDescription = null,
            tint = Ink.Muted,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.size(Dimens.SpaceSm))
        Text(
            text = stringResource(R.string.home_offline),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 12.sp,
            color = Ink.Muted,
        )
    }
}

/**
 * When the fair opens, and how far off that is.
 *
 * **A line of type, not a card.** This had a glass surface of its own for
 * several versions, and every attempt to make that surface look right failed the
 * same way: a card is a container, and there is one sentence to put in this one.
 * At a card's proportions it sat half empty, so the empty half was given an
 * illustration; the illustration went, and the content was centred in a pill
 * instead — which cured the emptiness by making the shape wrong, a 5:1 capsule
 * reading as a rectangle with strange corners against three 20dp cards, and off
 * the left edge every other thing on the page is aligned to. The surface was the
 * problem. Without one there is nothing left to look under-filled.
 *
 * It sits under the headline rather than in the card stack for the same reason.
 * This is the page's supporting type — the sentence that says what today is —
 * so it belongs with the greeting it qualifies, not between two cards a student
 * navigates from.
 *
 * The label and the figure are one line, told apart by colour rather than by a
 * separator: muted for the words, white for the number. A dot between them would
 * be the second one in "Fair is open · ends in".
 */
@Composable
private fun FairStatusLine(modifier: Modifier = Modifier) {
    Text(
        text = fairCountdown(),
        modifier = modifier
            // A pill now that it hugs its own content rather than a card's
            // width. This is the shape working the way it is supposed to: the
            // ends cap a single line of type instead of terminating a 5:1 box,
            // which is what made the full-width version read as a rectangle with
            // strange corners.
            .glassSurface(cornerRadius = Dimens.RadiusPill)
            // Measured off the line's own height rather than the card scale:
            // a badge's padding is a fraction of its type, and CardPadding here
            // would put it in a lozenge twice the size of what it holds.
            .padding(horizontal = Dimens.Space, vertical = Dimens.SpaceSm),
        fontFamily = AppSans,
        fontWeight = AppTextWeight,
        // A step under the 16sp this started at. In a badge the type is not the
        // page's supporting copy any more — it is the badge's contents, and at
        // 16sp the pill was as tall as a control and read as something to press.
        // 14sp is where it settles into a label.
        fontSize = 14.sp,
        lineHeight = 1.3.em,
        // Tabular figures, so the line does not shuffle sideways as a digit
        // rolls over. Inherited from Alan Sans, which carried `tnum` — worth
        // confirming Anuphan does, or this is doing nothing.
        style = TextStyle(fontFeatureSettings = "tnum"),
        color = Ink.Muted,
        maxLines = 1,
    )
}

/**
 * How long until the fair opens, recomputed on a timer.
 *
 * This was the string `"4h"` under the word "Ends in" — a constant, so it read
 * "4h" at nine in the morning and "4h" ten minutes before the fair closed. It is
 * the one figure on this screen a phone can work out exactly, which made
 * hardcoding it the least defensible thing on it.
 *
 * [produceState] ties the loop to this composable, so leaving Home stops the
 * clock rather than leaving it running in a pocket.
 */
@Composable
private fun fairCountdown(): AnnotatedString {
    val status by produceState<FairStatus>(
        initialValue = FairSchedule.statusAt(System.currentTimeMillis()),
    ) {
        while (true) {
            val current = FairSchedule.statusAt(System.currentTimeMillis())
            value = current
            delay(tickFor(current))
        }
    }

    val current = status
    val label = when (current) {
        is FairStatus.BeforeStart -> stringResource(R.string.home_opens_in)
        is FairStatus.Running -> stringResource(R.string.home_open_ends_in)
        FairStatus.Ended -> stringResource(R.string.home_stat_ended)
    }
    val figure = when (current) {
        is FairStatus.BeforeStart -> remainingTime(current.untilStartMillis)
        is FairStatus.Running -> remainingTime(current.remainingMillis)
        FairStatus.Ended -> {
            val ended = stringResource(R.string.home_stat_ended_value)
            buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White)) { append(ended) }
            }
        }
    }

    // Two spaces. The colour change is what separates these, and a single space
    // between muted words and a white figure closes up enough to read as one
    // word at 16sp.
    return buildAnnotatedString {
        append(label)
        append("  ")
        append(figure)
    }
}

/**
 * How long to wait before working the figure out again.
 *
 * A second, because the figure has a seconds place at every distance from the
 * fair — see [remainingTime]. It was briefly adaptive, a second inside the last
 * hour and half a minute above it, which was the right answer for a two-unit
 * figure that only showed seconds at the end; with the seconds always on screen
 * a coarser tick is just a clock that has stopped.
 *
 * One `Text` recomposing per second is what a countdown is. [produceState] ties
 * the loop to the composable, so it stops when Home leaves the composition
 * rather than running on in a pocket.
 */
private fun tickFor(status: FairStatus): Long = when (status) {
    // Nothing left to count. The loop stays alive rather than exiting so the
    // composable does not need a second shape for its terminal state.
    FairStatus.Ended -> 60_000L
    else -> 1_000L
}

/** Unit letters against the digits they belong to — "2" full size, "d" a little under. */
private const val UnitScale = 0.8f

/**
 * How long is left, largest unit first: "2d 21h 33m 36s".
 *
 * **All four units, down to the seconds.** It was cut to the two largest for a
 * version, on the argument that seconds three days out are a stopwatch telling a
 * student nothing. True of the information and wrong about what the badge is
 * for: the seconds are the only thing on Home that moves while you are looking
 * at it, and without them the badge is a fact that could have been printed. The
 * cost is a longer line and a recomposition per second, and both are worth it.
 *
 * A unit that has run out drops off the front rather than showing a zero: the
 * final hour reads "40m 12s", not "0d 0h 40m 12s". Trailing zeroes are kept —
 * "2d 0h 40m 12s" is right, because dropping the hours there leaves "2d 40m",
 * which reads as a typo.
 *
 * **The order used to be inverted and it made the card lie.** Hours and minutes
 * were the figure and the days trailed behind them at half size in muted ink, on
 * the reasoning that the days are the part a student already knows — the date is
 * on the line beside it — while the minutes are the part they cannot work out
 * for themselves. Coherent, and disproved by looking at it: at three days out it
 * read "23h 40m  2d", and every convention there is says the big bright thing is
 * the number and the small grey thing is a label. A student glancing at it
 * concluded the fair opened the next morning. It opened in three days.
 *
 * So the units run the way durations are written everywhere else, and nothing is
 * demoted a whole size class. The split is between *digits* and *unit letters*
 * rather than between one unit and another: both figures are the same size and
 * weight, and their letters ride a little under it. That reads as one number
 * with its units attached instead of two numbers of unequal rank.
 *
 * The trailing unit is zero-padded and the leading one is not. Without the
 * padding the line changes width every time a figure crosses ten and the caption
 * shuffles sideways under the reader; with it on the leading unit, "02d" is a
 * stopwatch affectation on something that only changes once a day. A trailing
 * zero is kept — "2d 00h" is right, because dropping it leaves a bare "2d" that
 * reads as a different, coarser figure.
 */
@Composable
private fun remainingTime(millis: Long): AnnotatedString {
    val total = (millis / 1_000L).coerceAtLeast(0L)
    val days = total / (60 * 60 * 24)
    val hours = (total / (60 * 60)) % 24
    val minutes = (total / 60) % 60
    val seconds = total % 60

    // Resolved out here: buildAnnotatedString's lambda is not composable.
    //
    // The suffix alone, not "%1$d d" — the number and its unit are styled
    // differently, so they cannot come from one format string. Thai carries its
    // own leading space inside the resource, which is why these are quoted over
    // there: " ชม." is spaced and "h" is not, and that is a fact about the
    // language rather than something to decide in Kotlin.
    val dayUnit = stringResource(R.string.home_unit_day)
    val hourUnit = stringResource(R.string.home_unit_hour)
    val minuteUnit = stringResource(R.string.home_unit_minute)
    val secondUnit = stringResource(R.string.home_unit_second)

    val shown = buildList {
        if (days > 0) add(days to dayUnit)
        if (days > 0 || hours > 0) add(hours to hourUnit)
        if (days > 0 || hours > 0 || minutes > 0) add(minutes to minuteUnit)
        add(seconds to secondUnit)
    }

    val unitStyle = SpanStyle(
        // Relative rather than absolute, so the ratio survives whatever size the
        // caption is set at.
        fontSize = UnitScale.em,
        fontWeight = AppTextWeight,
        // Dimmed against the digits, but nowhere near the muted ink of the label
        // beside them: these are part of the figure and have to read as part of
        // it. The job of the tone is to let the digits lead, not to push the
        // letters into the background.
        color = Color.White.copy(alpha = 0.72f),
    )

    return buildAnnotatedString {
        // White against the label's muted ink. This is the whole separation
        // between the two halves of the line, so it is the one thing here that
        // cannot be dropped.
        withStyle(SpanStyle(color = Color.White)) {
            shown.forEachIndexed { index, (value, unit) ->
                if (index > 0) append(" ")
                append(if (index == 0) value.toString() else value.toString().padStart(2, '0'))
                withStyle(unitStyle) { append(unit) }
            }
        }
    }
}

/**
 * The greeting, as the page's headline.
 *
 * Two lines by construction — see [greetingWith] — so [maxLines] is 2 rather
 * than unbounded: a name long enough to wrap would push the whole card stack
 * down the page, and an ellipsis on the end of it is the honest outcome.
 */
@Composable
private fun HomeHeader(name: String, modifier: Modifier = Modifier) {
    Text(
        text = greetingWith(name),
        modifier = modifier,
        fontFamily = AppSans,
        fontSize = 32.sp,
        lineHeight = 1.15.em,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun CheckpointsCard(
    progress: FairProgress,
    modifier: Modifier = Modifier,
) {
    val visited = progress.visited
    val total = progress.total
    val fraction = progress.progress

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = CardRadius)
            .padding(Dimens.CardPadding),
    ) {
        Text(
            text = stringResource(R.string.home_checkpoints),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 15.sp,
            color = Ink.Label,
        )

        Spacer(Modifier.height(Dimens.SpaceXs))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.home_checkpoints_count, visited),
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                // The size the countdown used to have when it led the page. This
                // card holds the lead slot now, so it holds the figure that goes
                // with it — otherwise "first" is only an ordering and not an
                // emphasis, and the two cards read as equals in a new order.
                fontSize = 40.sp,
                lineHeight = 1.em,
                // Accent once there is something to be pleased about, white
                // while it is nothing. Lime in this app means a thing has
                // happened — a scanned checkpoint, a filled bar — so a lime zero
                // would be the one number on the page claiming credit it has not
                // earned. It is also the only accent on this screen, which is
                // what makes the first scan visibly change the page.
                color = if (visited > 0) Palette.Accent else Color.White,
            )
            Spacer(Modifier.size(Dimens.SpaceSm))
            Text(
                text = pluralStringResource(R.plurals.home_checkpoints_total, total, total),
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 15.sp,
                color = Ink.Muted,
                modifier = Modifier.padding(bottom = Dimens.SpaceXs),
            )

            // The percentage sits above the bar, on the count's own line and
            // pushed to the far end of it. Below the bar it was a footnote to a
            // thing that had already been said — the bar is the percentage,
            // drawn — and it left the card ending on its faintest line before
            // the grid started. Up here the two figures read as one statement:
            // how many booths on the left, how far along on the right, and the
            // bar underneath as the picture of both.
            Spacer(Modifier.weight(1f))
            Text(
                // Truncates rather than rounds, so the number never claims
                // progress that hasn't been earned — 26/27 must not read as
                // "100 % complete".
                text = stringResource(
                    R.string.home_checkpoints_percent,
                    (fraction * 100).toInt(),
                ),
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 11.sp,
                color = Ink.Muted,
                modifier = Modifier.padding(bottom = Dimens.SpaceXs),
            )
        }

        Spacer(Modifier.height(12.dp))
        ProgressTrack(fraction = fraction)

        Spacer(Modifier.height(Dimens.Space))
        CheckpointGrid(
            visited = visited,
            total = total,
            scannedBooths = progress.visitedBoothIds,
        )
    }
}

/**
 * One cell per booth: filled once that booth is scanned, hollow until then.
 *
 * Cell *n* is booth *n*. It reads left to right in booth order, so the pattern of
 * filled cells is a map of which side of the hall has been walked — which is the
 * only thing a grid tells you that the number above it does not.
 */
@Composable
private fun CheckpointGrid(
    visited: Int,
    total: Int,
    scannedBooths: Set<Int>,
    modifier: Modifier = Modifier,
) {
    val rows = (total + GridColumns - 1) / GridColumns
    val summary = stringResource(R.string.home_checkpoints_desc, visited, total)

    Column(
        modifier = modifier
            .fillMaxWidth()
            // 27 undecorated boxes are 27 stops under TalkBack, none of which say
            // anything. The grid is a picture of one number, so it announces that
            // number and nothing else.
            .clearAndSetSemantics { contentDescription = summary },
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        repeat(rows) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                repeat(GridColumns) { column ->
                    val index = row * GridColumns + column
                    if (index < total) {
                        CheckpointCell(
                            // Cell n is booth id n. Ids run from 1 and the grid is
                            // indexed from 0.
                            scanned = (index + 1) in scannedBooths,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        // Keeps the last row's cells the same size as the rest
                        // when the total isn't a clean multiple of the columns.
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckpointCell(scanned: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .then(
                if (scanned) {
                    Modifier.background(LocalAccent.current)
                } else {
                    // A hairline outline, not glassSurface: at 20dp the frost and
                    // border stack into a chip that reads as filled, which is the
                    // opposite of what an un-scanned booth means.
                    //
                    // The outline was 22% white, which is legible on one cell and
                    // a waffle across 28 — and 28 is what every student sees
                    // until they scan something. At 10% the empty board recedes
                    // to a texture and the lime of a collected booth is the only
                    // thing with any weight in the card, which is the right way
                    // round for a grid whose whole job is to show what you have
                    // done rather than what you have not.
                    Modifier
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
                },
            ),
    )
}

@Composable
private fun ShortcutTile(
    @DrawableRes art: Int,
    @StringRes label: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .glassSurface(cornerRadius = CardRadius)
            .clip(RoundedCornerShape(CardRadius))
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.Space),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // An illustration, not a tinted glyph. The two tiles were a 26dp line
        // icon each — correct, legible and completely interchangeable with the
        // nav bar three centimetres below, which uses the same glyph for the
        // same destination. These are the only pieces of illustration on the
        // page and they are what make the tiles read as somewhere to go rather
        // than as two more buttons.
        Image(
            painter = painterResource(art),
            contentDescription = null,
            modifier = Modifier.size(76.dp),
        )
        Spacer(Modifier.height(Dimens.SpaceXs))
        Text(
            text = stringResource(label),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 14.sp,
            color = Ink.Label,
        )
    }
}

/**
 * What is on at the fair, on the page a student actually opens.
 *
 * This was a generic row — an illustration, "Programme", "What's on, and when",
 * a chevron — which is a door with a sign on it. The programme is the one thing
 * on this app that answers a question a student has *while standing in the
 * hall*, and making them open a page to find out what is happening now put a tap
 * between them and the answer. So the card answers it, and opening the page is
 * for the rest of the running order.
 *
 * Three states, and they are the same three [ProgramScreen] shows, resolved by
 * the same [stepsAt] against the same clock — the two must never disagree about
 * what is on. Something is running; or nothing is and something is next; or the
 * Student Union has not published a running order, in which case this falls back
 * to being the door it used to be rather than showing an empty card.
 *
 * The tick is half a minute, not the countdown's second: nothing here changes
 * more finely than an entry starting.
 */
@Composable
private fun ProgramCard(
    program: List<ProgramEntry>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now by produceState(initialValue = System.currentTimeMillis(), program) {
        while (true) {
            value = System.currentTimeMillis()
            delay(30_000)
        }
    }
    val steps = program.stepsAt(nowMillis = now, fairEndMillis = FairSchedule.endMillis)
    val running = steps.running()
    val step = running ?: steps.upNext()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = CardRadius)
            .clip(RoundedCornerShape(CardRadius))
            .clickable(onClick = onClick)
            .padding(Dimens.CardPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.home_program),
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 12.sp,
                letterSpacing = 0.4.sp,
                color = Ink.Muted,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = Ink.Faint,
                modifier = Modifier.size(16.dp),
            )
        }

        if (step == null) {
            // Nothing published, or the fair's running order has finished. The
            // old hint line, which is still true and is all there is to say.
            Spacer(Modifier.height(Dimens.SpaceXs))
            Text(
                text = stringResource(R.string.home_program_hint),
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 15.sp,
                color = Color.White,
            )
            return@Column
        }

        Spacer(Modifier.height(Dimens.Space))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The same lit dot the route uses for the step it is standing on, so
            // the card and the diagram behind it are visibly about one row.
            if (running != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Palette.Accent),
                )
                Spacer(Modifier.size(Dimens.SpaceSm))
            }
            Text(
                text = stringResource(
                    if (running != null) R.string.program_now else R.string.program_next,
                ),
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 12.sp,
                color = if (running != null) Palette.Accent else Ink.Muted,
            )
            Spacer(Modifier.size(Dimens.SpaceSm))
            Text(
                text = step.entry.timeRange(),
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 12.sp,
                color = Ink.Muted,
            )
        }

        Spacer(Modifier.height(Dimens.SpaceXs))
        Text(
            text = step.entry.displayTitle(),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 20.sp,
            lineHeight = 1.25.em,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        step.entry.whereLine()?.let { where ->
            Spacer(Modifier.height(Dimens.SpaceXs))
            Text(
                text = where,
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 13.sp,
                color = Ink.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun HomeScreenPreview() {
    SUClubFairTheme {
        // The shell supplies the backdrop in the app; stand one in for the preview.
        Box {
            MeshBackground()
            HomeScreen()
        }
    }
}
