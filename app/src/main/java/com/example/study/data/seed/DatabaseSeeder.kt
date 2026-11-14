package com.example.study.data.seed

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.study.data.db.StudyDatabase
import com.example.study.data.db.WordEntity
import com.google.gson.Gson
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 首次启动时把 assets 里的 CET-4 / CET-6 / PETS 词库解析并写入 Room。
 * 同时清理旧版本 PETS-B 词库名与测试词库（custom_test 等），并清空可再生成的缓存。
 * 只处理「当前词库为空才导入」，重复调用安全（幂等）；整个过程在 IO 线程 + 事务内完成。
 */
class DatabaseSeeder(
    private val context: Context,
    private val database: StudyDatabase
) {

    private val mutex = Mutex()

    @Volatile
    private var seeded = false

    suspend fun ensureSeeded() {
        if (seeded) return
        mutex.withLock {
            if (seeded) return
            withContext(Dispatchers.IO) {
                normalizeLibraries()
                seedLibrary(LIB_CET4, ASSET_CET4)
                seedLibrary(LIB_CET6, ASSET_CET6)
                seedLibrary(LIB_PETS, ASSET_PETS)
            }
            seeded = true
        }
    }

    /**
     * 数据清理：
     * 1. 删除测试词库（custom_test / test / 测试 等）及关联进度、收藏、计划、时长；
     * 2. 旧版本 PETS-B 更名为 PETS（若 PETS 已存在则丢弃旧副本）；
     * 3. 清空可再生成的缓存文件。
     */
    private suspend fun normalizeLibraries() {
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            TEST_LIBRARIES.forEach { testLibrary ->
                deleteLibraryData(db, testLibrary)
                db.execSQL("DELETE FROM plans WHERE library = ?", arrayOf(testLibrary))
                Log.i(TAG, "已清理测试词库：$testLibrary")
            }
            if (countWords(db, LEGACY_PETS) > 0) {
                if (countWords(db, LIB_PETS) == 0) {
                    listOf("words", "plans", "round_seen", "study_duration").forEach { table ->
                        db.execSQL(
                            "UPDATE $table SET library = ? WHERE library = ?",
                            arrayOf(LIB_PETS, LEGACY_PETS)
                        )
                    }
                    Log.i(TAG, "PETS-B 词库已更名为 PETS")
                } else {
                    deleteLibraryData(db, LEGACY_PETS)
                    db.execSQL("DELETE FROM plans WHERE library = ?", arrayOf(LEGACY_PETS))
                    Log.i(TAG, "PETS 已存在，已丢弃旧 PETS-B 副本")
                }
            }
        }
        runCatching { context.cacheDir.deleteRecursively() }
    }

    private fun deleteLibraryData(db: SupportSQLiteDatabase, library: String) {
        val wordIdSql = "DELETE FROM %s WHERE wordId IN (SELECT id FROM words WHERE library = ?)"
        listOf("favorites", "word_progress", "study_events", "plan_words").forEach { table ->
            db.execSQL(wordIdSql.format(table), arrayOf(library))
        }
        db.execSQL("DELETE FROM round_seen WHERE library = ?", arrayOf(library))
        db.execSQL("DELETE FROM study_duration WHERE library = ?", arrayOf(library))
        db.execSQL("DELETE FROM words WHERE library = ?", arrayOf(library))
    }

    private fun countWords(db: SupportSQLiteDatabase, library: String): Int {
        return db.query(
            "SELECT COUNT(*) FROM words WHERE library = ?",
            arrayOf(library)
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
    }

    private suspend fun seedLibrary(library: String, assetName: String) {
        val wordDao = database.wordDao()
        if (wordDao.countByLibrary(library) > 0) {
            Log.i(TAG, "$library 词库已存在，跳过导入")
            return
        }
        val words = readWordBank(assetName).mapNotNull { dto ->
            val spelling = dto.word?.trim().orEmpty()
            if (spelling.isEmpty()) {
                null
            } else {
                WordEntity(
                    word = spelling,
                    phonetic = dto.phonetic.orEmpty(),
                    pos = dto.pos.orEmpty(),
                    meaning = dto.meaning.orEmpty(),
                    example = dto.example.orEmpty(),
                    exampleZh = dto.exampleZh.orEmpty(),
                    phrase = dto.phrase.orEmpty(),
                    phraseZh = dto.phraseZh.orEmpty(),
                    library = library
                )
            }
        }
        database.withTransaction {
            words.chunked(INSERT_CHUNK).forEach { chunk -> wordDao.insertAll(chunk) }
        }
        Log.i(TAG, "$library 词库导入完成，共 ${words.size} 条")
    }

    private fun readWordBank(assetName: String): List<WordBankDto> {
        return context.assets.open(assetName).use { stream ->
            InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
                val array = Gson().fromJson(reader, Array<WordBankDto>::class.java)
                array?.toList().orEmpty()
            }
        }
    }

    companion object {
        private const val TAG = "DatabaseSeeder"
        private const val INSERT_CHUNK = 500

        const val LIB_CET4 = "CET-4"
        const val LIB_CET6 = "CET-6"
        const val LIB_PETS = "PETS"
        const val ASSET_CET4 = "wordbank_cet4.json"
        const val ASSET_CET6 = "wordbank_cet6.json"
        const val ASSET_PETS = "wordbank_pets.json"

        private const val LEGACY_PETS = "PETS-B"
        private val TEST_LIBRARIES = listOf("custom_test", "test", "测试", "测试词库")
    }
}
