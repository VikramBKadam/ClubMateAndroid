package com.clubmates.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Mirrors iOS AppTheme dark palette
val Background = Color(0xFF0A0A0F)
val Surface = Color(0xFF13131A)
val SurfaceVariant = Color(0xFF1C1C27)
val Primary = Color(0xFFE040FB)
val PrimaryVariant = Color(0xFFAA00FF)
val Secondary = Color(0xFF7C4DFF)
val Accent = Color(0xFFFF4081)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0B0C0)
val TextHint = Color(0xFF606070)
val Like = Color(0xFF4CAF50)
val Nope = Color(0xFFE53935)
val SuperLike = Color(0xFF2196F3)
val CardBackground = Color(0xFF1A1A24)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Accent,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF2A2A3A)
)

@Composable
fun ClubMatesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = ClubMatesTypography,
        content = content
    )
}
