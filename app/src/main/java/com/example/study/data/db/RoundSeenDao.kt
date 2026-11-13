package com.example.study.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RoundSeenDao {

    @Query("SELECT wordId FROM round_seen WHERE library = :library AND filter = :filter")
    suspend fun wordIds(library: String, filter: Int): List<Long>

    @Query("SELECT COUNT(*) FROM round_seen WHERE library = :library AND filter = :filter")
    suspend fun count(library: String, filter: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(roundSeen: RoundSeenEntity)

    @Query("DELETE FROM round_seen WHERE library = :library AND filter = :filter")
    suspend fun clear(library: String, filter: Int)

    @Query("DELETE FROM round_seen WHERE library = :library")
    suspend fun clearLibrary(library: String)

    /** 备份导出：全部本轮已浏览记录（带单词字符串）。 */
    @Query(
        """
        SELECT w.word AS word, w.library AS library, r.`filter` AS `filter`, r.seenAt AS seenAt
        FROM round_seen r JOIN words w ON r.wordId = w.id
        """
    )
    suspend fun allWithWords(): List<RoundSeenWithWord>

    /** 备份恢复：清空全部本轮已浏览记录。 */
    @Query("DELETE FROM round_seen")
    suspend fun deleteAll()
}
