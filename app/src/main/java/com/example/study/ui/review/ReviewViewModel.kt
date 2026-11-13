package com.example.study.ui.review

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.study.appContainer
import com.example.study.data.db.WordEntity
import com.example.study.data.repo.SettingsRepository
import com.example.study.data.repo.StudyRepository
import com.example.study.data.repo.WordRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewUiState(
    val library: String = SettingsRepository.LIB_CET4,
    val loading: Boolean = true,
    val queueSize: Int = 0,
    val index: Int = 0,
    val word: WordEntity? = null,
    val options: List<String> = emptyList(),
    val answerIndex: Int = -1,
    val selectedIndex: Int? = null,
    val answered: Boolean = false,
    val correct: Boolean? = null,
    val reviewed: Int = 0,
    val correctCount: Int = 0,
    val finished: Boolean = false,
    val message: String? = null
) {
    val accuracy: Int get() = if (reviewed == 0) 0 else correctCount * 100 / reviewed
}

/** 错题复习：只考错题本里的词，答对后该词最近一次记录变正确，自动移出错题本。 */
class ReviewViewModel(
    private val wordRepository: WordRepository,
    private val studyRepository: StudyRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private var advanceJob: Job? = null
    private var queue: List<WordEntity> = emptyList()

    init {
        viewModelScope.launch {
            wordRepository.ensureReady()
            val library = settingsRepository.currentLibrary.first()
            _uiState.update { it.copy(library = library) }
            loadQueue(library)
        }
    }

    fun selectOption(index: Int) {
        val state = _uiState.value
        val word = state.word ?: return
        if (state.answered || index !in state.options.indices) return

        val correct = index == state.answerIndex
        advanceJob?.cancel()
        _uiState.update {
            it.copy(
                selectedIndex = index,
                answered = true,
                correct = correct,
                reviewed = it.reviewed + 1,
                correctCount = it.correctCount + if (correct) 1 else 0
            )
        }
        viewModelScope.launch { studyRepository.recordQuizAnswer(word.id, correct) }
        advanceJob = viewModelScope.launch {
            delay(if (correct) ADVANCE_DELAY_CORRECT_MS else ADVANCE_DELAY_WRONG_MS)
            nextQuestion()
        }
    }

    fun restart() {
        viewModelScope.launch { loadQueue(_uiState.value.library) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        advanceJob?.cancel()
        super.onCleared()
    }

    private suspend fun loadQueue(library: String) {
        advanceJob?.cancel()
        _uiState.update {
            it.copy(
                loading = true,
                finished = false,
                reviewed = 0,
                correctCount = 0,
                index = 0,
                word = null,
                options = emptyList()
            )
        }
        queue = studyRepository.wrongWords(library).map { it.word }.shuffled()
        _uiState.update { it.copy(queueSize = queue.size) }
        if (queue.isEmpty()) {
            _uiState.update { it.copy(loading = false, finished = true) }
        } else {
            loadQuestion(0)
        }
    }

    private fun nextQuestion() {
        val next = _uiState.value.index + 1
        if (next >= queue.size) {
            _uiState.update { it.copy(finished = true, loading = false, word = null) }
        } else {
            loadQuestion(next)
        }
    }

    private fun loadQuestion(index: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, answered = false, selectedIndex = null, correct = null) }
            val word = queue.getOrNull(index)
            if (word == null) {
                _uiState.update { it.copy(loading = false, finished = true) }
                return@launch
            }
            val library = _uiState.value.library
            val distractors = wordRepository.randomMeanings(library, word.word, OPTION_COUNT - 1)
            val options = mutableListOf(word.meaning)
            distractors.forEach { meaning ->
                if (meaning.isNotBlank() && options.size < OPTION_COUNT && !options.contains(meaning)) {
                    options.add(meaning)
                }
            }
            if (options.size < OPTION_COUNT) {
                _uiState.update { it.copy(loading = false, message = "词库数据不足，无法生成复习题") }
                return@launch
            }
            options.shuffle()
            _uiState.update {
                it.copy(
                    loading = false,
                    index = index,
                    word = word,
                    options = options,
                    answerIndex = options.indexOf(word.meaning),
                    answered = false,
                    selectedIndex = null,
                    correct = null
                )
            }
            Log.d(TAG, "review word=${word.word} meaning=${word.meaning} options=$options")
        }
    }

    companion object {
        private const val TAG = "ReviewWrong"
        private const val OPTION_COUNT = 4
        private const val ADVANCE_DELAY_CORRECT_MS = 900L
        private const val ADVANCE_DELAY_WRONG_MS = 2200L

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                ReviewViewModel(
                    wordRepository = container.wordRepository,
                    studyRepository = container.studyRepository,
                    settingsRepository = container.settingsRepository
                )
            }
        }
    }
}
