package com.evgeniich.flashread.core.speedread

/**
 * Locates a word in book content, providing both the global content offset
 * and the local range within the displayed (trimmed) paragraph text.
 */
data class WordLocation(
    /** Character offset in book.content where the word starts. */
    val contentOffset: Int,
    /** Paragraph index (matching [splitBookParagraphs] index). */
    val paragraphIndex: Int,
    /** Start index within the trimmed paragraph text. */
    val localStart: Int,
    /** End index (exclusive) within the trimmed paragraph text. */
    val localEnd: Int,
)

/**
 * Returns the first word in the specified paragraph, or null if the paragraph
 * doesn't exist or has no words.
 */
fun firstWordInParagraph(content: String, paragraphIndex: Int): WordLocation? {
    val source = SpeedReadSource(content)
    var current = source.first() ?: return null
    while (current.paragraphIndex < paragraphIndex) {
        current = source.next(current) ?: return null
    }
    if (current.paragraphIndex != paragraphIndex) return null
    return wordLocationFromRef(source, current)
}

/**
 * Returns the word at the given local offset within a paragraph's displayed
 * (trimmed) text. If the offset is in whitespace, returns the nearest word
 * (next word preferred, previous if at end).
 */
fun wordAtParagraphOffset(content: String, paragraphIndex: Int, localCharOffset: Int): WordLocation? {
    val source = SpeedReadSource(content)
    var current = source.first() ?: return null
    while (current.paragraphIndex < paragraphIndex) {
        current = source.next(current) ?: return null
    }
    if (current.paragraphIndex != paragraphIndex) return null

    val paragraphContentStart = findParagraphContentStart(source.text, current.start)
    val targetOffset = paragraphContentStart + localCharOffset

    var firstInParagraph = current
    var lastInParagraph = current
    var bestMatch: SpeedReadTokenRef? = null

    while (current.paragraphIndex == paragraphIndex) {
        lastInParagraph = current
        if (targetOffset in current.start until current.end) {
            bestMatch = current
            break
        }
        if (bestMatch == null && current.start >= targetOffset) {
            bestMatch = current
        }
        current = source.next(current) ?: break
    }

    val result = bestMatch ?: lastInParagraph
    return wordLocationFromRef(source, result)
}

/**
 * Returns the word at the given content offset (for rendering a saved position).
 * If the offset is in whitespace, returns the next word (or null if past end).
 */
fun wordHighlightAtContentOffset(content: String, wordOffset: Int): WordLocation? {
    if (wordOffset < 0 || wordOffset >= content.length) return null
    val source = SpeedReadSource(content)
    var current = source.first() ?: return null
    var lastSeen = current

    while (true) {
        if (wordOffset in current.start until current.end) {
            return wordLocationFromRef(source, current)
        }
        if (current.start > wordOffset) {
            return wordLocationFromRef(source, current)
        }
        lastSeen = current
        current = source.next(current) ?: break
    }
    return wordLocationFromRef(source, lastSeen)
}

private fun wordLocationFromRef(source: SpeedReadSource, ref: SpeedReadTokenRef): WordLocation {
    val paragraphContentStart = findParagraphContentStart(source.text, ref.start)
    return WordLocation(
        contentOffset = ref.start,
        paragraphIndex = ref.paragraphIndex,
        localStart = ref.start - paragraphContentStart,
        localEnd = ref.end - paragraphContentStart,
    )
}

private fun findParagraphContentStart(text: String, offsetInParagraph: Int): Int {
    var lineStart = offsetInParagraph
    while (lineStart > 0 && text[lineStart - 1] != '\n') {
        lineStart--
    }
    while (lineStart < text.length && text[lineStart].isWhitespace() && text[lineStart] != '\n') {
        lineStart++
    }
    return lineStart
}

/**
 * Splits book content the same way the Reader does, so SpeedRead paragraph
 * indices line up with [com.evgeniich.flashread.core.model.ReadingPosition].
 */
fun splitBookParagraphs(content: String): List<String> {
    return content
        .replace("\r\n", "\n")
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

fun tokenizeBook(content: String): List<SpeedReadToken> {
    val source = SpeedReadSource(content)
    val tokens = ArrayList<SpeedReadToken>()
    source.forEachToken { start, end, paragraphIndex ->
        tokens.add(source.token(start, end, paragraphIndex))
        true
    }
    return tokens
}

fun chunkTokens(
    tokens: List<SpeedReadToken>,
    chunkSize: Int,
): List<SpeedReadChunk> {
    if (tokens.isEmpty()) return emptyList()
    val size = chunkSize.coerceIn(SpeedReadDefaults.MIN_CHUNK_SIZE, SpeedReadDefaults.MAX_CHUNK_SIZE)
    return tokens.chunked(size).mapIndexed { index, group ->
        SpeedReadChunk(
            tokens = group,
            startTokenIndex = index * size,
        )
    }
}

internal data class SpeedReadTokenRef(
    val paragraphIndex: Int,
    val start: Int,
    val end: Int,
)

/**
 * Walks book text word-by-word without building a full token list.
 * Line breaks match [splitBookParagraphs]: `\r\n` or `\n` start a new line,
 * blank lines are skipped, and each non-blank line is one paragraph.
 */
internal class SpeedReadSource(val text: String) {

    fun first(): SpeedReadTokenRef? {
        val line = nextContentLine(0) ?: return null
        return wordAt(line.first, paragraphIndex = 0, lineEnd = line.last + 1)
    }

    fun next(from: SpeedReadTokenRef): SpeedReadTokenRef? {
        val newline = findNewline(from.start)
        var i = from.end
        while (i < newline && text[i].isWhitespace()) i++
        if (i < newline) {
            return wordAt(i, from.paragraphIndex, newline)
        }
        val nextLine = nextContentLine(skipNewlineAt(newline)) ?: return null
        return wordAt(nextLine.first, from.paragraphIndex + 1, nextLine.last + 1)
    }

    fun previous(from: SpeedReadTokenRef): SpeedReadTokenRef? {
        val line = trimmedContent(rawLineStart(from.start), findNewline(from.start))
            ?: return previousLineLastWord(from)
        var i = from.start - 1
        while (i >= line.first && text[i].isWhitespace()) i--
        if (i >= line.first) {
            var start = i
            while (start > line.first && !text[start - 1].isWhitespace()) start--
            return SpeedReadTokenRef(from.paragraphIndex, start, i + 1)
        }
        return previousLineLastWord(from)
    }

    fun tokenAt(offset: Int, paragraphIndex: Int): SpeedReadTokenRef? {
        if (offset < 0 || offset >= text.length) return null
        if (text[offset].isWhitespace()) return null
        return wordAt(offset, paragraphIndex, findNewline(offset))
    }

    fun token(start: Int, end: Int, paragraphIndex: Int): SpeedReadToken {
        val word = text.substring(start, end)
        return SpeedReadToken(
            text = word,
            paragraphIndex = paragraphIndex,
            pauseMultiplier = SpeedReadTiming.pauseMultiplier(word),
            isSentenceEnd = SpeedReadTiming.isSentenceEnd(word),
        )
    }

    fun token(ref: SpeedReadTokenRef): SpeedReadToken = token(ref.start, ref.end, ref.paragraphIndex)

    inline fun forEachToken(action: (start: Int, end: Int, paragraphIndex: Int) -> Boolean) {
        var lineFrom = 0
        var paragraphIndex = 0
        while (true) {
            val line = nextContentLine(lineFrom) ?: return
            var i = line.first
            val lineEnd = line.last + 1
            while (i < lineEnd) {
                while (i < lineEnd && text[i].isWhitespace()) i++
                if (i >= lineEnd) break
                var end = i
                while (end < lineEnd && !text[end].isWhitespace()) end++
                if (!action(i, end, paragraphIndex)) return
                i = end
            }
            lineFrom = skipNewlineAt(findNewline(line.first))
            paragraphIndex++
        }
    }

    private fun previousLineLastWord(from: SpeedReadTokenRef): SpeedReadTokenRef? {
        if (from.paragraphIndex <= 0) return null
        val prevLine = previousContentLine(from.start) ?: return null
        return lastWordOnLine(prevLine, from.paragraphIndex - 1)
    }

    private fun wordAt(start: Int, paragraphIndex: Int, lineEnd: Int): SpeedReadTokenRef {
        var end = start
        while (end < lineEnd && !text[end].isWhitespace()) end++
        return SpeedReadTokenRef(paragraphIndex, start, end)
    }

    private fun lastWordOnLine(line: IntRange, paragraphIndex: Int): SpeedReadTokenRef {
        val lineEnd = line.last + 1
        var end = lineEnd
        while (end > line.first && text[end - 1].isWhitespace()) end--
        var start = end
        while (start > line.first && !text[start - 1].isWhitespace()) start--
        return SpeedReadTokenRef(paragraphIndex, start, end)
    }

    private fun nextContentLine(from: Int): IntRange? {
        var i = from
        while (i < text.length) {
            val newline = findNewline(i)
            val content = trimmedContent(i, newline)
            if (content != null) return content
            if (newline >= text.length) return null
            i = skipNewlineAt(newline)
        }
        return null
    }

    private fun previousContentLine(offsetInCurrentLine: Int): IntRange? {
        var lineStart = rawLineStart(offsetInCurrentLine)
        while (lineStart > 0) {
            val previous = previousRawLine(lineStart) ?: return null
            val content = trimmedContent(previous.first, previous.second)
            if (content != null) return content
            lineStart = previous.first
        }
        return null
    }

    private fun previousRawLine(currentRawLineStart: Int): Pair<Int, Int>? {
        if (currentRawLineStart <= 0) return null
        var newlinePos = currentRawLineStart - 1
        if (text[newlinePos] == '\n' && newlinePos > 0 && text[newlinePos - 1] == '\r') {
            newlinePos -= 1
        }
        return rawLineStart(newlinePos) to newlinePos
    }

    private fun trimmedContent(start: Int, end: Int): IntRange? {
        var a = start
        var b = end
        while (a < b && text[a].isWhitespace()) a++
        while (b > a && text[b - 1].isWhitespace()) b--
        return if (a < b) a until b else null
    }

    private fun rawLineStart(offset: Int): Int {
        var i = offset.coerceIn(0, text.length)
        while (i > 0) {
            if (text[i - 1] == '\n') return i
            i--
        }
        return 0
    }

    private fun findNewline(from: Int): Int {
        var i = from
        while (i < text.length) {
            if (text[i] == '\n') return i
            if (text[i] == '\r' && i + 1 < text.length && text[i + 1] == '\n') return i
            i++
        }
        return text.length
    }

    private fun skipNewlineAt(newlineIndex: Int): Int {
        if (newlineIndex >= text.length) return text.length
        return if (text[newlineIndex] == '\r') newlineIndex + 2 else newlineIndex + 1
    }
}
