package com.example.study.ui.listening

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study.ui.components.AnswerFeedbackBanner
import com.example.study.ui.components.AnswerOptionRow
import com.example.study.ui.components.SessionStatsCard
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary

@Composable
fun ListeningScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: ListeningViewModel = viewModel(factory = ListeningViewModel.factory(context))
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
                    text = "听音辨词",
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

            SessionStatsCard(
                answered = state.sessionAnswered,
                correct = state.sessionCorrect,
                accuracy = state.sessionAccuracy
            )

            val word = state.word
            if (state.loading || word == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(color = DuoGreen)
                    } else {
                        Text(
                            text = "当前词库没有可用单词",
                            style = MaterialTheme.typography.titleMedium,
                            color = DuoTextSecondary
                        )
                    }
                }
                return@Column
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(DuoSurface)
                    .border(2.dp, DuoBorder, RoundedCornerShape(28.dp))
                    .padding(vertical = 28.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = viewModel::replay, modifier = Modifier.size(96.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "重听发音",
                        tint = DuoBlue,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "听发音，选出正确的单词",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DuoTextSecondary
                )
                Text(
                    text = "点击喇叭可重听",
                    style = MaterialTheme.typography.bodySmall,
                    color = DuoTextSecondary
                )
            }

            if (state.answered) {
                AnswerFeedbackBanner(
                    correct = state.correct == true,
                    correctAnswer = word.word
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
