package com.example.study.data.db

import androidx.room.Entity

/**
 * 学习页「本轮已浏览」持久化：同一个 (词库, 筛选) 下一轮内已抽过的单词。
 * 进程被杀或设备重启后恢复本轮进度，避免本轮重复抽词。
 */
@Entity(tableName = "round_seen", primaryKeys = ["library", "filter", "wordId"])
data class RoundSeenEntity(
    val library: String,
    val filter: Int,
    val wordId: Long,
    val seenAt: Long
)
