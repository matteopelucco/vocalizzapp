package com.matteopelucco.vocalizzapp.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

// Palette ricavata dall'icona: blu elettrico (#1659F0) con accenti ciano.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1659F0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE7FF),
    onPrimaryContainer = Color(0xFF0B2A80),
    secondary = Color(0xFF0E9BD6),
    onSecondary = Color.White,
    background = Color(0xFFF5F7FC),
    onBackground = Color(0xFF101828),
    surface = Color.White,
    onSurface = Color(0xFF101828),
    surfaceVariant = Color(0xFFE8EDF8),
    onSurfaceVariant = Color(0xFF566074),
    outline = Color(0xFFB9C3D9),
    outlineVariant = Color(0xFFDCE3F1),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFCE8E6),
    onErrorContainer = Color(0xFF7A1B12),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8CAEFF),
    onPrimary = Color(0xFF052067),
    primaryContainer = Color(0xFF1B3FA0),
    onPrimaryContainer = Color(0xFFDDE7FF),
    secondary = Color(0xFF63D2FF),
    onSecondary = Color(0xFF00344A),
    background = Color(0xFF0B1020),
    onBackground = Color(0xFFE6EAF5),
    surface = Color(0xFF131A33),
    onSurface = Color(0xFFE6EAF5),
    surfaceVariant = Color(0xFF1D2542),
    onSurfaceVariant = Color(0xFFA6B0C8),
    outline = Color(0xFF55607C),
    outlineVariant = Color(0xFF283152),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF4A1A16),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 28.sp, letterSpacing = 0.1.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun VocalizzAppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

/** Colore per le avvertenze non bloccanti (es. "potrebbe essere lento"): ambra, leggibile in chiaro e scuro. */
@Composable
fun warningColor(darkTheme: Boolean = isSystemInDarkTheme()): Color =
    if (darkTheme) Color(0xFFFFB74D) else Color(0xFF9A5B00)
