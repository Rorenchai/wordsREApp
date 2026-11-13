package com.example.study.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 收藏的单词；wordId 唯一，保证同一单词只收藏一次。 */
@Entity(
    tableName = "favorites",
    indices = [Index(value = ["wordId"], unique = true)]
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val wordId: Long,
    val createdAt: Long
)
