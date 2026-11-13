package com.example.study.ui.plan

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.study.appContainer
import com.example.study.data.db.PlanEntity
import com.example.study.data.db.PlanWordDetail
import com.example.study.data.plan.EbbinghausSchedule
import com.example.study.data.plan.PlanRepository
import com.example.study.data.plan.PlanSummary
import com.example.study.data.repo.SettingsRepository
import com.example.study.data.repo.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlanUiState(
    val loading: Boolean = true,
    val activePlan: PlanEntity? = null,
    val summary: PlanSummary? = null,
    val todayNew: List<PlanWordDetail> = emptyList(),
    val dueReviews: List<PlanWordDetail> = emptyList(),
    // 创建表单
    val formLibrary: String = SettingsRepository.LIB_CET4,
    val libraryCounts: Map<String, Int> = emptyMap(),
    val formDays: Int = 60,
    val formDailyNewWords: Int = 40,
    val formReviewLimit: Int = EbbinghausSchedule.DEFAULT_DAILY_REVIEW_LIMIT,
    val formName: String = "",
    val suggestion: Int = 40,
    val feasible: Boolean = true,
    val creating: Boolean = false,
    val message: String? = null
)

/** 艾宾浩斯专项计划：创建向导 + 计划概览/今日任务。 */
class PlanViewModel(
    private val planRepository: PlanRepository,
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            wordRepository.ensureReady()
            val counts = SettingsRepository.LIBRARIES.associateWith { wordRepository.countWords(it) }
            val library = settingsRepository.currentLibrary.first()
            _uiState.update { it.copy(libraryCounts = counts, formLibrary = library) }
            applyDefaults(library, _uiState.value.formDays)
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val active = planRepository.activePlan()
            if (active == null) {
                _uiState.update { it.copy(loading = false, activePlan = null, summary = null) }
                return@launch
            }
            planRepository.ensureTodayNewWords(active, today)
            val summary = planRepository.summary(active, today)
            val todayNew = planRepository.todaysNewWords(active.id, today)
            val dueReviews = planRepository.dueReviews(active, today)
            _uiState.update {
                it.copy(
                    loading = false,
                    activePlan = active,
                    summary = summary,
                    todayNew = todayNew,
                    dueReviews = dueReviews
                )
            }
        }
    }

    fun setLibrary(library: String) {
        _uiState.update { it.copy(formLibrary = library) }
        applyDefaults(library, _uiState.value.formDays)
    }

    fun setDays(days: Int) {
        val safeDays = days.coerceIn(MIN_DAYS, MAX_DAYS)
        _uiState.update { it.copy(formDays = safeDays) }
        applyDefaults(_uiState.value.formLibrary, safeDays)
    }

    fun setDailyNewWords(value: Int) {
        _uiState.update {
            it.copy(
                formDailyNewWords = value.coerceIn(
                    EbbinghausSchedule.MIN_DAILY_NEW_WORDS,
                    EbbinghausSchedule.MAX_DAILY_NEW_WORDS
                )
            )
        }
        updateFeasibility()
    }

    fun setReviewLimit(value: Int) {
        _uiState.update {
            it.copy(
                formReviewLimit = value.coerceIn(
                    EbbinghausSchedule.MIN_DAILY_REVIEW_LIMIT,
                    EbbinghausSchedule.MAX_DAILY_REVIEW_LIMIT
                )
            )
        }
    }

    fun setName(name: String) {
        _uiState.update { it.copy(formName = name.take(20)) }
    }

    fun createPlan() {
        val state = _uiState.value
        if (state.creating) return
        viewModelScope.launch {
            _uiState.update { it.copy(creating = true) }
            runCatching {
                val today = LocalDate.now()
                val examDate = today.plusDays(state.formDays.toLong())
                val name = state.formName.ifBlank { "${state.formLibrary} 备考计划" }
                planRepository.createPlan(
                    name = name,
                    library = state.formLibrary,
                    today = today,
                    examDate = examDate,
                    dailyNewWords = state.formDailyNewWords,
                    dailyReviewLimit = state.formReviewLimit
                )
            }.onSuccess {
                _uiState.update { it.copy(message = "计划已创建") }
                refresh()
            }.onFailure { error ->
                _uiState.update { it.copy(message = "创建失败：${error.message}", creating = false) }
            }
        }
    }

    fun finishPlan() {
        val plan = _uiState.value.activePlan ?: return
        viewModelScope.launch {
            planRepository.finishPlan(plan.id)
            _uiState.update { it.copy(message = "计划已结束") }
            refresh()
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun applyDefaults(library: String, days: Int) {
        val words = _uiState.value.libraryCounts[library] ?: 0
        val suggestion = EbbinghausSchedule.suggestedDailyNewWords(words, days)
        _uiState.update {
            it.copy(
                formLibrary = library,
                formDays = days,
                formDailyNewWords = suggestion,
                suggestion = suggestion,
                formName = it.formName.ifBlank { "$library 备考计划" }
            )
        }
        updateFeasibility()
    }

    private fun updateFeasibility() {
        val state = _uiState.value
        val words = state.libraryCounts[state.formLibrary] ?: 0
        _uiState.update {
            it.copy(feasible = words <= state.formDays * state.formDailyNewWords)
        }
    }

    companion object {
        private const val MIN_DAYS = 7
        private const val MAX_DAYS = 365

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                PlanViewModel(
                    planRepository = container.planRepository,
                    wordRepository = container.wordRepository,
                    settingsRepository = container.settingsRepository
                )
            }
        }
    }
}
