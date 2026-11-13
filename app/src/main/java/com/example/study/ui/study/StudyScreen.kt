package com.example.study.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study.data.db.StudyStatus
import com.example.study.data.db.WordEntity
import com.example.study.ui.components.DuoButton
import com.example.study.ui.components.DuoButtonStyle
import com.example.study.ui.components.FilterChips
import com.example.study.ui.components.FlipCard
import com.example.study.ui.components.ProgressTrack
import com.example.study.ui.components.StatusPill
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoGold
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary

@Composable
fun StudyScreen(onOpenDictionary: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: StudyViewModel = viewModel(factory = StudyViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.refreshFromDatabase() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StudyHeader(state = state, onOpenDictionary = onOpenDictionary)
            DailyGoalCard(state)
            FilterChips(selected = state.filter, onSelect = viewModel::setFilter)
            StudyCard(
                state = state,
                onFlip = viewModel::flip,
                onSpeak = viewModel::speakCurrentWord,
                onToggleFavorite = viewModel::toggleFavorite
            )
            StatusActionRow(
                state = state,
                onFlip = viewModel::flip,
                onMark = viewModel::markStatus
            )
            DuoButton(
                text = "下一个",
                onClick = viewModel::nextWord,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loadingWord && state.word != null
            )
            RoundIndicator(state)
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun StudyHeader(state: StudyUiState, onOpenDictionary: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = if (state.streak > 0) DuoGold else DuoTextSecondary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${state.streak} 天连续",
                style = MaterialTheme.typography.titleMedium,
                color = DuoTextPrimary
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenDictionary) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "打开词典查询",
                    tint = DuoTextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(DuoSurface)
                    .border(2.dp, DuoBorder, RoundedCornerShape(percent = 50))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = state.library,
                    style = MaterialTheme.typography.labelMedium,
                    color = DuoTextSecondary
                )
            }
        }
    }
}

@Composable
private fun DailyGoalCard(state: StudyUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "今日目标",
                style = MaterialTheme.typography.titleMedium,
                color = DuoTextPrimary
            )
            Text(
                text = "${state.todayStudied} / ${state.dailyGoal}",
                style = MaterialTheme.typography.titleMedium,
                color = if (state.goalReached) DuoGreen else DuoBlue
            )
        }
        ProgressTrack(progress = state.goalProgress, modifier = Modifier.fillMaxWidth())
        val remaining = (state.dailyGoal - state.todayStudied).coerceAtLeast(0)
        Text(
            text = if (state.goalReached) {
                "今日目标已完成，继续复习也很棒"
            } else {
                "再学 $remaining 个不同单词即可完成今日目标"
            },
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary
        )
    }
}

@Composable
private fun StudyCard(
    state: StudyUiState,
    onFlip: () -> Unit,
    onSpeak: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val word = state.word
    Box(modifier = Modifier.fillMaxWidth().height(348.dp)) {
        if (state.loadingWord || word == null) {
            LoadingCard(
                initialized = state.initialized,
                hasFilterResult = word != null
            )
        } else {
            FlipCard(
                flipped = state.flipped,
                onFlip = onFlip,
                modifier = Modifier.fillMaxSize(),
                front = {
                    CardFace(
                        status = state.wordStatus,
                        isFavorite = state.isFavorite,
                        onToggleFavorite = onToggleFavorite
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = word.word,
                                style = MaterialTheme.typography.displayLarge,
                                color = DuoTextPrimary,
                                textAlign = TextAlign.Center
                            )
                            if (word.phonetic.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "/${word.phonetic}/",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = DuoTextSecondary
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            IconButton(onClick = onSpeak) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "播放发音",
                                    tint = DuoBlue,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "点击卡片翻面 · 查看释义",
                                style = MaterialTheme.typography.bodySmall,
                                color = DuoTextSecondary
                            )
                        }
                    }
                },
                back = {
                    CardFace(
                        status = state.wordStatus,
                        isFavorite = state.isFavorite,
                        onToggleFavorite = onToggleFavorite
                    ) {
                        BackContent(word = word)
                    }
                }
            )
        }
    }
}

@Composable
private fun LoadingCard(initialized: Boolean, hasFilterResult: Boolean) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .shadow(6.dp, shape)
            .clip(shape)
            .background(Color.White)
            .border(2.dp, DuoBorder, shape)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (initialized && !hasFilterResult) {
            Text(
                text = "当前筛选下没有单词",
                style = MaterialTheme.typography.titleMedium,
                color = DuoTextSecondary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "换一个筛选条件试试",
                style = MaterialTheme.typography.bodySmall,
                color = DuoTextSecondary
            )
        } else {
            CircularProgressIndicator(color = DuoGreen)
            Spacer(Modifier.height(14.dp))
            Text(
                text = "正在导入词库…",
                style = MaterialTheme.typography.titleMedium,
                color = DuoTextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "CET-4 + CET-6 共 8,535 个单词，首次启动只需一次",
                style = MaterialTheme.typography.bodySmall,
                color = DuoTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BackContent(word: WordEntity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = word.word,
            style = MaterialTheme.typography.titleLarge,
            color = DuoTextSecondary
        )
        val pos = word.pos.trim()
        val meaning = word.meaning.trim()
        val meaningText = when {
            pos.isBlank() -> meaning
            meaning.startsWith(pos) -> meaning
            else -> "$pos $meaning"
        }
        Text(
            text = meaningText,
            style = MaterialTheme.typography.bodyLarge,
            color = DuoTextPrimary
        )
        if (word.example.isNotBlank() || word.exampleZh.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "例句",
                    style = MaterialTheme.typography.labelMedium,
                    color = DuoBlue
                )
                if (word.example.isNotBlank()) {
                    Text(
                        text = word.example,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DuoTextPrimary
                    )
                }
                if (word.exampleZh.isNotBlank()) {
                    Text(
                        text = word.exampleZh,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DuoTextSecondary
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "常用搭配",
                style = MaterialTheme.typography.labelMedium,
                color = DuoGreen
            )
            Text(
                text = if (word.phrase.isBlank()) {
                    "暂无搭配"
                } else {
                    listOf(word.phrase, word.phraseZh)
                        .filter { it.isNotBlank() }
                        .joinToString("　")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (word.phrase.isBlank()) DuoTextSecondary else DuoTextPrimary
            )
        }
    }
}

/** 卡片正反两面共用外观：圆角白底 + 描边 + 状态徽章 + 收藏星标。 */
@Composable
private fun CardFace(
    status: Int,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .shadow(6.dp, shape)
            .clip(shape)
            .background(Color.White)
            .border(
                width = 2.dp,
                color = if (status == StudyStatus.MASTERED) DuoGreen else DuoBorder,
                shape = shape
            )
            .padding(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(status = status)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) "取消收藏" else "收藏",
                    tint = if (isFavorite) DuoGold else DuoTextSecondary
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun StatusActionRow(
    state: StudyUiState,
    onFlip: () -> Unit,
    onMark: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DuoButton(
            text = if (state.flipped) "看正面" else "翻面",
            onClick = onFlip,
            modifier = Modifier.weight(1f),
            style = DuoButtonStyle.Secondary,
            enabled = state.word != null
        )
        DuoButton(
            text = "学习中",
            onClick = { onMark(StudyStatus.LEARNING) },
            modifier = Modifier.weight(1f),
            style = DuoButtonStyle.Learning,
            enabled = state.word != null && state.wordStatus != StudyStatus.LEARNING
        )
        DuoButton(
            text = "已掌握",
            onClick = { onMark(StudyStatus.MASTERED) },
            modifier = Modifier.weight(1f),
            style = DuoButtonStyle.Correct,
            enabled = state.word != null && state.wordStatus != StudyStatus.MASTERED
        )
    }
}

@Composable
private fun RoundIndicator(state: StudyUiState) {
    val filterLabel = when (state.filter) {
        0 -> "未学习"
        1 -> "学习中"
        2 -> "已掌握"
        else -> "全部"
    }
    Text(
        text = "本轮已浏览 ${state.roundViewed} 个词 · 筛选：$filterLabel · 本轮内不重复抽词 · 进度自动保存",
        style = MaterialTheme.typography.bodySmall,
        color = DuoTextSecondary,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}
