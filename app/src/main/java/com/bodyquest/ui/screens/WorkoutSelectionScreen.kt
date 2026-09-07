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

private data class WorkoutOption(val id: String, val name: String, val description: String)

private val workouts = listOf(
    WorkoutOption("full_body", "Full Body", "Squat, push-up, and bicep curl in one session"),
    WorkoutOption("upper_body", "Upper Body", "Push-up and bicep curl"),
    WorkoutOption("lower_body", "Lower Body", "Squat"),
    WorkoutOption("custom", "Custom", "Pick your own exercises"),
)

@Composable
fun WorkoutSelectionScreen(onBack: () -> Unit, onSelect: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select a workout") },
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
            items(workouts) { workout ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BqSurface)
                        .clickable { onSelect(workout.id) }
                        .padding(20.dp),
                ) {
                    Text(workout.name, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(workout.description, style = MaterialTheme.typography.bodyMedium, color = BqTextSecondary)
                }
            }
        }
    }
}
