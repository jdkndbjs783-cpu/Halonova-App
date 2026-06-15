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

private val DarkSlateColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color.Black,
    secondary = Color(0xFF94A3B8),
    onSecondary = Color.White,
    tertiary = Color(0xFF10B981),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569)
)

private val CyberPinkColorScheme = darkColorScheme(
    primary = CyberNeonMagenta,
    onPrimary = Color.White,
    secondary = CyberNeonCyan,
    onSecondary = Color.Black,
    tertiary = CyberNeonGreen,
    background = CyberDarkBg,
    onBackground = TextPrimaryDark,
    surface = CyberDarkCard,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF3E1335),
    onSurfaceVariant = TextSecondaryDark,
    outline = CyberNeonMagenta
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
    themeMode: String = "cyber",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        "dark" -> DarkSlateColorScheme
        "cyber_pink" -> CyberPinkColorScheme
        else -> CyberDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
