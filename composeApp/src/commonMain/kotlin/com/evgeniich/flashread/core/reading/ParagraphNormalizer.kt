package com.evgeniich.flashread.core.reading

/** Max character length before a line is considered too long. */
const val MAX_PARAGRAPH_CHARS = 400

/** Max word count before a line is considered too long. */
const val MAX_PARAGRAPH_WORDS = 60

/** Hard fallback: insert a newline after every Nth word in an unsplittable segment. */
const val HARD_BREAK_EVERY_WORDS = 40

/**
 * Ensures long single-line texts become real newline-separated paragraphs at import time,
 * so [splitBookParagraphs] and SpeedRead newline splitting stay in sync.
 *
 * Walks [content] line by line (`\n` / `\r\n`). Short lines pass through unchanged
 * (line endings normalized to `\n`). Too-long lines are re-split at sentence boundaries,
 * with a word-count hard fallback. Idempotent; does not trim the document.
 */
fun normalizeParagraphs(content: String): String {
    if (content.isEmpty()) return content

    val result = StringBuilder(content.length)
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

        val line = content.substring(lineStart, lineEnd)
        if (isTooLong(line)) {
            result.append(reflowLongLine(line))
        } else {
            result.append(line)
        }

        if (lineEnd >= content.length) break
        result.append('\n')
        i = if (content[lineEnd] == '\r') lineEnd + 2 else lineEnd + 1
    }
    return result.toString()
}

private fun isTooLong(line: String): Boolean =
    line.length > MAX_PARAGRAPH_CHARS || countWords(line) > MAX_PARAGRAPH_WORDS

private fun reflowLongLine(line: String): String {
    val sentences = splitAtSentenceBoundaries(line)
    val segments = ArrayList<String>(sentences.size)
    for (sentence in sentences) {
        if (isTooLong(sentence)) {
            segments.addAll(hardBreakByWords(sentence))
        } else {
            segments.add(sentence)
        }
    }
    return segments.joinToString("\n")
}

/**
 * Breaks after '.', '!', '?', or ellipsis when followed by whitespace.
 * Punctuation stays on the ended sentence; following whitespace begins the next segment.
 */
private fun splitAtSentenceBoundaries(line: String): List<String> {
    val sentences = ArrayList<String>()
    var start = 0
    var i = 0
    while (i < line.length) {
        val ch = line[i]
        if (isSentenceEndPunctuation(ch) &&
            i + 1 < line.length &&
            line[i + 1].isWhitespace()
        ) {
            sentences.add(line.substring(start, i + 1))
            start = i + 1
        }
        i++
    }
    if (start < line.length) {
        sentences.add(line.substring(start))
    } else if (sentences.isEmpty()) {
        sentences.add(line)
    }
    return sentences
}

private fun isSentenceEndPunctuation(ch: Char): Boolean =
    ch == '.' || ch == '!' || ch == '?' || ch == '\u2026'

/**
 * Inserts a break after every [HARD_BREAK_EVERY_WORDS]-th word.
 * Whitespace after a break stays with the following segment.
 */
private fun hardBreakByWords(text: String): List<String> {
    val chunks = ArrayList<String>()
    var wordsInChunk = 0
    var chunkStart = 0
    var i = 0
    var inWord = false

    while (i < text.length) {
        if (text[i].isWhitespace()) {
            if (inWord) {
                inWord = false
                wordsInChunk++
                if (wordsInChunk == HARD_BREAK_EVERY_WORDS) {
                    chunks.add(text.substring(chunkStart, i))
                    chunkStart = i
                    wordsInChunk = 0
                }
            }
            i++
        } else {
            inWord = true
            i++
        }
    }

    if (chunkStart < text.length) {
        chunks.add(text.substring(chunkStart))
    } else if (chunks.isEmpty()) {
        chunks.add(text)
    }
    return chunks
}

private fun countWords(text: String): Int {
    var count = 0
    var inWord = false
    for (index in text.indices) {
        if (text[index].isWhitespace()) {
            inWord = false
        } else if (!inWord) {
            inWord = true
            count++
        }
    }
    return count
}
