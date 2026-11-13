package com.example.study.ui.match

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.study.appContainer
import com.example.study.data.plan.PlanRepository
import com.example.study.data.repo.SettingsRepository
import com.example.study.data.repo.WordRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 网格中的一张卡片：英文单词或其中文释义，pairId 相同的两张为一对。 */
data class MatchCard(
    val id: Int,
    val pairId: Long,
    val text: String,
    val isWord: Boolean,
    val matched: Boolean = false,
    val flipped: Boolean = false,
    val wrong: Boolean = false
)

data class MatchUiState(
    val library: String = SettingsRepository.LIB_CET4,
    val loading: Boolean = true,
    val cards: List<MatchCard> = emptyList(),
    val firstIndex: Int? = null,
    val locked: Boolean = false,
    val matchedPairs: Int = 0,
    val totalPairs: Int = PAIR_COUNT,
    val mistakes: Int = 0,
    val finished: Boolean = false,
    val message: String? = null
) {
    companion object {
        const val PAIR_COUNT = 6
    }
}

/**
 * 单词消消乐：6 对词义卡片打乱成 12 格网格，点两张配对；配对成功消除，失败短暂展示后翻回。
 * 全部配对完成后弹出结算，可再来一局。
 */
class MatchViewModel(
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository,
    private val planRepository: PlanRepository,
    private val planMode: Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchUiState())
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    private var hideJob: Job? = null

    init {
        viewModelScope.launch {
            wordRepository.ensureReady()
            loadGame()
        }
        viewModelScope.launch {
            settingsRepository.currentLibrary.collect { library ->
                val changed = _uiState.value.library != library
                _uiState.update { it.copy(library = library) }
                if (changed) loadGame()
            }
        }
    }

    fun onCardClick(index: Int) {
        val state = _uiState.value
        if (state.locked || state.finished || state.loading) return
        val card = state.cards.getOrNull(index) ?: return
        if (card.matched || card.flipped) return

        val flippedCards = state.cards.toMutableList()
        flippedCards[index] = card.copy(flipped = true)
        val firstIndex = state.firstIndex

        if (firstIndex == null) {
            _uiState.update { it.copy(cards = flippedCards, firstIndex = index) }
            return
        }

        val firstCard = state.cards[firstIndex]
        val isMatch = firstCard.pairId == card.pairId && firstCard.isWord != card.isWord
        if (isMatch) {
            val newCards = flippedCards.mapIndexed { i, c ->
                if (i == firstIndex || i == index) c.copy(matched = true, flipped = true, wrong = false) else c
            }
            val matchedPairs = state.matchedPairs + 1
            val finished = matchedPairs >= state.totalPairs
            _uiState.update {
                it.copy(
                    cards = newCards,
                    firstIndex = null,
                    matchedPairs = matchedPairs,
                    finished = finished
                )
            }
            Log.d(TAG, "matched pair ${firstCard.pairId} progress=$matchedPairs/${state.totalPairs}")
            if (planMode) {
                viewModelScope.launch {
                    val plan = planRepository.activePlan()
                    if (plan != null && planRepository.recordGameReview(plan, firstCard.pairId, true, LocalDate.now())) {
                        _uiState.update { it.copy(message = "已计入计划复习") }
                    }
                }
            }
        } else {
            val wrongCards = flippedCards.mapIndexed { i, c ->
                if (i == firstIndex || i == index) c.copy(wrong = true) else c
            }
            _uiState.update {
                it.copy(
                    cards = wrongCards,
                    locked = true,
                    mistakes = state.mistakes + 1
                )
            }
            hideJob?.cancel()
            hideJob = viewModelScope.launch {
                delay(FLIP_BACK_DELAY_MS)
                _uiState.update { current ->
                    current.copy(
                        cards = current.cards.mapIndexed { i, c ->
                            if (i == firstIndex || i == index) {
                                c.copy(flipped = false, wrong = false)
                            } else {
                                c
                            }
                        },
                        firstIndex = null,
                        locked = false
                    )
                }
            }
        }
    }

    fun restart() {
        hideJob?.cancel()
        loadGame()
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        hideJob?.cancel()
        super.onCleared()
    }

    private fun loadGame() {
        viewModelScope.launch {
            hideJob?.cancel()
            _uiState.update {
                it.copy(loading = true, cards = emptyList(), firstIndex = null, locked = false, matchedPairs = 0, mistakes = 0, finished = false)
            }
            val library = _uiState.value.library
            val poolWords = if (planMode) {
                planRepository.activePlan()?.let { plan ->
                    val pool = planRepository.gamePool(plan, LocalDate.now())
                    (pool.due + pool.fresh).distinctBy { it.id }
                } ?: emptyList()
            } else {
                emptyList()
            }
            val baseWords = if (poolWords.isNotEmpty()) {
                poolWords.shuffled().take(MatchUiState.PAIR_COUNT)
            } else {
                wordRepository.randomWordEntities(library, MatchUiState.PAIR_COUNT * 3)
            }
            val words = (baseWords + if (baseWords.size < MatchUiState.PAIR_COUNT) {
                wordRepository.randomWordEntities(library, MatchUiState.PAIR_COUNT * 2)
            } else {
                emptyList()
            })
                .distinctBy { it.meaning }
                .take(MatchUiState.PAIR_COUNT)
            if (words.size < MatchUiState.PAIR_COUNT) {
                _uiState.update { it.copy(loading = false, message = "词库数据不足，无法生成配对") }
                return@launch
            }
            val rawCards = words.flatMap { word ->
                listOf(
                    MatchCard(
                        id = 0,
                        pairId = word.id,
                        text = word.word,
                        isWord = true
                    ),
                    MatchCard(
                        id = 0,
                        pairId = word.id,
                        text = word.meaning,
                        isWord = false
                    )
                )
            }.shuffled().mapIndexed { index, card -> card.copy(id = index) }
            _uiState.update { it.copy(loading = false, cards = rawCards, totalPairs = words.size) }
            Log.d(TAG, "deal pairs=" + words.joinToString("; ") { "${it.word}->${it.meaning}" })
        }
    }

    companion object {
        private const val TAG = "MatchGame"
        private const val FLIP_BACK_DELAY_MS = 750L

        fun factory(context: Context, planMode: Boolean): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                MatchViewModel(
                    wordRepository = container.wordRepository,
                    settingsRepository = container.settingsRepository,
                    planRepository = container.planRepository,
                    planMode = planMode
                )
            }
        }
    }
}
