package com.bodyquest.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bodyquest.camera.CameraController
import com.bodyquest.coach.CoachingPhrases
import com.bodyquest.coach.CorrectionEvent
import com.bodyquest.form.SquatFormSnapshot
import com.bodyquest.pose.PoseSession
import com.bodyquest.ui.theme.*
import com.bodyquest.voice.CoachVoice
import com.bodyquest.workout.RepResult
import com.bodyquest.workout.SquatWorkoutSession
import com.bodyquest.workout.WorkoutResultsStore
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The primary workout screen (spec §23): camera + skeleton dominate the top; form score,
 * per-metric breakdown, and the correction banner sit below. Only squat is wired end-to-end
 * for this checkpoint (push-up/curl land in Phase 7).
 */
@Composable
fun LiveCoachScreen(exerciseId: String, onBack: () -> Unit, onFinishSet: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val controller = remember { CameraController(context) }
    val poseSession = remember { PoseSession(context) }
    val workoutSession = remember { SquatWorkoutSession() }
    val voice = remember { CoachVoice(context) }
    val lastRepFlow = remember { MutableStateFlow<RepResult?>(null) }
    val lastRep by lastRepFlow.collectAsState()
    val pose by poseSession.result.collectAsState()

    LaunchedEffect(Unit) { poseSession.warmUp() }
    DisposableEffect(Unit) {
        onDispose {
            controller.stop()
            poseSession.close()
            voice.close()
        }
    }

    val repCount = lastRep?.repNumber ?: 0
    val latestSnapshot = lastRep?.formSnapshot
    // Whichever is freshest: a just-issued correction, or a just-verified one (shown until the next rep replaces it).
    val activeCorrection: CorrectionEvent? = lastRep?.correction ?: lastRep?.verifiedCorrection

    // Speak the same thing the banner shows — corrections and their verification are the
    // one thing this screen must say out loud (spec §15); everything else stays silent.
    LaunchedEffect(lastRep) {
        val rep = lastRep ?: return@LaunchedEffect
        // Verification first, then any new correction — QUEUE_FLUSH means the second speak()
        // wins, and the *current* instruction matters more in real time than the past one.
        rep.verifiedCorrection?.let { verified ->
            voice.speak(if (verified.improved == true) CoachingPhrases.IMPROVED else CoachingPhrases.NOT_YET_IMPROVED)
        }
        rep.correction?.let { voice.speak(it.instruction) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${exerciseId.replaceFirstChar { it.uppercase() }} · Rep $repCount") },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            controller.start(lifecycleOwner, previewView) { frame ->
                                val result = poseSession.onFrame(frame)
                                if (result != null) {
                                    val rep = workoutSession.onFrame(result, frame.timestampMs)
                                    if (rep != null) lastRepFlow.value = rep
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                SkeletonOverlay(pose = pose, modifier = Modifier.fillMaxSize())

                // The correction message is the visually dominant element on this screen (spec §13/§23).
                activeCorrection?.let { correction ->
                    CorrectionBanner(correction, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
                }
            }

            FormPanel(
                snapshot = latestSnapshot,
                onPause = {},
                onStop = {
                    WorkoutResultsStore.lastSession = workoutSession.completedReps
                    WorkoutResultsStore.lastExerciseId = exerciseId
                    onFinishSet()
                },
            )
        }
    }
}

@Composable
private fun CorrectionBanner(correction: CorrectionEvent, modifier: Modifier = Modifier) {
    val verified = correction.improved
    val (bg, text) = when (verified) {
        true -> BqGood to "${(correction.beforeScore * 100).toInt()}% → ${(correction.afterScore!! * 100).toInt()}%  ·  Good. Your ${correction.issue.metricLabel.lowercase()} improved."
        false -> BqWarn to correction.instruction
        null -> BqWarn to correction.instruction
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg.copy(alpha = 0.95f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge.copy(color = Color.Black))
    }
}

@Composable
private fun FormPanel(snapshot: SquatFormSnapshot?, onPause: () -> Unit, onStop: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(BqSurface).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("FORM SCORE", style = MaterialTheme.typography.labelLarge, color = BqTextSecondary)
            Spacer(Modifier.weight(1f))
            Text(
                snapshot?.formScore?.let { "${(it * 100).toInt()}%" } ?: "--",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        Spacer(Modifier.height(12.dp))
        MetricRow("Depth", snapshot?.depth)
        MetricRow("Alignment", snapshot?.kneeAlignment)
        MetricRow("Torso", snapshot?.torsoStability)
        MetricRow("Tempo", snapshot?.tempo)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f)) { Text("PAUSE") }
            Button(onClick = onStop, modifier = Modifier.weight(1f)) { Text("STOP") }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: Float?) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        val color = when {
            value == null -> BqTextSecondary
            value >= 0.85f -> BqGood
            value >= 0.7f -> BqWarn
            else -> BqBad
        }
        val mark = when {
            value == null -> ""
            value >= 0.85f -> "✓"
            else -> "⚠"
        }
        Text(
            (value?.let { "${(it * 100).toInt()}%" } ?: "--") + (if (mark.isNotEmpty()) " $mark" else ""),
            style = MaterialTheme.typography.titleLarge,
            color = color,
        )
    }
}
