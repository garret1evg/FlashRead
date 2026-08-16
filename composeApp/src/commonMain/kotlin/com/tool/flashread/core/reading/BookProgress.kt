package com.tool.flashread.core.reading

import com.tool.flashread.core.speedread.splitBookParagraphs

fun bookProgressPercent(content: String, paragraphIndex: Int): Int {
    val paragraphs = splitBookParagraphs(content)
    if (paragraphs.isEmpty()) return 0
    return ((paragraphIndex * 100) / paragraphs.size).coerceIn(0, 100)
}

fun wordCount(content: String): Int {
    return content.split(Regex("\\s+")).count { it.isNotBlank() }
}
