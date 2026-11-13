package com.example.study.data.repo

import androidx.room.withTransaction
import com.example.study.data.db.ProgressDao
import com.example.study.data.db.RoundSeenDao
import com.example.study.data.db.RoundSeenEntity
import com.example.study.data.db.StudyDatabase
import com.example.study.data.db.StudyEventDao
import com.example.study.data.db.StudyEventEntity
import com.example.study.data.db.StudyStatus
import com.example.study.data.db.WordDao
import com.example.study.data.db.WordProgressEntity
import com.example.study.data.stats.LibraryStats
import com.example.study.data.stats.StudyDates
import com.example.study.data.stats.StudyStats
import java.time.LocalDate

/**
 * 学习记录仓库：把「翻转卡片后点学习中/已掌握」落库。
 * 一次有效学习 = 更新 word_progress（read-modify-write 累加）+ 追加一条 study_events。
 */
class StudyRepository(
    private val wordDao: WordDao,
    private val progressDao: ProgressDao,
    private val eventDao: StudyEventDao,
    private val roundSeenDao: RoundSeenDao,
    private val database: StudyDatabase
) {

    suspend fun findProgress(wordId: Long): WordProgressEntity? = progressDao.findByWordId(wordId)

    /**
     * 记录一次有效学习（同词同日会多次写事件，用于累计复习次数；今日目标按日期+单词去重）。
     * @param status 1 = 学习中，2 = 已掌握
     */
    suspend fun markStudied(wordId: Long, status: Int, now: Long = System.currentTimeMillis()) {
        database.withTransaction {
            val existing = progressDao.findByWordId(wordId)
            val progress = if (existing == null) {
                WordProgressEntity(
                    wordId = wordId,
                    status = status,
                    firstLearnedAt = now,
                    lastLearnedAt = now,
                    studyCount = 1,
                    masteredAt = if (status == StudyStatus.MASTERED) now else null
                )
            } else {
                existing.copy(
                    status = status,
                    lastLearnedAt = now,
                    studyCount = existing.studyCount + 1,
                    masteredAt = if (status == StudyStatus.MASTERED) {
                        existing.masteredAt ?: now
                    } else {
                        null
                    }
                )
            }
            progressDao.upsert(progress)
            eventDao.insert(
                StudyEventEntity(
                    date = StudyDates.formatDate(now),
                    wordId = wordId,
                    timestamp = now
                )
            )
        }
    }

    /** 记录一次答题（看词选译）：答题也算一次学习事件，并带上对错标记用于正确率统计。 */
    suspend fun recordQuizAnswer(wordId: Long, correct: Boolean, now: Long = System.currentTimeMillis()) {
        database.withTransaction {
            val existing = progressDao.findByWordId(wordId)
            val progress = if (existing == null) {
                WordProgressEntity(
                    wordId = wordId,
                    status = StudyStatus.LEARNING,
                    firstLearnedAt = now,
                    lastLearnedAt = now,
                    studyCount = 1,
                    masteredAt = null
                )
            } else {
                existing.copy(
                    lastLearnedAt = now,
                    studyCount = existing.studyCount + 1,
                    status = if (existing.status == StudyStatus.MASTERED) {
                        StudyStatus.MASTERED
                    } else {
                        StudyStatus.LEARNING
                    }
                )
            }
            progressDao.upsert(progress)
            eventDao.insert(
                StudyEventEntity(
                    date = StudyDates.formatDate(now),
                    wordId = wordId,
                    timestamp = now,
                    answerCorrect = if (correct) 1 else 0
                )
            )
        }
    }

    /** 记录页数据快照（当前词库）。 */
    suspend fun stats(library: String, today: LocalDate = StudyDates.today()): LibraryStats {
        val totalWords = wordDao.countByLibrary(library)
        val startedWords = progressDao.countStarted(library)
        val masteredWords = progressDao.countMastered(library)
        val todayStudied = eventDao.distinctWordsOn(library, StudyDates.format(today))
        val weekCounts = eventDao.dailyCounts(
            library = library,
            fromDate = StudyDates.format(today.minusDays(6)),
            toDate = StudyDates.format(today)
        )
        val streak = StudyStats.calculateStreak(eventDao.distinctDates(library), today)
        val totalEvents = eventDao.totalEvents(library)
        val quizAnswered = eventDao.quizAnswered(library)
        val quizCorrect = eventDao.quizCorrect(library)
        return LibraryStats(
            totalWords = totalWords,
            startedWords = startedWords,
            masteredWords = masteredWords,
            todayStudied = todayStudied,
            weekCounts = weekCounts,
            streak = streak,
            totalStudyEvents = totalEvents,
            quizAnswered = quizAnswered,
            quizCorrect = quizCorrect
        )
    }

    /** 重置当前词库的学习进度与学习事件（词库本身保留），并清空该词库的本轮记录。 */
    suspend fun resetLibrary(library: String) {
        database.withTransaction {
            progressDao.deleteByLibrary(library)
            eventDao.deleteByLibrary(library)
            roundSeenDao.clearLibrary(library)
        }
    }

    /** 读取 (词库, 筛选) 下的本轮已浏览词（用于进程重启后恢复轮次）。 */
    suspend fun loadRound(library: String, filter: Int): Set<Long> =
        roundSeenDao.wordIds(library, filter).toSet()

    /** 记录本轮已抽到某个词（持久化，跨进程保留）。 */
    suspend fun markRoundSeen(library: String, filter: Int, wordId: Long, now: Long = System.currentTimeMillis()) {
        roundSeenDao.insert(
            RoundSeenEntity(
                library = library,
                filter = filter,
                wordId = wordId,
                seenAt = now
            )
        )
    }

    /** 一轮抽满后重开一轮：清空该 (词库, 筛选) 的已浏览记录。 */
    suspend fun clearRound(library: String, filter: Int) {
        roundSeenDao.clear(library, filter)
    }
}
