package com.example.study.ui.vocab

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.study.appContainer
import com.example.study.data.db.WordEntity
import com.example.study.data.repo.SettingsRepository
import com.example.study.data.repo.WordRepository
import com.example.study.data.stats.estimateVocabularySize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class VocabTestUiState(
    val loading: Boolean = true,
    val index: Int = 0,
    val total: Int = 0,
    val word: WordEntity? = null,
    val options: List<String> = emptyList(),
    val answerIndex: Int = -1,
    val selectedIndex: Int? = null,
    val answered: Boolean = false,
    val correct: Boolean? = null,
    val correctCount: Int = 0,
    val answeredCount: Int = 0,
    val poolSize: Int = 0,
    val finished: Boolean = false,
    val estimate: Int = 0,
    val message: String? = null
) {
    val accuracy: Int get() = if (answeredCount == 0) 0 else correctCount * 100 / answeredCount
}

/** 词汇量评测：从 CET-4 + CET-6 词库随机抽 20 词做四选一，按正确率做猜测校正后估算词汇量。 */
class VocabTestViewModel(
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VocabTestUiState())
    val uiState: StateFlow<VocabTestUiState> = _uiState.asStateFlow()

    private var questions: List<WordEntity> = emptyList()
    private var advanceJob: Job? = null

    init {
        viewModelScope.launch {
            wordRepository.ensureReady()
            loadTest()
        }
    }

    fun selectOption(index: Int) {
        val state = _uiState.value
        val word = state.word ?: return
        if (state.answered || state.finished || index !in state.options.indices) return

        val correct = index == state.answerIndex
        val nextCorrect = state.correctCount + if (correct) 1 else 0
        val nextAnswered = state.answeredCount + 1
        _uiState.update {
            it.copy(
                selectedIndex = index,
                answered = true,
                correct = correct,
                correctCount = nextCorrect,
                answeredCount = nextAnswered
            )
        }
        advanceJob?.cancel()
        advanceJob = viewModelScope.launch {
            delay(ADVANCE_DELAY_MS)
            if (nextAnswered >= questions.size) {
                finishTest(nextCorrect, nextAnswered)
            } else {
                loadQuestion(_uiState.value.index + 1)
            }
        }
    }

    fun restart() {
        advanceJob?.cancel()
        viewModelScope.launch { loadTest() }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        advanceJob?.cancel()
        super.onCleared()
    }

    private suspend fun loadTest() {
        advanceJob?.cancel()
        _uiState.update {
            it.copy(
                loading = true,
                finished = false,
                index = 0,
                word = null,
                options = emptyList(),
                answered = false,
                selectedIndex = null,
                correct = null,
                correctCount = 0,
                answeredCount = 0,
                estimate = 0
            )
        }
        val pool = SettingsRepository.LIBRARIES.sumOf { wordRepository.countWords(it) }
        questions = wordRepository.randomWordEntitiesAll(SAMPLE_SIZE)
        _uiState.update { it.copy(poolSize = pool, total = questions.size) }
        if (questions.isEmpty()) {
            _uiState.update { it.copy(loading = false, message = "词库为空，无法评测") }
            return
        }
        loadQuestion(0)
    }

    private fun loadQuestion(index: Int) {
        viewModelScope.launch {
            val word = questions.getOrNull(index) ?: return@launch
            _uiState.update {
                it.copy(loading = true, index = index, answered = false, selectedIndex = null, correct = null)
            }
            val distractors = wordRepository.randomMeanings(word.library, word.word, OPTION_COUNT - 1)
            val options = mutableListOf(word.meaning)
            distractors.forEach { meaning ->
                if (meaning.isNotBlank() && options.size < OPTION_COUNT && !options.contains(meaning)) {
                    options.add(meaning)
                }
            }
            if (options.size < OPTION_COUNT) {
                _uiState.update { it.copy(loading = false, message = "词库数据不足，无法生成题目") }
                return@launch
            }
            options.shuffle()
            _uiState.update {
                it.copy(
                    loading = false,
                    word = word,
                    options = options,
                    answerIndex = options.indexOf(word.meaning)
                )
            }
        }
    }

    private suspend fun finishTest(correctCount: Int, answeredCount: Int) {
        val state = _uiState.value
        val estimate = estimateVocabularySize(correctCount, answeredCount, state.poolSize)
        settingsRepository.saveVocabEstimate(estimate, LocalDate.now().toString())
        _uiState.update {
            it.copy(
                finished = true,
                loading = false,
                word = null,
                estimate = estimate
            )
        }
    }

    companion object {
        private const val SAMPLE_SIZE = 20
        private const val OPTION_COUNT = 4
        private const val ADVANCE_DELAY_MS = 700L

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                VocabTestViewModel(
                    wordRepository = container.wordRepository,
                    settingsRepository = container.settingsRepository
                )
            }
        }
    }
}
