package com.su.clubfair.ui.welcome

import androidx.compose.animation.core.Animatable
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AppSans
import com.su.clubfair.ui.theme.AppTextWeight
import com.su.clubfair.ui.theme.Bitcount
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.Palette
import com.su.clubfair.data.FairSchedule
import com.su.clubfair.ui.theme.SUClubFairTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier,
    onGetStarted: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        MeshBackground()
        HalftoneArt()

        // A glow under the mark, not behind the whole screen.
        //
        // The page was a flat field with three objects floating in it, and the
        // flatness was the complaint — not the amount of content. A wide, very
        // soft accent bloom gives the wordmark something to sit on and puts the
        // app's one colour on a screen that otherwise has it only on a 66dp
        // knob. It is placed by fraction rather than centred so it sits under
        // the mark rather than under the middle of the phone.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Palette.Accent.copy(alpha = 0.13f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.32f, size.height * 0.58f),
                            radius = size.minDimension * 0.95f,
                        ),
                        radius = size.minDimension * 0.95f,
                        center = Offset(size.width * 0.32f, size.height * 0.58f),
                    )
                },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                // Sixteen, not the app's twenty-four. This is the one screen
                // whose subject is the wordmark itself, and a mark that keeps a
                // polite margin reads as a heading; letting it run closer to the
                // glass is what makes it a poster. The control below keeps its
                // own inset by sitting in a track.
                .padding(horizontal = 16.dp, vertical = 20.dp),
        ) {
            // Pushes the mark down to roughly the lower half of the screen.
            Spacer(Modifier.weight(1f))

            // Auto-sizes down on narrow screens so the wordmark never clips.
            BasicText(
                text = stringResource(R.string.welcome_title),
                // A smaller optical shift than the 40dp this carried under the
                // subtitle. Half of that gap was Alan Sans' ascent-to-cap space
                // at the top of the line under it; images have no such thing, so
                // only Bitcount's own descent is left to close.
                modifier = Modifier.offset(y = 20.dp),
                style = TextStyle(
                    fontFamily = Bitcount,
                    fontWeight = AppTextWeight,
                    // A vertical fade rather than flat white. The mark is drawn
                    // in dots, and a thousand identical white dots read as a
                    // texture; letting the bottom rows sit back a little gives
                    // the two words depth without any of them changing colour
                    // enough to look like a different ink.
                    brush = Brush.verticalGradient(
                        listOf(Color.White, Color.White.copy(alpha = 0.72f)),
                    ),
                    // Bitcount's cap height is 0.6em inside a 1.2em natural line
                    // box (0.84 up, 0.36 down). Stacked, that box is mostly air:
                    // at 0.78em the baselines close up to a gap of about 0.18em
                    // between "Club" and "Fair", which is what makes the two
                    // words read as one mark rather than two words that happen to
                    // be above each other. It also cuts the unused descent under
                    // the second line before the optical shift above has to.
                    lineHeight = 0.78.em,
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.None,
                    ),
                ),
                // Two lines and no explicit break: autoSize grows the type until
                // the *longest word* fills the column, and "Club Fair" has only
                // one place it can break, so one word per line falls out of the
                // measurement rather than being hard-coded into the string. A
                // wider screen is free to set it on one line instead.
                maxLines = 2,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 56.sp,
                    maxFontSize = 160.sp,
                    stepSize = 1.sp,
                ),
            )
            // The logos sit under the wordmark rather than above it, and the
            // tagline they replaced is gone.
            //
            // "Clubs, booths & a weekend to explore" was the largest thing on
            // the screen after the mark itself, and it was describing the app to
            // someone who had already opened it. What it took up is worth more
            // as the answer to whose event this is — which is a question a
            // student standing in a hall genuinely asks, and which two marks
            // answer without a sentence.
            //
            // Under, not over: reading down, the wordmark is the name and the
            // logos are the signature beneath it. Above the mark they were a
            // letterhead, which is what put them at the top in the first place —
            // that was right when a paragraph followed and wrong now that
            // nothing does.
            // Tight to the mark. These are a signature under a name, and a
            // signature that drifts away from what it signs stops reading as
            // one — most of what looks like the gap here is Bitcount's descent
            // anyway, which the 20dp shift above is already eating into.
            Spacer(Modifier.height(Dimens.SpaceSm))
            LogoRow(Modifier.fillMaxWidth())

            // When it is. The block was a name and two crests — who the event
            // belongs to, and nothing about the event — and this is the one fact
            // a student opening the app on the bus actually wants. It is not the
            // tagline that used to live here: a tagline described the app to
            // someone already holding it, where a date answers a question.
            //
            // Read off `FairSchedule` rather than written into a string, so the
            // screen cannot disagree with the countdown on Home. `DateUtils`
            // formats the range, because "22–23 Aug" and "22–23 ส.ค." are not one
            // string with the month swapped.
            Spacer(Modifier.height(Dimens.SpaceLg))
            FairDetails()

            Spacer(Modifier.height(Dimens.SpaceXl))
            DragToContinue(onComplete = onGetStarted)

            // Kept even with nothing under it now: the track would otherwise sit
            // flush against the gesture bar, and a control that close to the
            // system inset is one the thumb catches on the way past.
            Spacer(Modifier.weight(0.08f))
        }
    }
}

/**
 * The entry CTA: a lime knob you drag along a glass track to leave the screen.
 *
 * A tap would do the same job in less effort, which is the point — this is the
 * one gate into the app, and a gesture that takes a deliberate second is worth
 * more here than a control the thumb can hit by accident on a phone coming out
 * of a pocket. Everything past this screen is an ordinary button.
 *
 * Past [COMMIT_FRACTION] of the track the knob finishes the trip on its own;
 * short of it, it springs back. A drag that has to land exactly at the far end
 * turns a gesture into a target.
 */
@Composable
private fun DragToContinue(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val knobOffset = remember { Animatable(0f) }
    // Set on first layout: the travel is the track's width less the knob and the
    // padding either side of it, so the knob stops flush inside the far end.
    var travelPx by remember { mutableFloatStateOf(0f) }
    // Latched so a second drag can't fire navigation twice on the way out.
    var committed by remember { mutableStateOf(false) }

    val progress = if (travelPx > 0f) knobOffset.value / travelPx else 0f
    val hintLabel = stringResource(R.string.welcome_drag_hint)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TrackHeight)
            .glassSurface(CircleShape)
            .onSizeChanged { size ->
                travelPx = size.width - with(density) { (KnobSize + TrackInset * 2).toPx() }
            }
            // Drag is a gesture TalkBack can't perform and a test can't express.
            // A semantics action is reachable by both without putting a tap
            // gesture on the track itself, which would undo the deliberateness.
            .semantics {
                onClick(label = hintLabel) {
                    if (!committed) { committed = true; onComplete() }
                    true
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(R.string.welcome_drag_hint),
            modifier = Modifier
                .align(Alignment.Center)
                // Gone by the time the knob is a third of the way across: the
                // hint has done its job the moment the gesture starts, and text
                // sliding out from under a moving knob reads as a glitch.
                .graphicsLayer { alpha = (1f - progress * 3f).coerceIn(0f, 1f) },
            color = Ink.Label,
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 18.sp,
            // Wider than the app sets anything else. At a word this short, on a
            // control this wide, tight tracking reads as a label dropped in the
            // middle of a pill; letting it breathe makes it look placed.
            letterSpacing = 1.2.sp,
        )
        Box(
            modifier = Modifier
                .padding(TrackInset)
                .offset { IntOffset(knobOffset.value.roundToInt(), 0) }
                .size(KnobSize)
                .background(Palette.Accent, CircleShape)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            knobOffset.snapTo((knobOffset.value + delta).coerceIn(0f, travelPx))
                        }
                    },
                    onDragStopped = {
                        scope.launch {
                            // Read straight off the Animatable rather than the
                            // captured `progress`: this lambda outlives the
                            // composition that made it, and a stale fraction here
                            // is the difference between committing and springing
                            // back.
                            if (travelPx > 0f && knobOffset.value >= travelPx * COMMIT_FRACTION) {
                                knobOffset.animateTo(travelPx)
                                if (!committed) { committed = true; onComplete() }
                            } else {
                                knobOffset.animateTo(0f)
                            }
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = Palette.Ink,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

private val TrackHeight = 76.dp
private val KnobSize = 66.dp
private val TrackInset = 5.dp
private const val COMMIT_FRACTION = 0.55f

/**
 * The dithered cherry pieces from the su-clubfair website, behind everything.
 *
 * Same drawings, same generator: `lib/halftone.ts` over there resolves a
 * botanical field through an ordered dither into a grid of marks, and the vector
 * drawables here are its output rather than a redrawing of it. That matters more
 * than it sounds — the wordmark on this screen *is* a dot matrix, and until now
 * it was the only dot-matrix object in the app, which made it read as a font
 * someone picked rather than as the product's own language. Now the art agrees
 * with it.
 *
 * The bough bleeds off the top-right corner because that is the one piece drawn
 * to start outside its frame; the blossom sits bottom-left, well clear of the
 * drag track, where it fills the corner the composition leaves empty.
 *
 * Alpha lives here rather than in the drawables. Tone in a dither is carried by
 * the density of marks — every mark is fully present or fully absent — so the
 * opacity of the whole layer is a placement decision, not part of the drawing.
 */
@Composable
private fun HalftoneArt(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        // One piece, large, in the top-right corner. Nothing else.
        //
        // There were four — a bough here, a mirrored spray down the left side and
        // a blossom in each bottom corner — and four is a pattern rather than a
        // picture: the eye stops reading any of them and starts reading texture,
        // which is exactly what the wordmark is already made of. One drawing at
        // size keeps it an image, and leaves the lower half of the screen for the
        // mark, the logos and the control that actually live there.
        //
        // The bough is the right one to keep. It is the only piece drawn with its
        // trunk outside its own frame, so it can arrive from off the corner
        // instead of sitting inside the page like something that was placed.
        Image(
            painter = painterResource(R.drawable.art_halftone_bough),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alpha = 0.34f,
            modifier = Modifier
                .align(Alignment.TopEnd)
                // Wider than the screen and pushed off both edges it touches.
                // Cropping by the frame rather than by a `clip`, so the branch
                // is cut where the phone ends and not where a box does.
                .fillMaxWidth(1.45f)
                .offset(x = 130.dp, y = (-150).dp),
        )
    }
}

/**
 * Where and when, under the logos.
 *
 * The dates lead in white at the size of a statement; the hall sits under them in
 * muted ink. Both are read rather than written: the window comes from
 * `FairSchedule`, which the server feeds, so this cannot drift from the countdown
 * on Home or from what the Student Union last set.
 */
@Composable
private fun FairDetails(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.welcome_when, fairDateRange(), fairTimeRange()),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 18.sp,
            letterSpacing = 0.3.sp,
            color = Color.White,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = fairVenue(),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 13.sp,
            lineHeight = 1.35.em,
            color = Ink.Muted,
        )
    }
}

/**
 * The hall, preferring whatever su-server has been told.
 *
 * The built-in string is the fallback, not the answer. A venue that moves — and
 * one moved once already while this app was being written — is a row the Student
 * Union can edit, where the resource here can only change in a release that
 * students then have to install.
 *
 * Language is chosen the way `Booth.displayName` chooses it, off the resource
 * rather than a `Locale` comparison, so it follows the app's own setting instead
 * of the phone's.
 */
@Composable
private fun fairVenue(): String {
    val thai = booleanResource(R.bool.prefer_thai_names)
    val fromServer = if (thai) {
        FairSchedule.venue ?: FairSchedule.venueEn
    } else {
        FairSchedule.venueEn ?: FairSchedule.venue
    }
    return fromServer ?: stringResource(R.string.welcome_venue)
}

/**
 * The fair's date, in the reader's language.
 *
 * A range rather than a single instant even though the fair is one evening:
 * `formatDateRange` collapses a same-day span to "22 Aug" on its own, and it is
 * the call that keeps working on its own the day the fair runs two days again.
 */
@Composable
private fun fairDateRange(): String = DateUtils.formatDateRange(
    LocalContext.current,
    FairSchedule.startMillis,
    FairSchedule.endMillis,
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_YEAR,
)

/**
 * The hours, "16:00 – 21:30".
 *
 * Showable at all only because the fair is a single evening — the window in
 * `FairSchedule` *is* the opening hours now, where a two-day range would have
 * printed the moment it opened and the moment it closed two days later and
 * called that a time. Twelve- or twenty-four-hour is `DateUtils`' decision, from
 * the reader's own setting.
 */
@Composable
private fun fairTimeRange(): String = DateUtils.formatDateRange(
    LocalContext.current,
    FairSchedule.startMillis,
    FairSchedule.endMillis,
    DateUtils.FORMAT_SHOW_TIME,
)

@Composable
private fun LogoRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        // Left, on the wordmark's own edge. Centred, the pair floated in the
        // middle of a screen whose every other element — the mark, the track
        // under it — starts at the same left margin, and the row was the one
        // thing not on that line.
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Up from 52/34. Those sizes were set when this row sat alone at the top
        // of the screen with nothing to be measured against; under a wordmark
        // that auto-sizes to the full column width they read as a footnote to
        // it. The ratio between the two marks is unchanged — the university's
        // emblem is taller than the union's lockup by design.
        Image(
            painter = painterResource(R.drawable.logo_mfu),
            contentDescription = stringResource(R.string.logo_mfu),
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(76.dp),
        )
        Image(
            painter = painterResource(R.drawable.logo_su),
            contentDescription = stringResource(R.string.logo_su),
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(50.dp),
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun WelcomeScreenPreview() {
    SUClubFairTheme {
        WelcomeScreen()
    }
}
