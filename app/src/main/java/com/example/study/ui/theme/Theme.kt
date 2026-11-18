package com.example.study.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun lightScheme(preset: AccentPreset) = lightColorScheme(
    primary = preset.primary,
    onPrimary = Chocolate,
    primaryContainer = preset.soft,
    onPrimaryContainer = Chocolate,
    secondary = PeachPink,
    onSecondary = Chocolate,
    tertiary = Caramel,
    onTertiary = CreamIvory,
    error = Coral,
    onError = Chocolate,
    background = CreamIvory,
    onBackground = Chocolate,
    surface = CreamCard,
    onSurface = Chocolate,
    surfaceVariant = LightDuoColors.surface,
    onSurfaceVariant = MutedBrown,
    outline = OatBorder
)

private fun darkScheme(preset: AccentPreset) = darkColorScheme(
    primary = preset.primary,
    onPrimary = DarkCreamBackground,
    primaryContainer = DarkCreamBorder,
    onPrimaryContainer = DarkCreamText,
    secondary = DarkCreamPink,
    onSecondary = DarkCreamBackground,
    tertiary = preset.primary,
    onTertiary = DarkCreamBackground,
    error = Coral,
    onError = DarkCreamBackground,
    background = DarkCreamBackground,
    onBackground = DarkCreamText,
    surface = DarkCreamCard,
    onSurface = DarkCreamText,
    surfaceVariant = DarkDuoColors.surface,
    onSurfaceVariant = DarkCreamTextSecondary,
    outline = DarkCreamBorder
)

@Composable
fun StudyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentPreset: String = AccentPresets.DEFAULT_ID,
    content: @Composable () -> Unit
) {
    val preset = AccentPresets.byId(accentPreset)
    val base = if (darkTheme) DarkDuoColors else LightDuoColors
    val palette = base.copy(
        accent = preset.primary,
        accentShadow = preset.shadow,
        accentSoft = preset.soft,
        secondary = preset.secondary,
        secondaryShadow = preset.secondaryShadow
    )
    val view = LocalView.current

    CompositionLocalProvider(LocalDuoColors provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkScheme(preset) else lightScheme(preset),
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
