package com.example.study.data.seed

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
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
 * 首次启动时把 assets 里的 CET-4 / CET-6 词库解析并写入 Room。
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
                seedLibrary(LIB_CET4, ASSET_CET4)
                seedLibrary(LIB_CET6, ASSET_CET6)
                seedLibrary(LIB_PETS_B, ASSET_PETS_B)
            }
            seeded = true
        }
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
        const val LIB_PETS_B = "PETS-B"
        const val ASSET_CET4 = "wordbank_cet4.json"
        const val ASSET_CET6 = "wordbank_cet6.json"
        const val ASSET_PETS_B = "wordbank_petsb.json"
    }
}
