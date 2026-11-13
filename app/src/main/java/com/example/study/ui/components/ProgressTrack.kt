package com.example.study.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoGreen

/** 多邻国风格的圆角进度条。 */
@Composable
fun ProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    fillColor: Color = DuoGreen,
    trackColor: Color = DuoBorder
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "progress-track"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(percent = 50))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated.coerceIn(0.0001f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(percent = 50))
                .background(fillColor)
        )
    }
}