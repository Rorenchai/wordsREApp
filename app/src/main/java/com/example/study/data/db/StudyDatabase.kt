package com.example.study.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库：v1 = 词库 / 学习进度 / 学习事件；v2 新增收藏表；v3 学习事件增加答题对错标记；
 * v4 新增 round_seen（学习页本轮已浏览词持久化）；v5 新增 study_duration（学习时长）；
 * v6 新增 plans / plan_words（艾宾浩斯专项计划）。
 * schema 与旧 Java 版不兼容，使用独立数据库文件名 wordcard.db。
 */
@Database(
    entities = [
        WordEntity::class,
        WordProgressEntity::class,
        StudyEventEntity::class,
        FavoriteEntity::class,
        RoundSeenEntity::class,
        StudyDurationEntity::class,
        PlanEntity::class,
        PlanWordEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class StudyDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

    abstract fun progressDao(): ProgressDao

    abstract fun studyEventDao(): StudyEventDao

    abstract fun favoriteDao(): FavoriteDao

    abstract fun roundSeenDao(): RoundSeenDao

    abstract fun studyDurationDao(): StudyDurationDao

    abstract fun planDao(): PlanDao

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

        /** v3 → v4：新增本轮已浏览词表，用于学习页轮次持久化。 */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `round_seen` (" +
                        "`library` TEXT NOT NULL, " +
                        "`filter` INTEGER NOT NULL, " +
                        "`wordId` INTEGER NOT NULL, " +
                        "`seenAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`library`, `filter`, `wordId`))"
                )
            }
        }

        /** v4 → v5：新增学习时长表（日期 + 词库联合主键）。 */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `study_duration` (" +
                        "`date` TEXT NOT NULL, " +
                        "`library` TEXT NOT NULL, " +
                        "`durationMs` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`date`, `library`))"
                )
            }
        }

        /** v5 → v6：新增艾宾浩斯计划表与计划单词表。 */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `plans` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`library` TEXT NOT NULL, " +
                        "`startDate` TEXT NOT NULL, " +
                        "`examDate` TEXT NOT NULL, " +
                        "`dailyNewWords` INTEGER NOT NULL, " +
                        "`dailyReviewLimit` INTEGER NOT NULL, " +
                        "`status` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `plan_words` (" +
                        "`planId` INTEGER NOT NULL, " +
                        "`wordId` INTEGER NOT NULL, " +
                        "`batchDate` TEXT NOT NULL, " +
                        "`stage` INTEGER NOT NULL, " +
                        "`nextReviewDate` TEXT, " +
                        "`lastReviewDate` TEXT, " +
                        "`learnedAt` TEXT, " +
                        "`learnCount` INTEGER NOT NULL, " +
                        "`correctCount` INTEGER NOT NULL, " +
                        "`wrongCount` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`planId`, `wordId`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_words_planId` ON `plan_words` (`planId`)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_plan_words_planId_nextReviewDate` " +
                        "ON `plan_words` (`planId`, `nextReviewDate`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_plan_words_planId_batchDate` " +
                        "ON `plan_words` (`planId`, `batchDate`)"
                )
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
