package com.example.study.ui.tree

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study.ui.components.WordDetailDialog
import com.example.study.ui.theme.DuoBackground
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoCard
import com.example.study.ui.theme.DuoChocolate
import com.example.study.ui.theme.DuoGold
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary
import kotlin.math.ceil
import androidx.compose.foundation.gestures.detectTransformGestures

private const val FAMILY_PROMPT = """请帮我整理英语词族：
1. 以词根/同根词为依据分组，每组包含一个基础词（base）和同族派生词；
2. 输出 JSON 数组，每项格式为 {"word":"单词","phonetic":"音标","pos":"词性","meaning":"中文释义"}；
3. 只保留词库中真实存在的单词，不要编造，不要合并无关词；
4. 音标可留空，释义必须准确简洁。"""

@Composable
fun WordFamilyScreen(
    onBack: () -> Unit,
    onOpenCustomImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val viewModel: WordFamilyViewModel = viewModel(factory = WordFamilyViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(activity) {
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = previous ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    Box(modifier = modifier.fillMaxSize().background(DuoBackground)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = DuoTextPrimary
                    )
                }
                Text("同类词归纳", style = MaterialTheme.typography.headlineSmall, color = DuoTextPrimary)
                Spacer(Modifier.width(10.dp))
                Text(state.library, style = MaterialTheme.typography.labelMedium, color = DuoTextSecondary)
                Spacer(Modifier.weight(1f))
                Text("${state.visibleFamilies.size} 个词族", style = MaterialTheme.typography.labelSmall, color = DuoTextSecondary)
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("搜索单词 / 词根 / 释义") }
            )

            if (state.custom) {
                CustomGuideCard(
                    onCopyPrompt = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("词族归纳提示词", FAMILY_PROMPT))
                    },
                    onOpenImport = onOpenCustomImport
                )
            }

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DuoGreen)
                }

                state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error ?: "加载失败", color = DuoTextSecondary)
                        TextButton(onClick = viewModel::retry) { Text("重试") }
                    }
                }

                state.visibleFamilies.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (state.query.isBlank()) "该词库暂无可归纳的词族" else "没有匹配的词族",
                        color = DuoTextSecondary
                    )
                }

                else -> {
                    val family = state.selectedFamily
                    if (family != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = viewModel::previousFamily, enabled = state.selectedIndex > 0) {
                                Text("‹ 上一组")
                            }
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(state.visibleFamilies) { index, item ->
                                    FamilyChip(
                                        text = item.base,
                                        selected = index == state.selectedIndex,
                                        onClick = { viewModel.selectFamily(index) }
                                    )
                                }
                            }
                            TextButton(
                                onClick = viewModel::nextFamily,
                                enabled = state.selectedIndex < state.visibleFamilies.lastIndex
                            ) { Text("下一组 ›") }
                        }
                        Text(
                            text = "词根：${family.root} · 基础词：${family.base} · 点击节点查看详情/发音，拖动空白处浏览",
                            style = MaterialTheme.typography.labelSmall,
                            color = DuoTextSecondary
                        )
                        FamilyTreeViewport(
                            family = family,
                            query = state.query,
                            onWordClick = viewModel::openWord,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }

    state.detailWord?.let { word ->
        WordDetailDialog(
            word = word,
            onSpeak = { viewModel.speak(word.word) },
            onDismiss = viewModel::dismissDetail
        )
    }
}

@Composable
private fun CustomGuideCard(onCopyPrompt: () -> Unit, onOpenImport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("自定义词库 · 词族归纳指引", style = MaterialTheme.typography.titleSmall, color = DuoTextPrimary)
        Text(
            "推荐顺序：DeepSeek 优先，豆包其次。步骤：复制提示词 → 连同词库发给 AI → 保存返回的 JSON/CSV → 导入为自定义词库 → 回到本页查看。",
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = onCopyPrompt) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Text("复制提示词")
            }
            TextButton(onClick = onOpenImport) { Text("前往导入") }
        }
    }
}

@Composable
private fun FamilyChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) DuoGreen else DuoCard)
            .border(2.dp, if (selected) DuoGreen else DuoBorder, RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) DuoCard else DuoTextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun FamilyTreeViewport(
    family: WordFamily,
    query: String,
    onWordClick: (FamilyWord) -> Unit,
    modifier: Modifier = Modifier
) {
    var pan by remember(family) { mutableStateOf(Offset.Zero) }
    var zoom by remember(family) { mutableStateOf(1f) }
    val children = family.words.filter { it.word != family.base }
    val rows = ceil(children.size / 2.0).toInt().coerceAtLeast(1)
    val contentWidth = 900.dp
    val contentHeight = (150 + rows * 68).dp
    val rootX = 330.dp
    val rootY = 2.dp
    val columns = listOf(70.dp, 500.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(16.dp))
            .pointerInput(family) {
                detectTransformGestures { _, panChange, zoomChange, _ ->
                    pan += panChange
                    zoom = (zoom * zoomChange).coerceIn(0.7f, 1.5f)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .width(contentWidth)
                .height(contentHeight)
                .graphicsLayer {
                    translationX = pan.x
                    translationY = pan.y
                    scaleX = zoom
                    scaleY = zoom
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                }
        ) {
            val lineColor = DuoBorder
            Canvas(Modifier.fillMaxSize()) {
                val rootCenter = Offset(
                    x = rootX.toPx() + 100.dp.toPx(),
                    y = rootY.toPx() + 30.dp.toPx()
                )
                children.forEachIndexed { index, _ ->
                    val x = columns[index % 2].toPx() + 110.dp.toPx()
                    val y = 80.dp.toPx() + (index / 2) * 68.dp.toPx()
                    drawLine(
                        color = lineColor,
                        start = Offset(rootCenter.x, rootCenter.y + 30.dp.toPx()),
                        end = Offset(x, y),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            TreeWordNode(
                word = FamilyWord(family.base, "", "", "基础词 · ${family.root}"),
                root = true,
                highlighted = query.isNotBlank() && family.base.contains(query, true),
                onClick = { onWordClick(family.words.firstOrNull { it.word == family.base } ?: FamilyWord(family.base)) },
                modifier = Modifier.offset(x = rootX, y = rootY).width(220.dp)
            )
            children.forEachIndexed { index, word ->
                TreeWordNode(
                    word = word,
                    root = false,
                    highlighted = query.isNotBlank() && (
                        word.word.contains(query, true) || word.meaning.contains(query, true)
                        ),
                    onClick = { onWordClick(word) },
                    modifier = Modifier
                        .offset(x = columns[index % 2], y = (80 + (index / 2) * 68).dp)
                        .width(240.dp)
                )
            }
        }
    }
}

@Composable
private fun TreeWordNode(
    word: FamilyWord,
    root: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (root) DuoGreen.copy(alpha = 0.16f) else DuoCard)
            .border(
                width = if (highlighted) 2.dp else 1.dp,
                color = when {
                    highlighted -> DuoGold
                    root -> DuoGreen
                    else -> DuoBorder
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = word.word,
                style = MaterialTheme.typography.titleSmall,
                color = DuoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (word.phonetic.isNotBlank()) {
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "/${word.phonetic}/",
                    style = MaterialTheme.typography.labelSmall,
                    color = DuoTextSecondary,
                    maxLines = 1
                )
            }
        }
        Text(
            text = listOf(word.pos, word.meaning).filter { it.isNotBlank() }.joinToString(" "),
            style = MaterialTheme.typography.bodySmall,
            color = DuoTextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
