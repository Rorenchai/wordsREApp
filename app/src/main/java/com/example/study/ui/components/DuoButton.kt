package com.example.study.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBlueShadow
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoCard
import com.example.study.ui.theme.DuoGold
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoGreenShadow
import com.example.study.ui.theme.DuoRed
import com.example.study.ui.theme.DuoRedShadow
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextSecondary

/** 按钮语义色：主操作 / 正确(掌握) / 学习中 / 危险 / 次要。 */
enum class DuoButtonStyle { Primary, Correct, Learning, Danger, Secondary }

private data class ButtonColors(val face: Color, val shadow: Color)

@Composable
private fun colorsFor(style: DuoButtonStyle): ButtonColors = when (style) {
    DuoButtonStyle.Primary -> ButtonColors(DuoGreen, DuoGreenShadow)
    DuoButtonStyle.Correct -> ButtonColors(DuoGreen, DuoGreenShadow)
    DuoButtonStyle.Learning -> ButtonColors(DuoBlue, DuoBlueShadow)
    DuoButtonStyle.Danger -> ButtonColors(DuoRed, DuoRedShadow)
    DuoButtonStyle.Secondary -> ButtonColors(DuoCard, DuoBorder)
}

/**
 * 多邻国风格按钮：底部深色「3D 底边」+ 按下位移，全宽 16dp 圆角。
 */
@Composable
fun DuoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DuoButtonStyle = DuoButtonStyle.Primary,
    enabled: Boolean = true
) {
    val colors = colorsFor(style)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val offsetY by animateDpAsState(
        targetValue = if (pressed && enabled) 4.dp else 0.dp,
        label = "duo-button-press"
    )
    // 按下时整体轻微缩小，松开回弹，形成多邻国按钮的「软按压」手感
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "duo-button-scale"
    )
    val shape = RoundedCornerShape(16.dp)
    val faceColor = when {
        !enabled -> DuoSurface
        else -> colors.face
    }
    val shadowColor = when {
        !enabled -> DuoBorder
        else -> colors.shadow
    }
    val textColor = when {
        !enabled -> DuoTextSecondary
        style == DuoButtonStyle.Secondary -> DuoGreen
        else -> Color.White
    }

    Box(modifier = modifier.height(56.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .offset(y = 4.dp)
                .clip(shape)
                .background(shadowColor)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .offset(y = offsetY)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(shape)
                .background(faceColor)
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = textColor
            )
        }
    }
}

/** 金色数字/图标按钮的语义色，供其他组件复用。 */
val StreakGold: Color = DuoGold