package com.example.study.data.repo

import com.example.study.data.db.WordDao
import com.example.study.data.db.WordEntity
import com.example.study.data.seed.DatabaseSeeder

/** 词库读取：负责保证 seeding 完成，再提供随机抽词 / 查词能力。 */
class WordRepository(
    private val wordDao: WordDao,
    private val seeder: DatabaseSeeder
) {

    /** 冷启动先等待词库入库完成，避免查询到空表。 */
    suspend fun ensureReady() = seeder.ensureSeeded()

    suspend fun countWords(library: String): Int = wordDao.countByLibrary(library)

    /**
     * 随机抽一个词。
     * @param statusFilter -1 = 全部、0 = 未学习、1 = 学习中、2 = 已掌握
     */
    suspend fun randomWord(library: String, statusFilter: Int): WordEntity? =
        wordDao.randomWord(library, statusFilter)

    suspend fun findWord(id: Long): WordEntity? = wordDao.findById(id)

    suspend fun findWord(library: String, word: String): WordEntity? =
        wordDao.findByWord(library, word)
}
