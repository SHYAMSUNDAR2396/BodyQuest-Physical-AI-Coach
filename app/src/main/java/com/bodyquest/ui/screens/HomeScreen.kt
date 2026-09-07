package com.bodyquest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bodyquest.ui.theme.BqAccent
import com.bodyquest.ui.theme.BqSurface
import com.bodyquest.ui.theme.BqTextSecondary

@Composable
fun HomeScreen(
    onStartWorkout: () -> Unit,
    onProgress: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BodyQuest") },
                actions = {
                    IconButton(onClick = onProfile) { Text("👤") }
                    IconButton(onClick = onSettings) { Text("⚙") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
        ) {
            Text("No sessions yet", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Complete a workout to see your streak and form score here.",
                style = MaterialTheme.typography.bodyMedium,
                color = BqTextSecondary,
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BqAccent)
                    .clickable(onClick = onStartWorkout),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Text(
                    "QUICK START",
                    style = MaterialTheme.typography.titleLarge.copy(color = androidx.compose.ui.graphics.Color.Black),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text("Progress", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BqSurface)
                    .clickable(onClick = onProgress),
                contentAlignment = androidx.compose.ui.Alignment.CenterStart,
            ) {
                Text("  View progress trends →", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
