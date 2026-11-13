package com.example.study.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库：v1 = 词库 / 学习进度 / 学习事件；v2 新增收藏表；v3 学习事件增加答题对错标记。
 * schema 与旧 Java 版不兼容，使用独立数据库文件名 wordcard.db。
 */
@Database(
    entities = [
        WordEntity::class,
        WordProgressEntity::class,
        StudyEventEntity::class,
        FavoriteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class StudyDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

    abstract fun progressDao(): ProgressDao

    abstract fun studyEventDao(): StudyEventDao

    abstract fun favoriteDao(): FavoriteDao

    companion object {
        private const val DB_NAME = "wordcard.db"

        /** v1 → v2：新增收藏表与唯一索引，不动已有表。 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorites` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`wordId` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_favorites_wordId` " +
                        "ON `favorites` (`wordId`)"
                )
            }
        }

        /** v2 → v3：学习事件增加可空的答题对错列（旧事件保持 null）。 */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `study_events` ADD COLUMN `answerCorrect` INTEGER")
            }
        }

        @Volatile
        private var instance: StudyDatabase? = null

        fun get(context: Context): StudyDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
