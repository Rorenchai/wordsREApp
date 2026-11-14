package com.example.study.ui.overview

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.study.appContainer
import com.example.study.data.db.WordOverviewItem
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

/** 单词总览筛选：0 全部、1 已掌握、2 未掌握。 */
object OverviewFilter {
    const val ALL = 0
    const val MASTERED = 1
    const val UNMASTERED = 2
}

data class WordOverviewUiState(
    val library: String = SettingsRepository.LIB_CET4,
    val filter: Int = OverviewFilter.ALL,
    val query: String = "",
    val words: List<WordOverviewItem> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val message: String? = null
)

/** 单词总览：按「全部 / 已掌握 / 未掌握」筛选、搜索、分页浏览与详情发音。 */
class WordOverviewViewModel(
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository,
    private val ttsManager: TtsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordOverviewUiState())
    val uiState: StateFlow<WordOverviewUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            wordRepository.ensureReady()
            settingsRepository.currentLibrary.collect { library ->
                _uiState.update { it.copy(library = library) }
                reload()
            }
        }
        viewModelScope.launch {
            settingsRepository.accent.collect { accent -> ttsManager.setAccent(accent) }
        }
    }

    fun setFilter(filter: Int) {
        if (_uiState.value.filter == filter) return
        _uiState.update { it.copy(filter = filter) }
        reload()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250) // 输入防抖，避免每个字符都查库
            reload()
        }
    }

    fun refresh() = reload()

    fun loadMore() {
        val state = _uiState.value
        if (state.loading || state.loadingMore || !state.hasMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            val items = wordRepository.overviewWords(
                library = state.library,
                filter = state.filter,
                query = state.query,
                limit = PAGE_SIZE,
                offset = state.words.size
            )
            _uiState.update {
                it.copy(
                    words = it.words + items,
                    loadingMore = false,
                    hasMore = items.size == PAGE_SIZE
                )
            }
        }
    }

    fun speak(word: String) {
        if (!ttsManager.speak(word)) {
            _uiState.update { it.copy(message = "内置语音引擎初始化失败，暂时无法发音") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun reload() {
        val state = _uiState.value
        searchJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val items = wordRepository.overviewWords(
                library = state.library,
                filter = state.filter,
                query = state.query,
                limit = PAGE_SIZE,
                offset = 0
            )
            _uiState.update {
                it.copy(
                    words = items,
                    loading = false,
                    loadingMore = false,
                    hasMore = items.size == PAGE_SIZE
                )
            }
        }
    }

    override fun onCleared() {
        ttsManager.shutdown()
        super.onCleared()
    }

    companion object {
        private const val PAGE_SIZE = 80

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                WordOverviewViewModel(
                    wordRepository = container.wordRepository,
                    settingsRepository = container.settingsRepository,
                    ttsManager = TtsManager(context.applicationContext)
                )
            }
        }
    }
}
