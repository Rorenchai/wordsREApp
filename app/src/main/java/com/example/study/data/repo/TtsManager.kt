package com.example.study.data.repo

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 系统 TextToSpeech 封装：按设置切换 US / UK 口音。
 * 引擎缺失或语言包不可用时 speak() 返回 false，由界面提示用户。
 */
class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var available = false
    private var accent: String = SettingsRepository.ACCENT_US

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                applyLocale()
            }
        }
    }

    fun setAccent(newAccent: String) {
        accent = newAccent
        if (ready) applyLocale()
    }

    /** @return 是否成功发起朗读（false 表示系统无可用英语语音） */
    fun speak(text: String): Boolean {
        if (!ready) return false
        applyLocale()
        if (!available) return false
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        return true
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        available = false
    }

    private fun applyLocale() {
        val locale = if (accent == SettingsRepository.ACCENT_UK) Locale.UK else Locale.US
        val result = tts?.setLanguage(locale)
        available = result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    companion object {
        private const val UTTERANCE_ID = "study_word"
    }
}
