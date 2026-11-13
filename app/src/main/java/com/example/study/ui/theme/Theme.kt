package com.example.study.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudyColorScheme = lightColorScheme(
    primary = DuoGreen,
    onPrimary = Color.White,
    primaryContainer = DuoGreenSoft,
    onPrimaryContainer = DuoGreenShadow,
    secondary = DuoBlue,
    onSecondary = Color.White,
    tertiary = DuoGold,
    onTertiary = DuoTextPrimary,
    error = DuoRed,
    onError = Color.White,
    background = DuoBackground,
    onBackground = DuoTextPrimary,
    surface = DuoBackground,
    onSurface = DuoTextPrimary,
    surfaceVariant = DuoSurface,
    onSurfaceVariant = DuoTextSecondary,
    outline = DuoBorder
)

@Composable
fun StudyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StudyColorScheme,
        typography = StudyTypography,
        shapes = StudyShapes,
        content = content
    )
}
