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
import android.text.format.DateUtils
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AlanSans
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

            // Supporting text under the name, not a headline. It used to be
            // the biggest thing on the page, which put the emphasis on a line
            // of copy instead of on whose page this is.
            Spacer(Modifier.height(Dimens.SpaceSm))
            Text(
                text = stringResource(R.string.home_headline),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 1.3.em,
                color = Ink.Muted,
            )

            // Said once, at the top, and only when it is true. A stale-data notice
            // repeated per card would be four times the noise for one fact.
            if (offline) {
                Spacer(Modifier.height(Dimens.Space))
                OfflineNotice()
            }

            // Which of these two leads depends on what is actually happening.
            //
            // Before the fair opens there is no progress to report — the grid is
            // 28 empty cells on every phone in the university — and the one live
            // fact on the page is that it opens in ten days. Once it is running
            // that reverses: the countdown becomes a footnote and how many
            // booths you have collected is the reason you opened the app.
            //
            // The alternative was a fixed order, which meant one of the two was
            // always in the wrong place for half the fortnight.
            val opensLater = fairStatus() is FairStatus.BeforeStart

            Spacer(Modifier.height(Dimens.SpaceLg))
            if (opensLater) {
                FairStatusBanner(hero = true)
                Spacer(Modifier.height(Dimens.Space))
                CheckpointsCard(progress = progress)
            } else {
                CheckpointsCard(progress = progress)
                Spacer(Modifier.height(Dimens.Space))
                FairStatusBanner()
            }

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
    val emphasis = SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)

    return buildAnnotatedString {
        pushStyle(SpanStyle(fontWeight = FontWeight.Normal, color = Ink.Muted))
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
            fontFamily = AlanSans,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = Ink.Muted,
        )
    }
}

/** What the banner says: the big figure, and the line that frames it. */
private data class Countdown(val value: AnnotatedString, val label: String)

/**
 * When the fair opens, and how far off that is.
 *
 * One figure, not two. It carried the prize count in its right half for a
 * version, and that was a second thing competing with the only one on this page
 * that changes on its own — the prize total is already a tile directly below and
 * a whole page behind it, so it was being said three times. This says one thing.
 *
 * **Days, hours and minutes, with the last two carrying the weight** — see
 * [remainingTime]. The days are the part a student already knows, because the
 * date is on the line above; the minutes are the part they cannot work out for
 * themselves, and they are what makes this the one figure on Home that moves
 * while you are looking at it.
 *
 * The opening date is on the label rather than left implicit. A countdown alone
 * makes a student do the arithmetic to get to a date they can put in a calendar,
 * and it is formatted through `DateUtils`, so it follows the app's language
 * setting along with everything else.
 */
@Composable
private fun FairStatusBanner(
    modifier: Modifier = Modifier,
    /**
     * Whether this is the card the page is about.
     *
     * The only difference is emphasis — taller, and the app's lime on the figure
     * and the border. It is deliberately not a *different card*: a student who
     * scans their first booth on the morning of the 22nd should see this move
     * down the page, not turn into something they have to re-learn.
     */
    hero: Boolean = false,
) {
    val figureSize = if (hero) 40.sp else 30.sp
    val countdown = fairCountdown(figureSize)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = Dimens.RadiusLg)
            .height(if (hero) 128.dp else 96.dp)
            .padding(start = Dimens.CardPadding, end = Dimens.Space),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = countdown.label,
                fontFamily = AlanSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 1.1.em,
                color = Ink.Muted,
                maxLines = 1,
            )
            Spacer(Modifier.height(Dimens.SpaceXs))
            Text(
                text = countdown.value,
                fontFamily = AlanSans,
                fontWeight = FontWeight.Bold,
                fontSize = figureSize,
                lineHeight = 1.05.em,
                // White, like every other figure on the page.
                //
                // It was set in the accent, along with a lime hairline round
                // the card, to give Home some colour before anything is
                // scanned. It gave it too much: the figure is the largest
                // thing on the screen, so putting the app's one action colour
                // in it made a number look like a control, and it was the only
                // lime text anywhere. The lime is spent on the illustrations
                // now — small, on three cards, next to white type, which is
                // the same division the booth tiles use.
                color = Color.White,
                maxLines = 1,
            )
        }

        // Only while it is the card the page is about. On the small version it
        // would be a picture squeezed beside a figure, and the figure is the
        // only reason that version is on the page at all.
        if (hero) {
            Image(
                painter = painterResource(R.drawable.art_countdown),
                contentDescription = null,
                modifier = Modifier.size(84.dp),
            )
        }
    }
}

/** The fair's phase, polled on the same half-minute as the countdown. */
@Composable
private fun fairStatus(): FairStatus {
    val status by produceState<FairStatus>(
        initialValue = FairSchedule.statusAt(System.currentTimeMillis()),
    ) {
        while (true) {
            value = FairSchedule.statusAt(System.currentTimeMillis())
            delay(30_000)
        }
    }
    return status
}

/**
 * How long until the fair opens, recomputed on a timer.
 *
 * This was the string `"4h"` under the word "Ends in" — a constant, so it read
 * "4h" at nine in the morning and "4h" ten minutes before the fair closed. It is
 * the one figure on this screen a phone can work out exactly, which made
 * hardcoding it the least defensible thing on it.
 *
 * The poll is a half minute, which is finer than the unit shown for all but the
 * last hour, and deliberately so: it is what makes the banner change *while a
 * student is looking at it* rather than the next time they cold-start the app.
 * [produceState] ties the loop to this composable, so leaving Home stops the
 * clock.
 */
@Composable
private fun fairCountdown(big: TextUnit): Countdown {
    val status by produceState<FairStatus>(
        initialValue = FairSchedule.statusAt(System.currentTimeMillis()),
    ) {
        while (true) {
            value = FairSchedule.statusAt(System.currentTimeMillis())
            delay(30_000)
        }
    }

    return when (val current = status) {
        is FairStatus.BeforeStart -> Countdown(
            value = remainingTime(current.untilStartMillis, big),
            label = stringResource(R.string.home_opens_on, fairOpeningDate()),
        )

        is FairStatus.Running -> Countdown(
            value = remainingTime(current.remainingMillis, big),
            label = stringResource(R.string.home_open_ends_in),
        )

        FairStatus.Ended -> Countdown(
            value = AnnotatedString(stringResource(R.string.home_stat_ended_value)),
            label = stringResource(R.string.home_stat_ended),
        )
    }
}

/**
 * The opening date, in the reader's language.
 *
 * `DateUtils` rather than a hand-built pattern: "22 Aug" and "22 ส.ค." are not
 * the same string with a different month in it, and the ordering differs by
 * locale even where the words do not. The context is the one
 * `ProvideAppLanguage` provides, so this follows the in-app setting rather than
 * the phone's.
 */
@Composable
private fun fairOpeningDate(): String = DateUtils.formatDateTime(
    LocalContext.current,
    FairSchedule.startMillis,
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_YEAR,
)

/**
 * Days, hours and minutes — with the hours and minutes carrying the weight.
 *
 * It showed one unit for a while, the coarsest that still fit: "10 days" at ten
 * days out, hours on the final day, minutes in the last hour. The argument was
 * that nobody plans around minute twelve of day ten. True, and beside the point
 * — a countdown that moves once a day is a date with extra steps, and the whole
 * reason this figure is on the screen rather than the date alone is that it is
 * the one thing on Home that changes while you watch it.
 *
 * So all three, and the emphasis inverted from the obvious: the hours and
 * minutes are the figure, and the days trail behind them at half size in muted
 * ink. The days are the part a student already knows — the date is on the line
 * above — and the minutes are the part they cannot work out for themselves, so
 * the minutes get the start of the line and the days get the end of it.
 *
 * Sized off the caller's figure rather than a constant, so the hero card's 44sp
 * and the plain one's 34sp both get a half-size day count without a second
 * number to keep in step.
 */
@Composable
private fun remainingTime(millis: Long, big: TextUnit): AnnotatedString {
    val total = (millis / 60_000L).coerceAtLeast(0L)
    val days = total / (60 * 24)
    val hours = (total / 60) % 24
    val minutes = total % 60

    // Resolved out here: buildAnnotatedString's lambda is not composable.
    val dayText = stringResource(R.string.home_unit_days, days.toInt())
    val hourText = stringResource(R.string.home_unit_hours, hours.toInt())
    val minuteText = stringResource(R.string.home_unit_minutes, minutes.toInt())

    return buildAnnotatedString {
        append(hourText)
        append(" ")
        append(minuteText)
        // Days last, after the figure rather than in front of it. Leading with
        // them put the smallest, faintest thing on the line where the eye lands
        // first and made the reader step over it to reach the number they came
        // for. Trailing, it reads as the footnote it is.
        if (days > 0) {
            append("  ")
            withStyle(
                SpanStyle(
                    fontSize = big * 0.5f,
                    fontWeight = FontWeight.Medium,
                    color = Ink.Muted,
                ),
            ) {
                append(dayText)
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
        fontFamily = AlanSans,
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
            fontFamily = AlanSans,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = Ink.Label,
        )

        Spacer(Modifier.height(Dimens.SpaceXs))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.home_checkpoints_count, visited),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 1.em,
                color = Color.White,
            )
            Spacer(Modifier.size(Dimens.SpaceSm))
            Text(
                text = pluralStringResource(R.plurals.home_checkpoints_total, total, total),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = Ink.Muted,
                modifier = Modifier.padding(bottom = Dimens.SpaceXs),
            )
        }

        Spacer(Modifier.height(12.dp))
        ProgressTrack(fraction = fraction)

        Spacer(Modifier.height(Dimens.SpaceSm))
        Text(
            // Truncates rather than rounds, so the number never claims progress
            // that hasn't been earned — 26/27 must not read as "100 % complete".
            text = stringResource(R.string.home_checkpoints_percent, (fraction * 100).toInt()),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            color = Ink.Muted,
            modifier = Modifier.align(Alignment.End),
        )

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
            fontFamily = AlanSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Ink.Label,
        )
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
