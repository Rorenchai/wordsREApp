package com.example.study.ui.listening

import android.content.Context
import android.util.Log
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

data class ListeningUiState(
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
 * 听音辨词：自动播放目标单词发音，四选一选出正确拼写。
 * 答题结果复用 study_events.answerCorrect 落库，计入记录页答题统计。
 */
class ListeningViewModel(
    private val wordRepository: WordRepository,
    private val studyRepository: StudyRepository,
    private val settingsRepository: SettingsRepository,
    private val planRepository: PlanRepository,
    private val planMode: Boolean,
    private val ttsManager: TtsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListeningUiState())
    val uiState: StateFlow<ListeningUiState> = _uiState.asStateFlow()

    private var advanceJob: Job? = null
    private var playJob: Job? = null

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
            delay(if (correct) ADVANCE_DELAY_CORRECT_MS else ADVANCE_DELAY_WRONG_MS)
            loadNextQuestion()
        }
    }

    /** 重听当前单词。 */
    fun replay() {
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
        playJob?.cancel()
        ttsManager.shutdown()
        super.onCleared()
    }

    private fun loadNextQuestion() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true, answered = false, selectedIndex = null, correct = null)
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
            val distractors = wordRepository.randomWords(library, word.word, OPTION_COUNT - 1)
            val options = mutableListOf(word.word)
            distractors.forEach { candidate ->
                if (candidate.isNotBlank() && options.size < OPTION_COUNT && !options.contains(candidate)) {
                    options.add(candidate)
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
                    answerIndex = options.indexOf(word.word),
                    answered = false,
                    selectedIndex = null,
                    correct = null
                )
            }
            Log.d(TAG, "load target=${word.word} options=$options")
            autoPlay(word)
        }
    }

    /** 自动播放（TTS 引擎初始化是异步的，失败时短暂重试；切题后放弃旧任务）。 */
    private fun autoPlay(word: WordEntity) {
        playJob?.cancel()
        playJob = viewModelScope.launch {
            repeat(AUTO_PLAY_RETRY) { attempt ->
                if (_uiState.value.word?.id != word.id) return@launch
                if (ttsManager.speak(word.word)) return@launch
                if (attempt < AUTO_PLAY_RETRY - 1) delay(AUTO_PLAY_INTERVAL_MS)
            }
            if (_uiState.value.word?.id == word.id) {
                _uiState.update { it.copy(message = "系统未安装可用的英语语音引擎，无法发音") }
            }
        }
    }

    companion object {
        private const val TAG = "ListeningQuiz"
        private const val OPTION_COUNT = 4
        private const val ALL_STATUSES = -1
        private const val ADVANCE_DELAY_CORRECT_MS = 900L
        private const val ADVANCE_DELAY_WRONG_MS = 2200L
        private const val AUTO_PLAY_RETRY = 8
        private const val AUTO_PLAY_INTERVAL_MS = 350L

        fun factory(context: Context, planMode: Boolean): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                ListeningViewModel(
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
