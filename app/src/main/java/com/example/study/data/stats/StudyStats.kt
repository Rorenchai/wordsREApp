package com.example.study.data.stats

import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 学习日期的统一格式（本地时区，yyyy-MM-dd）。 */
object StudyDates {

    private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    fun formatDate(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        return Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate().format(FORMATTER)
    }

    fun format(date: LocalDate): String = date.format(FORMATTER)

    fun parse(date: String): LocalDate? = runCatching { LocalDate.parse(date, FORMATTER) }.getOrNull()

    fun today(zone: ZoneId = ZoneId.systemDefault()): LocalDate = LocalDate.now(zone)
}

/** 纯统计逻辑（可 JVM 单测，不依赖 Android / Room）。 */
object StudyStats {

    /**
     * 计算连续学习天数（streak）。
     * 规则：今天有学习记录则从今天往前数；今天还没学则从昨天往前数（当天未结束不清零）。
     * 日期字符串非法会被忽略；同一日期重复出现只算一天。
     */
    fun calculateStreak(dates: List<String>, today: LocalDate): Int {
        val learnedDays = dates.mapNotNull { StudyDates.parse(it) }.toSet()
        if (learnedDays.isEmpty()) return 0

        var cursor = if (learnedDays.contains(today)) today else today.minusDays(1)
        if (!learnedDays.contains(cursor)) return 0

        var streak = 0
        while (learnedDays.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}

/** 某个词库的统计快照，供记录页展示。 */
data class LibraryStats(
    val totalWords: Int,
    val startedWords: Int,
    val masteredWords: Int,
    val todayStudied: Int,
    val weekCounts: List<com.example.study.data.db.DailyCount>,
    val streak: Int,
    val totalStudyEvents: Long,
    val quizAnswered: Int = 0,
    val quizCorrect: Int = 0,
    val todayStudyMs: Long = 0L,
    val totalStudyMs: Long = 0L,
    val wrongBookCount: Int = 0,
    val monthStudyDates: List<String> = emptyList()
) {
    val untouchedWords: Int get() = (totalWords - startedWords).coerceAtLeast(0)

    val learnedRate: Int get() = percent(startedWords, totalWords)

    val masteredRate: Int get() = percent(masteredWords, totalWords)

    /** 学习进度百分比（保留一位小数，避免 4544 词时长期显示 0%）。 */
    val learnedPercent: Float get() = percentFloat(startedWords, totalWords)

    /** 掌握率百分比（保留一位小数）。 */
    val masteredPercent: Float get() = percentFloat(masteredWords, totalWords)

    /** 答题正确率（整数百分比）。 */
    val quizAccuracyPercent: Int get() = percent(quizCorrect, quizAnswered)

    private fun percent(value: Int, total: Int): Int {
        return if (total <= 0) 0 else (value * 100 / total).coerceIn(0, 100)
    }

    private fun percentFloat(value: Int, total: Int): Float {
        return if (total <= 0) 0f else (value * 100f / total).coerceIn(0f, 100f)
    }
}

/** 把学习时长格式化为「X 小时 Y 分钟 / X 分钟」。 */
fun formatStudyDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0 分钟"
    val minutes = durationMs / 60_000
    if (minutes < 1) return "不足 1 分钟"
    val hours = minutes / 60
    val rest = minutes % 60
    return if (hours > 0) "$hours 小时 $rest 分钟" else "$minutes 分钟"
}

/**
 * 词汇量估算（四选一抽样 + 猜测校正）：
 * 正确率 p 扣除 1/4 猜测基数后按 3/4 折算，乘以词库规模，结果取整到 50 并限制在词库范围内。
 */
fun estimateVocabularySize(correct: Int, answered: Int, poolSize: Int): Int {
    if (answered <= 0 || poolSize <= 0) return 0
    val p = correct.toFloat() / answered
    val corrected = ((p - GUESS_RATE) / (1f - GUESS_RATE)).coerceIn(0f, 1f)
    val raw = (corrected * poolSize / 50f).roundToInt() * 50
    return raw.coerceIn(0, poolSize)
}

private const val GUESS_RATE = 0.25f
