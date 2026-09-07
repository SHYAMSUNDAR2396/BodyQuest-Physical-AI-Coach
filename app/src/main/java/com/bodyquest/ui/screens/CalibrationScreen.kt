package com.bodyquest.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import com.bodyquest.camera.CameraController
import com.bodyquest.camera.FrameRateTracker
import com.bodyquest.core.hasFullBodyVisible
import com.bodyquest.pose.PoseSession
import com.bodyquest.ui.theme.BqAccent
import com.bodyquest.ui.theme.BqGood
import com.bodyquest.ui.theme.BqSurface
import com.bodyquest.ui.theme.BqTextSecondary
import com.bodyquest.voice.CoachVoice

@Composable
fun CalibrationScreen(exerciseId: String, onBack: () -> Unit, onReady: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calibration · ${exerciseId.replaceFirstChar { it.uppercase() }}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (hasPermission) {
                LiveCameraCalibration(onReady = onReady)
            } else {
                CameraPermissionRationale(onGrant = { launcher.launch(Manifest.permission.CAMERA) })
            }
        }
    }
}

@Composable
private fun CameraPermissionRationale(onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Camera access needed", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "BodyQuest uses your camera to track your body's pose in real time so it can " +
                "count reps and check your form. Video is processed on this device and is not uploaded.",
            style = MaterialTheme.typography.bodyMedium,
            color = BqTextSecondary,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant) { Text("Grant camera access") }
    }
}

@Composable
private fun LiveCameraCalibration(onReady: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { CameraController(context) }
    val fpsTracker = remember { FrameRateTracker() }
    var fps by remember { mutableStateOf(0) }
    val poseSession = remember { PoseSession(context) }
    val pose by poseSession.result.collectAsState()
    val delegateLabel by poseSession.delegateLabel.collectAsState()
    val voice = remember { CoachVoice(context) }
    LaunchedEffect(Unit) {
        poseSession.warmUp()
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.stop()
            poseSession.close()
            voice.close()
        }
    }

    // Positioning guidance (spec §8): keyed on the visibility flag itself, not the raw pose,
    // so this speaks once per state change rather than every frame.
    val fullBodyVisible = pose?.hasFullBodyVisible() == true
    LaunchedEffect(fullBodyVisible, pose == null) {
        if (pose == null) return@LaunchedEffect
        voice.speak(
            if (fullBodyVisible) "You're all set. Tap continue when ready."
            else "Step back so your full body is visible.",
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp)),
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        controller.start(lifecycleOwner, previewView) { frame ->
                            val count = fpsTracker.onFrame(frame.timestampMs)
                            fps = count
                            poseSession.onFrame(frame)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            SkeletonOverlay(pose = pose, modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BqSurface.copy(alpha = 0.85f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text("CAMERA FPS  $fps", style = MaterialTheme.typography.labelLarge, color = BqAccent)
                Text(
                    "POSE  ${if (pose?.isEmpty == false) "TRACKING" else "SEARCHING"} · ${pose?.inferenceTimeMs ?: 0} ms · $delegateLabel",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (pose?.isEmpty == false) BqGood else BqTextSecondary,
                )
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                if (fullBodyVisible) "You're all set. Tap continue when ready."
                else "Step back so your full body is visible, then continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (fullBodyVisible) BqGood else BqTextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onReady, modifier = Modifier.fillMaxWidth()) {
                Text("Continue")
            }
        }
    }
}
