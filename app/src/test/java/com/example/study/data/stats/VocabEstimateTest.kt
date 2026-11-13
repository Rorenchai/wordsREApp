package com.example.study.data.stats

import org.junit.Assert.assertEquals
import org.junit.Test

/** 词汇量估算公式（四选一猜测校正）单元测试。 */
class VocabEstimateTest {

    @Test
    fun allCorrectReturnsPoolSize() {
        assertEquals(8535, estimateVocabularySize(correct = 20, answered = 20, poolSize = 8535))
    }

    @Test
    fun noneCorrectReturnsZero() {
        assertEquals(0, estimateVocabularySize(correct = 0, answered = 20, poolSize = 8535))
    }

    @Test
    fun quarterCorrectIsZeroAfterGuessCorrection() {
        assertEquals(0, estimateVocabularySize(correct = 5, answered = 20, poolSize = 8535))
    }

    @Test
    fun halfCorrectEstimatesOneThirdOfPool() {
        assertEquals(2850, estimateVocabularySize(correct = 10, answered = 20, poolSize = 8535))
    }

    @Test
    fun zeroAnsweredReturnsZero() {
        assertEquals(0, estimateVocabularySize(correct = 0, answered = 0, poolSize = 8535))
    }

    @Test
    fun resultNeverExceedsPool() {
        val estimate = estimateVocabularySize(correct = 20, answered = 20, poolSize = 100)
        assertEquals(100, estimate)
    }
}
