package com.example.study.ui.wrongbook

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.study.appContainer
import com.example.study.data.db.WrongWordRow
import com.example.study.data.repo.SettingsRepository
import com.example.study.data.repo.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WrongBookUiState(
    val library: String = SettingsRepository.LIB_CET4,
    val loading: Boolean = true,
    val words: List<WrongWordRow> = emptyList()
)

/** 错题本：最近一次答题答错的词；复习答对后自动移出。 */
class WrongBookViewModel(
    private val studyRepository: StudyRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WrongBookUiState())
    val uiState: StateFlow<WrongBookUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val library = settingsRepository.currentLibrary.first()
            _uiState.update { it.copy(library = library) }
            refreshInternal(library)
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshInternal(_uiState.value.library) }
    }

    private suspend fun refreshInternal(library: String) {
        _uiState.update { it.copy(loading = true) }
        val words = studyRepository.wrongWords(library)
        _uiState.update { it.copy(loading = false, words = words) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                WrongBookViewModel(
                    studyRepository = container.studyRepository,
                    settingsRepository = container.settingsRepository
                )
            }
        }
    }
}
