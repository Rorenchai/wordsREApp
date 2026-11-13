package com.example.study.data.backup

import androidx.room.withTransaction
import com.example.study.data.db.FavoriteDao
import com.example.study.data.db.FavoriteEntity
import com.example.study.data.db.ProgressDao
import com.example.study.data.db.RoundSeenDao
import com.example.study.data.db.RoundSeenEntity
import com.example.study.data.db.StudyDatabase
import com.example.study.data.db.StudyDurationDao
import com.example.study.data.db.StudyDurationEntity
import com.example.study.data.db.StudyEventDao
import com.example.study.data.db.StudyEventEntity
import com.example.study.data.db.WordDao
import com.example.study.data.db.WordProgressEntity

/**
 * 学习数据备份：导出全部学习进度 / 事件 / 收藏 / 轮次 / 时长（以「单词 + 词库」为键，
 * 避免依赖本地自增 id）；导入时先清空旧数据再按单词还原，设置项由调用方单独恢复。
 */
class BackupRepository(
    private val wordDao: WordDao,
    private val progressDao: ProgressDao,
    private val eventDao: StudyEventDao,
    private val favoriteDao: FavoriteDao,
    private val roundSeenDao: RoundSeenDao,
    private val durationDao: StudyDurationDao,
    private val database: StudyDatabase
) {

    suspend fun export(): BackupData {
        val progress = progressDao.allWithWords().map {
            ProgressDto(
                word = it.word,
                library = it.library,
                status = it.status,
                firstLearnedAt = it.firstLearnedAt,
                lastLearnedAt = it.lastLearnedAt,
                studyCount = it.studyCount,
                masteredAt = it.masteredAt
            )
        }
        val events = eventDao.allWithWords().map {
            EventDto(
                word = it.word,
                library = it.library,
                date = it.date,
                timestamp = it.timestamp,
                answerCorrect = it.answerCorrect
            )
        }
        val favorites = favoriteDao.allWithWords().map {
            FavoriteDto(word = it.word, library = it.library, createdAt = it.createdAt)
        }
        val roundSeen = roundSeenDao.allWithWords().map {
            RoundSeenDto(word = it.word, library = it.library, filter = it.filter, seenAt = it.seenAt)
        }
        val durations = durationDao.all().map {
            DurationDto(date = it.date, library = it.library, durationMs = it.durationMs)
        }
        return BackupData(
            exportedAt = System.currentTimeMillis(),
            progress = progress,
            events = events,
            favorites = favorites,
            roundSeen = roundSeen,
            durations = durations
        )
    }

    suspend fun import(data: BackupData): BackupResult {
        var progressCount = 0
        var eventCount = 0
        var favoriteCount = 0
        var roundCount = 0
        var skipped = 0

        database.withTransaction {
            progressDao.deleteAll()
            eventDao.deleteAll()
            favoriteDao.deleteAll()
            roundSeenDao.deleteAll()
            durationDao.deleteAll()

            data.progress.forEach { dto ->
                val word = wordDao.findByWord(dto.library, dto.word)
                if (word == null) {
                    skipped++
                } else {
                    progressDao.upsert(
                        WordProgressEntity(
                            wordId = word.id,
                            status = dto.status,
                            firstLearnedAt = dto.firstLearnedAt,
                            lastLearnedAt = dto.lastLearnedAt,
                            studyCount = dto.studyCount,
                            masteredAt = dto.masteredAt
                        )
                    )
                    progressCount++
                }
            }
            data.events.forEach { dto ->
                val word = wordDao.findByWord(dto.library, dto.word)
                if (word == null) {
                    skipped++
                } else {
                    eventDao.insert(
                        StudyEventEntity(
                            date = dto.date,
                            wordId = word.id,
                            timestamp = dto.timestamp,
                            answerCorrect = dto.answerCorrect
                        )
                    )
                    eventCount++
                }
            }
            data.favorites.forEach { dto ->
                val word = wordDao.findByWord(dto.library, dto.word)
                if (word == null) {
                    skipped++
                } else {
                    favoriteDao.insert(FavoriteEntity(wordId = word.id, createdAt = dto.createdAt))
                    favoriteCount++
                }
            }
            data.roundSeen.forEach { dto ->
                val word = wordDao.findByWord(dto.library, dto.word)
                if (word == null) {
                    skipped++
                } else {
                    roundSeenDao.insert(
                        RoundSeenEntity(
                            library = dto.library,
                            filter = dto.filter,
                            wordId = word.id,
                            seenAt = dto.seenAt
                        )
                    )
                    roundCount++
                }
            }
            durationDao.insertAll(
                data.durations.map {
                    StudyDurationEntity(date = it.date, library = it.library, durationMs = it.durationMs)
                }
            )
        }

        return BackupResult(
            progress = progressCount,
            events = eventCount,
            favorites = favoriteCount,
            roundSeen = roundCount,
            durations = data.durations.size,
            skipped = skipped
        )
    }
}
