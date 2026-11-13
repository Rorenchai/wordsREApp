package com.example.study.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = !answered, onClick = onClick)
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
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