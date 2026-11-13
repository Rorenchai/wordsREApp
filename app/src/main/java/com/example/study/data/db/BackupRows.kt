package com.example.study.data.db

/** 备份导出用的连表行：进度（带单词字符串，跨设备/重装后可按 word+library 还原）。 */
data class ProgressWithWord(
    val word: String,
    val library: String,
    val status: Int,
    val firstLearnedAt: Long,
    val lastLearnedAt: Long,
    val studyCount: Int,
    val masteredAt: Long?
)

/** 备份导出用的连表行：学习事件。 */
data class EventWithWord(
    val word: String,
    val library: String,
    val date: String,
    val timestamp: Long,
    val answerCorrect: Int?
)

/** 备份导出用的连表行：收藏。 */
data class FavoriteWithWord(
    val word: String,
    val library: String,
    val createdAt: Long
)

/** 备份导出用的连表行：本轮已浏览。 */
data class RoundSeenWithWord(
    val word: String,
    val library: String,
    val filter: Int,
    val seenAt: Long
)
