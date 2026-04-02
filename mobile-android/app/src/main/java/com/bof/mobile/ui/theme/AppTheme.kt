package com.bof.mobile.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape

private val AppLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF0D6EFD),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E9FF),
    onPrimaryContainer = Color(0xFF001B3D),
    secondary = Color(0xFF0A7DDC),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6EBFF),
    onSecondaryContainer = Color(0xFF00294E),
    tertiary = Color(0xFF0FA38E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBFF6EA),
    onTertiaryContainer = Color(0xFF002A24),
    background = Color(0xFFF1F6FC),
    onBackground = Color(0xFF13213A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF13213A),
    surfaceVariant = Color(0xFFE7EDF6),
    onSurfaceVariant = Color(0xFF51617A),
    outline = Color(0xFF9DAEC2),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val AppDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF9CCBFF),
    onPrimary = Color(0xFF00315E),
    primaryContainer = Color(0xFF004982),
    onPrimaryContainer = Color(0xFFD3E6FF),
    secondary = Color(0xFF8BC8FF),
    onSecondary = Color(0xFF003259),
    secondaryContainer = Color(0xFF194B78),
    onSecondaryContainer = Color(0xFFD2E5FF),
    tertiary = Color(0xFF72E1CB),
    onTertiary = Color(0xFF003830),
    tertiaryContainer = Color(0xFF005146),
    onTertiaryContainer = Color(0xFFA1F7E7),
    background = Color(0xFF0C1421),
    onBackground = Color(0xFFE4ECF7),
    surface = Color(0xFF101A29),
    onSurface = Color(0xFFE4ECF7),
    surfaceVariant = Color(0xFF203046),
    onSurfaceVariant = Color(0xFFB8C5D8),
    outline = Color(0xFF7F8FA5),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 40.sp,
        lineHeight = 46.sp,
        fontWeight = FontWeight.Bold
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val BalanceCardStart = Color(0xFF0D6EFD)
val BalanceCardMid = Color(0xFF1A8DFF)
val BalanceCardEnd = Color(0xFF26C4D4)

@Composable
fun BankAppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) AppDarkColors else AppLightColors

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
