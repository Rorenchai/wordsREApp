package com.example.study.data.repo

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File
import java.util.concurrent.Executors

/**
 * 内置神经网络离线语音引擎：sherpa-onnx + Piper VITS 女声模型（LJSpeech）。
 *
 * - 引擎、模型与音素数据全部打包在 APK 内，不依赖系统 TTS、不访问网络；
 * - 22.05kHz 女声，比 eSpeak 规则合成更自然、更有语气；
 * - 对外接口保持 setAccent / speak / shutdown，调用方无需改动。
 */
class TtsManager(context: android.content.Context) {

    private val appContext = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile
    private var tts: OfflineTts? = null
    @Volatile
    private var ready = false
    @Volatile
    private var failed = false
    @Volatile
    private var accent: String = SettingsRepository.ACCENT_US
    @Volatile
    private var pendingText: String? = null
    @Volatile
    private var audioTrack: AudioTrack? = null
    @Volatile
    private var generation = 0

    init {
        executor.execute { initialize() }
    }

    /** 切换美式 / 英式口音：内置女声只有一套音色，英式略放慢语速模拟。 */
    fun setAccent(newAccent: String) {
        accent = newAccent
    }

    /**
     * 发起朗读。
     * @return true 表示请求已交给内置引擎（初始化中会暂存，就绪后自动补读）；false 表示引擎初始化失败
     */
    fun speak(text: String): Boolean {
        if (failed) return false
        if (!ready) {
            pendingText = text
            return true
        }
        val engine = tts ?: return false
        val currentGeneration = ++generation
        val speed = if (accent == SettingsRepository.ACCENT_UK) 0.92f else 1.0f
        executor.execute {
            try {
                val audio = engine.generate(text = text, sid = 0, speed = speed)
                if (currentGeneration != generation) return@execute
                Log.i(TAG, "合成完成：$text（${audio.samples.size} 采样点 / ${audio.sampleRate}Hz）")
                play(audio.samples, audio.sampleRate, currentGeneration)
            } catch (error: Throwable) {
                Log.e(TAG, "语音合成失败：$text", error)
            }
        }
        return true
    }

    fun shutdown() {
        stop()
        executor.execute {
            tts?.release()
            tts = null
            ready = false
        }
        executor.shutdown()
    }

    /** 停止当前播放（切换单词或离开页面时调用）。 */
    fun stop() {
        generation++
        val track = audioTrack
        audioTrack = null
        if (track != null) {
            runCatching {
                track.pause()
                track.flush()
            }
        }
    }

    private fun initialize() {
        try {
            // Piper 的 espeak 音素数据需要真实文件路径，先把 assets 里的模型与音素数据复制到私有目录
            val piperDir = preparePiperFiles()
            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = File(piperDir, "en_US-ljspeech-medium.onnx").absolutePath,
                        tokens = File(piperDir, "tokens.txt").absolutePath,
                        dataDir = File(piperDir, "espeak-ng-data").absolutePath,
                        // 略微提高噪声/噪声W，让女声更自然、有起伏
                        noiseScale = 0.75f,
                        noiseScaleW = 0.9f,
                        lengthScale = 1.0f
                    ),
                    numThreads = 2,
                    debug = false,
                    provider = "cpu"
                )
            )
            tts = OfflineTts(config = config)
            ready = true
            Log.i(TAG, "内置女声引擎就绪（Piper LJSpeech，${tts?.sampleRate()}Hz）")
            pendingText?.let { text ->
                pendingText = null
                speak(text)
            }
        } catch (error: Throwable) {
            failed = true
            Log.e(TAG, "内置女声引擎初始化失败", error)
        }
    }

    /** 首次启动把模型与 espeak 数据从 assets 复制到私有目录（约 20MB，只复制一次）。 */
    private fun preparePiperFiles(): File {
        val target = File(appContext.filesDir, "tts/piper")
        val marker = File(target, ".ready_v1")
        if (marker.exists()) return target
        target.deleteRecursively()
        target.mkdirs()
        copyAsset("tts/piper/en_US-ljspeech-medium.onnx", File(target, "en_US-ljspeech-medium.onnx"))
        copyAsset("tts/piper/tokens.txt", File(target, "tokens.txt"))
        copyAssetDir("tts/piper/espeak-ng-data", File(target, "espeak-ng-data"))
        marker.writeText("1")
        Log.i(TAG, "语音模型已复制到 ${target.absolutePath}")
        return target
    }

    private fun copyAssetDir(assetPath: String, dest: File) {
        val children = appContext.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            copyAsset(assetPath, dest)
            return
        }
        dest.mkdirs()
        children.forEach { child ->
            copyAssetDir("$assetPath/$child", File(dest, child))
        }
    }

    private fun copyAsset(assetPath: String, dest: File) {
        dest.parentFile?.mkdirs()
        appContext.assets.open(assetPath).use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
    }

    /** 用 AudioTrack 播放 float PCM（Piper 输出为 [-1,1] 单声道）。 */
    private fun play(samples: FloatArray, sampleRate: Int, currentGeneration: Int) {
        if (samples.isEmpty() || currentGeneration != generation) return
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        val bufferSize = maxOf(minBuffer, 4096 * 4)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audioTrack = track
        track.play()
        var offset = 0
        while (offset < samples.size && currentGeneration == generation) {
            val count = minOf(2048, samples.size - offset)
            val written = track.write(samples, offset, count, AudioTrack.WRITE_BLOCKING)
            if (written <= 0) break
            offset += written
        }
        if (audioTrack === track) audioTrack = null
        runCatching {
            track.stop()
            track.release()
        }
    }

    companion object {
        private const val TAG = "PiperTts"
    }
}
