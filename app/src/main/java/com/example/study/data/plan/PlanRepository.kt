package com.example.study.data.plan

import androidx.room.withTransaction
import com.example.study.data.db.PlanDao
import com.example.study.data.db.PlanEntity
import com.example.study.data.db.PlanWordDetail
import com.example.study.data.db.PlanWordEntity
import com.example.study.data.db.StudyDatabase
import com.example.study.data.db.WordDao
import java.time.LocalDate

/** 计划概览：目标/已加入/已学/已掌握 + 今日新词与到期复习。 */
data class PlanSummary(
    val plan: PlanEntity,
    val targetWords: Int,
    val addedWords: Int,
    val learnedWords: Int,
    val masteredWords: Int,
    val todayNewTotal: Int,
    val todayNewLearned: Int,
    val dueReviewCount: Int,
    val daysUntilExam: Int,
    val expectedNewWordsPerDay: Int
)

/**
 * 艾宾浩斯专项计划仓库：
 * - 创建计划（词库 / 考试日期 / 每日新词 / 每日复习上限）
 * - 每日按配额从目标词库随机分配新词（尚未学习的 plan_words）
 * - 学习新词后安排次日首复习；复习按 1/2/4/7/15 天推进，考试日期截断
 * - 计划进度统计
 */
class PlanRepository(
    private val planDao: PlanDao,
    private val wordDao: WordDao,
    private val database: StudyDatabase
) {

    suspend fun activePlan(): PlanEntity? = planDao.activePlan()

    suspend fun planById(planId: Long): PlanEntity? = planDao.planById(planId)

    suspend fun createPlan(
        name: String,
        library: String,
        today: LocalDate,
        examDate: LocalDate,
        dailyNewWords: Int,
        dailyReviewLimit: Int = EbbinghausSchedule.DEFAULT_DAILY_REVIEW_LIMIT
    ): Long {
        require(examDate.isAfter(today)) { "考试日期需晚于今天" }
        require(dailyNewWords in EbbinghausSchedule.MIN_DAILY_NEW_WORDS..EbbinghausSchedule.MAX_DAILY_NEW_WORDS) {
            "每日新词需在 ${EbbinghausSchedule.MIN_DAILY_NEW_WORDS}–${EbbinghausSchedule.MAX_DAILY_NEW_WORDS} 之间"
        }
        val plan = PlanEntity(
            name = name,
            library = library,
            startDate = today.toString(),
            examDate = examDate.toString(),
            dailyNewWords = dailyNewWords,
            dailyReviewLimit = dailyReviewLimit.coerceIn(
                EbbinghausSchedule.MIN_DAILY_REVIEW_LIMIT,
                EbbinghausSchedule.MAX_DAILY_REVIEW_LIMIT
            ),
            createdAt = System.currentTimeMillis()
        )
        return planDao.insertPlan(plan)
    }

    /** 保证今天的新词批次已分配（每天只分配一次），返回今天批次数量。 */
    suspend fun ensureTodayNewWords(plan: PlanEntity, today: LocalDate): Int {
        return database.withTransaction {
            val date = today.toString()
            val existing = planDao.countBatch(plan.id, date)
            if (existing > 0) {
                existing
            } else {
                val ids = planDao.candidateWordIds(plan.id, plan.library, plan.dailyNewWords)
                if (ids.isNotEmpty()) {
                    planDao.insertPlanWords(
                        ids.map {
                            PlanWordEntity(planId = plan.id, wordId = it, batchDate = date)
                        }
                    )
                }
                ids.size
            }
        }
    }

    suspend fun todaysNewWords(planId: Long, today: LocalDate): List<PlanWordDetail> =
        planDao.todaysNewWords(planId, today.toString())

    suspend fun dueReviews(plan: PlanEntity, today: LocalDate): List<PlanWordDetail> =
        planDao.dueReviews(plan.id, today.toString(), plan.dailyReviewLimit.coerceAtLeast(1))

    /** 学完一个今日新词：进入首复习安排（次日，若晚于考试日则不再安排）。 */
    suspend fun markNewLearned(plan: PlanEntity, wordId: Long, today: LocalDate) {
        val next = EbbinghausSchedule.afterLearning(today, LocalDate.parse(plan.examDate))
        planDao.markLearned(plan.id, wordId, today.toString(), next)
    }

    /** 提交一次到期复习，返回是否已完成全部复习轮次（掌握）。 */
    suspend fun submitReview(
        plan: PlanEntity,
        wordId: Long,
        correct: Boolean,
        today: LocalDate
    ): Boolean {
        val word = planDao.planWord(plan.id, wordId) ?: return false
        val update = EbbinghausSchedule.afterReview(
            stage = word.stage,
            reviewDate = today,
            correct = correct,
            examDate = LocalDate.parse(plan.examDate)
        )
        planDao.applyReview(
            planId = plan.id,
            wordId = wordId,
            stage = update.stage,
            nextReviewDate = update.nextReviewDate,
            date = today.toString(),
            correctIncrement = if (correct) 1 else 0,
            wrongIncrement = if (correct) 0 else 1
        )
        return update.mastered
    }

    suspend fun summary(plan: PlanEntity, today: LocalDate): PlanSummary {
        val days = EbbinghausSchedule.daysUntilExam(today, LocalDate.parse(plan.examDate))
        val libraryWords = wordDao.countByLibrary(plan.library)
        val targetWords = (plan.dailyNewWords * days).coerceAtMost(libraryWords)
        val added = planDao.countPlanWords(plan.id)
        val todayTotal = planDao.countBatch(plan.id, today.toString())
        val todayUnlearned = planDao.countBatchUnlearned(plan.id, today.toString())
        val due = planDao.dueReviews(plan.id, today.toString(), Int.MAX_VALUE).size
        return PlanSummary(
            plan = plan,
            targetWords = targetWords,
            addedWords = added,
            learnedWords = planDao.countLearned(plan.id),
            masteredWords = planDao.countMastered(plan.id, EbbinghausSchedule.REVIEW_STAGES),
            todayNewTotal = todayTotal,
            todayNewLearned = todayTotal - todayUnlearned,
            dueReviewCount = due,
            daysUntilExam = days,
            expectedNewWordsPerDay = EbbinghausSchedule.suggestedDailyNewWords(libraryWords, days)
        )
    }

    suspend fun finishPlan(planId: Long) = planDao.finishPlan(planId)

    /**
     * 小游戏计划词池：优先今日到期复习词，其次今日待学新词（去重）。
     */
    suspend fun gamePool(plan: PlanEntity, today: LocalDate): GamePlanPool {
        val due = planDao.dueReviews(plan.id, today.toString(), plan.dailyReviewLimit.coerceAtLeast(1))
            .map { it.word }
            .distinctBy { it.id }
        val dueIds = due.map { it.id }.toSet()
        val fresh = planDao.todaysNewWords(plan.id, today.toString())
            .map { it.word }
            .filter { it.id !in dueIds }
            .distinctBy { it.id }
        return GamePlanPool(due = due, fresh = fresh)
    }

    /**
     * 游戏作答对计划词的复习推进：仅当该词今日到期且今日尚未推进过时生效。
     * 答对推进一轮，答错次日重试；返回是否真正计入计划复习。
     */
    suspend fun recordGameReview(
        plan: PlanEntity,
        wordId: Long,
        correct: Boolean,
        today: LocalDate
    ): Boolean {
        val word = planDao.planWord(plan.id, wordId) ?: return false
        if (word.learnedAt == null || word.nextReviewDate == null) return false
        if (word.lastReviewDate == today.toString()) return false
        if (word.nextReviewDate > today.toString()) return false
        submitReview(plan, wordId, correct, today)
        return true
    }
}

/** 游戏计划词池：到期复习优先，新词其次。 */
data class GamePlanPool(
    val due: List<com.example.study.data.db.WordEntity>,
    val fresh: List<com.example.study.data.db.WordEntity>
) {
    val isEmpty: Boolean get() = due.isEmpty() && fresh.isEmpty()
    val size: Int get() = due.size + fresh.size

    fun randomWord(): com.example.study.data.db.WordEntity? {
        val pool = if (due.isNotEmpty()) due else fresh
        return pool.randomOrNull()
    }
}
