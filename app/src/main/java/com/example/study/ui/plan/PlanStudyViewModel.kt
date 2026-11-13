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
import com.example.study.data.plan.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class PlanStudyMode { NEW, REVIEW }

data class PlanStudyUiState(
    val loading: Boolean = true,
    val mode: PlanStudyMode = PlanStudyMode.NEW,
    val queueSize: Int = 0,
    val index: Int = 0,
    val word: PlanWordDetail? = null,
    val flipped: Boolean = false,
    val finished: Boolean = false,
    val doneCount: Int = 0,
    val correctCount: Int = 0,
    val message: String? = null
) {
    val title: String get() = if (mode == PlanStudyMode.NEW) "学习新词" else "计划复习"
}

/** 计划学习/复习：新词逐张学习，复习用艾宾浩斯「回想-判定」推进复习阶段。 */
class PlanStudyViewModel(
    private val mode: PlanStudyMode,
    private val planRepository: PlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanStudyUiState(mode = mode))
    val uiState: StateFlow<PlanStudyUiState> = _uiState.asStateFlow()

    private var plan: PlanEntity? = null
    private var queue: List<PlanWordDetail> = emptyList()

    init {
        viewModelScope.launch { loadQueue() }
    }

    fun flip() {
        _uiState.update { it.copy(flipped = !it.flipped) }
    }

    /** 新词：看完释义后标记「已学完」，安排次日首复习。 */
    fun completeNewWord() {
        val state = _uiState.value
        val word = state.word ?: return
        val activePlan = plan ?: return
        viewModelScope.launch {
            planRepository.markNewLearned(activePlan, word.word.id, LocalDate.now())
            _uiState.update { it.copy(doneCount = it.doneCount + 1) }
            advance()
        }
    }

    /** 复习：记住了 / 没记住，推进或次日重试艾宾浩斯阶段。 */
    fun submitReview(correct: Boolean) {
        val state = _uiState.value
        val word = state.word ?: return
        val activePlan = plan ?: return
        viewModelScope.launch {
            planRepository.submitReview(activePlan, word.word.id, correct, LocalDate.now())
            _uiState.update {
                it.copy(
                    doneCount = it.doneCount + 1,
                    correctCount = it.correctCount + if (correct) 1 else 0
                )
            }
            advance()
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private suspend fun loadQueue() {
        val today = LocalDate.now()
        val activePlan = planRepository.activePlan()
        plan = activePlan
        if (activePlan == null) {
            _uiState.update { it.copy(loading = false, finished = true, message = "当前没有进行中的计划") }
            return
        }
        planRepository.ensureTodayNewWords(activePlan, today)
        queue = if (mode == PlanStudyMode.NEW) {
            planRepository.todaysNewWords(activePlan.id, today)
        } else {
            planRepository.dueReviews(activePlan, today)
        }
        if (queue.isEmpty()) {
            _uiState.update { it.copy(loading = false, finished = true, queueSize = 0) }
        } else {
            loadQuestion(0)
        }
    }

    private fun advance() {
        val next = _uiState.value.index + 1
        if (next >= queue.size) {
            _uiState.update { it.copy(finished = true, loading = false, word = null) }
        } else {
            loadQuestion(next)
        }
    }

    private fun loadQuestion(index: Int) {
        val item = queue.getOrNull(index) ?: return
        _uiState.update {
            it.copy(
                loading = false,
                index = index,
                queueSize = queue.size,
                word = item,
                flipped = false
            )
        }
    }

    companion object {
        fun factory(context: Context, mode: PlanStudyMode): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                PlanStudyViewModel(mode, container.planRepository)
            }
        }
    }
}
