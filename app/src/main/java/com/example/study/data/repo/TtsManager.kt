package com.example.study.data.repo

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

/**
 * 内置离线语音引擎：在隐藏 WebView 中加载 assets/tts 内的 meSpeak（eSpeak）引擎，
 * 完全本地合成英语发音，不依赖系统 TextToSpeech 引擎与网络。
 *
 * 对外接口保持与旧系统 TTS 封装一致（setAccent / speak / shutdown），
 * 仅内部实现替换，避免改动各页面的调用方式。
 */
class TtsManager(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var ready = false
    private var accent: String = SettingsRepository.ACCENT_US
    private var pendingText: String? = null

    init {
        mainHandler.post { initEngine() }
    }

    /** 切换美式 / 英式音色（只影响下一次朗读） */
    fun setAccent(newAccent: String) {
        accent = newAccent
        Log.i(TAG, "发音口音切换为 $newAccent")
    }

    /**
     * 发起一次朗读。
     * @return true 表示请求已交给内置引擎（未就绪时会先暂存，加载完成后自动补读）
     */
    fun speak(text: String): Boolean {
        val safeText = JSONObject.quote(text)
        val safeAccent = JSONObject.quote(if (accent == SettingsRepository.ACCENT_UK) "UK" else "US")
        mainHandler.post {
            val view = webView
            if (!ready || view == null) {
                pendingText = text
                Log.i(TAG, "语音引擎尚未就绪，暂存待读：$text")
                return@post
            }
            view.evaluateJavascript("window.speakWord($safeText, $safeAccent);", null)
        }
        return true
    }

    fun shutdown() {
        mainHandler.post {
            webView?.destroy()
            webView = null
            ready = false
            pendingText = null
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initEngine() {
        if (webView != null) return
        val view = WebView(appContext)
        view.settings.javaScriptEnabled = true
        // 自动播放：允许无用户手势时播放合成音频（发音由用户点击触发）
        view.settings.mediaPlaybackRequiresUserGesture = false
        view.settings.allowFileAccess = true
        view.settings.allowContentAccess = true
        view.addJavascriptInterface(Bridge(), "AndroidTts")
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                Log.i(TAG, "内置语音引擎页面加载完成")
            }
        }
        view.loadUrl("file:///android_asset/tts/tts.html")
        webView = view
    }

    /** JS 桥：接收引擎就绪、朗读成功与错误事件，仅用于日志与状态同步 */
    inner class Bridge {
        @JavascriptInterface
        fun onReady() {
            ready = true
            Log.i(TAG, "内置语音引擎就绪")
            pendingText?.let { text ->
                pendingText = null
                speak(text)
            }
        }

        @JavascriptInterface
        fun onSpeak(text: String, voice: String) {
            Log.i(TAG, "内置语音合成：$text（音色 $voice）")
        }

        @JavascriptInterface
        fun onError(message: String) {
            Log.e(TAG, "内置语音引擎错误：$message")
        }

        @JavascriptInterface
        fun log(message: String) {
            Log.i(TAG, message)
        }
    }

    companion object {
        private const val TAG = "EmbeddedTts"
    }
}
