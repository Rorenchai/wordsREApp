package com.example.study.ui.wrongbook

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study.data.db.WordEntity
import com.example.study.data.db.WrongWordRow
import com.example.study.ui.components.DuoButton
import com.example.study.ui.components.WordDetailDialog
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoRed
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WrongBookScreen(onBack: () -> Unit, onStartReview: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: WrongBookViewModel = viewModel(factory = WrongBookViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var detailWord by remember { mutableStateOf<WordEntity?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
                    text = "错题本",
                    style = MaterialTheme.typography.headlineSmall,
                    color = DuoTextPrimary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${state.words.size} 个词",
                    style = MaterialTheme.typography.labelMedium,
                    color = DuoTextSecondary
                )
            }

            if (state.loading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DuoGreen)
                }
            } else if (state.words.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = DuoTextSecondary,
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "还没有错题",
                            style = MaterialTheme.typography.titleMedium,
                            color = DuoTextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "在「看词选译 / 听音辨词 / 单词拼写」中答错的词会自动收集到这里",
                            style = MaterialTheme.typography.bodySmall,
                            color = DuoTextSecondary,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                DuoButton(
                    text = "开始复习（${state.words.size} 个错词）",
                    onClick = onStartReview,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.words, key = { it.word.id }) { row ->
                        WrongWordCard(row = row, onClick = { detailWord = row.word })
                    }
                }
            }
        }
    }

    detailWord?.let { word ->
        WordDetailDialog(word = word, onSpeak = { }, onDismiss = { detailWord = null })
    }
}

@Composable
private fun WrongWordCard(row: WrongWordRow, onClick: () -> Unit) {
    val date = runCatching {
        Instant.ofEpochMilli(row.lastWrongAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("M 月 d 日"))
    }.getOrNull().orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row.word.word,
                style = MaterialTheme.typography.titleMedium,
                color = DuoTextPrimary
            )
            if (row.word.phonetic.isNotBlank()) {
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(
                    text = "/${row.word.phonetic}/",
                    style = MaterialTheme.typography.bodySmall,
                    color = DuoTextSecondary
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "答错 ${row.wrongCount} 次",
                style = MaterialTheme.typography.labelSmall,
                color = DuoRed
            )
        }
        Text(
            text = row.word.meaning,
            style = MaterialTheme.typography.bodyMedium,
            color = DuoTextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (date.isNotEmpty()) {
            Text(
                text = "最近答错：$date",
                style = MaterialTheme.typography.bodySmall,
                color = DuoBlue
            )
        }
    }
}
