package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberDarkColorScheme = darkColorScheme(
    primary = CyberNeonCyan,
    onPrimary = Color.Black,
    secondary = CyberNeonMagenta,
    onSecondary = Color.White,
    tertiary = CyberNeonGreen,
    background = CyberDarkBg,
    onBackground = TextPrimaryDark,
    surface = CyberDarkCard,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF16225B),
    onSurfaceVariant = TextSecondaryDark,
    outline = CyberNeonCyan
)

private val CyberLightColorScheme = lightColorScheme(
    primary = CyberLightPrimary,
    onPrimary = Color.White,
    secondary = CyberNeonMagenta,
    onSecondary = Color.White,
    tertiary = CyberNeonGreen,
    background = CyberLightBg,
    onBackground = TextPrimaryLight,
    surface = CyberLightCard,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = TextSecondaryLight,
    outline = CyberLightPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        CyberDarkColorScheme
    } else {
        CyberLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
