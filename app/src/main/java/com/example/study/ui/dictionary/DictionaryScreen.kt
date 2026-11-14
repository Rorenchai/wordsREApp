package com.example.study.ui.dictionary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study.data.db.WordEntity
import com.example.study.data.repo.SettingsRepository
import com.example.study.ui.components.WordDetailDialog
import com.example.study.ui.theme.DuoBackground
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoCard
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary

private data class ScopeOption(val value: String, val label: String)

private val scopeOptions = listOf(
    ScopeOption(DictionaryViewModel.SCOPE_CURRENT, "当前词库"),
    ScopeOption(SettingsRepository.LIB_CET4, "CET-4"),
    ScopeOption(SettingsRepository.LIB_CET6, "CET-6"),
    ScopeOption(SettingsRepository.LIB_PETS_B, "PETS-B"),
    ScopeOption(DictionaryViewModel.SCOPE_ALL, "全部")
)

@Composable
fun DictionaryScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: DictionaryViewModel = viewModel(factory = DictionaryViewModel.factory(context))
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
                    text = "词典查询",
                    style = MaterialTheme.typography.headlineSmall,
                    color = DuoTextPrimary
                )
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                placeholder = { Text("输入单词或中文释义") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = DuoTextSecondary)
                },
                trailingIcon = {
                    if (state.query.isNotBlank()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "清空", tint = DuoTextSecondary)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                scopeOptions.forEach { option ->
                    ScopeChip(
                        label = option.label,
                        selected = state.scope == option.value,
                        onClick = { viewModel.onScopeChange(option.value) }
                    )
                }
            }

            when {
                state.loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DuoGreen)
                    }
                }

                state.query.isBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "输入关键词开始搜索",
                                style = MaterialTheme.typography.titleMedium,
                                color = DuoTextPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "支持按英文单词或中文释义检索完整词库",
                                style = MaterialTheme.typography.bodySmall,
                                color = DuoTextSecondary
                            )
                        }
                    }
                }

                state.searched && state.results.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "未找到匹配的单词",
                            style = MaterialTheme.typography.titleMedium,
                            color = DuoTextSecondary
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 18.dp,
                            end = 18.dp,
                            bottom = 18.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.results, key = { it.id }) { word ->
                            DictionaryRow(word = word, onClick = { detailWord = word })
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

    detailWord?.let { word ->
        WordDetailDialog(
            word = word,
            onSpeak = { viewModel.speak(word.word) },
            onDismiss = { detailWord = null }
        )
    }
}

@Composable
private fun ScopeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) DuoGreen else DuoCard)
            .border(2.dp, if (selected) DuoGreen else DuoBorder, RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else DuoTextSecondary
        )
    }
}

@Composable
private fun DictionaryRow(word: WordEntity, onClick: () -> Unit) {
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
        Text(
            text = word.meaning,
            style = MaterialTheme.typography.bodyMedium,
            color = DuoTextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}