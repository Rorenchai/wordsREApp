package com.example.study.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.study.appContainer
import com.example.study.data.repo.SettingsRepository
import com.example.study.data.repo.StudyRepository
import com.example.study.data.repo.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val library: String = SettingsRepository.LIB_CET4,
    val dailyGoal: Int = SettingsRepository.DEFAULT_DAILY_GOAL,
    val accent: String = SettingsRepository.ACCENT_US,
    val wordCounts: Map<String, Int> = emptyMap(),
    val message: String? = null
)

class SettingsViewModel(
    private val wordRepository: WordRepository,
    private val studyRepository: StudyRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                SettingsViewModel(
                    wordRepository = container.wordRepository,
                    studyRepository = container.studyRepository,
                    settingsRepository = container.settingsRepository
                )
            }
        }
    }
}
