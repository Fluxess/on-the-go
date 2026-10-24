package ru.routealert.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import ru.routealert.app.data.ThemeMode

val Ink = Color(0xFF111111)
val Paper = Color(0xFFF2F3F7)
val Signal = Color(0xFFE10600)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1C1C1E),
    onPrimary = Color.White,
    secondary = Color(0xFF3A3A3C),
    onSecondary = Color.White,
    background = Color(0xFFE8EAF0),
    onBackground = Ink,
    surface = Color.White.copy(alpha = 0.72f),
    onSurface = Ink,
    surfaceVariant = Color(0xFFD8DCE6),
    onSurfaceVariant = Color(0xFF5C5C62),
    error = Signal,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF4F4F6),
    onPrimary = Ink,
    secondary = Color(0xFFC8C8CE),
    onSecondary = Ink,
    background = Color(0xFF0B0B0D),
    onBackground = Color(0xFFF4F4F6),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF4F4F6),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFB5B5BA),
    error = Signal,
    onError = Color.White
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp,
        lineBreak = androidx.compose.ui.text.style.LineBreak.Heading
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        letterSpacing = (-0.3).sp,
        lineBreak = androidx.compose.ui.text.style.LineBreak.Heading
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineBreak = androidx.compose.ui.text.style.LineBreak.Heading
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineBreak = androidx.compose.ui.text.style.LineBreak.Heading
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineBreak = androidx.compose.ui.text.style.LineBreak.Paragraph
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineBreak = androidx.compose.ui.text.style.LineBreak.Paragraph
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineBreak = androidx.compose.ui.text.style.LineBreak.Simple
    )
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun RouteAlertTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
