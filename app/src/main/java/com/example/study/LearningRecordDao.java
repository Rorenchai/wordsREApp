package com.example.study;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/** 答题记录的数据访问接口；所有调用都必须由 Repository 放到后台线程执行。 */
@Dao
public interface LearningRecordDao {

    /** 按单词查询已有累计记录，没有记录时返回 null */
    @Query("SELECT * FROM learning_records WHERE spelling = :spelling LIMIT 1")
    LearningRecord findBySpelling(String spelling);

    /** 首次作答时插入新记录 */
    @Insert
    void insert(LearningRecord record);

    /** 再次作答时更新已有记录，保留主键并累加计数 */
    @Update
    void update(LearningRecord record);

    /** 全表查询，供后续统计与 App Inspection 核对使用 */
    @Query("SELECT * FROM learning_records")
    List<LearningRecord> getAll();
}
