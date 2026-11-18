package com.example.study.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoCard
import com.example.study.ui.theme.DuoGold
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoRed
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary
import kotlinx.coroutines.delay

/**
 * 轻量奖励反馈：星星弹入 + 彩带粒子，用于已掌握、小游戏里程碑/过关等正向节点。
 * 不拦截触摸，1.2 秒后自动结束。
 */
@Composable
fun RewardBurst(
    visible: Boolean,
    title: String,
    subtitle: String? = null,
    onFinished: () -> Unit = {}
) {
    if (!visible) return
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        started = true
        delay(1200)
        onFinished()
    }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 850),
        label = "reward-burst"
    )
    val colors = listOf(DuoGreen, DuoGold, DuoBlue, DuoRed)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            repeat(36) { index ->
                val seed = (index * 47) % 97
                val delay = (seed % 28) / 100f
                val p = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
                if (p <= 0f) return@repeat
                val x = size.width * ((seed % 100) / 100f) +
                    (index % 2 * 12 - 6).dp.toPx() * p
                val y = size.height * 0.28f + size.height * 0.62f * p
                drawCircle(
                    color = colors[index % colors.size].copy(alpha = 1f - p * 0.45f),
                    radius = (2 + index % 3).dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
        Column(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = 0.7f + 0.3f * progress
                    scaleY = 0.7f + 0.3f * progress
                    alpha = progress
                }
                .clip(RoundedCornerShape(22.dp))
                .background(DuoCard.copy(alpha = 0.96f))
                .border(2.dp, DuoBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 30.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "⭐", fontSize = 42.sp)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = DuoTextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = DuoTextSecondary
                )
            }
        }
    }
}
