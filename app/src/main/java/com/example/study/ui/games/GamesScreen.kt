package com.example.study.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study.ui.theme.DuoBackground
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoGold
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary

private enum class GameAction { MATCH, QUIZ, LISTENING, SPELLING }

private data class GameEntry(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tint: Color,
    val action: GameAction? = null
)

private val gameEntries = listOf(
    GameEntry("单词消消乐", "词义配对，消除全部卡片", Icons.Filled.Extension, DuoGreen, action = GameAction.MATCH),
    GameEntry("看词选译", "四选一快速答题", Icons.Filled.Translate, DuoBlue, action = GameAction.QUIZ),
    GameEntry("听音辨词", "听发音选正确单词", Icons.Filled.Headphones, DuoGold, action = GameAction.LISTENING),
    GameEntry("单词拼写", "根据释义拼出单词", Icons.Filled.Keyboard, Color(0xFFA855F7), action = GameAction.SPELLING)
)

@Composable
fun GamesScreen(
    onOpenMatch: (Boolean) -> Unit,
    onOpenQuiz: (Boolean) -> Unit,
    onOpenListening: (Boolean) -> Unit,
    onOpenSpelling: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: GamesViewModel = viewModel(factory = GamesViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshPlan() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DuoBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "小游戏",
            style = MaterialTheme.typography.headlineMedium,
            color = DuoTextPrimary
        )
        Text(
            text = "4 个趣味模式已上线；开启「优先练计划词」可在游戏里同步复习计划词。",
            style = MaterialTheme.typography.bodyMedium,
            color = DuoTextSecondary
        )

        PlanBanner(state = state, onToggle = viewModel::setPlanMode)

        gameEntries.chunked(2).forEach { rowEntries ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowEntries.forEach { entry ->
                    val action: ((Boolean) -> Unit)? = when (entry.action) {
                        GameAction.MATCH -> onOpenMatch
                        GameAction.QUIZ -> onOpenQuiz
                        GameAction.LISTENING -> onOpenListening
                        GameAction.SPELLING -> onOpenSpelling
                        null -> null
                    }
                    GameCard(
                        entry = entry,
                        onClick = action?.let { callback -> { callback(state.planMode) } },
                        planMode = state.planMode,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun PlanBanner(state: GamesUiState, onToggle: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.hasPlan) "计划：${state.planName}" else "暂无专项计划",
                    style = MaterialTheme.typography.titleMedium,
                    color = DuoTextPrimary
                )
                Text(
                    text = if (state.hasPlan) {
                        "今日新词 ${state.todayNewLearned}/${state.todayNewTotal} · 待复习 ${state.dueReviewCount} · 词池 ${state.poolSize}"
                    } else {
                        "创建计划后，游戏可优先练习计划词并推进复习"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = DuoTextSecondary
                )
            }
            androidx.compose.material3.Switch(
                checked = state.planMode,
                onCheckedChange = onToggle,
                enabled = state.hasPlan
            )
        }
        Text(
            text = "优先练计划词：到期复习优先，其次今日新词；游戏答对到期复习词将推进艾宾浩斯阶段。",
            style = MaterialTheme.typography.bodySmall,
            color = DuoBlue
        )
    }
}

@Composable
private fun GameCard(
    entry: GameEntry,
    onClick: (() -> Unit)?,
    planMode: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(entry.tint.copy(alpha = 0.14f))
                .padding(10.dp)
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = entry.tint,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleMedium,
            color = DuoTextPrimary
        )
        Text(
            text = entry.description,
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary
        )
        Text(
            text = if (onClick != null) "立即开始" else "第二期开放",
            style = MaterialTheme.typography.labelSmall,
            color = if (onClick != null) DuoGreen else DuoGold
        )
        if (planMode && entry.action != null) {
            Text(
                text = "计划词优先",
                style = MaterialTheme.typography.labelSmall,
                color = DuoBlue
            )
        }
    }
}