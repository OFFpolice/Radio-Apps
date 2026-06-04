package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.example.ui.AppThemeSetting

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPink,
    secondary = SecondaryPink,
    tertiary = LightPink,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPink,
    secondary = SecondaryPink,
    tertiary = LightPink,
    background = Color(0xFFF5F5F7),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000)
)

@Composable
fun MyApplicationTheme(
    themeSetting: AppThemeSetting = AppThemeSetting.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeSetting) {
        AppThemeSetting.SYSTEM -> isSystemInDarkTheme()
        AppThemeSetting.LIGHT -> false
        AppThemeSetting.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colors = if (darkTheme) DarkThemeColors else LightThemeColors

    CompositionLocalProvider(
        LocalAppThemeColors provides colors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
