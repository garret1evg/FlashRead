package com.tool.flashread.core.reading

import com.tool.flashread.core.speedread.SpeedReadDefaults
import com.tool.flashread.core.speedread.splitBookParagraphs
import kotlin.math.ceil

fun bookProgressPercent(content: String, paragraphIndex: Int): Int {
    val paragraphs = splitBookParagraphs(content)
    if (paragraphs.isEmpty()) return 0
    return ((paragraphIndex * 100) / paragraphs.size).coerceIn(0, 100)
}

fun wordCount(content: String): Int {
    return content.split(Regex("\\s+")).count { it.isNotBlank() }
}

fun remainingWordCount(content: String, paragraphIndex: Int): Int {
    val paragraphs = splitBookParagraphs(content)
    if (paragraphs.isEmpty()) return 0
    val start = paragraphIndex.coerceIn(0, paragraphs.size)
    return paragraphs.drop(start).sumOf { wordCount(it) }
}

fun estimatedRemainingMinutes(remainingWords: Int, wpm: Int): Int {
    if (remainingWords <= 0) return 0
    val safeWpm = SpeedReadDefaults.snapWpm(wpm)
    return ceil(remainingWords.toDouble() / safeWpm).toInt()
}
