package com.qrforge.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
// Convenience color reference inside composables
import androidx.compose.ui.graphics.Color
private val DarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = DarkBackground,
    primaryContainer = DarkSurface2,
    onPrimaryContainer = AccentGreen,
    secondary = AccentViolet,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurface3,
    onSecondaryContainer = AccentViolet,
    tertiary = AccentBlue,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = DarkMuted,
    outline = DarkBorder,
    outlineVariant = DarkSurface3,
    error = AccentRed,
    onError = DarkBackground,
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00B87A),
    onPrimary = LightBackground,
    primaryContainer = Color(0xFFE0FFF4),
    onPrimaryContainer = Color(0xFF00663D),
    secondary = AccentViolet,
    onSecondary = LightBackground,
    secondaryContainer = Color(0xFFEEEDFF),
    onSecondaryContainer = Color(0xFF3D38AA),
    tertiary = AccentBlue,
    onTertiary = LightBackground,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurface2,
    onSurfaceVariant = LightMuted,
    outline = LightBorder,
    outlineVariant = Color(0xFFF0F0FA),
    error = AccentRed,
    onError = LightBackground,
)

@Composable
fun QRForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QRForgeTypography,
        content = content
    )
}


