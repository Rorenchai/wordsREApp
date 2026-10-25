package com.example.study;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * 单词表数据访问接口（阶段 2-1）。
 * 注意：Room 禁止在主线程查询/写入，调用方（阶段 2-3 的 Repository）必须用 Executor 异步执行。
 */
@Dao
public interface WordDao {

    /** 批量插入词库，阶段 2-2 首次启动解析 JSON 后调用 */
    @Insert
    void insertAll(List<Word> words);

    /** 随机抽一个单词；表为空时返回 null */
    @Query("SELECT * FROM words ORDER BY RANDOM() LIMIT 1")
    Word getRandomWord();
}
