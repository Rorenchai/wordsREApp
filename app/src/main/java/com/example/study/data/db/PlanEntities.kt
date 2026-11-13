package com.example.study.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 艾宾浩斯专项背词计划。 */
@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    /** 目标词库：CET-4 / CET-6 */
    val library: String,
    /** 计划开始日期 yyyy-MM-dd */
    val startDate: String,
    /** 备考截止（考试）日期 yyyy-MM-dd */
    val examDate: String,
    /** 每日新词数 */
    val dailyNewWords: Int,
    /** 每日复习上限 */
    val dailyReviewLimit: Int,
    /** 1 = 进行中，2 = 已完成/已结束 */
    val status: Int = STATUS_ACTIVE,
    val createdAt: Long
) {
    companion object {
        const val STATUS_ACTIVE = 1
        const val STATUS_FINISHED = 2
    }
}

/**
 * 计划中的单词：分配日期、艾宾浩斯阶段、下次复习日期。
 * stage = -1 表示已分配到今日新词但尚未学习；0..n 表示已完成的复习轮次。
 */
@Entity(
    tableName = "plan_words",
    primaryKeys = ["planId", "wordId"],
    indices = [Index("planId"), Index("planId", "nextReviewDate"), Index("planId", "batchDate")]
)
data class PlanWordEntity(
    val planId: Long,
    val wordId: Long,
    /** 作为新词分配的日期 yyyy-MM-dd */
    val batchDate: String,
    val stage: Int = STAGE_UNLEARNED,
    val nextReviewDate: String? = null,
    val lastReviewDate: String? = null,
    val learnedAt: String? = null,
    val learnCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0
) {
    companion object {
        const val STAGE_UNLEARNED = -1
    }
}

/** 计划词 + 单词内容的联表行（计划学习/复习页展示用）。 */
data class PlanWordDetail(
    @Embedded val word: WordEntity,
    val planId: Long,
    val batchDate: String,
    val stage: Int,
    val nextReviewDate: String?,
    val lastReviewDate: String?,
    val learnedAt: String?,
    val learnCount: Int,
    val correctCount: Int,
    val wrongCount: Int
)
