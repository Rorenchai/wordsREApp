package com.example.study.ui.quiz

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.study.appContainer
import com.example.study.data.db.WordEntity
import com.example.study.data.plan.PlanRepository
import com.example.study.data.repo.SettingsRepository
import com.example.study.data.repo.StudyRepository
import com.example.study.data.repo.TtsManager
import com.example.study.data.repo.WordRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class QuizUiState(
    val library: String = SettingsRepository.LIB_CET4,
    val loading: Boolean = true,
    val word: WordEntity? = null,
    val options: List<String> = emptyList(),
    val answerIndex: Int = -1,
    val selectedIndex: Int? = null,
    val answered: Boolean = false,
    val correct: Boolean? = null,
    val sessionAnswered: Int = 0,
    val sessionCorrect: Int = 0,
    val message: String? = null
) {
    val sessionAccuracy: Int
        get() = if (sessionAnswered == 0) 0 else sessionCorrect * 100 / sessionAnswered
}

/**
 * 看词选译（四选一答题）：随机主词 + 3 个干扰释义，判对错后自动换下一题。
 * 答题结果同时写入学习事件（带 answerCorrect 标记），供记录页正确率统计。
 */
class QuizViewModel(
    private val wordRepository: WordRepository,
    private val studyRepository: StudyRepository,
    private val settingsRepository: SettingsRepository,
    private val planRepository: PlanRepository,
    private val planMode: Boolean,
    private val ttsManager: TtsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

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
        viewModelScope.launch {
            settingsRepository.accent.collect { accent -> ttsManager.setAccent(accent) }
        }
    }

    /** 用户点选一个选项：先判对错、落库并展示反馈，约 1.1 秒后自动换下一题。 */
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
                sessionAnswered = it.sessionAnswered + 1,
                sessionCorrect = it.sessionCorrect + if (correct) 1 else 0
            )
        }
        viewModelScope.launch {
            studyRepository.recordQuizAnswer(word.id, correct)
            if (planMode) {
                val plan = planRepository.activePlan()
                if (plan != null && planRepository.recordGameReview(plan, word.id, correct, LocalDate.now())) {
                    _uiState.update { it.copy(message = "已计入计划复习：${word.word}") }
                }
            }
        }
        advanceJob = viewModelScope.launch {
            // 答错时停留更久，方便看清正确答案
            delay(if (correct) ADVANCE_DELAY_CORRECT_MS else ADVANCE_DELAY_WRONG_MS)
            loadNextQuestion()
        }
    }

    /** 跳过等待立即换下一题（可选的手动推进入口）。 */
    fun nextQuestion() {
        advanceJob?.cancel()
        loadNextQuestion()
    }

    fun speakWord() {
        val word = _uiState.value.word ?: return
        if (!ttsManager.speak(word.word)) {
            _uiState.update { it.copy(message = "系统未安装可用的英语语音引擎，无法发音") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        advanceJob?.cancel()
        ttsManager.shutdown()
        super.onCleared()
    }

    private fun loadNextQuestion() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    answered = false,
                    selectedIndex = null,
                    correct = null
                )
            }
            val library = _uiState.value.library
            val planWord = if (planMode) {
                planRepository.activePlan()?.let { plan ->
                    planRepository.gamePool(plan, LocalDate.now()).randomWord()
                }
            } else {
                null
            }
            val word = planWord ?: wordRepository.randomWord(library, ALL_STATUSES)
            if (word == null) {
                _uiState.update { it.copy(loading = false, word = null, options = emptyList()) }
                return@launch
            }
            val distractors = wordRepository.randomMeanings(library, word.word, OPTION_COUNT - 1)
            val options = mutableListOf(word.meaning)
            distractors.forEach { meaning ->
                if (meaning.isNotBlank() && options.size < OPTION_COUNT && !options.contains(meaning)) {
                    options.add(meaning)
                }
            }
            if (options.size < OPTION_COUNT) {
                // 极端情况下干扰项不足（词库异常），不生成不完整题目
                _uiState.update { it.copy(loading = false, message = "词库数据不足，无法生成题目") }
                return@launch
            }
            options.shuffle()
            _uiState.update {
                it.copy(
                    loading = false,
                    word = word,
                    options = options,
                    answerIndex = options.indexOf(word.meaning),
                    answered = false,
                    selectedIndex = null,
                    correct = null
                )
            }
        }
    }

    companion object {
        private const val OPTION_COUNT = 4
        private const val ALL_STATUSES = -1
        private const val ADVANCE_DELAY_CORRECT_MS = 900L
        private const val ADVANCE_DELAY_WRONG_MS = 2200L

        fun factory(context: Context, planMode: Boolean): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                QuizViewModel(
                    wordRepository = container.wordRepository,
                    studyRepository = container.studyRepository,
                    settingsRepository = container.settingsRepository,
                    planRepository = container.planRepository,
                    planMode = planMode,
                    ttsManager = TtsManager(context.applicationContext)
                )
            }
        }
    }
}
