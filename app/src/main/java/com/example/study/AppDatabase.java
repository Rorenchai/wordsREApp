package com.example.study;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room 数据库单例（阶段 2-1）。
 * exportSchema = false：本阶段不需要导出 schema 文件，避免构建时提示缺少 schema 目录。
 */
@Database(entities = {Word.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    /** 数据库文件名，最终位于应用数据目录 /data/data/com.example.study/databases/study.db */
    private static final String DATABASE_NAME = "study.db";

    /** volatile + 双重检查，保证多线程下只创建一个连接 */
    private static volatile AppDatabase instance;

    /** 单词表 Dao，上层通过它读写词库 */
    public abstract WordDao wordDao();

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
                            .build();
                }
            }
        }
        return instance;
    }
}
