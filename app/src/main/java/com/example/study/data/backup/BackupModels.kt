package com.example.study.data.backup

/** 备份文件根结构（JSON）。formatVersion 用于兼容未来格式升级。 */
data class BackupData(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val exportedAt: Long = 0L,
    val app: String = "单词记忆卡",
    val progress: List<ProgressDto> = emptyList(),
    val events: List<EventDto> = emptyList(),
    val favorites: List<FavoriteDto> = emptyList(),
    val roundSeen: List<RoundSeenDto> = emptyList(),
    val durations: List<DurationDto> = emptyList(),
    val settings: BackupSettings = BackupSettings()
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

data class ProgressDto(
    val word: String,
    val library: String,
    val status: Int,
    val firstLearnedAt: Long,
    val lastLearnedAt: Long,
    val studyCount: Int,
    val masteredAt: Long?
)

data class EventDto(
    val word: String,
    val library: String,
    val date: String,
    val timestamp: Long,
    val answerCorrect: Int?
)

data class FavoriteDto(
    val word: String,
    val library: String,
    val createdAt: Long
)

data class RoundSeenDto(
    val word: String,
    val library: String,
    val filter: Int,
    val seenAt: Long
)

data class DurationDto(
    val date: String,
    val library: String,
    val durationMs: Long
)

data class BackupSettings(
    val library: String = "CET-4",
    val dailyGoal: Int = 20,
    val accent: String = "US",
    val vocabEstimate: Int = 0,
    val vocabTestDate: String = ""
)

/** 导入结果统计。 */
data class BackupResult(
    val progress: Int = 0,
    val events: Int = 0,
    val favorites: Int = 0,
    val roundSeen: Int = 0,
    val durations: Int = 0,
    val skipped: Int = 0
)
