package com.example.study.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一次「有效学习」事件：翻到背面后点了「学习中」或「已掌握」。
 * 同词同日只计一次今日目标（按 date + wordId 去重），但每次复习都会新增一行用于累计学习次数。
 */
@Entity(
    tableName = "study_events",
    indices = [
        Index("date"),
        Index("wordId"),
        Index(value = ["wordId", "date"])
    ]
)
data class StudyEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 本地日期，格式 yyyy-MM-dd */
    val date: String,
    val wordId: Long,
    val timestamp: Long
)
