package com.example.study.data.db

/** 按日期分组的计数行（计划日历统计用）。 */
data class DayCountRow(
    val date: String,
    val count: Int
)
