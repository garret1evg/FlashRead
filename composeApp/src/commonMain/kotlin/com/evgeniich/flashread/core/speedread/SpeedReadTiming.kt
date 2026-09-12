package com.evgeniich.flashread.core.speedread

import kotlin.math.roundToLong

object SpeedReadDefaults {
    const val MIN_WPM = 100
    const val MAX_WPM = 1000
    const val WPM_STEP = 25
    const val DEFAULT_WPM = 300
    const val MIN_CHUNK_SIZE = 1
    const val MAX_CHUNK_SIZE = 4
    const val DEFAULT_CHUNK_SIZE = 1
    const val DEFAULT_SPRITZ_ENABLED = true
    const val DEFAULT_LOOP_ENABLED = false
    const val MIN_TEXT_SIZE = 24
    const val MAX_TEXT_SIZE = 48
    const val TEXT_SIZE_STEP = 2
    const val DEFAULT_TEXT_SIZE = 34
    val TEXT_SIZE_SLIDER_STEPS = (MAX_TEXT_SIZE - MIN_TEXT_SIZE) / TEXT_SIZE_STEP - 1
    val WPM_PRESETS = listOf(250, 400, 600, 800)
    val CHUNK_SIZES = (MIN_CHUNK_SIZE..MAX_CHUNK_SIZE).toList()
    val WPM_SLIDER_STEPS = (MAX_WPM - MIN_WPM) / WPM_STEP - 1

    fun snapWpm(wpm: Int): Int {
        val clamped = wpm.coerceIn(MIN_WPM, MAX_WPM)
        val offset = clamped - MIN_WPM
        val snappedOffset = ((offset + WPM_STEP / 2) / WPM_STEP) * WPM_STEP
        return (MIN_WPM + snappedOffset).coerceIn(MIN_WPM, MAX_WPM)
    }

    fun snapTextSize(size: Int): Int {
        val clamped = size.coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE)
        val offset = clamped - MIN_TEXT_SIZE
        val snappedOffset = ((offset + TEXT_SIZE_STEP / 2) / TEXT_SIZE_STEP) * TEXT_SIZE_STEP
        return (MIN_TEXT_SIZE + snappedOffset).coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE)
    }
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
