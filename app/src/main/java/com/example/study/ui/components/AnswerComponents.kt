package com.example.study.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoCard
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoGreenSoft
import com.example.study.ui.theme.DuoRed
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary

/** 答题模式通用：会话统计（已答 / 答对 / 正确率）。 */
@Composable
fun SessionStatsCard(answered: Int, correct: Int, accuracy: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatText(label = "已答", value = "$answered 题")
        StatText(label = "答对", value = "$correct 题", color = DuoGreen)
        StatText(label = "正确率", value = "$accuracy%", color = DuoBlue)
    }
}

@Composable
private fun StatText(label: String, value: String, color: Color = DuoTextPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = color)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = DuoTextSecondary)
    }
}

/** 答题模式通用：选项行（对错着色 + 选中态）。 */
@Composable
fun AnswerOptionRow(
    text: String,
    index: Int,
    answered: Boolean,
    answerIndex: Int,
    selectedIndex: Int?,
    onClick: () -> Unit
) {
    val isCorrectOption = index == answerIndex
    val isSelected = selectedIndex == index

    // 按压缩放：选中时轻微缩小，松开回弹，增加点按手感
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && !answered) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "answer-option-press"
    )
    // 答对时轻微放大脉冲
    val correctPulse = remember { Animatable(1f) }
    // 答错选中项左右抖动
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(answered, isCorrectOption, isSelected) {
        if (!answered) return@LaunchedEffect
        if (isCorrectOption) {
            correctPulse.animateTo(1.035f, tween(durationMillis = 130))
            correctPulse.animateTo(1f, tween(durationMillis = 170))
        } else if (isSelected) {
            shakeOffset.animateTo(-6f, tween(durationMillis = 60))
            shakeOffset.animateTo(6f, tween(durationMillis = 80))
            shakeOffset.animateTo(-4f, tween(durationMillis = 70))
            shakeOffset.animateTo(2f, tween(durationMillis = 70))
            shakeOffset.animateTo(0f, tween(durationMillis = 60))
        }
    }

    val background: Color
    val borderColor: Color
    val textColor: Color
    when {
        !answered -> {
            background = DuoCard
            borderColor = DuoBorder
            textColor = DuoTextPrimary
        }

        isCorrectOption -> {
            background = DuoGreenSoft
            borderColor = DuoGreen
            textColor = DuoGreen
        }

        isSelected -> {
            background = DuoRed.copy(alpha = 0.10f)
            borderColor = DuoRed
            textColor = DuoRed
        }

        else -> {
            background = DuoCard
            borderColor = DuoBorder
            textColor = DuoTextSecondary
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .offset(x = shakeOffset.value.dp)
            .graphicsLayer {
                scaleX = pressScale * correctPulse.value
                scaleY = pressScale * correctPulse.value
            }
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interaction,
                enabled = !answered,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${'A' + index}",
                style = MaterialTheme.typography.labelMedium,
                color = borderColor,
                modifier = Modifier.width(22.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }
    }
}

/** 答题模式通用：判对错反馈横幅。 */
@Composable
fun AnswerFeedbackBanner(correct: Boolean, correctAnswer: String) {
    val color = if (correct) DuoGreen else DuoRed
    // 反馈条出现时做一次缩放 + 淡入，避免生硬弹出
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val enterAlpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "feedback-alpha"
    )
    val enterScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.94f,
        animationSpec = tween(durationMillis = 220),
        label = "feedback-scale"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = enterAlpha
                scaleX = enterScale
                scaleY = enterScale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(if (correct) DuoGreenSoft else DuoRed.copy(alpha = 0.10f))
            .border(2.dp, color, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (correct) "回答正确" else "回答错误",
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        if (!correct) {
            Text(
                text = "正确答案：$correctAnswer",
                style = MaterialTheme.typography.bodyMedium,
                color = DuoTextSecondary,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = "即将自动进入下一题…",
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary
        )
    }
}