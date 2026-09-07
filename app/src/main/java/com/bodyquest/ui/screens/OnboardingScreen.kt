package com.bodyquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bodyquest.ui.theme.BqTextSecondary

private data class OnboardingPoint(val title: String, val body: String)

private val points = listOf(
    OnboardingPoint(
        "Camera usage",
        "BodyQuest watches how you move using your phone's camera to track your body's pose in real time.",
    ),
    OnboardingPoint(
        "Privacy",
        "Your workout video stays on your device unless you explicitly enable a feature that requires sharing.",
    ),
    OnboardingPoint(
        "Positioning",
        "Stand far enough back that your whole body is visible, with steady lighting and a clear background.",
    ),
    OnboardingPoint(
        "Supported exercises",
        "Squats, push-ups, and bicep curls are fully coached today. More exercises are on the way.",
    ),
)

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    Scaffold(
        bottomBar = {
            Box(Modifier.fillMaxWidth().padding(16.dp)) {
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Before you start", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))
            points.forEach { point ->
                Text(point.title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(point.body, style = MaterialTheme.typography.bodyMedium, color = BqTextSecondary)
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
