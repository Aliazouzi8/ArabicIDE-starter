package com.aliazouzi.arabicide.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object IdeColors {
    val Background = Color(0xFF1E1E1E)
    val Panel = Color(0xFF181818)
    val Surface = Color(0xFF252526)
    val SurfaceHigh = Color(0xFF2D2D30)
    val Border = Color(0xFF3C3C3C)
    val Accent = Color(0xFF4FA3FF)
    val Green = Color(0xFF4CC38A)
    val Red = Color(0xFFFF6B6B)
    val Yellow = Color(0xFFE5C07B)
    val Orange = Color(0xFFE8915A)
    val Purple = Color(0xFFB48EF0)
    val TextPrimary = Color(0xFFE6E6E6)
    val TextSecondary = Color(0xFF9DA5B4)
}

private val IdeColorScheme = darkColorScheme(
    primary = IdeColors.Accent,
    onPrimary = Color(0xFF002B52),
    primaryContainer = Color(0xFF0E4A82),
    onPrimaryContainer = Color(0xFFD6E8FF),
    secondary = IdeColors.Green,
    onSecondary = Color(0xFF00281A),
    background = IdeColors.Background,
    onBackground = IdeColors.TextPrimary,
    surface = IdeColors.Surface,
    onSurface = IdeColors.TextPrimary,
    surfaceVariant = IdeColors.SurfaceHigh,
    onSurfaceVariant = IdeColors.TextSecondary,
    surfaceContainer = IdeColors.Surface,
    surfaceContainerLow = IdeColors.Surface,
    surfaceContainerHigh = IdeColors.SurfaceHigh,
    surfaceContainerHighest = IdeColors.SurfaceHigh,
    outline = IdeColors.Border,
    outlineVariant = IdeColors.Border,
    error = IdeColors.Red,
)

@Composable
fun ArabicIDETheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = IdeColorScheme, content = content)
}
