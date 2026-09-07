package com.bodyquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bodyquest.ui.theme.BqAccent
import com.bodyquest.ui.theme.BqTextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1200)
        onFinished()
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "BODYQUEST",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = BqAccent,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your body is the controller.",
                style = MaterialTheme.typography.bodyLarge,
                color = BqTextSecondary,
            )
        }
    }
}
