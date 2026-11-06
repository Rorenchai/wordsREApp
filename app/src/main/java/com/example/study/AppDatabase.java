package com.example.study;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Room 数据库单例。
 * 阶段 3-1 升级到 version 2：新增 learning_records 表，保留原有 words 数据。
 * exportSchema = false：当前不需要导出 schema 文件，避免构建时提示缺少 schema 目录。
 */
@Database(entities = {Word.class, LearningRecord.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    /** 数据库文件名，最终位于应用数据目录 /data/data/com.example.study/databases/study.db */
    private static final String DATABASE_NAME = "study.db";

    /** volatile + 双重检查，保证多线程下只创建一个连接 */
    private static volatile AppDatabase instance;

    /** version 1 → 2：只新增答题记录表及其唯一索引，不删除或重建 words 表 */
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `learning_records` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "`spelling` TEXT, "
                            + "`exposureCount` INTEGER NOT NULL, "
                            + "`correctCount` INTEGER NOT NULL, "
                            + "`lastAnsweredAt` INTEGER NOT NULL)");
            database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_learning_records_spelling` "
                            + "ON `learning_records` (`spelling`)");
        }
    };

    /** 单词表 Dao，上层通过它读写词库 */
    public abstract WordDao wordDao();

    /** 答题记录 Dao，上层通过它累计每个单词的作答数据 */
    public abstract LearningRecordDao learningRecordDao();

    /**
     * 获取数据库单例，全局只创建一次。
     * 注意：插入与查询都必须放到子线程（Executor 等）执行，Room 不允许主线程访问数据库。
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME)
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }
}
