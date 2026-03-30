package com.bof.mobile.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF005B8E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCBE7FF),
    onPrimaryContainer = Color(0xFF001D31),
    secondary = Color(0xFF006A63),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF97F2E7),
    onSecondaryContainer = Color(0xFF00201D),
    tertiary = Color(0xFF815400),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB1),
    onTertiaryContainer = Color(0xFF291800),
    background = Color(0xFFF8FAFD),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDCE3EA),
    onSurfaceVariant = Color(0xFF40484F),
    outline = Color(0xFF70787F),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

@Composable
fun BankAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppLightColors,
        typography = Typography(),
        content = content
    )
}
