package com.tool.flashread.core.speedread

import kotlin.math.roundToLong

object SpeedReadDefaults {
    const val MIN_WPM = 100
    const val MAX_WPM = 800
    const val DEFAULT_WPM = 300
    const val MIN_CHUNK_SIZE = 1
    const val MAX_CHUNK_SIZE = 5
    const val DEFAULT_CHUNK_SIZE = 1
    const val DEFAULT_SPRITZ_ENABLED = true
    const val DEFAULT_LOOP_ENABLED = false
}

object SpeedReadTiming {
    const val COMMA_MULTIPLIER = 1.5
    const val SENTENCE_END_MULTIPLIER = 2.2
    const val LONG_WORD_LETTER_THRESHOLD = 8
    const val LONG_WORD_EXTRA_PER_LETTER = 0.05

    fun pauseMultiplier(word: String): Double {
        if (word.isEmpty()) return 1.0
        var multiplier = punctuationMultiplier(word)
        val coreLength = word.count { it.isLetterOrDigit() }
        if (coreLength > LONG_WORD_LETTER_THRESHOLD) {
            multiplier += (coreLength - LONG_WORD_LETTER_THRESHOLD) * LONG_WORD_EXTRA_PER_LETTER
        }
        return multiplier
    }

    fun isSentenceEnd(word: String): Boolean {
        return trailingPunctuation(word).any { it == '.' || it == '!' || it == '?' }
    }

    fun delayMs(wpm: Int, multiplier: Double): Long {
        val clampedWpm = wpm.coerceIn(SpeedReadDefaults.MIN_WPM, SpeedReadDefaults.MAX_WPM)
        val safeMultiplier = multiplier.coerceAtLeast(1.0)
        return (60_000.0 / clampedWpm * safeMultiplier).roundToLong().coerceAtLeast(1L)
    }

    fun chunkDelayMs(chunk: SpeedReadChunk, wpm: Int): Long {
        if (chunk.tokens.isEmpty()) return delayMs(wpm, 1.0)
        return chunk.tokens.sumOf { delayMs(wpm, it.pauseMultiplier) }
    }

    private fun punctuationMultiplier(word: String): Double {
        val trailing = trailingPunctuation(word)
        return when {
            trailing.any { it == '.' || it == '!' || it == '?' } -> SENTENCE_END_MULTIPLIER
            trailing.any { it == ',' || it == ';' } -> COMMA_MULTIPLIER
            else -> 1.0
        }
    }

    private fun trailingPunctuation(word: String): String {
        return word.takeLastWhile { !it.isLetterOrDigit() }
    }
}
