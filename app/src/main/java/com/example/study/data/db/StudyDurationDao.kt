package com.example.study.data.db

import androidx.room.Dao
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
}
