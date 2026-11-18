package com.example.study.ui.games

import com.example.study.data.repo.SettingsRepository
import com.example.study.data.repo.TtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 小游戏统一的“下一词自动朗读”封装。
 * 三个小游戏只调用 [speakNext]，自动朗读开关与 TTS 生命周期都由这里统一处理。
 */
class GameAutoRead(
    private val ttsManager: TtsManager,
    settingsRepository: SettingsRepository,
    scope: CoroutineScope
) {
    @Volatile
    private var enabled = true

    init {
        scope.launch {
            settingsRepository.autoReadEnabled.collect { enabled = it }
        }
    }

    /**
     * 朗读当前/下一单词。
     * @return false 表示自动朗读已开启但内置语音引擎初始化失败；关闭开关时返回 true（静默跳过）。
     */
    fun speakNext(word: String): Boolean {
        if (!enabled) return true
        return ttsManager.speak(word)
    }
}
