package com.example.study.ui.records

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.study.appContainer
import com.example.study.data.repo.SettingsRepository
import com.example.study.data.repo.StudyRepository
import com.example.study.data.stats.LibraryStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class RecordsUiState(
    val library: String = SettingsRepository.LIB_CET4,
    val loading: Boolean = true,
    val stats: LibraryStats? = null,
    val vocabEstimate: Int = 0,
    val vocabDate: String = "",
    val lastCheckInDate: String = ""
)

class RecordsViewModel(
    private val studyRepository: StudyRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordsUiState())
    val uiState: StateFlow<RecordsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.currentLibrary.collect { library ->
                _uiState.update { it.copy(library = library, loading = true) }
                refreshInternal(library)
            }
        }
        viewModelScope.launch {
            settingsRepository.vocabEstimate.collect { estimate ->
                _uiState.update { it.copy(vocabEstimate = estimate) }
            }
        }
        viewModelScope.launch {
            settingsRepository.vocabTestDate.collect { date ->
                _uiState.update { it.copy(vocabDate = date) }
            }
        }
        viewModelScope.launch {
            settingsRepository.lastCheckInDate.collect { date ->
                _uiState.update { it.copy(lastCheckInDate = date) }
            }
        }
    }

    /** 记录今日打卡（奖励弹窗由界面展示）。 */
    fun checkIn() {
        viewModelScope.launch {
            settingsRepository.setLastCheckInDate(LocalDate.now().toString())
        }
    }

    /** 每次进入记录页都刷新，保证刚学习的进度能立刻反映。 */
    fun refresh() {
        viewModelScope.launch { refreshInternal(_uiState.value.library) }
    }

    private suspend fun refreshInternal(library: String) {
        val stats = studyRepository.stats(library)
        _uiState.update { it.copy(stats = stats, loading = false) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                RecordsViewModel(container.studyRepository, container.settingsRepository)
            }
        }
    }
}
