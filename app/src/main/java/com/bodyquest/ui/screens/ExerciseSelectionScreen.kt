package com.bodyquest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bodyquest.ui.theme.BqSurface
import com.bodyquest.ui.theme.BqTextSecondary

private data class ExerciseOption(val id: String, val name: String, val description: String)

// The three exercises the plan makes exceptionally reliable (spec §9, §44).
private val exercises = listOf(
    ExerciseOption("squat", "Squat", "Depth, knee alignment, torso angle, tempo"),
    ExerciseOption("pushup", "Push-Up", "Body alignment, elbow path, depth"),
    ExerciseOption("bicep_curl", "Bicep Curl", "Elbow stability, range of motion, tempo"),
)

@Composable
fun ExerciseSelectionScreen(workoutId: String, onBack: () -> Unit, onSelect: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose an exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(exercises) { exercise ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BqSurface)
                        .clickable { onSelect(exercise.id) }
                        .padding(20.dp),
                ) {
                    Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(exercise.description, style = MaterialTheme.typography.bodyMedium, color = BqTextSecondary)
                }
            }
        }
    }
}
