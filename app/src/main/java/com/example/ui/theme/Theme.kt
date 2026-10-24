package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = StockGain,
    onPrimary = Color(0xFF003918),
    primaryContainer = StockGainBg,
    onPrimaryContainer = Color(0xFF74FF9B),
    secondary = StockAccent,
    onSecondary = Color.White,
    secondaryContainer = StockAccentSubtle,
    onSecondaryContainer = Color(0xFFB4D2FF),
    tertiary = StockLoss,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF008945),
    onPrimary = Color.White,
    secondary = Color(0xFF1E60D0),
    onSecondary = Color.White,
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF1A1F2C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1F2C),
    surfaceVariant = Color(0xFFE9EEF5),
    onSurfaceVariant = Color(0xFF556070),
    outline = Color(0xFFD3DCE6)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Stock traders heavily prefer sleek dark mode
    dynamicColor: Boolean = false, // Keep high-contrast fintech branding
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
