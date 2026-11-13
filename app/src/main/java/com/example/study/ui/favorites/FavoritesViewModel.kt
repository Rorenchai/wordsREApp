package com.example.study.ui.favorites

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.study.appContainer
import com.example.study.data.db.WordEntity
import com.example.study.data.repo.FavoriteRepository
import com.example.study.data.repo.SettingsRepository
import com.example.study.data.repo.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favorites: List<WordEntity> = emptyList(),
    val loading: Boolean = true,
    val message: String? = null
)

/** 收藏夹：跨词库统一展示，按收藏时间倒序；支持取消收藏与发音。 */
class FavoritesViewModel(
    private val favoriteRepository: FavoriteRepository,
    private val settingsRepository: SettingsRepository,
    private val ttsManager: TtsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            favoriteRepository.observeFavorites(FavoriteRepository.ALL_LIBRARIES).collect { words ->
                _uiState.update { it.copy(favorites = words, loading = false) }
            }
        }
        viewModelScope.launch {
            settingsRepository.accent.collect { accent -> ttsManager.setAccent(accent) }
        }
    }

    fun remove(word: WordEntity) {
        viewModelScope.launch {
            favoriteRepository.toggle(word.id)
            _uiState.update { it.copy(message = "已取消收藏：${word.word}") }
        }
    }

    fun speak(word: String) {
        if (!ttsManager.speak(word)) {
            _uiState.update { it.copy(message = "系统未安装可用的英语语音引擎，无法发音") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        ttsManager.shutdown()
        super.onCleared()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                FavoritesViewModel(
                    favoriteRepository = container.favoriteRepository,
                    settingsRepository = container.settingsRepository,
                    ttsManager = TtsManager(context.applicationContext)
                )
            }
        }
    }
}
