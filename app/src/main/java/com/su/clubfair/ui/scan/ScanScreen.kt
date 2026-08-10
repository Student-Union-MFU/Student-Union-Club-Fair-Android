package com.su.clubfair.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.su.clubfair.R
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.su.clubfair.data.ScanOutcome
import com.su.clubfair.ui.components.PillButton
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.components.liquidGlass
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.LocalAccent
import com.su.clubfair.ui.theme.Palette
import com.su.clubfair.ui.theme.SUClubFairTheme
import java.util.concurrent.Executors


/** The clear window, as a fraction of the shorter screen edge. */
private const val ReticleFraction = 0.68f

/**
 * The window's frame, in two weights.
 *
 * Four corner brackets on their own were the problem — they say "this corner"
 * four times and never say "this region", so the frame reads as one that has not
 * finished drawing itself. A bare square fixed that and lost everything else: a
 * plain rectangle on a camera feed is a rectangle on a camera feed.
 *
 * Both, then. [FrameStroke] closes the shape all the way round at low alpha, so
 * there is nothing left dangling; [CornerStroke] rides over it at full accent for
 * [CornerLength] out of each corner, which is where the eye actually aims. It is
 * one frame with emphasis in it rather than two ideas fighting.
 *
 * The radius comes back with them, on the app's own scale. It went to zero on the
 * argument that a QR code is hard-cornered, which is true and beside the point:
 * this is not a picture of the code, it is a card cut out of the scrim, and every
 * other card in the app has [Dimens.RadiusLg] on it.
 */
private val ReticleRadius = Dimens.RadiusLg
private val FrameStroke = 1.5.dp
private val FrameAlpha = 0.4f
private val CornerStroke = 3.dp
private val CornerLength = 40.dp

/** One pass of the sweep line, top to bottom. */
private const val SweepMillis = 2200

/**
 * How far under the status bar the header sits.
 *
 * A screen gutter's worth rather than the 12dp it had, which pinned the card to
 * the top edge and left it looking like a system bar rather than a card on the
 * feed. There is a lot of empty scrim between it and the window; some of that
 * belongs above it.
 */
private val HeaderDrop = Dimens.ScreenPadding * 2

/**
 * The Scan tab: read a booth's QR code with the camera.
 *
 * This replaced the student's own pass on this tab — the pass now opens as a
 * sheet from Profile. The two directions are different jobs: the pass is
 * something a booth scans off the student, this is the student scanning a booth.
 *
 * A scan is recorded now rather than shown and discarded. [outcome] is the
 * repository's answer — collected, already had it, or not a fair code — and the
 * card at the foot of the screen says which; see [ScanOutcome].
 *
 * The screen is deliberately not a dead end when the camera is unavailable.
 * Permission refused, no camera, a damaged code, a booth whose sign has gone:
 * all four used to end here, and all four now fall through to [ManualEntry].
 */
@Composable
fun ScanScreen(
    modifier: Modifier = Modifier,
    outcome: ScanOutcome? = null,
    hapticsEnabled: Boolean = true,
    onScanned: (String) -> Unit = {},
    onClearScan: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    // A scanner with the camera switched off is not a screen anyone wants to look
    // at, so ask on arrival rather than behind a button. The explainer below is
    // what a refusal lands on.
    LaunchedEffect(hasCamera) {
        if (hasCamera && !granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var torchOn by remember { mutableStateOf(false) }
    var hasTorch by remember { mutableStateOf(false) }

    // Bumping this rebuilds the analyzer, which is how a one-shot scanner is
    // pointed at the next booth.
    var attempt by remember { mutableStateOf(0) }

    /**
     * A booth read at arm's length is a phone nobody is looking at — it is held
     * up at a sign while the student watches the sign, not the screen. The buzz
     * is what says "done" without needing eyes on the display, which is why it
     * fires on the two conclusive outcomes and stays quiet for a code that was
     * simply not ours: a long buzz for every stray QR the camera swept past
     * would make the phone feel broken.
     */
    LaunchedEffect(outcome, hapticsEnabled) {
        if (!hapticsEnabled) return@LaunchedEffect
        when (outcome) {
            is ScanOutcome.Recorded -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            is ScanOutcome.AlreadyScanned ->
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)

            else -> Unit
        }
    }


    // What the header pane refracts: the feed, the scrim and the frame, and
    // nothing above them. A pane sampling a layer it is drawn inside samples
    // itself, so the chrome sits outside this — the same arrangement the booth
    // wall uses for its wallpaper.
    val viewfinder = rememberLayerBackdrop()

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(viewfinder),
        ) {
            // Something has to be behind the camera while it starts. Painted here
            // rather than left to the shell's copy because the camera surface
            // covers it anyway once it's up — this only exists to stop the screen
            // flashing black on the way in, and it's the abyss either way.
            MeshBackground()

            if (granted && hasCamera) {
                CameraFeed(
                    attempt = attempt,
                    torchOn = torchOn,
                    // The analyzer fires per frame; the ViewModel drops repeats
                    // while a result is on screen, so this stays a plain hand-up.
                    onDecoded = onScanned,
                    onTorchAvailable = { hasTorch = it },
                )
            }

            Reticle(active = outcome == null && granted && hasCamera)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = Dimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(HeaderDrop))
            // The title used to sit bare on the camera feed, which made this the
            // one screen in the app where text has nothing behind it. Over a dark
            // hall that was fine and over a bright booth sign it was white on
            // white — unreliable either way, and it made the tab read as a camera
            // app the rest of the app had been dropped into.
            //
            // [liquidGlass] rather than the frosted `glassSurface`, and this is
            // the one screen where that choice is not close. The nav bar takes
            // liquid glass because things *move* underneath it; nothing in the
            // app moves under a pane like a live camera feed does, so the
            // refraction has something to bend on every frame instead of a still
            // mesh. Same material as the bar at the other end of the screen, so
            // the two read as the same pane of the same app.
            val headerShape = RoundedCornerShape(Dimens.RadiusLg)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        backdrop = viewfinder,
                        shape = headerShape,
                        // No shader highlight. The library's edge lighting ramps
                        // its brightness around the outline by design, and at 1dp
                        // that doesn't read as light catching a rim — it reads as
                        // a border that fades out down one side, which looks like
                        // a rendering fault rather than a material. Same call the
                        // booth tiles make, for the same reason.
                        highlight = null,
                    )
                    // One hairline, even the whole way round, at the alpha
                    // `glassSurface` uses everywhere else in the app.
                    .border(1.dp, Color.White.copy(alpha = 0.22f), headerShape)
                    .padding(Dimens.CardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.scan_title),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White,
                )
                Spacer(Modifier.height(Dimens.SpaceXs))
                Text(
                    text = stringResource(R.string.scan_hint),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = Ink.Muted,
                    textAlign = TextAlign.Center,
                )
            }

            // The torch sits beside the window it lights, not down with the
            // result card: it is a control for the act of aiming, and it has to
            // be reachable while the phone is already held up at a sign.
            if (granted && hasCamera && hasTorch && outcome == null) {
                Spacer(Modifier.height(Dimens.SpaceLg))
                TorchButton(enabled = torchOn, onToggle = { torchOn = it })
            }

            Spacer(Modifier.weight(1f))

            when {
                outcome != null -> ResultCard(
                    outcome = outcome,
                    onAgain = {
                        onClearScan()
                        attempt++
                    },
                )

                !hasCamera -> Explainer(
                    title = stringResource(R.string.scan_unavailable_title),
                    body = stringResource(R.string.scan_unavailable_body),
                )

                !granted -> Explainer(
                    title = stringResource(R.string.scan_permission_title),
                    body = stringResource(R.string.scan_permission_denied_body),
                    cta = stringResource(R.string.scan_permission_cta),
                    onCta = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                )

                // Camera up, nothing read yet. Nothing to show: the reticle and the
                // header already say what to do, and a third instruction on the
                // same screen is noise.
                else -> Unit
            }

            Spacer(Modifier.height(Dimens.NavBarClearance))
        }
    }
}

/**
 * The camera preview, bound for as long as this is on screen.
 *
 * `PreviewView` rather than a raw surface: it handles the rotation, scaling and
 * surface lifecycle that CameraX otherwise leaves to the caller, and none of
 * that is interesting work.
 */
@Composable
private fun CameraFeed(
    attempt: Int,
    torchOn: Boolean,
    onDecoded: (String) -> Unit,
    onTorchAvailable: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Held so the torch can be driven after binding. CameraX exposes the light
    // through the bound camera's control, not through the use case, so there is
    // nothing else to keep a handle on.
    var camera by remember { mutableStateOf<Camera?>(null) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // COMPATIBLE, which is a `TextureView`, rather than the default
            // PERFORMANCE, which is a `SurfaceView`. A SurfaceView is punched
            // through the window and composited by the system, so it never draws
            // into the Compose canvas — which means it cannot be captured into a
            // `layerBackdrop`, and the header pane above would refract the mesh
            // while the camera showed through untouched underneath it. A
            // TextureView draws into the hierarchy like anything else. It costs a
            // copy per frame, which is what buys the glass its subject.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    // Analysis off the main thread; decoding a frame is far too much work for it.
    val executor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    AndroidView(factory = { previewView }, modifier = modifier.fillMaxSize())

    DisposableEffect(lifecycleOwner, attempt) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = CameraPreview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                // Decode the newest frame and drop the backlog; a queue of stale
                // frames only adds latency between pointing and reading.
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executor, QrAnalyzer { p -> mainExecutor.execute { onDecoded(p) } }) }

            provider.unbindAll()
            runCatching {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            }.onSuccess { bound ->
                camera = bound
                // Asked, not assumed. Plenty of devices have no flash unit, and
                // a torch button that silently does nothing is worse than none.
                onTorchAvailable(bound.cameraInfo.hasFlashUnit())
            }
        }, mainExecutor)

        onDispose {
            runCatching { future.get().unbindAll() }
            camera = null
            executor.shutdown()
        }
    }

    // Separate from the binding effect: the torch is toggled far more often than
    // the camera is rebound, and folding it in would tear down and rebuild the
    // whole session on every tap.
    LaunchedEffect(camera, torchOn) {
        runCatching { camera?.cameraControl?.enableTorch(torchOn) }
    }
}

/**
 * The scrim with a clear window punched through it, plus corner brackets.
 *
 * The hole is a real `BlendMode.Clear` into an offscreen layer, not a stack of
 * four rectangles around a gap — the rectangles have to be recomputed against
 * every screen size and they seam visibly at the corners once there's a radius.
 */
@Composable
private fun Reticle(active: Boolean, modifier: Modifier = Modifier) {
    val sweep by rememberInfiniteTransition(label = "reticle").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SweepMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    // Read once here rather than inside the Canvas: the draw block is not a
    // composable scope.
    val accent = LocalAccent.current

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        val side = minOf(size.width, size.height) * ReticleFraction
        val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
        val window = Size(side, side)
        val radius = CornerRadius(ReticleRadius.toPx())

        // [Palette.Base] rather than black, and a gradient rather than a flat
        // wash. Both are about matching the app rather than about the camera:
        // every other screen stands on that near-black green, and a flat 55%
        // black over a live feed is the house style of every scanner ever
        // shipped. Graded, it does a second job — it is densest at the top and
        // bottom, which is exactly where the header card and the nav bar sit, and
        // lightest across the band the window is cut from, so the feed stays
        // bright where a student is actually aiming.
        drawRect(
            brush = Brush.verticalGradient(
                // Light at the top on purpose. The header used to need a dark
                // ground under it to be readable; it is a pane of glass now, so
                // it carries its own legibility and the scrim behind it can get
                // out of the way — which it has to, because a pane over an opaque
                // scrim has nothing to refract and reads as a flat grey box. This
                // is what makes the material worth using here at all.
                0f to Palette.Base.copy(alpha = 0.46f),
                0.5f to Palette.Base.copy(alpha = 0.54f),
                // Still dense at the foot, where the nav bar and the result card
                // sit over it.
                1f to Palette.Base.copy(alpha = 0.90f),
            ),
        )
        drawRoundRect(
            color = Color.Transparent,
            topLeft = topLeft,
            size = window,
            cornerRadius = radius,
            blendMode = BlendMode.Clear,
        )

        if (!active) return@Canvas

        // The quiet line, all the way round, so nothing is left dangling.
        drawRoundRect(
            color = accent.copy(alpha = FrameAlpha),
            topLeft = topLeft,
            size = window,
            cornerRadius = radius,
            style = Stroke(width = FrameStroke.toPx()),
        )

        // The loud corners, over the top of it. Each is the whole outline drawn
        // again and clipped to one corner square — cheaper to read than four
        // hand-built L-paths, and the curve matches the window exactly because it
        // *is* the window's curve.
        val corner = CornerLength.toPx()
        listOf(
            Offset(topLeft.x, topLeft.y),
            Offset(topLeft.x + side - corner, topLeft.y),
            Offset(topLeft.x, topLeft.y + side - corner),
            Offset(topLeft.x + side - corner, topLeft.y + side - corner),
        ).forEach { at ->
            clipRect(at.x, at.y, at.x + corner, at.y + corner) {
                drawRoundRect(
                    color = accent,
                    topLeft = topLeft,
                    size = window,
                    cornerRadius = radius,
                    style = Stroke(width = CornerStroke.toPx()),
                )
            }
        }

        // A soft band travelling down the window — the one moving thing that says
        // the camera is live even when it's pointed at a blank wall.
        clipRect(topLeft.x, topLeft.y, topLeft.x + side, topLeft.y + side) {
            val y = topLeft.y + side * sweep
            val band = side * 0.18f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, accent.copy(alpha = 0.35f), Color.Transparent),
                    startY = y - band,
                    endY = y + band,
                ),
                topLeft = Offset(topLeft.x, y - band),
                size = Size(side, band * 2),
            )
        }
    }
}

/**
 * Glass card for the states where there's no camera to show.
 *
 * [secondaryCta] is the escape hatch — "type the number instead" — under the
 * action that would fix the problem properly. Below rather than beside, because
 * they are not equal choices: allowing the camera is what this screen is for,
 * and typing is what to do when that is not going to happen.
 */
@Composable
private fun Explainer(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    cta: String? = null,
    onCta: () -> Unit = {},
    secondaryCta: String? = null,
    onSecondaryCta: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = Dimens.RadiusLg)
            .padding(Dimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            fontFamily = AlanSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
        )
        Text(
            text = body,
            fontFamily = AlanSans,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = Ink.Muted,
        )
        if (cta != null) {
            Spacer(Modifier.height(Dimens.SpaceXs))
            PillButton(text = cta, onClick = onCta)
        }
        if (secondaryCta != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(Dimens.RadiusPill))
                    .clickable(role = Role.Button, onClick = onSecondaryCta),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = secondaryCta,
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * What the scan turned out to be.
 *
 * Three outcomes, three cards, and the middle one is the one that used to be
 * missing: re-scanning a booth you have already collected produced a card
 * identical to a fresh checkpoint, so a student walking back past a stall was
 * told they had gained something they had not. It says so now, in the accent's
 * quieter register rather than as an error, because nothing has gone wrong.
 *
 * The card is announced as a live region: it appears while the phone is held up
 * at a sign, which is exactly when nobody is looking at the screen.
 */
@Composable
private fun ResultCard(
    outcome: ScanOutcome,
    onAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(
            animationSpec = tween(240, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 2 },
        ) + fadeIn(tween(240)),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = modifier,
    ) {
        val accent = LocalAccent.current
        val booth = when (outcome) {
            is ScanOutcome.Recorded -> outcome.booth
            is ScanOutcome.AlreadyScanned -> outcome.booth
            else -> null
        }
        val problem = outcome is ScanOutcome.Rejected || outcome is ScanOutcome.Expired
        val tint = if (problem) Palette.Alert else accent

        val eyebrow = when (outcome) {
            is ScanOutcome.Recorded -> stringResource(R.string.scan_result_recorded)
            is ScanOutcome.AlreadyScanned -> stringResource(R.string.scan_result_already)
            // Queued is a success the student should know the shape of: the stamp
            // is theirs, it just has not reached the server yet.
            ScanOutcome.Queued -> stringResource(R.string.scan_result_queued)
            is ScanOutcome.Expired -> stringResource(R.string.scan_result_expired)
            is ScanOutcome.Rejected -> stringResource(R.string.scan_result_unknown)
        }
        val headline = booth?.let { it.nameEn ?: it.name }
            ?: when (outcome) {
                ScanOutcome.Queued -> stringResource(R.string.scan_result_queued_title)
                is ScanOutcome.Expired -> stringResource(R.string.scan_result_expired_title)
                else -> stringResource(R.string.scan_result_unknown_body)
            }
        val body = when (outcome) {
            is ScanOutcome.AlreadyScanned -> stringResource(R.string.scan_result_already_body)
            ScanOutcome.Queued -> stringResource(R.string.scan_result_queued_body)
            // The server's own message where it sent one — it knows whether the
            // code was expired or simply not ours.
            is ScanOutcome.Expired -> outcome.message
            is ScanOutcome.Rejected -> outcome.message
            else -> null
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(cornerRadius = Dimens.RadiusLg)
                .padding(Dimens.CardPadding)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Assertive },
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The club's own glyph, on the plate the booth tiles and the booth
                // panel both use. Without it this card was the one place a club
                // appears in the app as text alone, and a scan that comes back as
                // a line of type reads like a receipt rather than like the booth
                // you are standing in front of.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(Dimens.RadiusMd))
                        .background(tint.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(
                            booth?.icon ?: R.drawable.ic_alert_circle,
                        ),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(Dimens.Space))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = booth?.let { "$eyebrow · ${it.displayCode}" } ?: eyebrow,
                        fontFamily = AlanSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp,
                        color = tint,
                    )
                    Text(
                        text = headline,
                        fontFamily = AlanSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                    )
                    if (body != null) {
                        Text(
                            text = body,
                            fontFamily = AlanSans,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = Ink.Muted,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Dimens.SpaceXs))
            PillButton(text = stringResource(R.string.scan_again), onClick = onAgain)
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun ScanScreenPreview() {
    // No camera binds under inspection, so this renders the chrome over the art.
    SUClubFairTheme { ScanScreen() }
}
