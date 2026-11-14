package com.example.study.ui.study

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.study.appContainer
import com.example.study.data.db.StudyStatus
import com.example.study.data.db.WordEntity
import com.example.study.data.repo.FavoriteRepository
import com.example.study.data.plan.PlanRepository
import com.example.study.data.repo.SettingsRepository
import com.example.study.data.repo.StudyRepository
import com.example.study.data.repo.TtsManager
import com.example.study.data.repo.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class StudyUiState(
    val initialized: Boolean = false,
    val loadingWord: Boolean = true,
    val library: String = SettingsRepository.LIB_CET4,
    val dailyGoal: Int = SettingsRepository.DEFAULT_DAILY_GOAL,
    val todayStudied: Int = 0,
    val streak: Int = 0,
    val word: WordEntity? = null,
    val wordStatus: Int = 0,
    val flipped: Boolean = false,
    val filter: Int = -1,
    val roundViewed: Int = 0,
    val accent: String = SettingsRepository.ACCENT_US,
    val isFavorite: Boolean = false,
    val hasPlan: Boolean = false,
    val planName: String = "",
    val planDaysLeft: Int = 0,
    val planTargetWords: Int = 0,
    val planLearnedWords: Int = 0,
    val planMasteredWords: Int = 0,
    val planTodayNewTotal: Int = 0,
    val planTodayNewLearned: Int = 0,
    val planDueReviewCount: Int = 0,
    val message: String? = null
) {
    val goalProgress: Float
        get() = if (dailyGoal <= 0) 0f else (todayStudied.toFloat() / dailyGoal).coerceIn(0f, 1f)

    val goalReached: Boolean get() = dailyGoal > 0 && todayStudied >= dailyGoal
}

class StudyViewModel(
    private val wordRepository: WordRepository,
    private val studyRepository: StudyRepository,
    private val settingsRepository: SettingsRepository,
    private val favoriteRepository: FavoriteRepository,
    private val planRepository: PlanRepository,
    private val ttsManager: TtsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    /** 学习页停留计时起点（学习时长统计）。 */
    private var sessionStartMs: Long? = null

    /** 本轮已抽过的词，保证一轮内不重复；持久化到 round_seen，进程重启后恢复。 */
    private val roundSeen = mutableSetOf<Long>()

    init {
        viewModelScope.launch {
            val library = settingsRepository.currentLibrary.first()
            val goal = settingsRepository.dailyGoal.first()
            val accent = settingsRepository.accent.first()
            _uiState.update { it.copy(library = library, dailyGoal = goal, accent = accent) }
            ttsManager.setAccent(accent)
            wordRepository.ensureReady()
            roundSeen.clear()
            roundSeen.addAll(studyRepository.loadRound(library, _uiState.value.filter))
            _uiState.update { it.copy(initialized = true, roundViewed = roundSeen.size) }
            loadPlanSummary()
            loadNextWordInternal()
        }
        viewModelScope.launch {
            settingsRepository.dailyGoal.collect { goal -> _uiState.update { it.copy(dailyGoal = goal) } }
        }
        viewModelScope.launch {
            settingsRepository.accent.collect { accent ->
                ttsManager.setAccent(accent)
                _uiState.update { it.copy(accent = accent) }
            }
        }
        viewModelScope.launch {
            settingsRepository.currentLibrary.collect { library ->
                val previous = _uiState.value.library
                _uiState.update { it.copy(library = library) }
                if (previous != library && _uiState.value.initialized) {
                    roundSeen.clear()
                    roundSeen.addAll(studyRepository.loadRound(library, _uiState.value.filter))
                    _uiState.update { it.copy(roundViewed = roundSeen.size) }
                    loadNextWordInternal()
                }
            }
        }
        viewModelScope.launch {
            favoriteRepository.favoriteIds.collect { favoriteIds ->
                val currentWordId = _uiState.value.word?.id
                _uiState.update {
                    it.copy(isFavorite = currentWordId != null && favoriteIds.contains(currentWordId))
                }
            }
        }
    }

    fun flip() {
        _uiState.update { it.copy(flipped = !it.flipped) }
    }

    fun setFilter(filter: Int) {
        if (_uiState.value.filter == filter) return
        viewModelScope.launch {
            val library = _uiState.value.library
            val persisted = studyRepository.loadRound(library, filter)
            roundSeen.clear()
            roundSeen.addAll(persisted)
            _uiState.update { it.copy(filter = filter, roundViewed = persisted.size) }
            loadNextWordInternal()
        }
    }

    fun nextWord() {
        if (_uiState.value.loadingWord) return
        loadNextWordInternal()
    }

    /**
     * 标记学习状态（1 学习中 / 2 已掌握）。
     * 只有翻到背面后的标记才算「有效学习」，写入 Room 并刷新今日目标与 streak。
     */
    fun markStatus(status: Int) {
        val state = _uiState.value
        val word = state.word ?: return
        if (!state.flipped) {
            _uiState.update { it.copy(message = "先点卡片翻到背面，再标记学习状态") }
            return
        }
        viewModelScope.launch {
            studyRepository.markStudied(word.id, status)
            val stats = studyRepository.stats(state.library)
            val reachedJustNow = !state.goalReached && stats.todayStudied >= state.dailyGoal
            _uiState.update {
                it.copy(
                    wordStatus = status,
                    todayStudied = stats.todayStudied,
                    streak = stats.streak,
                    message = when {
                        reachedJustNow -> "今日目标完成！继续保持"
                        status == StudyStatus.MASTERED -> "已掌握：${word.word}"
                        else -> "已标记为学习中：${word.word}"
                    }
                )
            }
        }
    }

    fun speakCurrentWord() {
        val word = _uiState.value.word ?: return
        if (!ttsManager.speak(word.word)) {
            _uiState.update { it.copy(message = "内置语音引擎初始化失败，暂时无法发音") }
        }
    }

    /** 收藏 / 取消收藏当前单词。 */
    fun toggleFavorite() {
        val word = _uiState.value.word ?: return
        viewModelScope.launch {
            val nowFavorite = favoriteRepository.toggle(word.id)
            _uiState.update {
                it.copy(
                    isFavorite = nowFavorite,
                    message = if (nowFavorite) "已加入收藏：${word.word}" else "已取消收藏：${word.word}"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /** 学习页进入/回到前台：开始计时（学习时长统计）。 */
    fun onStudySessionStart() {
        if (sessionStartMs == null) sessionStartMs = System.currentTimeMillis()
    }

    /** 学习页离开/切后台：上报本次停留时长，累计到 study_duration。 */
    fun onStudySessionStop() {
        val start = sessionStartMs ?: return
        sessionStartMs = null
        val elapsed = System.currentTimeMillis() - start
        if (elapsed < MIN_SESSION_MS) return
        viewModelScope.launch {
            studyRepository.addStudyDuration(_uiState.value.library, elapsed)
        }
    }

    /**
     * 从数据库重新读取当前词库的今日目标/streak、当前词状态与本轮进度。
     * 用于从记录页/词库页返回学习页时刷新（例如在词库页重置进度后，本轮应一并清空）。
     */
    fun refreshFromDatabase() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.initialized) return@launch
            loadPlanSummary()
            val persistedRound = studyRepository.loadRound(state.library, state.filter)
            roundSeen.clear()
            roundSeen.addAll(persistedRound)
            val stats = studyRepository.stats(state.library)
            val status = state.word?.let { studyRepository.findProgress(it.id)?.status } ?: 0
            _uiState.update {
                it.copy(
                    todayStudied = stats.todayStudied,
                    streak = stats.streak,
                    wordStatus = status,
                    roundViewed = persistedRound.size
                )
            }
        }
    }

    override fun onCleared() {
        onStudySessionStop()
        ttsManager.shutdown()
        super.onCleared()
    }

    /** 加载当前专项计划的今日任务摘要（无计划时清空）。 */
    private suspend fun loadPlanSummary() {
        val plan = planRepository.activePlan()
        if (plan == null) {
            _uiState.update {
                it.copy(
                    hasPlan = false,
                    planName = "",
                    planDaysLeft = 0,
                    planTargetWords = 0,
                    planLearnedWords = 0,
                    planMasteredWords = 0,
                    planTodayNewTotal = 0,
                    planTodayNewLearned = 0,
                    planDueReviewCount = 0
                )
            }
            return
        }
        val today = LocalDate.now()
        planRepository.ensureTodayNewWords(plan, today)
        val summary = planRepository.summary(plan, today)
        _uiState.update {
            it.copy(
                hasPlan = true,
                planName = plan.name,
                planDaysLeft = summary.daysUntilExam,
                planTargetWords = summary.targetWords,
                planLearnedWords = summary.learnedWords,
                planMasteredWords = summary.masteredWords,
                planTodayNewTotal = summary.todayNewTotal,
                planTodayNewLearned = summary.todayNewLearned,
                planDueReviewCount = summary.dueReviewCount
            )
        }
    }

    private fun loadNextWordInternal() {
        viewModelScope.launch {
            val snapshot = _uiState.value
            if (!snapshot.initialized) return@launch
            _uiState.update { it.copy(loadingWord = true, flipped = false) }
            val word = pickWord(snapshot.library, snapshot.filter)
            val status = word?.let { studyRepository.findProgress(it.id)?.status } ?: 0
            val favorite = word?.let { favoriteRepository.isFavorite(it.id) } ?: false
            val stats = studyRepository.stats(snapshot.library)
            _uiState.update {
                it.copy(
                    word = word,
                    wordStatus = status,
                    loadingWord = false,
                    flipped = false,
                    isFavorite = favorite,
                    todayStudied = stats.todayStudied,
                    streak = stats.streak,
                    roundViewed = roundSeen.size
                )
            }
        }
    }

    /** 随机抽词并做本轮去重；一轮抽满仍无新词时重开一轮。抽到的词立即持久化。 */
    private suspend fun pickWord(library: String, filter: Int): WordEntity? {
        repeat(MAX_PICK_TRIES) {
            val candidate = wordRepository.randomWord(library, filter) ?: return null
            if (roundSeen.add(candidate.id)) {
                studyRepository.markRoundSeen(library, filter, candidate.id)
                return candidate
            }
        }
        roundSeen.clear()
        studyRepository.clearRound(library, filter)
        return wordRepository.randomWord(library, filter)?.also {
            roundSeen.add(it.id)
            studyRepository.markRoundSeen(library, filter, it.id)
        }
    }

    companion object {
        const val FILTER_ALL = -1
        private const val MAX_PICK_TRIES = 40
        private const val MIN_SESSION_MS = 3_000L

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                StudyViewModel(
                    wordRepository = container.wordRepository,
                    studyRepository = container.studyRepository,
                    settingsRepository = container.settingsRepository,
                    favoriteRepository = container.favoriteRepository,
                    planRepository = container.planRepository,
                    ttsManager = TtsManager(context.applicationContext)
                )
            }
        }
    }
}
