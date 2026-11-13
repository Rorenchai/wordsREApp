package com.example.study.data.seed

/** 词库 JSON（assets/wordbank_cet*.json）的元素结构，字段与参考应用保持一致。 */
data class WordBankDto(
    val word: String? = null,
    val phonetic: String? = null,
    val pos: String? = null,
    val meaning: String? = null,
    val example: String? = null,
    val exampleZh: String? = null,
    val phrase: String? = null,
    val phraseZh: String? = null,
    val library: String? = null
)
