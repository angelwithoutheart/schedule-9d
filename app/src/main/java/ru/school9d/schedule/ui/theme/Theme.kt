package ru.school9d.schedule.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppPalette {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF1C1C1E)
    val SurfaceMuted = Color(0xFF252528)
    val SurfaceElevated = Color(0xFF2C2C2E)
    val Separator = Color(0xFF2C2C2E)
    val Accent = Color(0xFF0A84FF)
    val Green = Color(0xFF30D158)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8E8E93)
    val TextTertiary = Color(0xFF636366)
    val Danger = Color(0xFFFF453A)
    val DarkText = Color(0xFFFFFFFF)

    // Legacy aliases used by the share dialog.
    val Border = Separator
    val TextMuted = TextSecondary
}

private val DarkScheme = darkColorScheme(
    primary = AppPalette.Accent,
    onPrimary = Color.White,
    secondary = AppPalette.Accent,
    onSecondary = Color.White,
    background = AppPalette.Background,
    onBackground = AppPalette.TextPrimary,
    surface = AppPalette.Surface,
    onSurface = AppPalette.TextPrimary,
    surfaceVariant = AppPalette.SurfaceMuted,
    onSurfaceVariant = AppPalette.TextSecondary,
    outline = AppPalette.Separator,
    error = AppPalette.Danger
)

@Composable
fun Schedule9DTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = Typography(),
        content = content
    )
}
