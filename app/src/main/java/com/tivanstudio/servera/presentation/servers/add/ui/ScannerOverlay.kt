package com.tivanstudio.servera.presentation.servers.add.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tivanstudio.servera.R
import com.tivanstudio.servera.data.scanner.TextRecognitionAnalyzer
import com.tivanstudio.servera.domain.entity.ScannedCredentials
import com.tivanstudio.servera.domain.parser.ScannedTextParser
import com.tivanstudio.servera.presentation.theme.PrimaryGreen
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

/** How long the user may stare at an unrecognized screen before being told what to aim at. */
private const val NOTHING_FOUND_AFTER_MS = 15_000L

/**
 * Full-screen camera overlay that reads connection details off a hoster panel.
 *
 * Nothing is applied on its own: a reading has to repeat before it is even shown, and the user
 * still has to press the apply button. Frames are never written to disk and recognized text is
 * never logged -- it carries the user's addresses and logins.
 *
 * Only the host, port and login are read. A password is masked on the screens this scans, and
 * one misread character in it fails authentication without saying why; a server name is not
 * read either, since the labels around it are usually in a script ML Kit does not recognize.
 *
 * @param onCandidate every non-empty parse, whether or not it is confirmed or applied.
 * @param onResult the reading the user chose to apply.
 */
@Composable
fun ScannerOverlay(
    onCandidate: (ScannedCredentials) -> Unit,
    onResult: (ScannedCredentials) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val context = LocalContext.current
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
                isGranted -> CameraScanner(onCandidate = onCandidate, onResult = onResult)
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
    onResult: (ScannedCredentials) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // The analyzer is remembered once, so it must not capture the first lambda it was given.
    val currentOnCandidate by rememberUpdatedState(onCandidate)

    var confirmed by remember { mutableStateOf<ScannedCredentials?>(null) }
    var isCameraBroken by remember { mutableStateOf(false) }
    var showNothingFound by remember { mutableStateOf(false) }

    val stabilizer = remember { ReadingStabilizer() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            // Recognition is slower than the camera, and a queued frame only goes stale.
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }
    val analyzer = remember {
        TextRecognitionAnalyzer { text ->
            val reading = ScannedTextParser.parse(text)
            if (reading.isEmpty) return@TextRecognitionAnalyzer
            currentOnCandidate(reading)
            if (stabilizer.offer(reading)) confirmed = reading
        }
    }
    val cameraProvider = remember { mutableStateOf<ProcessCameraProvider?>(null) }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    DisposableEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                provider.unbindAll()
                provider.bindToLifecycle(
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
            cameraProvider.value?.unbindAll()
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

    if (isCameraBroken) {
        Message(
            text = stringResource(R.string.scan_camera_unavailable),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
        )
    } else if (confirmed == null) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(16.dp))
            Message(text = stringResource(R.string.scan_hint))
            if (showNothingFound) {
                Spacer(Modifier.height(8.dp))
                Message(text = stringResource(R.string.scan_nothing_found))
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
                    stabilizer.reset()
                    confirmed = null
                }
            )
        }
    }
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

/**
 * Accepts a reading only once it repeats.
 *
 * OCR reads the same panel differently from one frame to the next -- an address off by a digit
 * shows up for a frame and is gone again. Acting on the first hit would hand the user whichever
 * frame happened to land first.
 *
 * Frames that parse to nothing are ignored rather than treated as a break in the run: focus
 * drifts constantly, and a blurred frame in between is not evidence of a different reading.
 */
private class ReadingStabilizer {
    private var previous: ScannedCredentials? = null

    /** @return true when this reading matches the one before it. */
    fun offer(reading: ScannedCredentials): Boolean {
        val repeated = reading == previous
        previous = reading
        return repeated
    }

    fun reset() {
        previous = null
    }
}
