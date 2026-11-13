package com.example.study.ui.games

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.study.appContainer
import com.example.study.data.plan.PlanRepository
import com.example.study.data.repo.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GamesUiState(
    val hasPlan: Boolean = false,
    val planName: String = "",
    val todayNewLearned: Int = 0,
    val todayNewTotal: Int = 0,
    val dueReviewCount: Int = 0,
    val planMode: Boolean = false,
    val poolSize: Int = 0
)

/** 游戏页：展示今日计划上下文，并记住「优先练计划词」开关。 */
class GamesViewModel(
    private val planRepository: PlanRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamesUiState())
    val uiState: StateFlow<GamesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.gamePlanMode.collect { enabled ->
                _uiState.update { it.copy(planMode = enabled) }
            }
        }
        viewModelScope.launch { refreshPlan() }
    }

    fun refreshPlan() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val plan = planRepository.activePlan()
            if (plan == null) {
                _uiState.update { it.copy(hasPlan = false, planName = "", todayNewLearned = 0, todayNewTotal = 0, dueReviewCount = 0, poolSize = 0) }
                return@launch
            }
            planRepository.ensureTodayNewWords(plan, today)
            val summary = planRepository.summary(plan, today)
            val pool = planRepository.gamePool(plan, today)
            _uiState.update {
                it.copy(
                    hasPlan = true,
                    planName = plan.name,
                    todayNewLearned = summary.todayNewLearned,
                    todayNewTotal = summary.todayNewTotal,
                    dueReviewCount = summary.dueReviewCount,
                    poolSize = pool.size
                )
            }
        }
    }

    fun setPlanMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGamePlanMode(enabled) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                GamesViewModel(container.planRepository, container.settingsRepository)
            }
        }
    }
}
