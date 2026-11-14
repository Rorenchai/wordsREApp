package com.example.study.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.study.data.importer.CustomLibraryParser
import com.example.study.data.importer.CustomWordEntry
import com.example.study.data.repo.SettingsRepository
import com.example.study.ui.components.DuoButton
import com.example.study.ui.components.DuoButtonStyle
import com.example.study.ui.theme.DuoBackground
import com.example.study.ui.theme.DuoBlue
import com.example.study.ui.theme.DuoBorder
import com.example.study.ui.theme.DuoCard
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
    var showImportConfirm by rememberSaveable { mutableStateOf(false) }
    var pendingCustomImport by remember { mutableStateOf<PendingCustomImport?>(null) }
    var licenseDialog by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackup(uri)
    }
    val customImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = readTextFromUri(context, uri)
            val fileName = queryDisplayName(context, uri) ?: "自定义词库.txt"
            val entries = content?.let { runCatching { CustomLibraryParser.parse(fileName, it) }.getOrNull() }
            if (entries.isNullOrEmpty()) {
                viewModel.showMessage("未解析到单词，请检查文件格式（JSON / CSV / TXT）")
            } else {
                pendingCustomImport = PendingCustomImport(
                    defaultName = CustomLibraryParser.defaultLibraryName(fileName),
                    entries = entries
                )
            }
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.enableReminder() else viewModel.onNotificationPermissionDenied()
    }

    fun onReminderToggle(enabled: Boolean) {
        if (!enabled) {
            viewModel.disableReminder()
            return
        }
        val needsPermission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.enableReminder()
        }
    }

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
                state.libraries.forEach { item ->
                    LibraryRow(
                        name = item.library,
                        count = item.count,
                        selected = state.library == item.library,
                        onClick = { viewModel.selectLibrary(item.library) }
                    )
                }
            }

            SettingsSection(title = "自定义词库") {
                Text(
                    text = "可导入自己整理的词库文件，导入后作为独立词库出现在上方列表，"
                        + "学习、计划、收藏与备份均可使用。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DuoTextSecondary
                )
                Text(
                    text = "支持格式：\n"
                        + "• JSON：[{\"word\":\"apple\",\"phonetic\":\"ˈæpl\",\"meaning\":\"n. 苹果\"}]\n"
                        + "• CSV / TXT：每行「单词,释义」（也支持 Tab / | 分隔，# 开头为注释）",
                    style = MaterialTheme.typography.bodySmall,
                    color = DuoTextSecondary
                )
                DuoButton(
                    text = "导入词库文件",
                    onClick = { customImportLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    style = DuoButtonStyle.Secondary
                )
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

            SettingsSection(title = "发音") {
                Text(
                    text = "内置 Piper 神经网络女声（LJSpeech，完全离线），不依赖系统语音包；美式 / 英式仅语速略有差异",
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

            SettingsSection(title = "外观") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionChip(
                        text = "跟随系统",
                        selected = state.themeMode == SettingsRepository.THEME_SYSTEM,
                        onClick = { viewModel.setThemeMode(SettingsRepository.THEME_SYSTEM) }
                    )
                    OptionChip(
                        text = "浅色",
                        selected = state.themeMode == SettingsRepository.THEME_LIGHT,
                        onClick = { viewModel.setThemeMode(SettingsRepository.THEME_LIGHT) }
                    )
                    OptionChip(
                        text = "深色",
                        selected = state.themeMode == SettingsRepository.THEME_DARK,
                        onClick = { viewModel.setThemeMode(SettingsRepository.THEME_DARK) }
                    )
                }
                Text(
                    text = "深浅色会应用到全部页面，并记住你的选择。",
                    style = MaterialTheme.typography.bodySmall,
                    color = DuoTextSecondary
                )
            }

            SettingsSection(title = "数据备份") {
                Text(
                    text = "导出学习进度 / 学习记录 / 收藏 / 本轮进度 / 学习时长与设置到 JSON 文件；导入会覆盖当前全部学习数据。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DuoTextSecondary
                )
                DuoButton(
                    text = "导出备份",
                    onClick = {
                        val stamp = java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                        exportLauncher.launch("单词记忆卡备份_$stamp.json")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = DuoButtonStyle.Secondary
                )
                DuoButton(
                    text = "导入备份",
                    onClick = { showImportConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    style = DuoButtonStyle.Secondary
                )
            }

            SettingsSection(title = "学习提醒") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "每日提醒",
                            style = MaterialTheme.typography.titleMedium,
                            color = DuoTextPrimary
                        )
                        Text(
                            text = "每天定时提醒你完成背单词目标",
                            style = MaterialTheme.typography.bodySmall,
                            color = DuoTextSecondary
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = state.reminderEnabled,
                        onCheckedChange = ::onReminderToggle
                    )
                }
                if (state.reminderEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(19, 20, 21, 22).forEach { hour ->
                            OptionChip(
                                text = "%02d:00".format(hour),
                                selected = state.reminderHour == hour,
                                onClick = { viewModel.setReminderTime(hour) }
                            )
                        }
                    }
                    Text(
                        text = "由系统 WorkManager 每日调度，实际触发时间可能有几分钟误差。",
                        style = MaterialTheme.typography.bodySmall,
                        color = DuoTextSecondary
                    )
                }
                TextButton(onClick = viewModel::postTestReminder) {
                    Text("发送测试提醒", color = DuoBlue)
                }
            }

            SettingsSection(title = "关于与许可") {
                Text(
                    text = "词库来源：KyleBing/english-vocabulary（BSD 3-Clause License）· PETS-B 词库由项目整理\n" +
                        "发音：内置 Piper 神经网络女声（LJSpeech，离线），不依赖系统 TTS\n" +
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
                    TextButton(onClick = {
                        licenseDialog = "离线语音引擎许可（sherpa-onnx / Piper）" to "licenses/piper-tts.txt"
                    }) {
                        Text("语音引擎许可", color = DuoBlue)
                    }
                }
                Text(
                    text = "版本 1.1.0 · 单词记忆卡",
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

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("导入备份") },
            text = {
                Text("导入将覆盖当前全部学习进度、学习记录、收藏、本轮进度、学习时长，以及当前词库 / 每日目标 / 口音设置，且不可撤销。是否继续？")
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    importLauncher.launch(arrayOf("application/json"))
                }) {
                    Text("选择备份文件", color = DuoRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) { Text("取消") }
            }
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

    pendingCustomImport?.let { pending ->
        var name by rememberSaveable(pending) { mutableStateOf(pending.defaultName) }
        AlertDialog(
            onDismissRequest = { pendingCustomImport = null },
            title = { Text("导入自定义词库") },
            text = {
                Column {
                    Text(
                        text = "已解析 ${pending.entries.size} 个单词，保存为以下词库名（重名会自动合并去重）：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DuoTextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(40) },
                        singleLine = true,
                        label = { Text("词库名称") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val libraryName = name.trim().ifBlank { "自定义词库" }
                    pendingCustomImport = null
                    viewModel.importCustomLibrary(libraryName, pending.entries)
                }) { Text("导入", color = DuoBlue) }
            },
            dismissButton = {
                TextButton(onClick = { pendingCustomImport = null }) { Text("取消") }
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
            .background(if (selected) DuoGreen.copy(alpha = 0.12f) else DuoCard)
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
            .background(if (selected) DuoGreen else DuoCard)
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

/** 待确认的自定义词库导入：默认词库名 + 已解析词条。 */
private data class PendingCustomImport(
    val defaultName: String,
    val entries: List<CustomWordEntry>
)

/** 读取用户选择的词库文件（限制最大 20MB，避免误选超大文件卡顿）。 */
private fun readTextFromUri(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            val text = reader.readText()
            if (text.length <= MAX_CUSTOM_IMPORT_CHARS) text else null
        }
    } catch (error: Exception) {
        null
    }
}

/** 查询文件显示名，用于生成默认词库名。 */
private fun queryDisplayName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (error: Exception) {
        null
    }
}

private const val MAX_CUSTOM_IMPORT_CHARS = 20_000_000