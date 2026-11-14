package com.example.study.data.db

/** 词库名称与词数（内置 CET-4 / CET-6 / PETS-B 与用户自定义词库统一展示）。 */
data class LibraryCount(
    val library: String,
    val count: Int
)
