package com.su.clubfair.ui.welcome

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Bitcount
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.Palette
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            // Pushes the copy block down to roughly the lower half of the screen.
            Spacer(Modifier.weight(1f))

            // Auto-sizes down on narrow screens so the wordmark never clips.
            BasicText(
                text = stringResource(R.string.welcome_title),
                // Drawn lower than it is laid out, which closes the gap to the
                // subtitle without moving the subtitle (and the control under it)
                // up. What is left between the two blocks after `lineHeight` has
                // been squeezed is not leading at all — it is Bitcount's descent
                // below its baseline plus Alan Sans' ascent-to-cap gap, and no
                // `Trim` reaches either. An optical shift is the only tool.
                modifier = Modifier.offset(y = 40.dp),
                style = TextStyle(
                    fontFamily = Bitcount,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
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
            // Grows until the longest line fills the content width. lineHeight is
            // in em so the leading tracks whatever size autoSize settles on.
            BasicText(
                text = stringResource(R.string.welcome_subtitle),
                style = TextStyle(
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 1.13.em,
                    // Drop the leading above the first line only. Between lines
                    // the 1.13em is doing real work; above the first it is just
                    // more distance from the wordmark this block sits under.
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Proportional,
                        trim = LineHeightStyle.Trim.FirstLineTop,
                    ),
                ),
                maxLines = 3,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 36.sp,
                    maxFontSize = 64.sp,
                    stepSize = 0.5.sp,
                ),
            )

            Spacer(Modifier.height(Dimens.SpaceXl))
            DragToContinue(onComplete = onGetStarted)

            Spacer(Modifier.weight(0.08f))
            LogoRow(Modifier.fillMaxWidth())
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
            fontFamily = AlanSans,
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
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

@Composable
private fun LogoRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_mfu),
            contentDescription = stringResource(R.string.logo_mfu),
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(52.dp),
        )
        Image(
            painter = painterResource(R.drawable.logo_su),
            contentDescription = stringResource(R.string.logo_su),
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(34.dp),
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
