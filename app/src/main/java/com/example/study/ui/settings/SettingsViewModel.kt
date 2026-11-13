package com.example.study.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.study.appContainer
import com.example.study.data.backup.BackupData
import com.example.study.data.backup.BackupRepository
import com.example.study.data.backup.BackupSettings
import com.example.study.data.repo.SettingsRepository
import com.example.study.data.repo.StudyRepository
import com.example.study.data.repo.WordRepository
import com.example.study.reminder.ReminderScheduler
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val library: String = SettingsRepository.LIB_CET4,
    val dailyGoal: Int = SettingsRepository.DEFAULT_DAILY_GOAL,
    val accent: String = SettingsRepository.ACCENT_US,
    val wordCounts: Map<String, Int> = emptyMap(),
    val message: String? = null,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val themeMode: String = SettingsRepository.THEME_SYSTEM
)

class SettingsViewModel(
    private val wordRepository: WordRepository,
    private val studyRepository: StudyRepository,
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
    private val reminderScheduler: ReminderScheduler,
    private val appContext: Context
) : ViewModel() {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            wordRepository.ensureReady()
            val counts = SettingsRepository.LIBRARIES.associateWith { library ->
                wordRepository.countWords(library)
            }
            _uiState.update { it.copy(wordCounts = counts) }
        }
        viewModelScope.launch {
            settingsRepository.currentLibrary.collect { library ->
                _uiState.update { it.copy(library = library) }
            }
        }
        viewModelScope.launch {
            settingsRepository.dailyGoal.collect { goal ->
                _uiState.update { it.copy(dailyGoal = goal) }
            }
        }
        viewModelScope.launch {
            settingsRepository.accent.collect { accent ->
                _uiState.update { it.copy(accent = accent) }
            }
        }
        viewModelScope.launch {
            settingsRepository.reminderEnabled.collect { enabled ->
                _uiState.update { it.copy(reminderEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.reminderHour.collect { hour ->
                _uiState.update { it.copy(reminderHour = hour) }
            }
        }
        viewModelScope.launch {
            settingsRepository.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
    }

    fun selectLibrary(library: String) {
        viewModelScope.launch {
            settingsRepository.setCurrentLibrary(library)
            _uiState.update { it.copy(message = "已切换到 $library 词库") }
        }
    }

    fun selectDailyGoal(goal: Int) {
        viewModelScope.launch {
            settingsRepository.setDailyGoal(goal)
            _uiState.update { it.copy(message = "每日目标已设为 $goal 个单词") }
        }
    }

    fun selectAccent(accent: String) {
        viewModelScope.launch {
            settingsRepository.setAccent(accent)
            _uiState.update { it.copy(message = "发音口音已切换为 $accent") }
        }
    }

    fun resetCurrentLibrary() {
        viewModelScope.launch {
            val library = _uiState.value.library
            studyRepository.resetLibrary(library)
            _uiState.update { it.copy(message = "已重置 $library 的学习进度（词库保留）") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /** 开启每日提醒（通知权限由界面先申请）。 */
    fun enableReminder() {
        viewModelScope.launch {
            val hour = _uiState.value.reminderHour
            settingsRepository.setReminderEnabled(true)
            reminderScheduler.schedule(hour, 0)
            _uiState.update { it.copy(message = "已开启每日提醒：每天 ${"%02d".format(hour)}:00") }
        }
    }

    fun disableReminder() {
        viewModelScope.launch {
            settingsRepository.setReminderEnabled(false)
            reminderScheduler.cancel()
            _uiState.update { it.copy(message = "已关闭每日提醒") }
        }
    }

    fun setReminderTime(hour: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(hour)
            if (_uiState.value.reminderEnabled) {
                reminderScheduler.schedule(hour, 0)
            }
            _uiState.update { it.copy(message = "提醒时间已设为 ${"%02d".format(hour)}:00") }
        }
    }

    fun postTestReminder() {
        val ok = reminderScheduler.postTestNotification()
        _uiState.update {
            it.copy(
                message = if (ok) "已发送测试提醒，请下拉通知栏查看" else "发送失败：请先允许通知权限"
            )
        }
    }

    fun onNotificationPermissionDenied() {
        _uiState.update { it.copy(message = "未授予通知权限，无法开启每日提醒（可在系统设置中开启）") }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
            val label = when (mode) {
                SettingsRepository.THEME_LIGHT -> "浅色"
                SettingsRepository.THEME_DARK -> "深色"
                else -> "跟随系统"
            }
            _uiState.update { it.copy(message = "外观已切换为：$label") }
        }
    }

    /** 导出学习数据到用户选择的 JSON 文件。 */
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val data = backupRepository.export().copy(
                    settings = BackupSettings(
                        library = settingsRepository.currentLibrary.first(),
                        dailyGoal = settingsRepository.dailyGoal.first(),
                        accent = settingsRepository.accent.first(),
                        vocabEstimate = settingsRepository.vocabEstimate.first(),
                        vocabTestDate = settingsRepository.vocabTestDate.first()
                    )
                )
                val json = gson.toJson(data)
                appContext.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("无法打开写入目标")
                _uiState.update {
                    it.copy(message = "备份已导出：进度 ${data.progress.size} 条、事件 ${data.events.size} 条、收藏 ${data.favorites.size} 条")
                }
            }.onFailure { error ->
                _uiState.update { it.copy(message = "导出失败：${error.message ?: "未知错误"}") }
            }
        }
    }

    /** 从用户选择的 JSON 文件恢复学习数据（会覆盖现有数据）。 */
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = appContext.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: error("无法读取文件")
                val data = gson.fromJson(json, BackupData::class.java)
                    ?: error("备份文件内容为空")
                require(data.formatVersion == BackupData.CURRENT_FORMAT_VERSION) {
                    "不支持的备份版本：${data.formatVersion}"
                }
                val result = backupRepository.import(data)
                settingsRepository.setCurrentLibrary(data.settings.library)
                settingsRepository.setDailyGoal(data.settings.dailyGoal)
                settingsRepository.setAccent(data.settings.accent)
                settingsRepository.saveVocabEstimate(data.settings.vocabEstimate, data.settings.vocabTestDate)
                _uiState.update {
                    it.copy(
                        message = "恢复完成：进度 ${result.progress} 条、事件 ${result.events} 条、收藏 ${result.favorites} 条（跳过 ${result.skipped} 条）"
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(message = "导入失败：${error.message ?: "文件格式错误"}") }
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                SettingsViewModel(
                    wordRepository = container.wordRepository,
                    studyRepository = container.studyRepository,
                    settingsRepository = container.settingsRepository,
                    backupRepository = container.backupRepository,
                    reminderScheduler = ReminderScheduler(context.applicationContext),
                    appContext = context.applicationContext
                )
            }
        }
    }
}
