package com.example.study.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE wordId = :wordId")
    suspend fun deleteByWordId(wordId: Long)

    @Query("SELECT COUNT(*) FROM favorites WHERE wordId = :wordId")
    suspend fun countByWordId(wordId: Long): Int

    /** 收藏的 wordId 列表，用于学习页星标状态实时刷新。 */
    @Query("SELECT wordId FROM favorites")
    fun observeFavoriteIds(): Flow<List<Long>>

    /** 收藏列表（按收藏时间倒序）；library 传 "ALL" 表示全部词库。 */
    @Query(
        """
        SELECT w.* FROM words w
        JOIN favorites f ON w.id = f.wordId
        WHERE (:library = 'ALL' OR w.library = :library)
        ORDER BY f.createdAt DESC
        """
    )
    fun observeFavorites(library: String): Flow<List<WordEntity>>

    @Query("SELECT COUNT(*) FROM favorites")
    fun observeCount(): Flow<Int>

    /** 备份导出：全部收藏（带单词字符串）。 */
    @Query(
        """
        SELECT w.word AS word, w.library AS library, f.createdAt AS createdAt
        FROM favorites f JOIN words w ON f.wordId = w.id
        ORDER BY f.createdAt
        """
    )
    suspend fun allWithWords(): List<FavoriteWithWord>

    /** 备份恢复：清空全部收藏。 */
    @Query("DELETE FROM favorites")
    suspend fun deleteAll()
}
