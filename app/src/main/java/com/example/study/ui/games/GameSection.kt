package com.example.study.ui.games

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary

/**
 * 学习页单词卡片下方的小游戏入口区：四个游戏卡片 + 计划词优先开关。
 * 替代原底部「游戏」页，减少一级导航。
 */
@Composable
fun GameSection(
    onOpenGame: (game: String, planMode: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: GamesViewModel = viewModel(factory = GamesViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshPlan() }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "小游戏",
                style = MaterialTheme.typography.titleMedium,
                color = DuoTextPrimary
            )
            if (state.hasPlan) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (state.planMode) "计划词优先" else "优先练计划词",
                        style = MaterialTheme.typography.bodySmall,
                        color = DuoTextSecondary
                    )
                    Switch(
                        checked = state.planMode,
                        onCheckedChange = viewModel::setPlanMode,
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }
        if (state.hasPlan) {
            Text(
                text = "今日新词 ${state.todayNewLearned}/${state.todayNewTotal} · 待复习 ${state.dueReviewCount} · 词池 ${state.poolSize}",
                style = MaterialTheme.typography.bodySmall,
                color = DuoTextSecondary
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GameTile(
                icon = "🎯",
                name = "看词选译",
                onClick = { onOpenGame("quiz", state.planMode) },
                modifier = Modifier.weight(1f)
            )
            GameTile(
                icon = "🎧",
                name = "听音辨词",
                onClick = { onOpenGame("listening", state.planMode) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GameTile(
                icon = "⌨️",
                name = "单词拼写",
                onClick = { onOpenGame("spelling", state.planMode) },
                modifier = Modifier.weight(1f)
            )
            GameTile(
                icon = "🧩",
                name = "单词消消乐",
                onClick = { onOpenGame("match", state.planMode) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun GameTile(
    icon: String,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, style = MaterialTheme.typography.titleLarge)
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextPrimary,
            textAlign = TextAlign.Center
        )
    }
}
