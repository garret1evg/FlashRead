package com.evgeniich.flashread.core.speedread

/**
 * Keeps a multi-word flash on one line when it fits, otherwise wraps at a word
 * boundary onto a second line. For single long words that don't fit, breaks
 * at character boundaries across multiple lines.
 */
internal fun wrapFlashText(
    text: String,
    maxWidthPx: Int,
    measureWidthPx: (String) -> Int,
    maxLines: Int = 2,
): String {
    if (text.isEmpty() || maxWidthPx <= 0) return text
    if (measureWidthPx(text) <= maxWidthPx) return text
    val words = text.split(' ').filter { it.isNotEmpty() }

    // Single word that doesn't fit - break by characters
    if (words.size < 2) {
        return wrapLongWord(text, maxWidthPx, measureWidthPx, maxLines)
    }

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

/**
 * Breaks a single long word into multiple lines at character boundaries.
 */
private fun wrapLongWord(
    word: String,
    maxWidthPx: Int,
    measureWidthPx: (String) -> Int,
    maxLines: Int,
): String {
    if (word.isEmpty() || maxWidthPx <= 0) return word

    val lines = mutableListOf<String>()
    var remaining = word

    while (remaining.isNotEmpty() && lines.size < maxLines) {
        if (measureWidthPx(remaining) <= maxWidthPx) {
            lines.add(remaining)
            remaining = ""
            break
        }

        // Find how many characters fit on this line
        var charCount = remaining.length
        while (charCount > 1 && measureWidthPx(remaining.take(charCount)) > maxWidthPx) {
            charCount--
        }

        // Ensure at least one character per line to avoid infinite loop
        charCount = charCount.coerceAtLeast(1)

        lines.add(remaining.take(charCount))
        remaining = remaining.drop(charCount)
    }

    // If there's still remaining text and we've reached max lines, append to last line
    if (remaining.isNotEmpty() && lines.isNotEmpty()) {
        lines[lines.lastIndex] = lines.last() + remaining
    }

    return lines.joinToString("\n")
}

/**
 * Spritz aligns the pivot letter to the horizontal center. A word can still
 * overflow even when its total width is less than the container, because most
 * of the letters sit after the ORP.
 */
internal fun spritzWordOverflows(
    wordWidthPx: Float,
    pivotCenterInWordPx: Float,
    containerWidthPx: Int,
    paddingPx: Int,
): Boolean {
    if (wordWidthPx <= 0f || containerWidthPx <= 0) return false
    val centerX = containerWidthPx / 2f
    val wordLeft = centerX - pivotCenterInWordPx
    val wordRight = wordLeft + wordWidthPx
    return wordLeft < paddingPx || wordRight > containerWidthPx - paddingPx
}
