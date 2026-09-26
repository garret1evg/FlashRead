package com.evgeniich.flashread.core.reading

import com.evgeniich.flashread.core.model.Book
import com.evgeniich.flashread.core.speedread.SpeedReadDefaults
import com.evgeniich.flashread.core.speedread.splitBookParagraphs
import kotlin.math.ceil

fun bookProgressPercent(contentOffset: Int, contentLength: Int): Int {
    if (contentLength <= 0) return 0
    return ((contentOffset.toLong() * 100L) / contentLength).toInt().coerceIn(0, 100)
}

/**
 * Start offsets (in the original [content]) of each non-blank paragraph line,
 * matching [splitBookParagraphs] item indices. `\r\n` is treated as a single
 * newline; blank lines are skipped. Each offset points at the first
 * non-whitespace character of that line.
 */
fun paragraphStartOffsets(content: String): List<Int> {
    val offsets = ArrayList<Int>()
    var i = 0
    while (i < content.length) {
        val lineStart = i
        var lineEnd = i
        while (lineEnd < content.length) {
            val ch = content[lineEnd]
            if (ch == '\n') break
            if (ch == '\r' && lineEnd + 1 < content.length && content[lineEnd + 1] == '\n') break
            lineEnd++
        }

        var contentStart = lineStart
        while (contentStart < lineEnd && content[contentStart].isWhitespace()) contentStart++
        var contentEnd = lineEnd
        while (contentEnd > contentStart && content[contentEnd - 1].isWhitespace()) contentEnd--

        if (contentStart < contentEnd) {
            offsets.add(contentStart)
        }

        if (lineEnd >= content.length) break
        i = if (content[lineEnd] == '\r') lineEnd + 2 else lineEnd + 1
    }
    return offsets
}

fun wordCount(content: String): Int = countWordsIn(content)

fun Book.withReadingStats(): Book {
    val normalized = normalizeParagraphs(content)
    val words = countWordsIn(normalized)
    if (content == normalized && wordCount == words) return this
    return copy(content = normalized, wordCount = words)
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
