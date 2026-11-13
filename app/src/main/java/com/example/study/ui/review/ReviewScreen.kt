package com.example.study.ui.review

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study.ui.components.AnswerFeedbackBanner
import com.example.study.ui.components.AnswerOptionRow
import com.example.study.ui.components.DuoButton
import com.example.study.ui.components.SessionStatsCard
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary

@Composable
fun ReviewScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
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
                    text = "错题复习",
                    style = MaterialTheme.typography.headlineSmall,
                    color = DuoTextPrimary
                )
                Spacer(Modifier.weight(1f))
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

            if (state.finished) {
                ReviewFinishedCard(state = state, onRestart = viewModel::restart, onBack = onBack)
                return@Column
            }

            SessionStatsCard(
                answered = state.reviewed,
                correct = state.correctCount,
                accuracy = state.accuracy
            )
            Text(
                text = "第 ${state.index + 1} / ${state.queueSize} 题 · 答对后自动移出错题本",
                style = MaterialTheme.typography.bodySmall,
                color = DuoTextSecondary
            )

            val word = state.word
            if (state.loading || word == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DuoGreen)
                }
                return@Column
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(DuoSurface)
                    .border(2.dp, DuoBorder, RoundedCornerShape(28.dp))
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "选出正确的中文释义",
                    style = MaterialTheme.typography.bodySmall,
                    color = DuoTextSecondary
                )
            }

            if (state.answered) {
                AnswerFeedbackBanner(
                    correct = state.correct == true,
                    correctAnswer = word.meaning
                )
            }

            state.options.forEachIndexed { index, option ->
                AnswerOptionRow(
                    text = option,
                    index = index,
                    answered = state.answered,
                    answerIndex = state.answerIndex,
                    selectedIndex = state.selectedIndex,
                    onClick = { viewModel.selectOption(index) }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ReviewFinishedCard(state: ReviewUiState, onRestart: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 22.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.queueSize == 0) {
            Text(
                text = "暂无错词可复习",
                style = MaterialTheme.typography.titleLarge,
                color = DuoTextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "去「看词选译 / 听音辨词 / 单词拼写」练几题，答错的词会自动进入错题本",
                style = MaterialTheme.typography.bodyMedium,
                color = DuoTextSecondary,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "本轮复习完成",
                style = MaterialTheme.typography.titleLarge,
                color = DuoGreen
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "复习 ${state.reviewed} 词 · 答对 ${state.correctCount} 词 · 正确率 ${state.accuracy}%",
                style = MaterialTheme.typography.bodyLarge,
                color = DuoTextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "答对的词已自动移出错题本",
                style = MaterialTheme.typography.bodySmall,
                color = DuoTextSecondary
            )
        }
        Spacer(Modifier.height(18.dp))
        DuoButton(text = "返回错题本", onClick = onBack, modifier = Modifier.fillMaxWidth())
        if (state.queueSize > 0) {
            TextButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                Text("再来一轮")
            }
        }
    }
}
