package com.example.study.data.importer

import com.google.gson.JsonParser

/** 自定义词库中的一条词（导入前的最小数据：单词 + 释义，可选音标等扩展字段）。 */
data class CustomWordEntry(
    val word: String,
    val meaning: String,
    val phonetic: String = "",
    val pos: String = "",
    val example: String = "",
    val exampleZh: String = "",
    val phrase: String = "",
    val phraseZh: String = ""
)

/**
 * 自定义词库解析器：支持两种常见格式，方便用户自行准备词库文件。
 *
 * 1. JSON：
 *    - `[{"word":"apple","phonetic":"ˈæpl","meaning":"n. 苹果"}]`
 *    - 或 `{"words":[ ... ]}`
 *    字段兼容 word/spelling/headWord/term、meaning/trans/translation/definition。
 * 2. 文本（CSV / TXT）：
 *    - 每行 `单词,释义`、`单词<TAB>释义` 或 `单词|释义`；# 开头为注释，表头自动跳过。
 */
object CustomLibraryParser {

    fun parse(fileName: String, content: String): List<CustomWordEntry> {
        // Windows 记事本等工具可能写入 UTF-8 BOM，先去掉再判断格式，避免 JSON 被当成逐行文本解析
        val trimmed = content.removePrefix("\uFEFF").trim()
        if (trimmed.isEmpty()) return emptyList()
        return if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            parseJson(trimmed) ?: emptyList()
        } else {
            parseDelimited(trimmed)
        }
    }

    /** 从文件名推导默认词库名（去掉扩展名，过长截断）。 */
    fun defaultLibraryName(fileName: String): String {
        val base = fileName.substringBeforeLast('.', fileName).trim()
        val cleaned = base.replace(Regex("[\\\\/:*?\"<>|]"), " ").trim()
        return (cleaned.ifBlank { "自定义词库" }).take(40)
    }

    private fun parseJson(text: String): List<CustomWordEntry>? {
        return try {
            val root = JsonParser.parseString(text)
            val array = when {
                root.isJsonArray -> root.asJsonArray
                root.isJsonObject && root.asJsonObject.has("words") -> root.asJsonObject.getAsJsonArray("words")
                else -> return null
            }
            val result = ArrayList<CustomWordEntry>(array.size())
            array.forEach { element ->
                if (!element.isJsonObject) return@forEach
                val obj = element.asJsonObject
                val word = firstString(obj, listOf("word", "spelling", "headWord", "term", "name"))
                val meaning = firstString(obj, listOf("meaning", "trans", "translation", "definition", "translationZh"))
                if (word.isNotBlank() && meaning.isNotBlank()) {
                    result += CustomWordEntry(
                        word = word.trim(),
                        meaning = meaning.trim(),
                        phonetic = firstString(obj, listOf("phonetic", "ipa", "pronunciation")),
                        pos = firstString(obj, listOf("pos", "partOfSpeech")),
                        example = firstString(obj, listOf("example", "sentence")),
                        exampleZh = firstString(obj, listOf("exampleZh", "sentenceZh")),
                        phrase = firstString(obj, listOf("phrase", "collocation")),
                        phraseZh = firstString(obj, listOf("phraseZh", "collocationZh"))
                    )
                }
            }
            result
        } catch (error: Exception) {
            null
        }
    }

    private fun parseDelimited(text: String): List<CustomWordEntry> {
        val result = ArrayList<CustomWordEntry>()
        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val parts = when {
                line.contains('\t') -> line.split('\t')
                line.contains('|') -> line.split('|')
                else -> line.split(',', '，')
            }.map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size < 2) return@forEach
            // 跳过 CSV 表头（word, meaning, 单词, 释义 等）
            val first = parts[0].lowercase()
            if (first == "word" || first == "单词" || first == "spelling") return@forEach
            val word = parts[0]
            val meaning = parts.drop(1).joinToString("；")
            if (word.isNotBlank() && meaning.isNotBlank()) {
                result += CustomWordEntry(word = word, meaning = meaning)
            }
        }
        return result
    }

    private fun firstString(obj: com.google.gson.JsonObject, keys: List<String>): String {
        keys.forEach { key ->
            val value = obj.get(key)
            if (value != null && !value.isJsonNull && value.isJsonPrimitive) {
                return value.asString.trim()
            }
        }
        return ""
    }
}
