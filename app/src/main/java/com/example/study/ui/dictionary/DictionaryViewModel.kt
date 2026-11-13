package com.example.study.ui.dictionary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.study.appContainer
import com.example.study.data.db.WordEntity
import com.example.study.data.repo.SettingsRepository
import com.example.study.data.repo.TtsManager
import com.example.study.data.repo.WordRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DictionaryUiState(
    val query: String = "",
    val scope: String = "CURRENT",
    val currentLibrary: String = SettingsRepository.LIB_CET4,
    val results: List<WordEntity> = emptyList(),
    val loading: Boolean = false,
    val searched: Boolean = false,
    val message: String? = null
)

/** 词典搜索：按单词 / 释义在完整词库中检索（默认当前词库，可切词库或全部）。 */
class DictionaryViewModel(
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository,
    private val ttsManager: TtsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DictionaryUiState())
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            wordRepository.ensureReady()
            settingsRepository.currentLibrary.collect { library ->
                _uiState.update { it.copy(currentLibrary = library) }
                if (_uiState.value.query.isNotBlank()) scheduleSearch(immediate = true)
            }
        }
        viewModelScope.launch {
            settingsRepository.accent.collect { accent -> ttsManager.setAccent(accent) }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        if (query.isBlank()) {
            searchJob?.cancel()
            _uiState.update { it.copy(results = emptyList(), searched = false, loading = false) }
        } else {
            scheduleSearch()
        }
    }

    fun onScopeChange(scope: String) {
        if (_uiState.value.scope == scope) return
        _uiState.update { it.copy(scope = scope) }
        if (_uiState.value.query.isNotBlank()) scheduleSearch(immediate = true)
    }

    fun speak(word: String) {
        if (!ttsManager.speak(word)) {
            _uiState.update { it.copy(message = "系统未安装可用的英语语音引擎，无法发音") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        ttsManager.shutdown()
        super.onCleared()
    }

    private fun scheduleSearch(immediate: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (!immediate) delay(SEARCH_DEBOUNCE_MS)
            performSearch()
        }
    }

    private suspend fun performSearch() {
        val state = _uiState.value
        val query = state.query.trim()
        if (query.isEmpty()) return
        _uiState.update { it.copy(loading = true) }
        val library = when (state.scope) {
            SCOPE_CURRENT -> state.currentLibrary
            SCOPE_ALL -> ALL_LIBRARIES
            else -> state.scope
        }
        val results = wordRepository.searchWords(library, query, SEARCH_LIMIT)
        _uiState.update { it.copy(results = results, loading = false, searched = true) }
    }

    companion object {
        const val SCOPE_CURRENT = "CURRENT"
        const val SCOPE_ALL = "ALL"
        private const val ALL_LIBRARIES = "ALL"
        private const val SEARCH_LIMIT = 80
        private const val SEARCH_DEBOUNCE_MS = 250L

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                DictionaryViewModel(
                    wordRepository = container.wordRepository,
                    settingsRepository = container.settingsRepository,
                    ttsManager = TtsManager(context.applicationContext)
                )
            }
        }
    }
}
