package com.example.study.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Creamy Kawaii 品牌色板：奶油金 / 杏仁白 / 蜜桃粉 / 可可深棕
val CreamGold = Color(0xFFD9BB59)
val CreamGoldDark = Color(0xFFB99A3F)
val CreamGoldSoft = Color(0xFFF7EBC4)
val CreamIvory = Color(0xFFFFF8E8)
val CreamCard = Color(0xFFFFFCF4)
val PeachPink = Color(0xFFF8D9CC)
val PeachPinkDark = Color(0xFFE4BFAE)
val Caramel = Color(0xFFB99578)
val CaramelDark = Color(0xFF9A7A60)
val Chocolate = Color(0xFF57483B)
val OatBorder = Color(0xFFEADCBF)
val OatGray = Color(0xFFD8CBB7)
val MutedBrown = Color(0xFF9B8977)
val Sage = Color(0xFF91A88B)
val SageDark = Color(0xFF5F7A59)
val SageSoft = Color(0xFFEAF0E6)
val Coral = Color(0xFFD98978)
val CoralDark = Color(0xFFB96F61)

// 深色模式：保留奶油金的品牌识别度，不做简单反色
val DarkCreamBackground = Color(0xFF292522)
val DarkCreamCard = Color(0xFF39322D)
val DarkCreamBorder = Color(0xFF51463D)
val DarkCreamText = Color(0xFFFFF4DF)
val DarkCreamTextSecondary = Color(0xFFC0AE99)
val DarkCreamPink = Color(0xFFC99280)

// 兼容现有 UI 的语义别名：主品牌色由主题色预设动态决定
val DuoGreen: Color
    @Composable get() = LocalDuoColors.current.accent
val DuoGreenShadow: Color
    @Composable get() = LocalDuoColors.current.accentShadow
val DuoGreenSoft: Color
    @Composable get() = LocalDuoColors.current.accentSoft
val DuoRed = Coral
val DuoRedShadow = CoralDark
val DuoBlue = Caramel
val DuoBlueShadow = CaramelDark
val DuoGold = CreamGold
val DuoGoldSoft = CreamGoldSoft

/** 可选全局主题色预设。 */
data class AccentPreset(
    val id: String,
    val label: String,
    val primary: Color,
    val shadow: Color,
    val soft: Color
)

object AccentPresets {
    const val DEFAULT_ID = "cream"

    val all = listOf(
        AccentPreset("cream", "奶油金", CreamGold, CreamGoldDark, CreamGoldSoft),
        AccentPreset("peach", "蜜桃粉", Color(0xFFE7A58F), Color(0xFFC98670), Color(0xFFFBE4DC)),
        AccentPreset("sage", "鼠尾草", Sage, Color(0xFF718A6B), SageSoft),
        AccentPreset("sky", "海盐蓝", Color(0xFF7FA9C9), Color(0xFF6287A6), Color(0xFFE3EEF7)),
        AccentPreset("lavender", "薰衣草", Color(0xFFB49AC8), Color(0xFF9278A8), Color(0xFFF0E8F7))
    )

    fun byId(id: String): AccentPreset = all.firstOrNull { it.id == id } ?: all.first()
}

/** 学习状态的辅助色：未学习 / 学习中 / 已掌握。 */
val DuoSuccess = Sage
val DuoSuccessShadow = Color(0xFF718A6B)
val DuoSuccessSoft = SageSoft
val DuoSuccessText = SageDark
val DuoLearning = CreamGold
val DuoLearningSoft = CreamGoldSoft
val DuoNeutral = OatGray
val DuoPeach = PeachPink
val DuoPeachShadow = PeachPinkDark
val DuoChocolate = Chocolate

/** 随深浅色模式切换的主题色。 */
data class DuoColors(
    val background: Color,
    val card: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean,
    val accent: Color = CreamGold,
    val accentShadow: Color = CreamGoldDark,
    val accentSoft: Color = CreamGoldSoft
)

val LightDuoColors = DuoColors(
    background = CreamIvory,
    card = CreamCard,
    surface = Color(0xFFF7EDDA),
    border = OatBorder,
    textPrimary = Chocolate,
    textSecondary = MutedBrown,
    isDark = false
)

val DarkDuoColors = DuoColors(
    background = DarkCreamBackground,
    card = DarkCreamCard,
    surface = Color(0xFF332D28),
    border = DarkCreamBorder,
    textPrimary = DarkCreamText,
    textSecondary = DarkCreamTextSecondary,
    isDark = true
)

val LocalDuoColors = staticCompositionLocalOf { LightDuoColors }

/** 页面背景（杏仁白 / 深可可）。 */
val DuoBackground: Color
    @Composable get() = LocalDuoColors.current.background

/** 卡片背景（象牙奶油白 / 深色卡片）。 */
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
