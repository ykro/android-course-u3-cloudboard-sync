package com.awa2027.cloudboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Solo modo claro.
private val LightColors =
    lightColorScheme(
        primary = Color(0xFF1D4ED8),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF0F766E),
        background = Color(0xFFF8FAFC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF1F5F9),
        error = Color(0xFFB91C1C),
        // Los snackbars usan los colores "inverse": por defecto son oscuros. Aquí también son claros.
        inverseSurface = Color(0xFFE2E8F0),
        inverseOnSurface = Color(0xFF0F172A),
        inversePrimary = Color(0xFF1D4ED8),
    )

@Composable
fun CloudBoardTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
