package com.example.study.data.stats

import java.time.Instant
import java.time.LocalDate
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
    val totalStudyEvents: Long
) {
    val untouchedWords: Int get() = (totalWords - startedWords).coerceAtLeast(0)

    val learnedRate: Int get() = percent(startedWords, totalWords)

    val masteredRate: Int get() = percent(masteredWords, totalWords)

    private fun percent(value: Int, total: Int): Int {
        return if (total <= 0) 0 else (value * 100 / total).coerceIn(0, 100)
    }
}
