package com.example.study.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 全新重构后的数据库（v1）。
 * 与旧 Java 版 schema 不兼容，使用新的数据库文件名 wordcard.db，旧 study.db 自然废弃；
 * 同时保留 fallbackToDestructiveMigration 作为开发期兜底。
 */
@Database(
    entities = [WordEntity::class, WordProgressEntity::class, StudyEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StudyDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

    abstract fun progressDao(): ProgressDao

    abstract fun studyEventDao(): StudyEventDao

    companion object {
        private const val DB_NAME = "wordcard.db"

        @Volatile
        private var instance: StudyDatabase? = null

        fun get(context: Context): StudyDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
