package com.tivanstudio.servera.presentation.servers.add.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tivanstudio.servera.R
import com.tivanstudio.servera.data.scanner.RecognizedFrame
import com.tivanstudio.servera.data.scanner.RecognizedLine
import com.tivanstudio.servera.data.scanner.TextRecognitionAnalyzer
import com.tivanstudio.servera.domain.entity.ScannedCredentials
import com.tivanstudio.servera.domain.parser.ScannedTextParser
import com.tivanstudio.servera.presentation.theme.PrimaryGreen
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.math.max
import androidx.compose.ui.geometry.Rect as ViewRect
import androidx.compose.ui.geometry.Size as ViewSize

/** How long the user may stare at an empty area before being told what to aim at. */
private const val NOTHING_FOUND_AFTER_MS = 15_000L

/**
 * The part of the frame the scanner works on, as fractions of the upright frame.
 *
 * Drawn as the reticle and used to filter recognized lines, from this one constant, so the box
 * on screen is exactly the area that counts. ML Kit ignores [android.media.Image] crop rects --
 * honouring one would mean converting every frame to a Bitmap, which costs more than it saves --
 * so the whole frame is recognized and the area is applied to the results. What it buys is
 * accuracy, not speed: the panel header, the menu and the footer drop out of the text, and their
 * false matches with them.
 */
private val AIM_REGION = RectF(0.05f, 0.30f, 0.95f, 0.70f)

/** Readings kept for the vote. */
private const val VOTE_WINDOW = 6

/** Votes one value needs before a field is taken as read. */
private const val VOTES_TO_CONFIRM = 3

/** 1280x720 is more than enough for text on a screen, and much quicker per frame than the default. */
private val ANALYSIS_RESOLUTION = Size(1280, 720)

/**
 * Full-screen camera overlay that reads connection details off a hoster panel.
 *
 * Nothing is applied on its own: a reading has to win a vote across several frames before it is
 * even shown, and the user still has to press the apply button. Frames are never written to disk
 * and neither recognized text nor its coordinates are logged -- they carry the user's addresses
 * and logins.
 *
 * Only the host, port and login are read. A password is masked on the screens this scans, and
 * one misread character in it fails authentication without saying why; a server name is not
 * read either, since the labels around it are usually in a script ML Kit does not recognize.
 *
 * @param onCandidate every non-empty parse, whether or not it is confirmed or applied.
 * @param onResult the reading the user chose to apply.
 * @param onPermissionDenied camera access was refused; the flag is true when the system will
 *   not ask again.
 */
@Composable
fun ScannerOverlay(
    onCandidate: (ScannedCredentials) -> Unit,
    onResult: (ScannedCredentials) -> Unit,
    onPermissionDenied: (permanently: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val context = LocalContext.current
        val activity = LocalActivity.current
        val currentOnPermissionDenied by rememberUpdatedState(onPermissionDenied)
        var isGranted by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
            )
        }
        var isDenied by remember { mutableStateOf(false) }
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            isGranted = granted
            isDenied = !granted
            if (!granted) {
                // We have just asked, so a rationale the system declines to show means the
                // refusal is permanent rather than a first-time "not now".
                val permanently = activity == null || !ActivityCompat
                    .shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                currentOnPermissionDenied(permanently)
            }
        }

        // Asked when the scanner opens rather than at app start: a permission prompt only
        // makes sense next to the thing that needs it.
        LaunchedEffect(Unit) {
            if (!isGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when {
                isGranted -> CameraScanner(
                    onCandidate = onCandidate,
                    onResult = onResult,
                    onDismiss = onDismiss
                )
                // A second refusal is silent -- the system stops showing its dialog -- so the
                // only way left is the app settings page.
                isDenied -> PermissionDenied(
                    onOpenSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    }
                )
                // Otherwise the system dialog is still up and the black ground is the screen.
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun BoxScope.CameraScanner(
    onCandidate: (ScannedCredentials) -> Unit,
    onResult: (ScannedCredentials) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // The analyzer is remembered once, so it must not capture the first lambda it was given.
    val currentOnCandidate by rememberUpdatedState(onCandidate)

    var confirmed by remember { mutableStateOf<ScannedCredentials?>(null) }
    var viewfinder by remember { mutableStateOf(Viewfinder()) }
    var isCameraBroken by remember { mutableStateOf(false) }
    var showNothingFound by remember { mutableStateOf(false) }

    val reader = remember { FrameReader() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            // Recognition is slower than the camera, and a queued frame only goes stale.
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            ANALYSIS_RESOLUTION,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()
            )
            .build()
    }
    val analyzer = remember {
        TextRecognitionAnalyzer { frame ->
            val aimed = frame.linesInAimRegion()
            val reading = reader.read(aimed.joinToString("\n") { it.text })
            if (!reading.isEmpty) {
                currentOnCandidate(reading)
                reader.vote(reading)
            }
            viewfinder = Viewfinder(frame, aimed, reading, reader.hostVotes())
            reader.confirmed()?.let { confirmed = it }
        }
    }
    val cameraProvider = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val camera = remember { mutableStateOf<Camera?>(null) }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    DisposableEffect(Unit) {
        // Since 1.4.0 CameraX checks camera availability strictly: a camera held by another
        // app, or momentarily absent, throws out of getInstance, get() or bindToLifecycle
        // where 1.3.4 carried on regardless. All three are guarded, and a failure shows the
        // unavailable notice rather than a black screen or a crash.
        val future = try {
            ProcessCameraProvider.getInstance(context)
        } catch (e: Exception) {
            isCameraBroken = true
            null
        }
        future?.addListener({
            try {
                val provider = future.get()
                // The preview keeps the default resolution; the user should not be able to see
                // that analysis runs on a smaller frame.
                val preview = Preview.Builder().build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                provider.unbindAll()
                camera.value = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
                cameraProvider.value = provider
            } catch (e: Exception) {
                // No usable back camera, or another app holds it. Typing the address still works.
                isCameraBroken = true
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            // Releasing can throw for the same reasons acquiring can, and a throw here would
            // take the whole screen down on the way out.
            runCatching { cameraProvider.value?.unbindAll() }
            executor.shutdown()
            analyzer.close()
        }
    }

    // Analysis is suspended while a reading waits for the user, and taken up again on retry.
    DisposableEffect(confirmed) {
        if (confirmed == null) imageAnalysis.setAnalyzer(executor, analyzer) else imageAnalysis.clearAnalyzer()
        onDispose { }
    }

    LaunchedEffect(confirmed) {
        if (confirmed != null) return@LaunchedEffect
        showNothingFound = false
        delay(NOTHING_FOUND_AFTER_MS)
        showNothingFound = true
    }

    // Tapping refocuses. The commonest reason for "it will not read" is a camera focused on
    // something else, and this is the cheapest fix for it there is.
    val focusRing = remember { Animatable(0f) }
    var focusAt by remember { mutableStateOf<Offset?>(null) }
    var focusTick by remember { mutableStateOf(0) }
    LaunchedEffect(focusTick) {
        if (focusTick == 0) return@LaunchedEffect
        focusRing.snapTo(1f)
        focusRing.animateTo(0f, tween(durationMillis = 600))
        focusAt = null
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val control = camera.value?.cameraControl ?: return@detectTapGestures
                    val point = previewView.meteringPointFactory
                        .createPoint(offset.x, offset.y)
                    control.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
                    focusAt = offset
                    focusTick++
                }
            }
    ) {
        drawReticle()
        // One box, around the line the host was taken from. Outlining every recognized block
        // would be a screenful of jumping rectangles saying nothing about what will be used.
        val frame = viewfinder.frame
        val hostRect = if (frame == null) null else viewfinder.hostLine()?.viewRect(frame, size)
        hostRect?.let { rect ->
            drawRect(
                color = PrimaryGreen,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = 3.dp.toPx())
            )
        }
        focusAt?.let { at ->
            drawCircle(
                color = Color.White.copy(alpha = focusRing.value),
                radius = (24.dp.toPx() + 16.dp.toPx() * focusRing.value),
                center = at,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }

    if (isCameraBroken) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Message(text = stringResource(R.string.scan_camera_unavailable))
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    } else if (confirmed == null) {
        // Laid out against the same fractions as the reticle so the status sits under it.
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(AIM_REGION.bottom))
            Column(
                modifier = Modifier
                    .weight(1f - AIM_REGION.bottom)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))
                ScanStatusPlate(status = viewfinder.status, hostVotes = viewfinder.hostVotes)
                // Only worth saying when the area is empty. With text in view the problem is
                // what is in it, and this advice would send the user looking the wrong way.
                if (showNothingFound && viewfinder.status == ScanStatus.Searching) {
                    Spacer(Modifier.height(8.dp))
                    Message(text = stringResource(R.string.scan_nothing_found))
                }
            }
        }
    }

    AnimatedVisibility(
        visible = confirmed != null,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        confirmed?.let { reading ->
            ReadingCard(
                reading = reading,
                // Taking the result is what closes the overlay: the view model owns that flag.
                onApply = { onResult(reading) },
                onRetry = {
                    reader.reset()
                    viewfinder = Viewfinder()
                    confirmed = null
                }
            )
        }
    }
}

private fun DrawScope.drawReticle() {
    val left = AIM_REGION.left * size.width
    val top = AIM_REGION.top * size.height
    drawRoundRect(
        color = Color.White.copy(alpha = 0.9f),
        topLeft = Offset(left, top),
        size = ViewSize(AIM_REGION.width() * size.width, AIM_REGION.height() * size.height),
        cornerRadius = CornerRadius(12.dp.toPx()),
        style = Stroke(width = 2.dp.toPx())
    )
}

/** What the last frame told us, in the form the viewfinder draws. */
private data class Viewfinder(
    val frame: RecognizedFrame? = null,
    val aimed: List<RecognizedLine> = emptyList(),
    val reading: ScannedCredentials = ScannedCredentials(),
    val hostVotes: Int = 0
) {
    val status: ScanStatus
        get() = when {
            aimed.isEmpty() -> ScanStatus.Searching
            reading.isEmpty -> ScanStatus.NoMatch
            else -> ScanStatus.Confirming
        }

    /** The aimed-at line the host was read from, if any. */
    fun hostLine(): RecognizedLine? {
        val host = reading.host ?: return null
        return aimed.firstOrNull { it.text.contains(host) }
    }
}

private enum class ScanStatus(@StringRes val textRes: Int) {
    /** Nothing in the aimed-at area. */
    Searching(R.string.scan_status_searching),

    /**
     * Text is being read, but none of it parses. The most useful of the three: it says the
     * camera is working and the problem is where it points, instead of leaving the user
     * shaking the phone at a screen they think has frozen.
     */
    NoMatch(R.string.scan_status_no_match),

    /** Something parsed and is being voted on. */
    Confirming(R.string.scan_status_confirming)
}

@Composable
private fun ScanStatusPlate(status: ScanStatus, hostVotes: Int) {
    Row(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(status.textRes),
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        // The wait before the card appears otherwise reads as the app being slow.
        if (status == ScanStatus.Confirming) {
            Spacer(Modifier.width(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(VOTES_TO_CONFIRM) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (index < hostVotes) PrimaryGreen
                                else Color.White.copy(alpha = 0.35f),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

/** Lines whose centre falls inside [AIM_REGION], in reading order. */
private fun RecognizedFrame.linesInAimRegion(): List<RecognizedLine> {
    val width = uprightWidth.toFloat()
    val height = uprightHeight.toFloat()
    if (width <= 0f || height <= 0f) return emptyList()
    return lines
        // By centre, not by containment: a line poking out past the edge is still part of what
        // the camera is aimed at, and dropping it would lose the address it sits on.
        .filter {
            AIM_REGION.contains(it.box.exactCenterX() / width, it.box.exactCenterY() / height)
        }
        .sortedBy { it.box.top }
}

/**
 * Where this line lands on a [PreviewView] of [viewSize] running in FILL_CENTER.
 *
 * FILL_CENTER covers the view and crops the overflow, so the scale is the larger of the two
 * ratios and the offset is half of what got cropped on each axis. Skipping this puts the box
 * somewhere near the text on one handset and far off it on another, which is exactly the kind
 * of error that survives testing on a single device.
 */
private fun RecognizedLine.viewRect(frame: RecognizedFrame, viewSize: ViewSize): ViewRect? {
    val sourceWidth = frame.uprightWidth.toFloat()
    val sourceHeight = frame.uprightHeight.toFloat()
    if (sourceWidth <= 0f || sourceHeight <= 0f) return null

    val scale = max(viewSize.width / sourceWidth, viewSize.height / sourceHeight)
    val dx = (viewSize.width - sourceWidth * scale) / 2f
    val dy = (viewSize.height - sourceHeight * scale) / 2f
    return ViewRect(
        left = box.left * scale + dx,
        top = box.top * scale + dy,
        right = box.right * scale + dx,
        bottom = box.bottom * scale + dy
    )
}

/**
 * Turns frames into a reading, and decides when one is trustworthy.
 *
 * Confirmation is a majority vote over a sliding window rather than two identical readings in a
 * row. Two in a row can be the same OCR mistake twice: under steadily poor light a digit is
 * misread the same way every time. A vote survives that, because the correct reading usually
 * turns up more often than any one wrong version of it.
 *
 * Every method here runs on the single analysis thread.
 */
private class FrameReader {
    private val window = ArrayDeque<ScannedCredentials>()
    private var lastText: String? = null
    private var lastReading = ScannedCredentials()

    /**
     * The reading for this frame.
     *
     * A monitor does not move, so consecutive frames carry identical text; that is parsed once
     * and the result reused. The caller still votes with it -- skipping the vote on an unchanged
     * frame would mean a still picture never confirms anything.
     */
    fun read(text: String): ScannedCredentials {
        if (text != lastText) {
            lastText = text
            lastReading = ScannedTextParser.parse(text)
        }
        return lastReading
    }

    /** Empty readings are not offered, and do not clear the window: a blurred frame is an absence
     *  of data, not a different reading. */
    fun vote(reading: ScannedCredentials) {
        window.addLast(reading)
        while (window.size > VOTE_WINDOW) window.removeFirst()
    }

    /** Votes behind the leading host value, for the progress the viewfinder shows. */
    fun hostVotes(): Int = leader { it.host }?.second ?: 0

    /**
     * The reading to show, or null while the host is undecided. Fields are voted on one by one,
     * so a port or login that has not settled yet comes back null rather than half-guessed.
     */
    fun confirmed(): ScannedCredentials? {
        val host = elected { it.host } ?: return null
        return ScannedCredentials(
            host = host,
            port = elected { it.port },
            login = elected { it.login }
        )
    }

    fun reset() {
        window.clear()
        lastText = null
        lastReading = ScannedCredentials()
    }

    private fun <T : Any> leader(field: (ScannedCredentials) -> T?): Pair<T, Int>? =
        window.mapNotNull(field)
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.let { it.key to it.value }

    private fun <T : Any> elected(field: (ScannedCredentials) -> T?): T? =
        leader(field)?.takeIf { it.second >= VOTES_TO_CONFIRM }?.first
}

@Composable
private fun ReadingCard(
    reading: ScannedCredentials,
    onApply: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            reading.host?.let { ReadingRow(stringResource(R.string.server_host_hint), it) }
            reading.port?.let { ReadingRow(stringResource(R.string.server_port_hint), it.toString()) }
            reading.login?.let { ReadingRow(stringResource(R.string.server_login_hint), it) }

            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.scan_retry))
                }
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        stringResource(R.string.scan_apply),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PermissionDenied(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.scan_permission_required),
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onOpenSettings,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                stringResource(R.string.scan_open_settings),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/** Text laid over the camera, which is whatever colour the world happens to be. */
@Composable
private fun Message(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Color.White,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}
