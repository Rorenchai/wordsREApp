package com.example.study.ui.plan

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study.ui.components.DuoButton
import com.example.study.ui.components.DuoButtonStyle
import com.example.study.ui.components.FlipCard
import com.example.study.ui.components.ProgressTrack
import com.example.study.ui.theme.DuoBackground
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoCard
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoRed
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary

@Composable
fun PlanStudyScreen(mode: PlanStudyMode, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: PlanStudyViewModel = viewModel(factory = PlanStudyViewModel.factory(context, mode))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    Box(modifier = modifier.fillMaxSize().background(DuoBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 6.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = DuoTextPrimary
                    )
                }
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = DuoTextPrimary
                )
                Spacer(Modifier.weight(1f))
                if (!state.finished && state.queueSize > 0) {
                    Text(
                        text = "第 ${state.index + 1} / ${state.queueSize} 个",
                        style = MaterialTheme.typography.labelMedium,
                        color = DuoTextSecondary
                    )
                }
            }

            if (state.loading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DuoGreen)
                }
                return@Column
            }

            if (state.finished) {
                PlanStudyFinishedCard(state = state, onBack = onBack)
                return@Column
            }

            if (state.queueSize > 0) {
                ProgressTrack(
                    progress = (state.index.toFloat() / state.queueSize).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val word = state.word
            if (word == null) {
                Text(
                    text = "没有待处理的任务",
                    style = MaterialTheme.typography.titleMedium,
                    color = DuoTextSecondary
                )
                return@Column
            }

            // 切换到下一个词时卡片淡入 + 轻微放大，与学习页保持一致的过渡
            var cardAppeared by remember(word.word.id) { mutableStateOf(false) }
            LaunchedEffect(word.word.id) { cardAppeared = true }
            val cardAlpha by animateFloatAsState(
                targetValue = if (cardAppeared) 1f else 0f,
                animationSpec = tween(durationMillis = 220),
                label = "plan-card-alpha"
            )
            val cardScale by animateFloatAsState(
                targetValue = if (cardAppeared) 1f else 0.96f,
                animationSpec = tween(durationMillis = 240),
                label = "plan-card-scale"
            )

            FlipCard(
                flipped = state.flipped,
                onFlip = viewModel::flip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .graphicsLayer {
                        alpha = cardAlpha
                        scaleX = cardScale
                        scaleY = cardScale
                    },
                front = { PlanStudyFrontFace(word.word) },
                back = { PlanStudyBackFace(word.word) }
            )

            // 双 FlipCard 叠放（仅显示当前面）：用一个占位布局避免重复渲染
            when (mode) {
                PlanStudyMode.NEW -> {
                    DuoButton(
                        text = if (state.flipped) "已学完，下一个" else "翻面查看释义",
                        onClick = {
                            if (state.flipped) viewModel.completeNewWord() else viewModel.flip()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                PlanStudyMode.REVIEW -> {
                    if (state.flipped) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DuoButton(
                                text = "没记住",
                                onClick = { viewModel.submitReview(false) },
                                modifier = Modifier.weight(1f),
                                style = DuoButtonStyle.Danger
                            )
                            DuoButton(
                                text = "记住了",
                                onClick = { viewModel.submitReview(true) },
                                modifier = Modifier.weight(1f),
                                style = DuoButtonStyle.Correct
                            )
                        }
                    } else {
                        DuoButton(
                            text = "翻面查看释义",
                            onClick = viewModel::flip,
                            modifier = Modifier.fillMaxWidth(),
                            style = DuoButtonStyle.Secondary
                        )
                    }
                }
            }

            Text(
                text = if (mode == PlanStudyMode.NEW) {
                    "学完新词后自动安排次日首次复习（艾宾浩斯第 1 轮）。"
                } else {
                    "按回忆结果判定：记住了进入下一轮复习，没记住次日重试。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = DuoTextSecondary
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PlanStudyFinishedCard(state: PlanStudyUiState, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 22.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (state.mode == PlanStudyMode.NEW) "今日新词学习完成" else "本轮复习完成",
            style = MaterialTheme.typography.titleLarge,
            color = DuoGreen
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (state.mode == PlanStudyMode.NEW) {
                "本次学习 ${state.doneCount} 个新词，已安排明日首次复习。"
            } else {
                "本次复习 ${state.doneCount} 词 · 记住 ${state.correctCount} · 正确率 ${if (state.doneCount > 0) state.correctCount * 100 / state.doneCount else 0}%"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = DuoTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        DuoButton(text = "返回计划", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}
@Composable
private fun PlanStudyFrontFace(word: com.example.study.data.db.WordEntity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(DuoCard)
            .border(2.dp, DuoBorder, RoundedCornerShape(28.dp))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = word.word,
            style = MaterialTheme.typography.displayMedium,
            color = DuoTextPrimary,
            textAlign = TextAlign.Center
        )
        if (word.phonetic.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "/${word.phonetic}/",
                style = MaterialTheme.typography.bodyLarge,
                color = DuoTextSecondary
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "点击卡片翻面查看释义",
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary
        )
    }
}

@Composable
private fun PlanStudyBackFace(word: com.example.study.data.db.WordEntity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(DuoCard)
            .border(2.dp, DuoBorder, RoundedCornerShape(28.dp))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = word.meaning,
            style = MaterialTheme.typography.titleLarge,
            color = DuoTextPrimary,
            textAlign = TextAlign.Center
        )
        if (word.example.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = word.example,
                style = MaterialTheme.typography.bodyMedium,
                color = DuoTextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = word.exampleZh,
                style = MaterialTheme.typography.bodySmall,
                color = DuoTextSecondary,
                textAlign = TextAlign.Center
            )
        }
        if (word.phrase.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "搭配：${word.phrase}",
                style = MaterialTheme.typography.bodySmall,
                color = DuoTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
