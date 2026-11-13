package com.example.study.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlanDao {

    @Insert
    suspend fun insertPlan(plan: PlanEntity): Long

    @Query("SELECT * FROM plans WHERE status = 1 ORDER BY id DESC LIMIT 1")
    suspend fun activePlan(): PlanEntity?

    @Query("SELECT * FROM plans WHERE id = :planId")
    suspend fun planById(planId: Long): PlanEntity?

    @Query("UPDATE plans SET status = 2 WHERE id = :planId")
    suspend fun finishPlan(planId: Long)

    @Query("SELECT COUNT(*) FROM plan_words WHERE planId = :planId")
    suspend fun countPlanWords(planId: Long): Int

    @Query("SELECT COUNT(*) FROM plan_words WHERE planId = :planId AND learnedAt IS NOT NULL")
    suspend fun countLearned(planId: Long): Int

    @Query("SELECT COUNT(*) FROM plan_words WHERE planId = :planId AND learnedAt IS NOT NULL AND stage >= :stages")
    suspend fun countMastered(planId: Long, stages: Int): Int

    @Query("SELECT COUNT(*) FROM plan_words WHERE planId = :planId AND batchDate = :date")
    suspend fun countBatch(planId: Long, date: String): Int

    @Query("SELECT COUNT(*) FROM plan_words WHERE planId = :planId AND batchDate = :date AND learnedAt IS NULL")
    suspend fun countBatchUnlearned(planId: Long, date: String): Int

    @Query("SELECT wordId FROM plan_words WHERE planId = :planId")
    suspend fun plannedWordIds(planId: Long): List<Long>

    /** 从目标词库中随机挑选尚未加入该计划的单词（每日新词批次）。 */
    @Query(
        """
        SELECT id FROM words
        WHERE library = :library AND id NOT IN (SELECT wordId FROM plan_words WHERE planId = :planId)
        ORDER BY RANDOM()
        LIMIT :count
        """
    )
    suspend fun candidateWordIds(planId: Long, library: String, count: Int): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlanWords(items: List<PlanWordEntity>)

    @Query("SELECT * FROM plan_words WHERE planId = :planId AND wordId = :wordId")
    suspend fun planWord(planId: Long, wordId: Long): PlanWordEntity?

    @Query("SELECT batchDate AS date, COUNT(*) AS count FROM plan_words WHERE planId = :planId GROUP BY batchDate")
    suspend fun newCountsByBatch(planId: Long): List<DayCountRow>

    @Query(
        """
        SELECT batchDate AS date, SUM(CASE WHEN learnedAt IS NOT NULL THEN 1 ELSE 0 END) AS count
        FROM plan_words WHERE planId = :planId GROUP BY batchDate
        """
    )
    suspend fun newLearnedByBatch(planId: Long): List<DayCountRow>

    @Query(
        """
        SELECT lastReviewDate AS date, COUNT(*) AS count
        FROM plan_words WHERE planId = :planId AND lastReviewDate IS NOT NULL
        GROUP BY lastReviewDate
        """
    )
    suspend fun reviewDoneByDate(planId: Long): List<DayCountRow>

    @Query(
        """
        SELECT nextReviewDate AS date, COUNT(*) AS count
        FROM plan_words WHERE planId = :planId AND nextReviewDate IS NOT NULL
        GROUP BY nextReviewDate
        """
    )
    suspend fun reviewScheduledByDate(planId: Long): List<DayCountRow>

    @Query(
        """
        SELECT w.*, pw.planId AS planId, pw.batchDate AS batchDate, pw.stage AS stage,
               pw.nextReviewDate AS nextReviewDate, pw.lastReviewDate AS lastReviewDate,
               pw.learnedAt AS learnedAt, pw.learnCount AS learnCount,
               pw.correctCount AS correctCount, pw.wrongCount AS wrongCount
        FROM plan_words pw JOIN words w ON pw.wordId = w.id
        WHERE pw.planId = :planId AND pw.batchDate = :date AND pw.learnedAt IS NULL
        ORDER BY w.word
        """
    )
    suspend fun todaysNewWords(planId: Long, date: String): List<PlanWordDetail>

    @Query(
        """
        SELECT w.*, pw.planId AS planId, pw.batchDate AS batchDate, pw.stage AS stage,
               pw.nextReviewDate AS nextReviewDate, pw.lastReviewDate AS lastReviewDate,
               pw.learnedAt AS learnedAt, pw.learnCount AS learnCount,
               pw.correctCount AS correctCount, pw.wrongCount AS wrongCount
        FROM plan_words pw JOIN words w ON pw.wordId = w.id
        WHERE pw.planId = :planId AND pw.learnedAt IS NOT NULL
          AND pw.nextReviewDate IS NOT NULL AND pw.nextReviewDate <= :today
        ORDER BY pw.nextReviewDate, w.word
        LIMIT :limit
        """
    )
    suspend fun dueReviews(planId: Long, today: String, limit: Int): List<PlanWordDetail>

    @Query(
        """
        UPDATE plan_words
        SET learnedAt = :date, stage = 0, lastReviewDate = :date,
            nextReviewDate = :nextReviewDate, learnCount = learnCount + 1
        WHERE planId = :planId AND wordId = :wordId
        """
    )
    suspend fun markLearned(planId: Long, wordId: Long, date: String, nextReviewDate: String?)

    @Query(
        """
        UPDATE plan_words
        SET stage = :stage, nextReviewDate = :nextReviewDate, lastReviewDate = :date,
            correctCount = correctCount + :correctIncrement,
            wrongCount = wrongCount + :wrongIncrement
        WHERE planId = :planId AND wordId = :wordId
        """
    )
    suspend fun applyReview(
        planId: Long,
        wordId: Long,
        stage: Int,
        nextReviewDate: String?,
        date: String,
        correctIncrement: Int,
        wrongIncrement: Int
    )
}
