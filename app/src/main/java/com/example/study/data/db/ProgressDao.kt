package com.example.study.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ProgressDao {

    @Query("SELECT * FROM word_progress WHERE wordId = :wordId")
    suspend fun findByWordId(wordId: Long): WordProgressEntity?

    @Upsert
    suspend fun upsert(progress: WordProgressEntity)

    /** 当前词库「学过」的词数（有进度记录即算学过）。 */
    @Query(
        """
        SELECT COUNT(*) FROM word_progress p
        JOIN words w ON p.wordId = w.id
        WHERE w.library = :library
        """
    )
    suspend fun countStarted(library: String): Int

    /** 当前词库「已掌握」的词数。 */
    @Query(
        """
        SELECT COUNT(*) FROM word_progress p
        JOIN words w ON p.wordId = w.id
        WHERE w.library = :library AND p.status = 2
        """
    )
    suspend fun countMastered(library: String): Int

    /** 重置某词库的学习进度。 */
    @Query("DELETE FROM word_progress WHERE wordId IN (SELECT id FROM words WHERE library = :library)")
    suspend fun deleteByLibrary(library: String)

    /** 备份导出：全部学习进度（带单词字符串）。 */
    @Query(
        """
        SELECT w.word AS word, w.library AS library, p.status AS status,
               p.firstLearnedAt AS firstLearnedAt, p.lastLearnedAt AS lastLearnedAt,
               p.studyCount AS studyCount, p.masteredAt AS masteredAt
        FROM word_progress p JOIN words w ON p.wordId = w.id
        """
    )
    suspend fun allWithWords(): List<ProgressWithWord>

    /** 备份恢复：清空全部学习进度。 */
    @Query("DELETE FROM word_progress")
    suspend fun deleteAll()
}
