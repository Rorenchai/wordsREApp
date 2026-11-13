package com.example.study.data.db

import androidx.room.Entity

/**
 * 学习时长：按「日期 + 词库」累计前端停留毫秒数。
 * 由学习页在退出/后台时 flush，用于记录页「学习时长」展示。
 */
@Entity(tableName = "study_duration", primaryKeys = ["date", "library"])
data class StudyDurationEntity(
    val date: String,
    val library: String,
    val durationMs: Long
)
