package com.example.study.data.stats

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyStatsTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 13)

    private fun days(vararg offsets: Long): List<String> {
        return offsets.map { StudyDates.format(today.minusDays(it)) }
    }

    @Test
    fun emptyDatesReturnsZero() {
        assertEquals(0, StudyStats.calculateStreak(emptyList(), today))
    }

    @Test
    fun todayPlusPreviousDays() {
        assertEquals(3, StudyStats.calculateStreak(days(0, 1, 2), today))
    }

    @Test
    fun onlyYesterdayStillCounts() {
        // 今天还没学，但昨天的连续记录尚未中断
        assertEquals(1, StudyStats.calculateStreak(days(1), today))
    }

    @Test
    fun gapBreaksStreak() {
        // 今天学了、前天学了，昨天缺 → 只算 1 天
        assertEquals(1, StudyStats.calculateStreak(days(0, 2), today))
    }

    @Test
    fun duplicateDatesCountOnce() {
        assertEquals(2, StudyStats.calculateStreak(days(0, 0, 1, 1), today))
    }

    @Test
    fun twoDaysMissedBreaksStreak() {
        assertEquals(0, StudyStats.calculateStreak(days(2, 3), today))
    }

    @Test
    fun invalidDateIgnored() {
        assertEquals(1, StudyStats.calculateStreak(listOf("not-a-date", StudyDates.format(today)), today))
    }

    @Test
    fun futuristicDatesAreIgnoredForStreak() {
        val future = StudyDates.format(today.plusDays(3))
        assertEquals(1, StudyStats.calculateStreak(listOf(future, StudyDates.format(today)), today))
    }
}
