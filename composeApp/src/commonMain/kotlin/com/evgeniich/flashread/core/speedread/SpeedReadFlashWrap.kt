package com.evgeniich.flashread.core.speedread

/**
 * Keeps a multi-word flash on one line when it fits, otherwise wraps at a word
 * boundary onto a second line.
 */
internal fun wrapFlashText(
    text: String,
    maxWidthPx: Int,
    measureWidthPx: (String) -> Int,
): String {
    if (text.isEmpty() || maxWidthPx <= 0) return text
    if (measureWidthPx(text) <= maxWidthPx) return text
    val words = text.split(' ').filter { it.isNotEmpty() }
    if (words.size < 2) return text

    var firstLineWordCount = 1
    while (firstLineWordCount < words.lastIndex) {
        val candidate = words.take(firstLineWordCount + 1).joinToString(" ")
        if (measureWidthPx(candidate) > maxWidthPx) break
        firstLineWordCount++
    }
    val firstLine = words.take(firstLineWordCount).joinToString(" ")
    val secondLine = words.drop(firstLineWordCount).joinToString(" ")
    return "$firstLine\n$secondLine"
}
