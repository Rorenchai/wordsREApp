package com.example.study.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.study.data.db.WordEntity
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary

/** 单词详情弹窗：音标、词性释义、双语例句、常用搭配、发音。词典与收藏夹共用。 */
@Composable
fun WordDetailDialog(
    word: WordEntity,
    onSpeak: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(word.word, style = MaterialTheme.typography.headlineSmall)
                    if (word.phonetic.isNotBlank()) {
                        Text(
                            text = "/${word.phonetic}/",
                            style = MaterialTheme.typography.bodySmall,
                            color = DuoTextSecondary
                        )
                    }
                }
                IconButton(onClick = onSpeak) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "播放发音",
                        tint = DuoBlue
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = listOf(word.pos, word.meaning).filter { it.isNotBlank() }.joinToString(" "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = DuoTextPrimary
                )
                if (word.example.isNotBlank() || word.exampleZh.isNotBlank()) {
                    Text("例句", style = MaterialTheme.typography.labelMedium, color = DuoBlue)
                    if (word.example.isNotBlank()) {
                        Text(word.example, style = MaterialTheme.typography.bodyMedium, color = DuoTextPrimary)
                    }
                    if (word.exampleZh.isNotBlank()) {
                        Text(word.exampleZh, style = MaterialTheme.typography.bodyMedium, color = DuoTextSecondary)
                    }
                }
                if (word.phrase.isNotBlank() || word.phraseZh.isNotBlank()) {
                    Text("常用搭配", style = MaterialTheme.typography.labelMedium, color = DuoGreen)
                    Text(
                        text = listOf(word.phrase, word.phraseZh)
                            .filter { it.isNotBlank() }
                            .joinToString("　"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = DuoTextPrimary
                    )
                }
                Text(
                    text = "词库：${word.library}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DuoTextSecondary,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
