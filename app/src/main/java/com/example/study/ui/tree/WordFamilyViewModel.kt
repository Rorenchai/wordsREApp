package com.example.study.ui.tree

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.study.appContainer
import com.example.study.data.db.WordEntity
import com.example.study.data.repo.SettingsRepository
import com.example.study.data.repo.StudyRepository
import com.example.study.data.repo.TtsManager
import com.example.study.data.repo.WordRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FamilyWord(
    val word: String = "",
    val phonetic: String = "",
    val pos: String = "",
    val meaning: String = ""
)

data class WordFamily(
    val root: String = "",
    val base: String = "",
    val words: List<FamilyWord> = emptyList()
)

data class WordFamilyFile(
    val library: String = "",
    val families: List<WordFamily> = emptyList()
)

data class WordFamilyUiState(
    val library: String = SettingsRepository.LIB_CET4,
    val custom: Boolean = false,
    val families: List<WordFamily> = emptyList(),
    val visibleFamilies: List<WordFamily> = emptyList(),
    val selectedIndex: Int = 0,
    val query: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val detailWord: WordEntity? = null,
    val message: String? = null
) {
    val selectedFamily: WordFamily? get() = visibleFamilies.getOrNull(selectedIndex)
}

/** 同类词/词族归纳：内置三套词库读取预生成 JSON；自定义词库运行时按词干推导。 */
class WordFamilyViewModel(
    private val context: Context,
    private val wordRepository: WordRepository,
    private val studyRepository: StudyRepository,
    private val settingsRepository: SettingsRepository,
    private val ttsManager: TtsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordFamilyUiState())
    val uiState: StateFlow<WordFamilyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.currentLibrary.collect { library -> loadLibrary(library) }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { state ->
            val visible = filterFamilies(state.families, query)
            state.copy(query = query, visibleFamilies = visible, selectedIndex = 0)
        }
    }

    fun selectFamily(index: Int) {
        _uiState.update { state ->
            if (index in state.visibleFamilies.indices) state.copy(selectedIndex = index) else state
        }
    }

    fun previousFamily() = selectFamily(_uiState.value.selectedIndex - 1)

    fun nextFamily() = selectFamily(_uiState.value.selectedIndex + 1)

    fun openWord(word: FamilyWord) {
        _uiState.update {
            it.copy(
                detailWord = WordEntity(
                    word = word.word,
                    phonetic = word.phonetic,
                    pos = word.pos,
                    meaning = word.meaning,
                    example = "",
                    exampleZh = "",
                    phrase = "",
                    phraseZh = "",
                    library = it.library
                )
            )
        }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(detailWord = null) }
    }

    fun speak(word: String) {
        if (!ttsManager.speak(word)) {
            _uiState.update { it.copy(message = "内置语音引擎初始化失败，暂时无法发音") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun retry() {
        viewModelScope.launch { loadLibrary(_uiState.value.library) }
    }

    private suspend fun loadLibrary(library: String) {
        _uiState.update { it.copy(library = library, loading = true, error = null) }
        try {
            wordRepository.ensureReady()
            val builtIn = library == SettingsRepository.LIB_CET4 ||
                library == SettingsRepository.LIB_CET6 ||
                library == SettingsRepository.LIB_PETS
            val families = if (builtIn) {
                loadAssetFamilies(library).ifEmpty { deriveFamilies(library) }
            } else {
                deriveFamilies(library)
            }
            val visible = filterFamilies(families, "")
            _uiState.update {
                it.copy(
                    custom = !builtIn,
                    families = families,
                    visibleFamilies = visible,
                    selectedIndex = 0,
                    loading = false
                )
            }
        } catch (error: Throwable) {
            _uiState.update {
                it.copy(loading = false, error = "词族加载失败：${error.message ?: "未知错误"}")
            }
        }
    }

    private suspend fun loadAssetFamilies(library: String): List<WordFamily> = withContext(Dispatchers.IO) {
        val slug = when (library) {
            SettingsRepository.LIB_CET4 -> "cet4"
            SettingsRepository.LIB_CET6 -> "cet6"
            SettingsRepository.LIB_PETS -> "pets"
            else -> ""
        }
        if (slug.isBlank()) return@withContext emptyList()
        runCatching {
            context.assets.open("word_families_$slug.json").use { stream ->
                val file = Gson().fromJson(
                    stream.bufferedReader(Charsets.UTF_8),
                    WordFamilyFile::class.java
                )
                file?.families.orEmpty()
            }
        }.getOrDefault(emptyList())
    }

    private suspend fun deriveFamilies(library: String): List<WordFamily> = withContext(Dispatchers.IO) {
        val words = studyRepository.wordsOfLibrary(library)
        val grouped = linkedMapOf<String, MutableList<WordEntity>>()
        words.forEach { word ->
            val stem = stem(word.word)
            if (stem.length >= 3) grouped.getOrPut(stem) { mutableListOf() }.add(word)
        }
        grouped.mapNotNull { (root, members) ->
            if (members.size < 2) return@mapNotNull null
            val sorted = members.distinctBy { it.word.lowercase() }.sortedBy { it.word.length }
            val base = sorted.first().word
            val common = sorted.all { it.word.lowercase().take(3) == base.lowercase().take(3) }
            if (!common) return@mapNotNull null
            WordFamily(
                root = root,
                base = base,
                words = sorted.take(12).sortedBy { it.word.lowercase() }.map {
                    FamilyWord(it.word, it.phonetic, it.pos, it.meaning)
                }
            )
        }.sortedBy { it.base.lowercase() }
    }

    private fun filterFamilies(families: List<WordFamily>, query: String): List<WordFamily> {
        val q = query.trim()
        if (q.isBlank()) return families
        return families.filter { family ->
            family.base.contains(q, ignoreCase = true) ||
                family.root.contains(q, ignoreCase = true) ||
                family.words.any {
                    it.word.contains(q, ignoreCase = true) || it.meaning.contains(q, ignoreCase = true)
                }
        }
    }

    private fun stem(raw: String): String {
        var word = raw.lowercase()
        var changed = true
        while (changed) {
            changed = false
            for (suffix in SUFFIXES) {
                if (word.length - suffix.length >= 3 && word.endsWith(suffix)) {
                    word = word.dropLast(suffix.length)
                    changed = true
                    break
                }
            }
        }
        if (word.length >= 4 && word.endsWith("e")) word = word.dropLast(1)
        return word
    }

    override fun onCleared() {
        ttsManager.stop()
        super.onCleared()
    }

    companion object {
        private val SUFFIXES = listOf(
            "ation", "ition", "tion", "sion", "ment", "ness", "ity", "ive", "ous",
            "ful", "less", "able", "ible", "ance", "ence", "al", "ly", "er", "or",
            "ing", "ed", "es", "s", "ion"
        )

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.applicationContext.appContainer
                WordFamilyViewModel(
                    context = context.applicationContext,
                    wordRepository = container.wordRepository,
                    studyRepository = container.studyRepository,
                    settingsRepository = container.settingsRepository,
                    ttsManager = container.ttsManager
                )
            }
        }
    }
}
