package com.huinatete.touchtuner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.huinatete.touchtuner.data.DefaultPalette
import com.huinatete.touchtuner.data.ThemePalette

val LocalTouchTunerColors = staticCompositionLocalOf { DefaultPalette }

val ErrorRed = Color(0xFFFF6B6B)
val WarningAmber = Color(0xFFF5C542)

private val AppTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.sp
    )
)

@Composable
fun TouchTunerTheme(
    palette: ThemePalette,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = palette.accent,
        onPrimary = palette.onAccent,
        primaryContainer = palette.accentSoft,
        onPrimaryContainer = palette.text,
        inversePrimary = palette.accent,
        secondary = palette.accent,
        onSecondary = palette.onAccent,
        secondaryContainer = palette.accentSoft,
        onSecondaryContainer = palette.text,
        tertiary = palette.accent,
        onTertiary = palette.onAccent,
        background = palette.background,
        onBackground = palette.text,
        surface = palette.card,
        onSurface = palette.text,
        surfaceVariant = palette.card,
        onSurfaceVariant = palette.muted,
        surfaceTint = palette.accent,
        inverseSurface = palette.text,
        inverseOnSurface = palette.background,
        error = ErrorRed,
        onError = Color.White,
        errorContainer = ErrorRed.copy(alpha = 0.2f),
        onErrorContainer = palette.text,
        outline = palette.border,
        outlineVariant = palette.border,
        scrim = Color.Black,
        surfaceBright = palette.card,
        surfaceDim = palette.background,
        surfaceContainer = palette.card,
        surfaceContainerHigh = palette.card,
        surfaceContainerHighest = palette.card,
        surfaceContainerLow = palette.card,
        surfaceContainerLowest = palette.background
    )

    androidx.compose.runtime.CompositionLocalProvider(LocalTouchTunerColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
