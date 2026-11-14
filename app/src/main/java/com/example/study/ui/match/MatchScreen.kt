package com.example.study.ui.match

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study.ui.theme.DuoBackground
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoCard
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoRed
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary
import kotlin.math.roundToInt

@Composable
fun MatchScreen(onBack: () -> Unit, planMode: Boolean = false, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: MatchViewModel = viewModel(factory = MatchViewModel.factory(context, planMode))
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
                .padding(horizontal = 16.dp)
                .padding(top = 6.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    text = "单词消消乐",
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(DuoSurface)
                    .border(2.dp, DuoBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "已配对 ${state.matchedPairs} / ${state.totalPairs}",
                    style = MaterialTheme.typography.titleMedium,
                    color = DuoTextPrimary
                )
                Text(
                    text = "错误 ${state.mistakes} 次",
                    style = MaterialTheme.typography.titleMedium,
                    color = DuoRed
                )
            }

            Text(
                text = "点击一张单词卡，再点它的中文释义即可消除该卡片",
                style = MaterialTheme.typography.bodySmall,
                color = DuoTextSecondary
            )

            if (state.loading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DuoGreen)
                }
            } else {
                state.cards.chunked(COLUMNS).forEach { rowCards ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowCards.forEach { card ->
                            MatchCardView(
                                card = card,
                                selected = state.firstIndex != null && state.cards.getOrNull(state.firstIndex!!)?.id == card.id,
                                onClick = { viewModel.onCardClick(card.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(COLUMNS - rowCards.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp, vertical = 8.dp)
        )
    }

    if (state.finished) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("本局完成") },
            text = { Text("全部 ${state.totalPairs} 对词义配对完成，错误 ${state.mistakes} 次。") },
            confirmButton = {
                TextButton(onClick = viewModel::restart) { Text("再来一局") }
            },
            dismissButton = {
                TextButton(onClick = onBack) { Text("返回") }
            }
        )
    }
}

@Composable
private fun MatchCardView(
    card: MatchCard,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background: Color
    val borderColor: Color
    val textColor: Color
    when {
        card.matched -> {
            background = DuoCard
            borderColor = DuoBorder
            textColor = Color.Transparent
        }

        card.wrong -> {
            background = DuoRed.copy(alpha = 0.10f)
            borderColor = DuoRed
            textColor = DuoRed
        }

        selected -> {
            background = DuoBlue.copy(alpha = 0.10f)
            borderColor = DuoBlue
            textColor = DuoTextPrimary
        }

        else -> {
            background = DuoCard
            borderColor = DuoBorder
            textColor = DuoTextPrimary
        }
    }
    val shape = RoundedCornerShape(14.dp)

    // 配对成功：卡片淡出并缩小；配错：左右轻微抖动
    val matchedAlpha by animateFloatAsState(
        targetValue = if (card.matched) 0f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "match-card-alpha"
    )
    val matchedScale by animateFloatAsState(
        targetValue = if (card.matched) 0.88f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "match-card-scale"
    )
    val wrongShake = remember { Animatable(0f) }
    LaunchedEffect(card.wrong) {
        if (card.wrong) {
            wrongShake.animateTo(-4f, tween(durationMillis = 60))
            wrongShake.animateTo(4f, tween(durationMillis = 70))
            wrongShake.animateTo(-2f, tween(durationMillis = 60))
            wrongShake.animateTo(0f, tween(durationMillis = 60))
        }
    }

    Box(
        modifier = modifier
            .height(112.dp)
            .graphicsLayer {
                alpha = matchedAlpha
                scaleX = matchedScale
                scaleY = matchedScale
            }
            .offset { IntOffset(wrongShake.value.roundToInt(), 0) }
            .clip(shape)
            .background(background)
            .border(2.dp, borderColor, shape)
            .clickable(enabled = !card.matched && !card.wrong, onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!card.matched) {
            Text(
                text = card.text,
                style = if (card.isWord) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp)
                },
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = if (card.isWord) 2 else 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private const val COLUMNS = 3