package com.example.study.data.repo

import com.example.study.data.db.LibraryCount
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

    /** 全部词库词数（含自定义词库），词汇量评测与统计使用。 */
    suspend fun countAllWords(): Int = wordDao.countAll()

    /** 词库列表（内置 + 用户导入的自定义词库，按内置优先排序）。 */
    suspend fun libraries(): List<LibraryCount> = wordDao.libraryCounts()

    /** 批量导入自定义词库；重复单词按 (word, library) 忽略，返回实际新增条数。 */
    suspend fun importLibrary(library: String, words: List<WordEntity>): Int {
        val before = wordDao.countByLibrary(library)
        words.chunked(500).forEach { chunk -> wordDao.insertAll(chunk) }
        return wordDao.countByLibrary(library) - before
    }

    /**
     * 随机抽一个词。
     * @param statusFilter -1 = 全部、0 = 未学习、1 = 学习中、2 = 已掌握
     */
    suspend fun randomWord(library: String, statusFilter: Int): WordEntity? =
        wordDao.randomWord(library, statusFilter)

    suspend fun findWord(id: Long): WordEntity? = wordDao.findById(id)

    suspend fun findWord(library: String, word: String): WordEntity? =
        wordDao.findByWord(library, word)

    /** 词典搜索：library 传 "ALL" 表示跨词库检索。 */
    suspend fun searchWords(library: String, query: String, limit: Int): List<WordEntity> =
        wordDao.searchWords(library, query, limit)

    /** 四选一干扰项：随机取若干条非本题释义。 */
    suspend fun randomMeanings(library: String, excludeSpelling: String, count: Int): List<String> =
        wordDao.randomMeanings(library, excludeSpelling, count)

    /** 听音辨词干扰项：随机取若干条非本题单词拼写。 */
    suspend fun randomWords(library: String, excludeWord: String, count: Int): List<String> =
        wordDao.randomWords(library, excludeWord, count)

    /** 消消乐等模式：随机取若干完整词条。 */
    suspend fun randomWordEntities(library: String, count: Int): List<WordEntity> =
        wordDao.randomWordEntities(library, count)

    /** 跨词库随机抽词（词汇量评测）。 */
    suspend fun randomWordEntitiesAll(count: Int): List<WordEntity> =
        wordDao.randomWordEntitiesAll(count)
}
