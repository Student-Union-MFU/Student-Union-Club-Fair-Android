package com.su.clubfair.ui.qr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.ByteMatrix
import com.google.zxing.qrcode.encoder.Encoder
import com.su.clubfair.R
import com.su.clubfair.ui.components.PillButton
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.model.PreviewStudent
import com.su.clubfair.ui.model.Student
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.Palette
import com.su.clubfair.ui.theme.SUClubFairTheme

/**
 * Ink for the code — see [StyledQr] for why it isn't the other way round.
 *
 * Neutral, and the one colour in the app that is **not** taken from the
 * scheme. This is the surface a stranger's phone has to decode in a queue, and
 * the failure documented on [StyledQr] was found by decoding a screenshot, not
 * by looking at one. A green-black would almost certainly be fine; "almost
 * certainly" is the wrong standard for the screen that lets a student collect a
 * stamp. The panel behind it takes [Palette.Paper], which is where the theming
 * is free.
 */
private val QrInk = Color(0xFF10141A)
private val TicketRadius = Dimens.RadiusLg
private val NotchRadius = 14.dp

/** Where the perforation sits, as a fraction of the ticket's height. */
private const val StubLine = 0.80f

/**
 * The student's pass — their own ID as a QR code. Display only; nothing here
 * scans, so no camera permission and no `CAMERA` entry in the manifest.
 */
@Composable
fun QrTicketScreen(
    modifier: Modifier = Modifier,
    student: Student = PreviewStudent,
    onClose: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        // This one keeps its own backdrop: it's a sheet over the shell, not a
        // tab, so it isn't standing on the shell's copy.
        MeshBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = Dimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Ticket(student = student)
            Spacer(Modifier.height(Dimens.Space))
            Text(
                text = stringResource(R.string.qr_hint),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = Ink.Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            PillButton(text = stringResource(R.string.scan_close), onClick = onClose)
            Spacer(Modifier.height(Dimens.SpaceLg))
        }
    }
}

@Composable
private fun Ticket(student: Student, modifier: Modifier = Modifier) {
    val shape = remember { TicketShape(TicketRadius, NotchRadius, StubLine) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(shape = shape)
            .padding(horizontal = Dimens.SpaceLg),
    ) {
        Spacer(Modifier.height(Dimens.SpaceLg))
        Text(
            text = stringResource(R.string.qr_title),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color.White,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(Dimens.SpaceXs))
        Text(
            text = stringResource(R.string.qr_subtitle),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = Ink.Muted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(Dimens.SpaceLg))
        // The QR sits on a light panel, like the paper ticket it's imitating.
        // That is not just styling — see StyledQr for why it has to be this way
        // round.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(Dimens.RadiusMd))
                .background(Palette.Paper)
                .padding(Dimens.Space),
        ) {
            StyledQr(
                content = student.studentId,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.height(Dimens.SpaceLg))
        Perforation()
        Spacer(Modifier.height(Dimens.Space))

        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Label(stringResource(R.string.qr_admit))
                Text(
                    text = stringResource(R.string.qr_event_dates),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = Color.White,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = student.studentId,
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                )
                Text(
                    text = student.major,
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = Ink.Muted,
                )
                Text(
                    text = student.school,
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = Ink.Muted,
                )
            }
        }
        Spacer(Modifier.height(Dimens.SpaceLg))
    }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        fontFamily = AlanSans,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        letterSpacing = 1.4.sp,
        color = Ink.Faint,
        modifier = modifier,
    )
}

/** The tear line, drawn between the two notches the ticket shape cuts out. */
@Composable
private fun Perforation(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        drawLine(
            color = Ink.Faint,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 9f)),
        )
    }
}

/**
 * A rounded rectangle with a semicircle bitten out of each side, at [stubLine]
 * down the height — the reason a paper ticket tears where it's meant to.
 *
 * Built as a real [Outline] rather than by painting circles on top, because the
 * pane is translucent: anything painted over the notch would still show the glass
 * fill underneath it instead of the background.
 */
private class TicketShape(
    private val corner: Dp,
    private val notch: Dp,
    private val stubLine: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val cornerPx = with(density) { corner.toPx() }
        val notchPx = with(density) { notch.toPx() }
        val y = size.height * stubLine

        val body = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(Offset.Zero, size),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                ),
            )
        }
        val notches = Path().apply {
            addOval(Rect(center = Offset(0f, y), radius = notchPx))
            addOval(Rect(center = Offset(size.width, y), radius = notchPx))
        }
        return Outline.Generic(Path.combine(PathOperation.Difference, body, notches))
    }
}

/**
 * The QR, drawn module by module so it can be styled: rounded dots for data and
 * rounded finder patterns, matching the mockup.
 *
 * Error correction is [ErrorCorrectionLevel.H] (~30% recoverable) because the
 * logo in the middle covers real modules — the code still decodes with that area
 * obscured, which a lower level would not survive.
 *
 * **Dark modules on a light panel, deliberately.** The mockup draws the code
 * light-on-dark, straight onto the card. That was built and then tested by
 * decoding a screenshot of the running app: as rendered it *failed to decode*,
 * and the same image inverted read back `6831503029` first time. ZXing — which is
 * what most Android scanning apps are built on — will not read an inverted code,
 * so a booth queue would have stalled on it. Phone cameras and ML Kit often cope;
 * plenty of scanners do not, and a stamp rally across 27 booths is not the place
 * to find out which.
 *
 * If the booth-side scanner is known to handle inversion, swapping [QrInk] and
 * [Palette.Paper] restores the original look.
 */
@Composable
private fun StyledQr(content: String, modifier: Modifier = Modifier) {
    val matrix = remember(content) {
        runCatching { Encoder.encode(content, ErrorCorrectionLevel.H).matrix }.getOrNull()
    } ?: return

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) { drawQr(matrix) }
        Image(
            painter = painterResource(R.drawable.logo_su),
            contentDescription = null,
            colorFilter = ColorFilter.tint(QrInk),
            modifier = Modifier.fillMaxWidth(LogoFraction),
        )
    }
}

/** Share of the QR's width taken by the centre logo, and by the gap cleared for it. */
private const val LogoFraction = 0.20f
private const val LogoClearFraction = 0.26f

private fun DrawScope.drawQr(matrix: ByteMatrix) {
    val modules = matrix.width
    val cell = size.minDimension / modules
    val dot = cell * 0.86f
    val dotRadius = CornerRadius(dot * 0.35f, dot * 0.35f)
    val inset = (cell - dot) / 2f

    val clearHalf = modules * LogoClearFraction / 2f
    val centre = modules / 2f

    for (y in 0 until modules) {
        for (x in 0 until modules) {
            if (matrix.get(x, y).toInt() != 1) continue
            // Finder patterns get drawn as whole shapes below, not as dots.
            if (isFinder(x, y, modules)) continue
            // Clear a hole for the logo. Safe at error-correction level H.
            if (kotlin.math.abs(x + 0.5f - centre) < clearHalf &&
                kotlin.math.abs(y + 0.5f - centre) < clearHalf
            ) {
                continue
            }
            drawRoundRect(
                color = QrInk,
                topLeft = Offset(x * cell + inset, y * cell + inset),
                size = Size(dot, dot),
                cornerRadius = dotRadius,
            )
        }
    }

    listOf(0 to 0, modules - 7 to 0, 0 to modules - 7).forEach { (fx, fy) ->
        drawFinder(Offset(fx * cell, fy * cell), cell)
    }
}

/** The three 7x7 alignment squares at the corners of every QR code. */
private fun DrawScope.isFinder(x: Int, y: Int, modules: Int): Boolean =
    (x < 7 && y < 7) || (x >= modules - 7 && y < 7) || (x < 7 && y >= modules - 7)

private fun DrawScope.drawFinder(topLeft: Offset, cell: Float) {
    val outer = cell * 7f
    val ring = cell
    drawRoundRect(
        color = QrInk,
        topLeft = Offset(topLeft.x + ring / 2f, topLeft.y + ring / 2f),
        size = Size(outer - ring, outer - ring),
        cornerRadius = CornerRadius(cell * 2f, cell * 2f),
        style = Stroke(width = ring),
    )
    drawRoundRect(
        color = QrInk,
        topLeft = Offset(topLeft.x + cell * 2f, topLeft.y + cell * 2f),
        size = Size(cell * 3f, cell * 3f),
        cornerRadius = CornerRadius(cell * 0.9f, cell * 0.9f),
    )
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun QrTicketPreview() {
    SUClubFairTheme {
        QrTicketScreen()
    }
}
