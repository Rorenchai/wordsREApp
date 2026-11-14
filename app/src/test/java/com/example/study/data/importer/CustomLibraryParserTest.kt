package com.example.study.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomLibraryParserTest {

    @Test
    fun `解析 JSON 数组并保留扩展字段`() {
        val json = """
            [
              {"word":"apple","phonetic":"ˈæpl","meaning":"n. 苹果","example":"An apple a day."},
              {"spelling":"banana","translation":"n. 香蕉"}
            ]
        """.trimIndent()
        val entries = CustomLibraryParser.parse("MyWords.json", json)
        assertEquals(2, entries.size)
        assertEquals("apple", entries[0].word)
        assertEquals("n. 苹果", entries[0].meaning)
        assertEquals("ˈæpl", entries[0].phonetic)
        assertEquals("banana", entries[1].word)
        assertEquals("n. 香蕉", entries[1].meaning)
    }

    @Test
    fun `解析带 UTF8 BOM 的 JSON`() {
        val json = "\uFEFF" + """[{"word":"apple","meaning":"n. 苹果"}]"""
        val entries = CustomLibraryParser.parse("bom.json", json)
        assertEquals(1, entries.size)
        assertEquals("apple", entries[0].word)
        assertEquals("n. 苹果", entries[0].meaning)
    }

    @Test
    fun `解析 words 包裹的 JSON`() {
        val json = """{"words":[{"word":"cat","meaning":"n. 猫"}]}"""
        val entries = CustomLibraryParser.parse("cat.json", json)
        assertEquals(1, entries.size)
        assertEquals("cat", entries[0].word)
    }

    @Test
    fun `解析 CSV 文本并跳过注释与表头`() {
        val text = """
            # 我的词库
            word,meaning,phonetic
            apple,n. 苹果,/ˈæpl/
            banana,n. 香蕉
            hello,你好；喂
        """.trimIndent()
        val entries = CustomLibraryParser.parse("custom.csv", text)
        assertEquals(3, entries.size)
        assertEquals("apple", entries[0].word)
        assertEquals("n. 苹果；/ˈæpl/", entries[0].meaning)
        assertEquals("hello", entries[2].word)
    }

    @Test
    fun `解析 Tab 分隔文本`() {
        val entries = CustomLibraryParser.parse("x.txt", "run\tv. 跑\njump\tv. 跳")
        assertEquals(2, entries.size)
        assertEquals("v. 跑", entries[0].meaning)
    }

    @Test
    fun `非法内容返回空列表`() {
        assertTrue(CustomLibraryParser.parse("bad.json", "not a word list").isEmpty())
        assertTrue(CustomLibraryParser.parse("empty.txt", "").isEmpty())
    }

    @Test
    fun `文件名推导默认词库名`() {
        assertEquals("MyWords", CustomLibraryParser.defaultLibraryName("MyWords.json"))
        assertEquals("自定义词库", CustomLibraryParser.defaultLibraryName(".txt"))
        assertEquals("单词 表", CustomLibraryParser.defaultLibraryName("单词:表.csv"))
    }
}
