package com.bodyquest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

// BodyQuest identity: near-black ground, cyan AI accent, semantic states.
val BqBackground = Color(0xFF0A0B0D)
val BqSurface = Color(0xFF15171A)
val BqSurfaceElevated = Color(0xFF1D2024)
val BqTextPrimary = Color(0xFFF5F6F7)
val BqTextSecondary = Color(0xFF9AA0A6)
val BqAccent = Color(0xFF22D3EE)
val BqGood = Color(0xFF34D399)
val BqWarn = Color(0xFFFBBF24)
val BqBad = Color(0xFFF87171)

private val BqColorScheme = darkColorScheme(
    background = BqBackground,
    surface = BqSurface,
    surfaceVariant = BqSurfaceElevated,
    primary = BqAccent,
    onPrimary = BqBackground,
    onBackground = BqTextPrimary,
    onSurface = BqTextPrimary,
    secondary = BqGood,
    error = BqBad,
)

private val BqTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, color = BqTextPrimary),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = BqTextPrimary),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = BqTextPrimary),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, color = BqTextPrimary),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, color = BqTextSecondary),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, color = BqTextPrimary),
)

@Composable
fun BodyQuestTheme(content: @Composable () -> Unit) {
    // ponytail: dark-only identity by product design (spec §22); add light mode if requested.
    MaterialTheme(colorScheme = BqColorScheme, typography = BqTypography, content = content)
}
