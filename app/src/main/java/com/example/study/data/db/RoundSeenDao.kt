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
}
