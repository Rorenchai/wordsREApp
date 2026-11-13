package com.example.study.data.db

import androidx.room.Embedded

/** 错题本条目：单词信息 + 累计答错次数 + 最近一次答错时间。 */
data class WrongWordRow(
    @Embedded val word: WordEntity,
    val wrongCount: Int,
    val lastWrongAt: Long
)
