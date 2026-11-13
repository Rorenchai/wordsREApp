package com.example.study.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// 多邻国风格品牌色（深浅色模式通用）
val DuoGreen = Color(0xFF58CC02)
val DuoGreenShadow = Color(0xFF46A302)
val DuoGreenSoft = Color(0xFFE7F8D8)
val DuoRed = Color(0xFFFF4B4B)
val DuoRedShadow = Color(0xFFD33131)
val DuoBlue = Color(0xFF1CB0F6)
val DuoBlueShadow = Color(0xFF1387BD)
val DuoGold = Color(0xFFFFC800)
val DuoGoldSoft = Color(0xFFFFF4CC)

/** 随深浅色模式切换的主题色。 */
data class DuoColors(
    val background: Color,
    val card: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean
)

val LightDuoColors = DuoColors(
    background = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    surface = Color(0xFFF7F7F7),
    border = Color(0xFFE5E5E5),
    textPrimary = Color(0xFF3C3C3C),
    textSecondary = Color(0xFF777777),
    isDark = false
)

val DarkDuoColors = DuoColors(
    background = Color(0xFF121212),
    card = Color(0xFF1F1F1F),
    surface = Color(0xFF262626),
    border = Color(0xFF3A3A3A),
    textPrimary = Color(0xFFEDEDED),
    textSecondary = Color(0xFFA0A0A0),
    isDark = true
)

val LocalDuoColors = staticCompositionLocalOf { LightDuoColors }

/** 页面背景（白 / 深灰）。 */
val DuoBackground: Color
    @Composable get() = LocalDuoColors.current.background

/** 卡片背景（白 / 深色卡片）。 */
val DuoCard: Color
    @Composable get() = LocalDuoColors.current.card

val DuoSurface: Color
    @Composable get() = LocalDuoColors.current.surface

val DuoBorder: Color
    @Composable get() = LocalDuoColors.current.border

val DuoTextPrimary: Color
    @Composable get() = LocalDuoColors.current.textPrimary

val DuoTextSecondary: Color
    @Composable get() = LocalDuoColors.current.textSecondary
