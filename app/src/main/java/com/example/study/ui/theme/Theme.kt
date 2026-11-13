package com.example.study.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = DuoGreen,
    onPrimary = Color.White,
    primaryContainer = DuoGreenSoft,
    onPrimaryContainer = DuoGreenShadow,
    secondary = DuoBlue,
    onSecondary = Color.White,
    tertiary = DuoGold,
    onTertiary = LightDuoColors.textPrimary,
    error = DuoRed,
    onError = Color.White,
    background = LightDuoColors.background,
    onBackground = LightDuoColors.textPrimary,
    surface = LightDuoColors.background,
    onSurface = LightDuoColors.textPrimary,
    surfaceVariant = LightDuoColors.surface,
    onSurfaceVariant = LightDuoColors.textSecondary,
    outline = LightDuoColors.border
)

private val DarkColorScheme = darkColorScheme(
    primary = DuoGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A00),
    onPrimaryContainer = DuoGreenSoft,
    secondary = DuoBlue,
    onSecondary = Color.White,
    tertiary = DuoGold,
    onTertiary = Color(0xFF221A00),
    error = DuoRed,
    onError = Color.White,
    background = DarkDuoColors.background,
    onBackground = DarkDuoColors.textPrimary,
    surface = DarkDuoColors.background,
    onSurface = DarkDuoColors.textPrimary,
    surfaceVariant = DarkDuoColors.surface,
    onSurfaceVariant = DarkDuoColors.textSecondary,
    outline = DarkDuoColors.border
)

@Composable
fun StudyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkDuoColors else LightDuoColors
    val view = LocalView.current

    CompositionLocalProvider(LocalDuoColors provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = StudyTypography,
            shapes = StudyShapes,
            content = content
        )
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
}
