package com.su.clubfair.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.components.GlassIconButton
import com.su.clubfair.ui.components.ProgressTrack
import com.su.clubfair.ui.components.StatEntry
import com.su.clubfair.ui.components.StatPane
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.model.PlaceholderStudent
import com.su.clubfair.ui.model.Student
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.LocalAccent
import com.su.clubfair.ui.theme.SUClubFairTheme

private val CardRadius = Dimens.RadiusLg

/** Booths per row in the checkpoint grid; 27 booths lands on exactly three rows. */
private const val GridColumns = 9



@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    student: Student = PlaceholderStudent,
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
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
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

            Spacer(Modifier.height(Dimens.SpaceLg))
            CheckpointsCard(visited = student.visited, total = student.total)

            Spacer(Modifier.height(Dimens.Space))
            StatPane(
                entries = listOf(
                    StatEntry("#${student.rank}", stringResource(R.string.home_stat_rank)),
                    StatEntry("${student.prizes}", stringResource(R.string.home_stat_prizes)),
                    StatEntry(
                        stringResource(R.string.home_stat_left_value),
                        stringResource(R.string.home_stat_left),
                    ),
                ),
            )

            Spacer(Modifier.height(Dimens.Space))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space)) {
                ShortcutTile(
                    // Matches the Booths tab in the nav bar, since they lead to
                    // the same place. Not a group-of-people glyph: it meant the
                    // right thing but was too close to the Profile icon to tell
                    // apart at a glance.
                    icon = R.drawable.ic_shapes,
                    label = R.string.home_clubs,
                    onClick = onOpenClubs,
                    modifier = Modifier.weight(1f),
                )
                ShortcutTile(
                    icon = R.drawable.ic_gift,
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
    }
}

/**
 * "Welcome back, <name>" as one line, with the name carrying the weight.
 *
 * One line rather than two, and so the emphasis has to come from weight and
 * colour instead of size: the name is bold white against a normal-weight muted
 * greeting at the same size. Stacked, the two could be far apart in size; side by
 * side that much difference would put their baselines visibly out of sympathy.
 *
 * Built by splitting the template on its placeholder rather than appending the
 * name to a prefix, so a translation is free to put the name first — which
 * several languages do — without the styling landing on the wrong half.
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
            append(' ')
            withStyle(emphasis) { append(name) }
        } else {
            append(template.substring(0, at))
            withStyle(emphasis) { append(name) }
            append(template.substring(at + NamePlaceholder.length))
        }
    }
}

/**
 * Stands in for the name while the template is formatted, so the surrounding copy
 * can be recovered either side of it. Anything a name can't contain will do.
 */
private const val NamePlaceholder = "\u0000"

/** The greeting, as the page's headline. */
@Composable
private fun HomeHeader(name: String, modifier: Modifier = Modifier) {
    Text(
        text = greetingWith(name),
        modifier = modifier,
        fontFamily = AlanSans,
        fontSize = 32.sp,
        lineHeight = 1.15.em,
    )
}

@Composable
private fun CheckpointsCard(
    visited: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val fraction = if (total > 0) visited.toFloat() / total else 0f

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
        CheckpointGrid(visited = visited, total = total)
    }
}

/** One cell per booth: filled once scanned, hollow until then. */
@Composable
private fun CheckpointGrid(
    visited: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val rows = (total + GridColumns - 1) / GridColumns

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        repeat(rows) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                repeat(GridColumns) { column ->
                    val index = row * GridColumns + column
                    if (index < total) {
                        CheckpointCell(
                            scanned = index < visited,
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
    val shape = RoundedCornerShape(Dimens.SpaceXs + 2.dp)
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
                    Modifier
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), shape)
                },
            ),
    )
}

@Composable
private fun ShortcutTile(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .glassSurface(cornerRadius = CardRadius)
            .clip(RoundedCornerShape(CardRadius))
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.SpaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.height(Dimens.SpaceSm))
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
