package ru.school9d.schedule.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppPalette {
    val Background = Color(0xFF0F1118)
    val Surface = Color(0xFF161821)
    val SurfaceElevated = Color(0xFF232736)
    val Border = Color(0xFF2E3346)
    val Accent = Color(0xFF5CA8FF)
    val AccentSecondary = Color(0xFF8B7BFF)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextMuted = Color(0xFF8E92A4)
    val Danger = Color(0xFFFF8A80)
    val DarkText = Color(0xFF0B1220)
}

private val DarkScheme = darkColorScheme(
    primary = AppPalette.Accent,
    onPrimary = AppPalette.DarkText,
    secondary = AppPalette.AccentSecondary,
    onSecondary = AppPalette.DarkText,
    background = AppPalette.Background,
    onBackground = AppPalette.TextPrimary,
    surface = AppPalette.Surface,
    onSurface = AppPalette.TextPrimary,
    surfaceVariant = AppPalette.SurfaceElevated,
    onSurfaceVariant = AppPalette.TextMuted,
    outline = AppPalette.Border,
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
