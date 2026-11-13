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

    /**
     * 词典搜索：按单词或释义模糊匹配；library 传 "ALL" 表示跨词库。
     * 排序优先级：完全匹配 > 前缀匹配 > 包含匹配，其次按字母序。
     */
    @Query(
        """
        SELECT * FROM words
        WHERE (:library = 'ALL' OR library = :library)
          AND (word LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%')
        ORDER BY
            CASE
                WHEN LOWER(word) = LOWER(:query) THEN 0
                WHEN LOWER(word) LIKE LOWER(:query) || '%' THEN 1
                ELSE 2
            END,
            word ASC
        LIMIT :limit
        """
    )
    suspend fun searchWords(library: String, query: String, limit: Int): List<WordEntity>

    /** 随机取若干个非本题的释义作四选一干扰项。 */
    @Query(
        """
        SELECT meaning FROM words
        WHERE library = :library AND word != :excludeSpelling
        ORDER BY RANDOM()
        LIMIT :count
        """
    )
    suspend fun randomMeanings(library: String, excludeSpelling: String, count: Int): List<String>

    /** 随机取若干个非本题的单词拼写，作为听音辨词的干扰项。 */
    @Query(
        """
        SELECT word FROM words
        WHERE library = :library AND word != :excludeWord
        ORDER BY RANDOM()
        LIMIT :count
        """
    )
    suspend fun randomWords(library: String, excludeWord: String, count: Int): List<String>
}
