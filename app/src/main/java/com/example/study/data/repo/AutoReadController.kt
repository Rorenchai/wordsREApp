package com.example.study.data.repo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 全局自动朗读封装：首页单词卡与三个小游戏的“切到下一词自动发音”共用。
 * 统一读取自动朗读开关，并复用 AppContainer 中唯一的 TtsManager。
 */
class AutoReadController(
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
     * @return false 表示自动朗读已开启但语音引擎初始化失败；关闭开关时静默返回 true。
     */
    fun speakNext(word: String): Boolean {
        if (!enabled) return true
        return ttsManager.speak(word)
    }
}
