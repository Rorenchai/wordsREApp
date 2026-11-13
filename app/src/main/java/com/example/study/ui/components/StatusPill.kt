package com.example.study.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.study.data.db.StudyStatus
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoGreenSoft
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextSecondary

/** 三档学习状态徽章：未学习 / 学习中 / 已掌握。 */
@Composable
fun StatusPill(status: Int, modifier: Modifier = Modifier) {
    val label: String
    val background: Color
    val foreground: Color
    when (status) {
        StudyStatus.MASTERED -> {
            label = "已掌握"
            background = DuoGreenSoft
            foreground = DuoGreen
        }
        StudyStatus.LEARNING -> {
            label = "学习中"
            background = DuoBlue.copy(alpha = 0.12f)
            foreground = DuoBlue
        }
        else -> {
            label = "未学习"
            background = DuoSurface
            foreground = DuoTextSecondary
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .border(1.dp, foreground.copy(alpha = 0.35f), RoundedCornerShape(percent = 50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = foreground)
    }
}
