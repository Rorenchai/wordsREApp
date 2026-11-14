package com.example.study.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.rememberTextMeasurer

/**
 * 单词标题：强制单行显示，按可用宽度自动缩小字号，保证所有字母都在同一行内完整显示。
 *
 * 实现方式：先用基础字号测量整词宽度，若超过卡片可用宽度，再按比例缩小字号后重新绘制，
 * 避免长单词换行或末尾被截断。
 */
@Composable
fun SingleLineWord(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidth = constraints.maxWidth
        val textMeasurer = rememberTextMeasurer()
        val baseFontSize = style.fontSize
        val measured = textMeasurer.measure(
            text = AnnotatedString(text),
            style = style.copy(fontSize = baseFontSize),
            maxLines = 1,
            softWrap = false
        )
        val measuredWidth = measured.size.width.coerceAtLeast(1)
        val scale = if (availableWidth > 0 && measuredWidth > availableWidth) {
            availableWidth.toFloat() / measuredWidth
        } else {
            1f
        }
        // 最少保留 8sp，保证极长单词也能全部显示在同一行
        val fittedSize = (baseFontSize.value * scale).coerceAtLeast(8f).sp
        BasicText(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            style = style.copy(
                color = color,
                fontSize = fittedSize,
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}
