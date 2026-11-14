package com.example.study.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study.data.db.WordEntity
import com.example.study.ui.components.StatusPill
import com.example.study.ui.components.WordDetailDialog
import com.example.study.ui.theme.DuoBackground
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoCard
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary

@Composable
fun WordOverviewScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: WordOverviewViewModel = viewModel(factory = WordOverviewViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var detailWord by remember { mutableStateOf<WordEntity?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DuoBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(top = 14.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "单词总览",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DuoTextPrimary
                )
                Text(
                    text = state.library,
                    style = MaterialTheme.typography.labelMedium,
                    color = DuoTextSecondary
                )
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("搜索单词或中文释义") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = DuoTextSecondary)
                },
                trailingIcon = {
                    if (state.query.isNotBlank()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "清空搜索", tint = DuoTextSecondary)
                        }
                    }
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverviewFilterChip(
                    text = "全部",
                    selected = state.filter == OverviewFilter.ALL,
                    onClick = { viewModel.setFilter(OverviewFilter.ALL) }
                )
                OverviewFilterChip(
                    text = "未掌握",
                    selected = state.filter == OverviewFilter.UNMASTERED,
                    onClick = { viewModel.setFilter(OverviewFilter.UNMASTERED) }
                )
                OverviewFilterChip(
                    text = "已掌握",
                    selected = state.filter == OverviewFilter.MASTERED,
                    onClick = { viewModel.setFilter(OverviewFilter.MASTERED) }
                )
            }

            when {
                state.loading && state.words.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DuoGreen)
                    }
                }

                state.words.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("没有符合条件的单词", color = DuoTextSecondary)
                            Spacer(Modifier.height(6.dp))
                            Text("换个筛选条件或搜索词试试", style = MaterialTheme.typography.bodySmall, color = DuoTextSecondary)
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.words, key = { it.word.id }) { item ->
                            OverviewRow(
                                word = item.word,
                                status = item.status,
                                onClick = { detailWord = item.word }
                            )
                        }
                        item {
                            if (state.hasMore) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !state.loadingMore) { viewModel.loadMore() }
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.loadingMore) {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = DuoGreen)
                                    } else {
                                        Text("加载更多（已显示 ${state.words.size} 个）", color = DuoBlue)
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("已显示全部 ${state.words.size} 个单词", style = MaterialTheme.typography.bodySmall, color = DuoTextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp, vertical = 8.dp)
        )
    }

    detailWord?.let { word ->
        WordDetailDialog(
            word = word,
            onSpeak = { viewModel.speak(word.word) },
            onDismiss = { detailWord = null }
        )
    }
}

@Composable
private fun OverviewFilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) DuoGreen else DuoCard)
            .border(2.dp, if (selected) DuoGreen else DuoBorder, RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) DuoCard else DuoTextSecondary
        )
    }
}

@Composable
private fun OverviewRow(word: WordEntity, status: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = word.word,
                style = MaterialTheme.typography.titleMedium,
                color = DuoTextPrimary
            )
            if (word.phonetic.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "/${word.phonetic}/",
                    style = MaterialTheme.typography.bodySmall,
                    color = DuoTextSecondary
                )
            }
            Spacer(Modifier.weight(1f))
            StatusPill(status = status)
        }
        Text(
            text = word.meaning,
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
