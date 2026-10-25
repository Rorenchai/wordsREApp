package com.example.study;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 单词实体（阶段 2-1 起由普通 POJO 升级为 Room 表）。
 * 表名 words，字段：id（自增主键）、spelling（拼写）、meaning（释义）、level（词库级别）。
 */
@Entity(tableName = "words")
public class Word {

    /** 自增主键；阶段 1 的内存词没有 id，允许为 null */
    @PrimaryKey(autoGenerate = true)
    private Long id;

    private String spelling;

    private String meaning;

    /** 词库级别，如 "CET3" / "CET4" / "CET6"；阶段 1 的硬编码词没有级别，允许为 null */
    private String level;

    /** Room 需要一个无参构造函数来创建从数据库读出的对象 */
    public Word() {
    }

    /**
     * 阶段 1 沿用的两参构造函数，保证 QuizViewModel 里的 new Word(spelling, meaning) 不受影响。
     * 加 @Ignore 让 Room 明确忽略它、只用上面的无参构造函数，避免编译期多条构造函数警告。
     */
    @Ignore
    public Word(String spelling, String meaning) {
        this.spelling = spelling;
        this.meaning = meaning;
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

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}
