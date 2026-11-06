package com.example.study;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 单个单词的累计答题记录。
 * spelling 使用唯一索引，保证同一个词始终只有一行，后续作答只累加计数。
 */
@Entity(tableName = "learning_records", indices = {
        @Index(value = "spelling", unique = true)
})
public class LearningRecord {

    /** 自增主键；新记录插入前由 Room 生成 */
    @PrimaryKey(autoGenerate = true)
    private Long id;

    /** 关联 words.spelling，按单词累计 */
    private String spelling;

    /** 曝光次数：每答一题加 1，答错也计入 */
    private int exposureCount;

    /** 答对次数：仅答对时加 1 */
    private int correctCount;

    /** 最近一次作答的毫秒时间戳 */
    private long lastAnsweredAt;

    public LearningRecord() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSpelling() {
        return spelling;
    }

    public void setSpelling(String spelling) {
        this.spelling = spelling;
    }

    public int getExposureCount() {
        return exposureCount;
    }

    public void setExposureCount(int exposureCount) {
        this.exposureCount = exposureCount;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public long getLastAnsweredAt() {
        return lastAnsweredAt;
    }

    public void setLastAnsweredAt(long lastAnsweredAt) {
        this.lastAnsweredAt = lastAnsweredAt;
    }
}
