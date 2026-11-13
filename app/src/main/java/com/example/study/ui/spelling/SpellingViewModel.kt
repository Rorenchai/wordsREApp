package com.example.study.ui.spelling

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpellingUiState(
    val library: String = SettingsRepository.LIB_CET4,
    val loading: Boolean = true,
    val word: WordEntity? = null,
    val input: String = "",
    val answered: Boolean = false,
    val correct: Boolean? = null,
    val sessionAnswered: Int = 0,
    val sessionCorrect: Int = 0,
    val message: String? = null
) {
    val sessionAccuracy: Int
        get() = if (sessionAnswered == 0) 0 else sessionCorrect * 100 / sessionAnswered
}

/** 单词拼写：根据中文释义输入正确拼写，判对错后自动换题。 */
class SpellingViewModel(
    private val wordRepository: WordRepository,
    private val studyRepository: StudyRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpellingUiState())
    val uiState: StateFlow<SpellingUiState> = _uiState.asStateFlow()

    private var advanceJob: Job? = null

    init {
        viewModelScope.launch {
            wordRepository.ensureReady()
            loadNextQuestion()
        }
        viewModelScope.launch {
            settingsRepository.currentLibrary.collect { library ->
                val changed = _uiState.value.library != library
                _uiState.update { it.copy(library = library) }
                if (changed) loadNextQuestion()
            }
        }
    }

    fun onInputChange(text: String) {
        if (_uiState.value.answered) return
        _uiState.update { it.copy(input = text.take(MAX_INPUT_LENGTH)) }
    }

    fun submit() {
        val state = _uiState.value
        val word = state.word ?: return
        if (state.answered) return
        val answer = state.input.trim()
        if (answer.isEmpty()) {
            _uiState.update { it.copy(message = "请先输入单词拼写") }
            return
        }

        val correct = answer.equals(word.word, ignoreCase = true)
        advanceJob?.cancel()
        _uiState.update {
            it.copy(
                answered = true,
                correct = correct,
                sessionAnswered = it.sessionAnswered + 1,
                sessionCorrect = it.sessionCorrect + if (correct) 1 else 0
            )
        }
        viewModelScope.launch { studyRepository.recordQuizAnswer(word.id, correct) }
        advanceJob = viewModelScope.launch {
            delay(if (correct) ADVANCE_DELAY_CORRECT_MS else ADVANCE_DELAY_WRONG_MS)
            loadNextQuestion()
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        advanceJob?.cancel()
        super.onCleared()
    }

    private fun loadNextQuestion() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true, input = "", answered = false, correct = null)
            }
            val word = wordRepository.randomWord(_uiState.value.library, ALL_STATUSES)
            if (word == null) {
                _uiState.update { it.copy(loading = false, word = null) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    loading = false,
                    word = word,
                    input = "",
                    answered = false,
                    correct = null
                )
            }
            Log.d(TAG, "load target=${word.word} meaning=${word.meaning}")
        }
    }

    companion object {
        private const val TAG = "SpellingQuiz"
        private const val ALL_STATUSES = -1
        private const val MAX_INPUT_LENGTH = 40
        private const val ADVANCE_DELAY_CORRECT_MS = 900L
        private const val ADVANCE_DELAY_WRONG_MS = 2200L

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                SpellingViewModel(
                    wordRepository = container.wordRepository,
                    studyRepository = container.studyRepository,
                    settingsRepository = container.settingsRepository
                )
            }
        }
    }
}
