package com.example.study.ui.favorites

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study.data.db.WordEntity
import com.example.study.ui.components.WordDetailDialog
import com.example.study.ui.theme.DuoBackground
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoGold
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary

@Composable
fun FavoritesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: FavoritesViewModel = viewModel(factory = FavoritesViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var detailWord by remember { mutableStateOf<WordEntity?>(null) }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    Box(modifier = modifier.fillMaxSize().background(DuoBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(top = 14.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "收藏夹",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DuoTextPrimary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${state.favorites.size} 个单词",
                    style = MaterialTheme.typography.labelMedium,
                    color = DuoTextSecondary
                )
            }
            Text(
                text = "跨词库统一管理；点右侧星标可取消收藏，点单词查看详情",
                style = MaterialTheme.typography.bodySmall,
                color = DuoTextSecondary,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
            )

            if (!state.loading && state.favorites.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = DuoTextSecondary,
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "还没有收藏的单词",
                            style = MaterialTheme.typography.titleMedium,
                            color = DuoTextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "在学习页点卡片右上角的星标即可收藏",
                            style = MaterialTheme.typography.bodySmall,
                            color = DuoTextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.favorites, key = { it.id }) { word ->
                        FavoriteRow(
                            word = word,
                            onClick = { detailWord = word },
                            onRemove = { viewModel.remove(word) }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp, vertical = 8.dp)
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
private fun FavoriteRow(word: WordEntity, onClick: () -> Unit, onRemove: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            // 左右/上下留足内边距，避免文字贴住圆角边缘或被星标挤压
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = word.word,
                style = MaterialTheme.typography.titleMedium,
                color = DuoTextPrimary
            )
            if (word.phonetic.isNotBlank()) {
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(
                    text = "/${word.phonetic}/",
                    style = MaterialTheme.typography.bodySmall,
                    color = DuoTextSecondary
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = word.library,
                style = MaterialTheme.typography.labelSmall,
                color = DuoBlue
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = word.meaning,
                style = MaterialTheme.typography.bodyMedium,
                color = DuoTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "取消收藏",
                    tint = DuoGold
                )
            }
        }
    }
}