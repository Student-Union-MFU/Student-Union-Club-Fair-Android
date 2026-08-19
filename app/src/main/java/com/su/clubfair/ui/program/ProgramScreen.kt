package com.su.clubfair.ui.program

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.data.FairSchedule
import com.su.clubfair.ui.components.SectionLabel
import com.su.clubfair.ui.components.SheetHeader
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.model.ProgramEntry
import com.su.clubfair.ui.model.ProgramStatus
import com.su.clubfair.ui.model.ProgramStep
import com.su.clubfair.ui.model.displayDetail
import com.su.clubfair.ui.model.displayLocation
import com.su.clubfair.ui.model.displayTitle
import com.su.clubfair.ui.model.previewProgram
import com.su.clubfair.ui.model.running
import com.su.clubfair.ui.model.stepsAt
import com.su.clubfair.ui.model.upNext
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AppSans
import com.su.clubfair.ui.theme.AppTextWeight
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.Palette
import com.su.clubfair.ui.theme.SUClubFairTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

private val CardRadius = Dimens.RadiusLg

/**
 * How often the screen re-reads the clock.
 *
 * A minute is the resolution the running order is written at — nothing here is
 * timed more finely than a printed schedule — and it is what stops an entry
 * sitting on "happening now" for an hour after it ended while the student holds
 * the page open.
 */
private const val TickMillis = 30_000L

/**
 * What is on at the fair, and what is on **now**.
 *
 * Two readings of one list, in the order a student wants them. The card at the
 * top answers "what is happening this minute", which is the only question most
 * people open this page to ask; the route under it answers "and what else", and
 * is where someone planning their afternoon goes.
 *
 * The whole thing is `clubfair_program` — `GET /clubfair/program`, published
 * entries only, already ordered by `starts_at`. Nothing about the running order
 * is compiled into the app: the Student Union adds, moves and unpublishes rows,
 * and this follows without a release. That is the same bargain the booths and
 * the prize tiers are on.
 *
 * The one thing the app decides is where the clock falls, and the rule is in
 * [stepsAt]: an entry with no `ends_at` runs until the next one starts. See it
 * for why that beats inventing a duration.
 */
@Composable
fun ProgramScreen(
    program: List<ProgramEntry>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    // Re-read on a tick rather than once per composition. The page is one a
    // student leaves open on a table between events, which is exactly the case
    // where a status frozen at the moment the screen opened is wrong and looks
    // authoritative.
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(TickMillis)
        }
    }

    val steps = program.stepsAt(nowMillis = now, fairEndMillis = FairSchedule.endMillis)

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPadding),
    ) {
        Spacer(Modifier.height(Dimens.Space))
        SheetHeader(
            title = stringResource(R.string.program_title),
            onBack = onBack,
            backDescription = stringResource(R.string.profile_back),
        )

        Spacer(Modifier.height(Dimens.SpaceLg))

        if (steps.isEmpty()) {
            EmptyProgram()
        } else {
            NowCard(steps = steps)

            Spacer(Modifier.height(Dimens.SpaceXl))
            SectionLabel(stringResource(R.string.program_route_section))
            Spacer(Modifier.height(Dimens.SpaceLg))
            ProgramRoute(steps = steps)
        }

        Spacer(Modifier.height(Dimens.SpaceXl))
        Spacer(Modifier.height(Dimens.NavBarClearance))
    }
}

/**
 * The state this screen is in today.
 *
 * `clubfair_program` has no rows, so this is not a rare edge — it is what every
 * student sees until the Student Union fills the table in. It says the running
 * order is not published yet rather than that something went wrong, because
 * those are different facts and only one of them is true.
 */
@Composable
private fun EmptyProgram() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = CardRadius)
            .padding(Dimens.CardPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The tile's own illustration, dimmed — the mark the student just
        // tapped, rather than the announcements bell this used to borrow. A page
        // that opens onto a different symbol than the door into it reads as the
        // wrong page having loaded.
        Image(
            painter = painterResource(R.drawable.art_program),
            contentDescription = null,
            alpha = 0.35f,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(Dimens.Space))
        Text(
            text = stringResource(R.string.program_empty_title),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 16.sp,
            color = Color.White,
        )
        Spacer(Modifier.height(Dimens.SpaceXs))
        Text(
            text = stringResource(R.string.program_empty_body),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 13.sp,
            lineHeight = 1.45.em,
            color = Ink.Muted,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * What is on this minute, at the top where it is read first.
 *
 * Three states, and the third is the one worth being careful about. Something is
 * running; or nothing is and something is next; or the fair's running order is
 * finished. Falling back from "now" to "next" matters more than it looks: a
 * student opening this at 11:40 between two events is asking the same question
 * either way, and a card that says only "nothing is happening" has answered the
 * literal question and none of the real one.
 */
@Composable
private fun NowCard(steps: List<ProgramStep>) {
    val running = steps.running()
    val next = steps.upNext()
    val step = running ?: next

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = CardRadius)
            .padding(Dimens.CardPadding),
    ) {
        if (step == null) {
            // The finished state is the one a student is most likely to be
            // reading — the fair ends and this card stays on screen for the rest
            // of the evening — and it was a single sentence floating in a card
            // sized for four lines, which reads as a card that failed to fill
            // rather than as an answer.
            //
            // A ticked node and two lines give it the shape the other two states
            // have: a mark, a heading, and something underneath. The mark is the
            // route's own Done node — see [StepNode] — because that is what every
            // row below it is now wearing, and a different tick for the same fact
            // would be a second visual vocabulary on one screen.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        // The halo, not the filled node. At 40dp a solid accent
                        // disc is the brightest thing on the page and would make
                        // "nothing is on" the loudest statement on it; the ring
                        // the running step wears is the right weight for a
                        // heading mark.
                        .background(Palette.Accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = Palette.Accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(Dimens.Space))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.program_all_done),
                        modifier = Modifier.semantics { heading() },
                        fontFamily = AppSans,
                        fontWeight = AppTextWeight,
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(Dimens.SpaceXs))
                    Text(
                        text = stringResource(R.string.program_all_done_body),
                        fontFamily = AppSans,
                        fontWeight = AppTextWeight,
                        fontSize = 13.sp,
                        lineHeight = 1.45.em,
                        color = Ink.Muted,
                    )
                }
            }
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // The dot is the same mark the route below uses for the running
            // step, so the card and the diagram are visibly about the same row.
            if (running != null) LiveDot()
            if (running != null) Spacer(Modifier.width(Dimens.SpaceSm))
            Text(
                text = stringResource(
                    if (running != null) R.string.program_now else R.string.program_next,
                ),
                modifier = Modifier.semantics { heading() },
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 12.sp,
                letterSpacing = 0.4.sp,
                color = if (running != null) Palette.Accent else Ink.Muted,
            )
        }

        Spacer(Modifier.height(Dimens.SpaceSm))
        Text(
            text = step.entry.displayTitle(),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 22.sp,
            lineHeight = 1.25.em,
            color = Color.White,
        )

        Spacer(Modifier.height(Dimens.SpaceXs))
        Text(
            text = step.entry.timeRange(),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 14.sp,
            color = Palette.Accent,
        )

        step.entry.whereLine()?.let { where ->
            Spacer(Modifier.height(Dimens.SpaceXs))
            Text(
                text = where,
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 13.sp,
                color = Ink.Muted,
            )
        }

        step.entry.displayDetail()?.let { detail ->
            Spacer(Modifier.height(Dimens.Space))
            Text(
                text = detail,
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 13.sp,
                lineHeight = 1.45.em,
                color = Ink.Label,
            )
        }
    }
}

/**
 * The running order as a route: one rail, one node per entry, top to bottom.
 *
 * Vertical rather than the winding road `PrizeRoadmap` draws, and the difference
 * is what the two diagrams are of. The prize route is a *distance* — stops at
 * fifteen and twenty-eight booths, where the shape carries how far apart they
 * are. This is a *sequence*: the interesting thing about the third entry is that
 * it comes after the second, and a straight rail says that and nothing else.
 * Reading down also matches the times, which a student is scanning as a column
 * of clock faces.
 *
 * The rail is drawn per row rather than as one line behind the column, so a step
 * can be as tall as its own content without anything having to agree in advance
 * on how tall a row is. `IntrinsicSize.Min` is what lets the rail stretch to
 * whatever the card next to it turned out to be.
 */
@Composable
private fun ProgramRoute(steps: List<ProgramStep>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space),
    ) {
        steps.forEachIndexed { index, step ->
            StepRow(
                step = step,
                isFirst = index == 0,
                isLast = index == steps.lastIndex,
            )
        }
    }
}

/** Width of the rail column, and the line drawn down the middle of it. */
private val RailWidth = 28.dp
private val RailLine = 2.dp

/**
 * How far down the rail the node sits.
 *
 * Lined up with the first line of text in the card beside it rather than centred
 * on the card. A node centred vertically drifts away from its own title as soon
 * as one entry has a detail line and the next does not, and the thing a reader
 * is matching the dot to is the title.
 */
private val NodeInset = 14.dp

@Composable
private fun StepRow(step: ProgramStep, isFirst: Boolean, isLast: Boolean) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        StepRail(status = step.status, isFirst = isFirst, isLast = isLast)
        Spacer(Modifier.width(Dimens.SpaceSm))
        StepCard(step = step, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StepRail(status: ProgramStatus, isFirst: Boolean, isLast: Boolean) {
    // Walked road behind the node, road still to come in front of it — the same
    // reading the prize route uses, so "lit means done" means one thing in this
    // app rather than two.
    val behind = if (status == ProgramStatus.Upcoming) Ink.Faint else Palette.Accent
    val ahead = if (status == ProgramStatus.Done) Palette.Accent else Ink.Faint

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(RailWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(RailLine)
                .height(NodeInset)
                .background(if (isFirst) Color.Transparent else behind.copy(alpha = 0.45f)),
        )
        StepNode(status)
        Box(
            modifier = Modifier
                .width(RailLine)
                .weight(1f)
                .background(if (isLast) Color.Transparent else ahead.copy(alpha = 0.45f)),
        )
    }
}

private val NodeSize = 12.dp
private val RunningNodeSize = 16.dp
private val RunningHalo = 26.dp

/**
 * One stop on the route, in the three states the diagram has to tell apart at a
 * glance and without colour alone.
 *
 * Done is a solid tick, running is a filled dot inside a ring, upcoming is an
 * outline. Size and fill carry it as well as hue, which is what keeps the
 * sequence legible for a student who cannot separate the lime from the grey.
 */
@Composable
private fun StepNode(status: ProgramStatus) {
    Box(
        modifier = Modifier.size(RunningHalo),
        contentAlignment = Alignment.Center,
    ) {
        when (status) {
            ProgramStatus.Done -> Box(
                modifier = Modifier
                    .size(NodeSize + 6.dp)
                    .clip(CircleShape)
                    .background(Palette.Accent.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = Palette.Ink,
                    modifier = Modifier.size(11.dp),
                )
            }

            ProgramStatus.Running -> {
                Box(
                    modifier = Modifier
                        .size(RunningHalo)
                        .clip(CircleShape)
                        .background(Palette.Accent.copy(alpha = 0.16f)),
                )
                Box(
                    modifier = Modifier
                        .size(RunningNodeSize)
                        .clip(CircleShape)
                        .background(Palette.Accent),
                )
            }

            ProgramStatus.Upcoming -> Box(
                modifier = Modifier
                    .size(NodeSize)
                    .clip(CircleShape)
                    .border(RailLine, Ink.Faint, CircleShape),
            )
        }
    }
}

@Composable
private fun StepCard(step: ProgramStep, modifier: Modifier = Modifier) {
    val running = step.status == ProgramStatus.Running

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = Dimens.RadiusMd)
            .then(
                // The running step gets an outline rather than a different fill.
                // A second glass tone beside the first reads as two materials;
                // one hairline in the accent reads as emphasis.
                if (running) {
                    Modifier.border(
                        1.dp,
                        Palette.Accent.copy(alpha = 0.55f),
                        RoundedCornerShape(Dimens.RadiusMd),
                    )
                } else {
                    Modifier
                },
            )
            .padding(Dimens.Space),
    ) {
        Text(
            text = step.entry.timeRange(),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 12.sp,
            color = when (step.status) {
                ProgramStatus.Running -> Palette.Accent
                ProgramStatus.Done -> Ink.Faint
                ProgramStatus.Upcoming -> Ink.Muted
            },
        )
        Spacer(Modifier.height(Dimens.SpaceXs / 2))
        Text(
            text = step.entry.displayTitle(),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 15.sp,
            lineHeight = 1.3.em,
            // A finished entry stays legible rather than going to a whisper: the
            // student may be checking what they missed, which is a real reason to
            // read it.
            color = if (step.status == ProgramStatus.Done) Ink.Label else Color.White,
        )

        step.entry.whereLine()?.let { where ->
            Spacer(Modifier.height(Dimens.SpaceXs))
            Text(
                text = where,
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 12.sp,
                color = Ink.Muted,
            )
        }

        step.entry.displayDetail()?.let { detail ->
            Spacer(Modifier.height(Dimens.SpaceXs))
            Text(
                text = detail,
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 12.sp,
                lineHeight = 1.45.em,
                color = Ink.Muted,
            )
        }
    }
}

/** The filled dot that marks the running entry, at the size the card wants it. */
@Composable
private fun LiveDot() {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Palette.Accent),
    )
}

/**
 * Which part of the fair an entry happens in — one thing, never two.
 *
 * `internal` because Home's programme card renders the same entry and must say
 * it the same way — two spellings of the same place one tap apart is how the two
 * screens drift.
 *
 * **The venue is not in this.** It used to read "Zone B · MFU", and the second
 * half was true of every entry in the running order and of the whole fair: it is
 * all at Mae Fah Luang. A line that says the same thing on every row is not
 * information, it is furniture, and here it was crowding the one part that does
 * vary.
 *
 * So the zone wins when there is one — the letter is painted on the signage in
 * the hall, which makes it the thing a student can actually match against, the
 * same rule the booths page follows. [displayLocation] is the fallback for the
 * entries that do not happen on the floor at all: an opening on the main stage,
 * a draw at the Student Union desk. Those are places too, just not lettered
 * ones, and naming them is the whole reason that column is free text.
 */
@Composable
internal fun ProgramEntry.whereLine(): String? {
    zoneCode?.takeIf { it.isNotBlank() }
        ?.let { return stringResource(R.string.program_zone, it) }
    return displayLocation()
}

/**
 * "09:00 – 10:30", or "16:00" for an entry with no end.
 *
 * Fixed to Bangkok, for the reason `FairSchedule` is: the fair runs on campus
 * time whatever a visiting student's phone is set to, and an entry that reads
 * 05:00 because someone landed with their phone still on London time is worse
 * than useless at a fair they are standing in.
 *
 * The *format* still follows the reader's locale — `HH:mm` against the current
 * `Locale` — so the clock is the campus's and the numerals are the reader's.
 */
@Composable
internal fun ProgramEntry.timeRange(): String {
    // From the configuration rather than `Locale.getDefault()`, which is the
    // process default and does not follow this app's in-composition language
    // override — `ProvideAppLanguage` swaps `LocalConfiguration`, so a student
    // who picks English in Settings would otherwise keep the phone's numerals
    // here while every string around them changed. Lint flags the static read
    // for exactly this reason.
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) {
        SimpleDateFormat("HH:mm", locale)
            .apply { timeZone = TimeZone.getTimeZone(CampusZone) }
    }
    val start = formatter.format(Date(startsAtMillis))
    val end = endsAtMillis?.let { formatter.format(Date(it)) }
    return if (end == null) start else stringResource(R.string.program_time_range, start, end)
}

private const val CampusZone = "Asia/Bangkok"

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun ProgramScreenPreview() {
    SUClubFairTheme {
        Box(Modifier.fillMaxSize()) {
            MeshBackground()
            ProgramScreen(program = previewProgram(System.currentTimeMillis()))
        }
    }
}

/** What every student sees until `clubfair_program` has rows in it. */
@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun ProgramEmptyPreview() {
    SUClubFairTheme {
        Box(Modifier.fillMaxSize()) {
            MeshBackground()
            ProgramScreen(program = emptyList())
        }
    }
}
