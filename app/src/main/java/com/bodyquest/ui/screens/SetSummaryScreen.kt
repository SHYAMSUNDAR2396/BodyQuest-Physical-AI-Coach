package com.bodyquest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bodyquest.coach.CorrectionEvent
import com.bodyquest.ui.theme.*
import com.bodyquest.workout.RepResult
import com.bodyquest.workout.WorkoutResultsStore

/**
 * Set Summary (spec §21): reps, form, corrections, and the quality trend for the set just
 * finished. Reads whatever [WorkoutResultsStore] holds — real numbers from the session that
 * just ran, never placeholder data.
 */
@Composable
fun SetSummaryScreen(onDone: () -> Unit) {
    val reps = WorkoutResultsStore.lastSession
    val exerciseId = WorkoutResultsStore.lastExerciseId
    val scores = reps.mapNotNull { it.formSnapshot.formScore }
    val avgScore = scores.takeIf { it.isNotEmpty() }?.average()?.toFloat()
    val corrections = reps.mapNotNull { it.correction ?: it.verifiedCorrection }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Set Summary · ${exerciseId.replaceFirstChar { it.uppercase() }}") }) },
        bottomBar = {
            Box(Modifier.fillMaxWidth().padding(16.dp)) {
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        },
    ) { padding ->
        if (reps.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No reps recorded for this set.", color = BqTextSecondary)
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatTile("REPS", "${reps.size}", modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    StatTile("AVG FORM", avgScore?.let { "${(it * 100).toInt()}%" } ?: "--", modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    StatTile("CORRECTIONS", "${corrections.size}", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(24.dp))
                Text("Form quality by rep", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
            }
            items(reps) { rep -> RepTimelineRow(rep) }

            if (corrections.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Text("Corrections", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                }
                items(corrections) { correction -> CorrectionRow(correction) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(BqSurface).padding(16.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = BqTextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun RepTimelineRow(rep: RepResult) {
    val score = rep.formSnapshot.formScore
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("REP ${rep.repNumber}", style = MaterialTheme.typography.bodyMedium, color = BqTextSecondary, modifier = Modifier.width(64.dp))
        Box(modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(BqSurface)) {
            val fraction = (score ?: 0f).coerceIn(0f, 1f)
            val color = when {
                score == null -> BqTextSecondary
                score >= 0.85f -> BqGood
                score >= 0.7f -> BqWarn
                else -> BqBad
            }
            Box(
                modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(RoundedCornerShape(5.dp)).background(color),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(score?.let { "${(it * 100).toInt()}%" } ?: "--", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(48.dp))
    }
}

@Composable
private fun CorrectionRow(correction: CorrectionEvent) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(BqSurface).padding(16.dp),
    ) {
        Text(correction.issue.metricLabel, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(correction.instruction, style = MaterialTheme.typography.bodyMedium, color = BqTextSecondary)
        Spacer(Modifier.height(8.dp))
        val afterScore = correction.afterScore
        if (afterScore != null) {
            val color = if (correction.improved == true) BqGood else BqBad
            Text(
                "${(correction.beforeScore * 100).toInt()}% → ${(afterScore * 100).toInt()}%" +
                    (if (correction.improved == true) "  ·  Improved" else "  ·  Not yet improved"),
                style = MaterialTheme.typography.bodyLarge,
                color = color,
            )
        } else {
            Text("${(correction.beforeScore * 100).toInt()}%  ·  Awaiting verification", style = MaterialTheme.typography.bodyLarge, color = BqTextSecondary)
        }
    }
}
