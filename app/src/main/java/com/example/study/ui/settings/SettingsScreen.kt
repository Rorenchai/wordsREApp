package com.example.study.ui.settings

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.study.data.repo.SettingsRepository
import com.example.study.ui.components.DuoButton
import com.example.study.ui.components.DuoButtonStyle
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoRed
import com.example.study.ui.theme.DuoSurface
import com.example.study.ui.theme.DuoTextPrimary
import com.example.study.ui.theme.DuoTextSecondary

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomGoalDialog by rememberSaveable { mutableStateOf(false) }
    var licenseDialog by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 14.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "词库与设置",
                style = MaterialTheme.typography.headlineMedium,
                color = DuoTextPrimary
            )

            SettingsSection(title = "词库") {
                SettingsRepository.LIBRARIES.forEach { library ->
                    LibraryRow(
                        name = library,
                        count = state.wordCounts[library],
                        selected = state.library == library,
                        onClick = { viewModel.selectLibrary(library) }
                    )
                }
            }

            SettingsSection(title = "每日目标") {
                Text(
                    text = "当前目标：${state.dailyGoal} 个不同单词 / 天",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DuoTextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsRepository.DAILY_GOAL_OPTIONS.forEach { goal ->
                        OptionChip(
                            text = "$goal",
                            selected = state.dailyGoal == goal,
                            onClick = { viewModel.selectDailyGoal(goal) }
                        )
                    }
                    OptionChip(
                        text = "自定义",
                        selected = state.dailyGoal !in SettingsRepository.DAILY_GOAL_OPTIONS,
                        onClick = { showCustomGoalDialog = true }
                    )
                }
            }

            SettingsSection(title = "发音口音") {
                Text(
                    text = "点击学习页的喇叭按钮时使用的语音口音",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DuoTextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionChip(
                        text = "美式 US",
                        selected = state.accent == SettingsRepository.ACCENT_US,
                        onClick = { viewModel.selectAccent(SettingsRepository.ACCENT_US) }
                    )
                    OptionChip(
                        text = "英式 UK",
                        selected = state.accent == SettingsRepository.ACCENT_UK,
                        onClick = { viewModel.selectAccent(SettingsRepository.ACCENT_UK) }
                    )
                }
            }

            SettingsSection(title = "学习数据") {
                Text(
                    text = "重置仅清空当前词库（${state.library}）的学习状态与学习记录，词库本身与另一套词库不受影响。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DuoTextSecondary
                )
                DuoButton(
                    text = "重置当前词库进度",
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    style = DuoButtonStyle.Danger
                )
            }

            SettingsSection(title = "关于与许可") {
                Text(
                    text = "词库来源：KyleBing/english-vocabulary（BSD 3-Clause License）\n" +
                        "字体：Nunito（SIL Open Font License 1.1）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DuoTextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = {
                        licenseDialog = "词库许可证（BSD 3-Clause）" to "licenses/wordbank-BSD-3.txt"
                    }) {
                        Text("词库许可证", color = DuoBlue)
                    }
                    TextButton(onClick = {
                        licenseDialog = "字体许可证（SIL OFL 1.1）" to "licenses/Nunito-OFL.txt"
                    }) {
                        Text("字体许可证", color = DuoBlue)
                    }
                }
                Text(
                    text = "版本 1.0 · 单词记忆卡",
                    style = MaterialTheme.typography.bodySmall,
                    color = DuoTextSecondary
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp, vertical = 8.dp)
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("确认重置学习进度？") },
            text = {
                Text("将清空 ${state.library} 词库下的全部「学习中 / 已掌握」状态与学习记录，且不可恢复。")
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    viewModel.resetCurrentLibrary()
                }) { Text("确认重置", color = DuoRed) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("取消") }
            }
        )
    }

    if (showCustomGoalDialog) {
        var goalText by rememberSaveable { mutableStateOf(state.dailyGoal.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomGoalDialog = false },
            title = { Text("自定义每日目标") },
            text = {
                Column {
                    Text(
                        text = "请输入每天的单词目标（${SettingsRepository.MIN_DAILY_GOAL} - ${SettingsRepository.MAX_DAILY_GOAL}）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DuoTextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { input ->
                            goalText = input.filter { it.isDigit() }.take(3)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("每日目标") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val goal = goalText.toIntOrNull()
                    if (goal != null && goal in SettingsRepository.MIN_DAILY_GOAL..SettingsRepository.MAX_DAILY_GOAL) {
                        showCustomGoalDialog = false
                        viewModel.selectDailyGoal(goal)
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomGoalDialog = false }) { Text("取消") }
            }
        )
    }

    licenseDialog?.let { (title, assetPath) ->
        LicenseDialog(
            title = title,
            assetPath = assetPath,
            onDismiss = { licenseDialog = null }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DuoSurface)
            .border(2.dp, DuoBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = DuoTextPrimary
        )
        content()
    }
}

@Composable
private fun LibraryRow(
    name: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) DuoGreen.copy(alpha = 0.12f) else Color.White)
            .border(
                2.dp,
                if (selected) DuoGreen else DuoBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) DuoGreen else DuoTextPrimary
            )
            Text(
                text = if (count == null) "词数统计中…" else "${formatCount(count)} 个单词",
                style = MaterialTheme.typography.bodySmall,
                color = DuoTextSecondary
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "已选择",
                tint = DuoGreen
            )
        }
    }
}

@Composable
private fun OptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) DuoGreen else Color.White)
            .border(
                2.dp,
                if (selected) DuoGreen else DuoBorder,
                RoundedCornerShape(percent = 50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else DuoTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LicenseDialog(
    title: String,
    assetPath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val licenseText by produceState(initialValue = "许可证文本加载中…", key1 = assetPath) {
        value = runCatching {
            context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrElse { "许可证文本加载失败：$assetPath" }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = licenseText,
                    style = MaterialTheme.typography.bodySmall,
                    color = DuoTextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

private fun formatCount(count: Int): String {
    return String.format("%,d", count)
}
