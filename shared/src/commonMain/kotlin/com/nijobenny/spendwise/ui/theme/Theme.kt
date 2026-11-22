package com.nijobenny.spendwise.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Premium dark palette: deep midnight base with violet → cyan accents. */
object SpendColors {
    val Background = Color(0xFF0A0A12)
    val Surface = Color(0xFF14141F)
    val SurfaceHigh = Color(0xFF1C1C2A)
    val Outline = Color(0xFF2A2A3C)
    val TextPrimary = Color(0xFFF5F5FA)
    val TextMuted = Color(0xFF9696AE)

    val Violet = Color(0xFF8B5CF6)
    val Indigo = Color(0xFF6366F1)
    val Cyan = Color(0xFF22D3EE)
    val Pink = Color(0xFFF472B6)
    val Rose = Color(0xFFFB7185)
    val Emerald = Color(0xFF34D399)

    val PrimaryGradient = Brush.linearGradient(listOf(Violet, Indigo, Cyan))
    val ButtonGradient = Brush.horizontalGradient(listOf(Violet, Indigo, Cyan))
}

private val DarkScheme = darkColorScheme(
    primary = SpendColors.Violet,
    onPrimary = Color.White,
    secondary = SpendColors.Cyan,
    onSecondary = Color(0xFF00161B),
    tertiary = SpendColors.Pink,
    background = SpendColors.Background,
    onBackground = SpendColors.TextPrimary,
    surface = SpendColors.Surface,
    onSurface = SpendColors.TextPrimary,
    surfaceVariant = SpendColors.SurfaceHigh,
    onSurfaceVariant = SpendColors.TextMuted,
    error = SpendColors.Rose,
    outline = SpendColors.Outline,
)

private val SpendTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 38.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.3.sp),
)

private val SpendShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun SpendWiseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = SpendTypography,
        shapes = SpendShapes,
        content = content,
    )
}
