package com.evgeniich.flashread.core.speedread

data class ContextWindow(
    val beforeText: String,
    val afterText: String,
)

private const val MAX_CONTEXT_TOKENS = 80

/**
 * Selects previous/following context around the active word or word group.
 *
 * Up to [maxLines] of whole words are packed using [measureWidthPx] against
 * [maxWidthPx]. Distant content is trimmed first: the start of previous
 * context and the end of following context. Active tokens are never included.
 */
fun extractSpeedReadContext(
    content: String,
    position: SpeedReadPosition,
    chunkSize: Int,
    maxWidthPx: Int,
    measureWidthPx: (String) -> Int,
    maxLines: Int = 2,
): ContextWindow {
    val empty = ContextWindow(beforeText = "", afterText = "")
    if (content.isEmpty() || maxWidthPx <= 0 || maxLines <= 0) return empty

    val source = SpeedReadSource(content)
    val size = chunkSize.coerceIn(
        SpeedReadDefaults.MIN_CHUNK_SIZE,
        SpeedReadDefaults.MAX_CHUNK_SIZE,
    )
    val firstActive = source.tokenAt(position.offset, position.paragraphIndex) ?: return empty
    var lastActive = firstActive
    repeat(size - 1) {
        lastActive = source.next(lastActive) ?: return@repeat
    }

    val beforeWords = collectNeighborTokens(
        source = source,
        startExclusive = firstActive,
        step = source::previous,
        maxWidthPx = maxWidthPx,
        measureWidthPx = measureWidthPx,
        maxLines = maxLines,
    )
    val afterWords = collectNeighborTokens(
        source = source,
        startExclusive = lastActive,
        step = source::next,
        maxWidthPx = maxWidthPx,
        measureWidthPx = measureWidthPx,
        maxLines = maxLines,
    )
    return ContextWindow(
        beforeText = packContextLines(
            wordsNearestFirst = beforeWords,
            maxWidthPx = maxWidthPx,
            measureWidthPx = measureWidthPx,
            maxLines = maxLines,
            nearestAtEnd = true,
        ),
        afterText = packContextLines(
            wordsNearestFirst = afterWords,
            maxWidthPx = maxWidthPx,
            measureWidthPx = measureWidthPx,
            maxLines = maxLines,
            nearestAtEnd = false,
        ),
    )
}

private fun collectNeighborTokens(
    source: SpeedReadSource,
    startExclusive: SpeedReadTokenRef,
    step: (SpeedReadTokenRef) -> SpeedReadTokenRef?,
    maxWidthPx: Int,
    measureWidthPx: (String) -> Int,
    maxLines: Int,
): List<String> {
    val words = ArrayList<String>()
    var current = startExclusive
    var collectedWidth = 0
    val widthBudget = maxLines * maxWidthPx
    repeat(MAX_CONTEXT_TOKENS) {
        current = step(current) ?: return words
        val word = source.text.substring(current.start, current.end)
        words.add(word)
        if (collectedWidth > 0) {
            collectedWidth += measureWidthPx(" ")
        }
        collectedWidth += measureWidthPx(word)
        if (collectedWidth > widthBudget) return words
    }
    return words
}

/**
 * Packs [wordsNearestFirst] into at most [maxLines].
 *
 * When [nearestAtEnd] is true (previous context), each line is filled by
 * prepending more distant words so the nearest sit at the end of the bottom
 * line. When false (following context), nearest words fill the top line first.
 * A single overflowing word occupies its line and is not split.
 */
private fun packContextLines(
    wordsNearestFirst: List<String>,
    maxWidthPx: Int,
    measureWidthPx: (String) -> Int,
    maxLines: Int,
    nearestAtEnd: Boolean,
): String {
    if (wordsNearestFirst.isEmpty()) return ""
    val linesNearestFirst = ArrayList<String>(maxLines)
    var line = ""
    for (word in wordsNearestFirst) {
        if (linesNearestFirst.size >= maxLines) break
        val candidate = when {
            line.isEmpty() -> word
            nearestAtEnd -> "$word $line"
            else -> "$line $word"
        }
        if (line.isEmpty() || measureWidthPx(candidate) <= maxWidthPx) {
            line = candidate
        } else {
            linesNearestFirst.add(line)
            if (linesNearestFirst.size >= maxLines) break
            line = word
        }
    }
    if (line.isNotEmpty() && linesNearestFirst.size < maxLines) {
        linesNearestFirst.add(line)
    }
    val lines = if (nearestAtEnd) linesNearestFirst.asReversed() else linesNearestFirst
    return lines.joinToString("\n")
}
