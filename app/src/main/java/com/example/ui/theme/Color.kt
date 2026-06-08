package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Premium Theme Color Container
class AppThemeColors(
    val bg: Color,
    val cardBg: Color,
    val searchBg: Color,
    val activeCardBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color
)

val DarkThemeColors = AppThemeColors(
    bg = Color(0xFF121212),
    cardBg = Color(0xFF1E1E1E),
    searchBg = Color(0xFF2C2C2C),
    activeCardBg = Color(0x26C2185B), // 15% opacity
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB0B0B0),
    textMuted = Color(0xFF666666)
)

val LightThemeColors = AppThemeColors(
    bg = Color(0xFFF5F5F7),
    cardBg = Color(0xFFFFFFFF),
    searchBg = Color(0xFFE2E2E5),
    activeCardBg = Color(0x1CC2185B), // Elegant soft pink tint
    textPrimary = Color(0xFF000000),
    textSecondary = Color(0xFF000000),
    textMuted = Color(0xFF000000)
)

val LocalAppThemeColors = staticCompositionLocalOf { DarkThemeColors }

// Dynamic visual hooks mapped to elements
val DarkBg @Composable get() = LocalAppThemeColors.current.bg
val CardBg @Composable get() = LocalAppThemeColors.current.cardBg
val SearchBg @Composable get() = LocalAppThemeColors.current.searchBg
val ActiveCardBg @Composable get() = LocalAppThemeColors.current.activeCardBg

val PrimaryPink = Color(0xFFE91E63)
val SecondaryPink = Color(0xFFC2185B)
val LightPink = Color(0xFFF06292)

val TextPrimary @Composable get() = LocalAppThemeColors.current.textPrimary
val TextSecondary @Composable get() = LocalAppThemeColors.current.textSecondary
val TextMuted @Composable get() = LocalAppThemeColors.current.textMuted
