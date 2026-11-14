package com.example.study.data.db

import androidx.room.Embedded

/** 单词总览列表项：单词本体 + 当前学习状态（0 未学习 / 1 学习中 / 2 已掌握）。 */
data class WordOverviewItem(
    @Embedded val word: WordEntity,
    val status: Int = 0
)
