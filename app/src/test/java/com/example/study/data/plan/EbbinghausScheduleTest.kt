package com.example.study.data.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** 艾宾浩斯复习调度算法单元测试。 */
class EbbinghausScheduleTest {

    private val exam = LocalDate.of(2027, 1, 31)

    @Test
    fun learningSchedulesFirstReviewTomorrow() {
        val learned = LocalDate.of(2026, 11, 13)
        assertEquals("2026-11-14", EbbinghausSchedule.afterLearning(learned, exam))
    }

    @Test
    fun learningAfterExamHasNoReview() {
        val learned = LocalDate.of(2027, 1, 31)
        assertNull(EbbinghausSchedule.afterLearning(learned, exam))
    }

    @Test
    fun correctReviewAdvancesToNextInterval() {
        val update = EbbinghausSchedule.afterReview(
            stage = 0,
            reviewDate = LocalDate.of(2026, 11, 14),
            correct = true,
            examDate = exam
        )
        assertEquals(1, update.stage)
        assertEquals("2026-11-16", update.nextReviewDate)
        assertFalse(update.mastered)
    }

    @Test
    fun lastStageMarksMastered() {
        val update = EbbinghausSchedule.afterReview(
            stage = EbbinghausSchedule.REVIEW_STAGES - 1,
            reviewDate = LocalDate.of(2026, 12, 1),
            correct = true,
            examDate = exam
        )
        assertEquals(EbbinghausSchedule.REVIEW_STAGES, update.stage)
        assertNull(update.nextReviewDate)
        assertTrue(update.mastered)
    }

    @Test
    fun wrongReviewRetriesTomorrowWithoutAdvancing() {
        val update = EbbinghausSchedule.afterReview(
            stage = 2,
            reviewDate = LocalDate.of(2026, 11, 20),
            correct = false,
            examDate = exam
        )
        assertEquals(2, update.stage)
        assertEquals("2026-11-21", update.nextReviewDate)
        assertFalse(update.mastered)
    }

    @Test
    fun reviewBeyondExamIsNotScheduled() {
        val update = EbbinghausSchedule.afterReview(
            stage = 0,
            reviewDate = LocalDate.of(2027, 1, 30),
            correct = true,
            examDate = exam
        )
        assertEquals(1, update.stage)
        assertNull(update.nextReviewDate)
        assertFalse(update.mastered)
    }

    @Test
    fun wrongReviewBeyondExamIsNotScheduled() {
        val update = EbbinghausSchedule.afterReview(
            stage = 1,
            reviewDate = LocalDate.of(2027, 1, 31),
            correct = false,
            examDate = exam
        )
        assertEquals(1, update.stage)
        assertNull(update.nextReviewDate)
    }

    @Test
    fun suggestedDailyWordsMatchesExamPreparation() {
        // 4544 词 / 60 天 ≈ 76 词/天（≤ 80 上限）
        assertEquals(76, EbbinghausSchedule.suggestedDailyNewWords(4544, 60))
        // 30 天备考需要 152 词/天，被上限截到 80
        assertEquals(80, EbbinghausSchedule.suggestedDailyNewWords(4544, 30))
        // 小词库也至少 10 词/天
        assertEquals(10, EbbinghausSchedule.suggestedDailyNewWords(100, 60))
    }

    @Test
    fun feasibilityUsesDailyNewLimit() {
        assertTrue(EbbinghausSchedule.isFeasible(4544, 60, 80))
        assertFalse(EbbinghausSchedule.isFeasible(4544, 30, 80))
    }

    @Test
    fun daysUntilExamAtLeastOne() {
        val today = LocalDate.of(2026, 11, 13)
        assertEquals(79, EbbinghausSchedule.daysUntilExam(today, exam))
        assertEquals(1, EbbinghausSchedule.daysUntilExam(today, today))
    }
}
