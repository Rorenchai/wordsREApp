package com.example.study.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** 最近 7 天柱状图用的聚合结果。 */
data class DailyCount(
    val date: String,
    val count: Int
)

@Dao
interface StudyEventDao {

    @Insert
    suspend fun insert(event: StudyEventEntity)

    /** 某天学习过的不同单词数（今日目标口径）。 */
    @Query(
        """
        SELECT COUNT(DISTINCT e.wordId) FROM study_events e
        JOIN words w ON e.wordId = w.id
        WHERE w.library = :library AND e.date = :date
        """
    )
    suspend fun distinctWordsOn(library: String, date: String): Int

    /** 累计有效学习次数（复习重复计）。 */
    @Query(
        """
        SELECT COUNT(*) FROM study_events e
        JOIN words w ON e.wordId = w.id
        WHERE w.library = :library
        """
    )
    suspend fun totalEvents(library: String): Long

    /** 最近 N 天每天的不同词数，供柱状图使用。 */
    @Query(
        """
        SELECT e.date AS date, COUNT(DISTINCT e.wordId) AS count
        FROM study_events e
        JOIN words w ON e.wordId = w.id
        WHERE w.library = :library AND e.date BETWEEN :fromDate AND :toDate
        GROUP BY e.date
        """
    )
    suspend fun dailyCounts(library: String, fromDate: String, toDate: String): List<DailyCount>

    /** 有学习记录的日期（倒序），用于计算 streak。 */
    @Query(
        """
        SELECT DISTINCT e.date FROM study_events e
        JOIN words w ON e.wordId = w.id
        WHERE w.library = :library
        ORDER BY e.date DESC
        """
    )
    suspend fun distinctDates(library: String): List<String>

    /** 答题模式已答题数（只看有对错标记的事件）。 */
    @Query(
        """
        SELECT COUNT(*) FROM study_events e
        JOIN words w ON e.wordId = w.id
        WHERE w.library = :library AND e.answerCorrect IS NOT NULL
        """
    )
    suspend fun quizAnswered(library: String): Int

    /** 答题模式答对数。 */
    @Query(
        """
        SELECT COUNT(*) FROM study_events e
        JOIN words w ON e.wordId = w.id
        WHERE w.library = :library AND e.answerCorrect = 1
        """
    )
    suspend fun quizCorrect(library: String): Int

    /** 指定日期范围内的学习日期（打卡日历用）。 */
    @Query(
        """
        SELECT DISTINCT e.date FROM study_events e
        JOIN words w ON e.wordId = w.id
        WHERE w.library = :library AND e.date BETWEEN :fromDate AND :toDate
        ORDER BY e.date
        """
    )
    suspend fun datesBetween(library: String, fromDate: String, toDate: String): List<String>

    /**
     * 错题本：最近一次答题是「答错」的词，附带累计答错次数与最近答错时间。
     * 在复习/答题中答对后，最近一次记录变为正确，自动移出错题本。
     */
    @Query(
        """
        SELECT w.*,
            (SELECT COUNT(*) FROM study_events e2
             WHERE e2.wordId = w.id AND e2.answerCorrect = 0) AS wrongCount,
            latest.timestamp AS lastWrongAt
        FROM words w
        JOIN (
            SELECT e.wordId AS wordId, e.answerCorrect AS answerCorrect, e.timestamp AS timestamp
            FROM study_events e
            WHERE e.answerCorrect IS NOT NULL
              AND e.timestamp = (
                  SELECT MAX(e3.timestamp) FROM study_events e3
                  WHERE e3.wordId = e.wordId AND e3.answerCorrect IS NOT NULL
              )
        ) latest ON latest.wordId = w.id
        WHERE w.library = :library AND latest.answerCorrect = 0
        ORDER BY latest.timestamp DESC
        """
    )
    suspend fun wrongWords(library: String): List<WrongWordRow>

    @Query(
        """
        SELECT COUNT(*) FROM words w
        JOIN (
            SELECT e.wordId AS wordId, e.answerCorrect AS answerCorrect, e.timestamp AS timestamp
            FROM study_events e
            WHERE e.answerCorrect IS NOT NULL
              AND e.timestamp = (
                  SELECT MAX(e3.timestamp) FROM study_events e3
                  WHERE e3.wordId = e.wordId AND e3.answerCorrect IS NOT NULL
              )
        ) latest ON latest.wordId = w.id
        WHERE w.library = :library AND latest.answerCorrect = 0
        """
    )
    suspend fun wrongWordCount(library: String): Int

    /** 重置某词库的学习事件。 */
    @Query("DELETE FROM study_events WHERE wordId IN (SELECT id FROM words WHERE library = :library)")
    suspend fun deleteByLibrary(library: String)
}
