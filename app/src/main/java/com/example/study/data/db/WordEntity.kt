package com.example.study.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 词库中的一条单词（CET-4 / CET-6 各一套，library 区分）。 */
@Entity(
    tableName = "words",
    indices = [Index(value = ["word", "library"], unique = true)]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val word: String,
    val phonetic: String,
    val pos: String,
    val meaning: String,
    val example: String,
    val exampleZh: String,
    val phrase: String,
    val phraseZh: String,
    val library: String
)
