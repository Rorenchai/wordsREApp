package com.example.study.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    /** 批量入库；重复的 (word, library) 直接忽略，保证 seeding 幂等。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<WordEntity>)

    @Query("SELECT COUNT(*) FROM words WHERE library = :library")
    suspend fun countByLibrary(library: String): Int

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun findById(id: Long): WordEntity?

    @Query("SELECT * FROM words WHERE library = :library AND word = :word LIMIT 1")
    suspend fun findByWord(library: String, word: String): WordEntity?

    /**
     * 随机抽一个词。
     * @param statusFilter -1 = 全部、0 = 未学习、1 = 学习中、2 = 已掌握
     */
    @Query(
        """
        SELECT w.* FROM words w
        LEFT JOIN word_progress p ON w.id = p.wordId
        WHERE w.library = :library
          AND (:statusFilter = -1 OR COALESCE(p.status, 0) = :statusFilter)
        ORDER BY RANDOM()
        LIMIT 1
        """
    )
    suspend fun randomWord(library: String, statusFilter: Int): WordEntity?
}
