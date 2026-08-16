package com.tool.flashread.core.reading

import com.tool.flashread.core.model.Book
import com.tool.flashread.core.speedread.SpeedReadDefaults
import com.tool.flashread.core.speedread.splitBookParagraphs
import kotlin.math.ceil

fun bookProgressPercent(content: String, paragraphIndex: Int): Int {
    return bookProgressPercent(paragraphIndex, paragraphCount(content))
}

fun bookProgressPercent(paragraphIndex: Int, paragraphCount: Int): Int {
    if (paragraphCount <= 0) return 0
    return ((paragraphIndex * 100) / paragraphCount).coerceIn(0, 100)
}

fun wordCount(content: String): Int = countWordsIn(content)

fun paragraphCount(content: String): Int = countParagraphsIn(content)

fun Book.withReadingStats(): Book {
    val words = countWordsIn(content)
    val paragraphs = countParagraphsIn(content)
    if (wordCount == words && paragraphCount == paragraphs) return this
    return copy(wordCount = words, paragraphCount = paragraphs)
}

private fun countWordsIn(content: String): Int {
    var count = 0
    var inWord = false
    for (index in content.indices) {
        if (content[index].isWhitespace()) {
            inWord = false
        } else if (!inWord) {
            inWord = true
            count++
        }
    }
    return count
}

private fun countParagraphsIn(content: String): Int {
    var count = 0
    var lineHasContent = false
    for (index in content.indices) {
        val ch = content[index]
        if (ch == '\n') {
            if (lineHasContent) count++
            lineHasContent = false
        } else if (!ch.isWhitespace()) {
            lineHasContent = true
        }
    }
    if (lineHasContent) count++
    return count
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
