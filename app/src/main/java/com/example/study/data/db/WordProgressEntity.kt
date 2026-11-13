package com.example.study.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 学习状态的三个档位：未学习（无记录）/ 学习中 / 已掌握。 */
object StudyStatus {
    const val LEARNING = 1
    const val MASTERED = 2
}

/**
 * 单个单词的学习进度；只存「学过」的词，未学习 = 查不到记录。
 * 主键就是 wordId，保证一个词只有一条进度。
 */
@Entity(tableName = "word_progress")
data class WordProgressEntity(
    @PrimaryKey val wordId: Long,
    val status: Int,
    val firstLearnedAt: Long,
    val lastLearnedAt: Long,
    val studyCount: Int,
    val masteredAt: Long?
)
