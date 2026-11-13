package com.example.study.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoTextSecondary

/** 柱状图的一根柱子。 */
data class DayBar(val label: String, val count: Int)

/** 最近 7 天学习柱状图（Compose Canvas）。 */
@Composable
fun WeekBarChart(
    bars: List<DayBar>,
    modifier: Modifier = Modifier,
    barColor: Color = DuoGreen
) {
    val maxCount = (bars.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            if (bars.isEmpty()) return@Canvas
            val slotWidth = size.width / bars.size
            val barWidth = slotWidth * 0.5f
            val baseline = size.height
            bars.forEachIndexed { index, bar ->
                val heightRatio = bar.count.toFloat() / maxCount.toFloat()
                val barHeight = (size.height - 6.dp.toPx()) * heightRatio
                val left = slotWidth * index + (slotWidth - barWidth) / 2f
                val top = baseline - barHeight
                drawRoundRect(
                    color = if (bar.count == 0) DuoBorder else barColor,
                    topLeft = Offset(left, if (bar.count == 0) baseline - 4.dp.toPx() else top),
                    size = Size(barWidth, if (bar.count == 0) 4.dp.toPx() else barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            bars.forEach { bar ->
                Text(
                    text = bar.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = DuoTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
