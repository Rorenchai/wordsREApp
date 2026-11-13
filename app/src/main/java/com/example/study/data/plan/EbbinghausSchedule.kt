package com.example.study.data.plan

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * 艾宾浩斯复习调度（纯逻辑，可 JVM 单测）。
 *
 * 依据检索资料：背单词复习点采用 1 / 2 / 4 / 7 / 15 天（新东方艾宾浩斯背词法；
 * 另含 5 分钟 / 30 分钟 / 12 小时的短期巩固，本应用以「当天再巩固」体现，不额外落库）。
 * 复习若越过备考截止日期则不再安排，保证计划在考试前收口。
 */
object EbbinghausSchedule {

    /** 首次及后续复习间隔（天），完成全部间隔即视为计划内掌握。 */
    val REVIEW_INTERVALS_DAYS: List<Int> = listOf(1, 2, 4, 7, 15)

    /** 每词需要完成的复习轮次。 */
    val REVIEW_STAGES: Int = REVIEW_INTERVALS_DAYS.size

    /** 首次复习日期：学习次日。 */
    fun firstReviewDate(learnDate: LocalDate): LocalDate = learnDate.plusDays(REVIEW_INTERVALS_DAYS[0].toLong())

    /** 复习结果对阶段与下次复习日期的影响。 */
    data class ReviewUpdate(
        val stage: Int,
        val nextReviewDate: String?,
        val mastered: Boolean
    )

    /**
     * 复习提交后的状态推进。
     * - 答对：进入下一轮，若已完成全部轮次则掌握；下次复习 = 今天 + 下一轮间隔。
     * - 答错：阶段不变，次日重试。
     * - 下次复习晚于考试日期：不再安排（nextReviewDate = null）。
     */
    fun afterReview(
        stage: Int,
        reviewDate: LocalDate,
        correct: Boolean,
        examDate: LocalDate
    ): ReviewUpdate {
        if (!correct) {
            val retry = reviewDate.plusDays(1)
            return if (retry.isAfter(examDate)) {
                ReviewUpdate(stage = stage, nextReviewDate = null, mastered = false)
            } else {
                ReviewUpdate(stage = stage, nextReviewDate = retry.toString(), mastered = false)
            }
        }

        val nextStage = (stage + 1).coerceAtMost(REVIEW_STAGES)
        if (nextStage >= REVIEW_STAGES) {
            return ReviewUpdate(stage = REVIEW_STAGES, nextReviewDate = null, mastered = true)
        }
        val interval = REVIEW_INTERVALS_DAYS[nextStage].toLong()
        val nextDate = reviewDate.plusDays(interval)
        return if (nextDate.isAfter(examDate)) {
            ReviewUpdate(stage = nextStage, nextReviewDate = null, mastered = false)
        } else {
            ReviewUpdate(stage = nextStage, nextReviewDate = nextDate.toString(), mastered = false)
        }
    }

    /** 学习新词后的首次复习安排。 */
    fun afterLearning(learnDate: LocalDate, examDate: LocalDate): String? {
        val first = firstReviewDate(learnDate)
        return if (first.isAfter(examDate)) null else first.toString()
    }

    /** 建议每日新词数：词库总量 / 备考天数，向上取整，限制在 10–80（超过 80 注意力与复盘质量下降）。 */
    fun suggestedDailyNewWords(libraryWords: Int, days: Int): Int {
        if (libraryWords <= 0) return 0
        val safeDays = days.coerceAtLeast(1)
        val raw = ceil(libraryWords.toDouble() / safeDays).roundToInt()
        return raw.coerceIn(MIN_DAILY_NEW_WORDS, MAX_DAILY_NEW_WORDS)
    }

    /** 备考天数（今天到考试日期，至少 1 天）。 */
    fun daysUntilExam(today: LocalDate, examDate: LocalDate): Int {
        val days = ChronoUnit.DAYS.between(today, examDate).toInt()
        return days.coerceAtLeast(1)
    }

    /** 计划能否在考试前覆盖整个词库（每日上限 80 词的可行性提示）。 */
    fun isFeasible(libraryWords: Int, days: Int, dailyNewWords: Int): Boolean {
        return libraryWords <= days.coerceAtLeast(1) * dailyNewWords.coerceAtLeast(1)
    }

    const val MIN_DAILY_NEW_WORDS = 10
    const val MAX_DAILY_NEW_WORDS = 80
    const val DEFAULT_DAILY_REVIEW_LIMIT = 120
    const val MIN_DAILY_REVIEW_LIMIT = 20
    const val MAX_DAILY_REVIEW_LIMIT = 300
}
