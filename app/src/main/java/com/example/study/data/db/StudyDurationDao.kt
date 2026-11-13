package com.example.study.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StudyDurationDao {

    /** 累计时长（按日期 + 词库 upsert 相加）。 */
    @Query(
        """
        INSERT INTO study_duration (`date`, `library`, `durationMs`)
        VALUES (:date, :library, :durationMs)
        ON CONFLICT(`date`, `library`)
        DO UPDATE SET durationMs = durationMs + :durationMs
        """
    )
    suspend fun addDuration(library: String, date: String, durationMs: Long)

    @Query("SELECT COALESCE(durationMs, 0) FROM study_duration WHERE `date` = :date AND library = :library")
    suspend fun durationOn(library: String, date: String): Long?

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM study_duration WHERE library = :library")
    suspend fun totalDuration(library: String): Long

    /** 备份导出：全部学习时长。 */
    @Query("SELECT * FROM study_duration")
    suspend fun all(): List<StudyDurationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<StudyDurationEntity>)

    /** 备份恢复：清空全部学习时长。 */
    @Query("DELETE FROM study_duration")
    suspend fun deleteAll()
}
