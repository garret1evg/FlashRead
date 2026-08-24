package com.evgeniich.flashread.core.youtube

import com.evgeniich.flashread.core.speedread.splitBookParagraphs

/**
 * Turns caption snippets into reader paragraphs: collapse caption line breaks,
 * join with spaces, then split on sentence-ending punctuation so
 * [splitBookParagraphs] sees real paragraphs.
 */
internal object YouTubeTranscriptText {
    fun join(snippets: List<String>): String {
        val body = snippets
            .map(::normalizeSnippet)
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        if (body.isEmpty()) return ""
        return splitSentences(body).joinToString("\n")
    }

    fun paragraphCount(text: String): Int = splitBookParagraphs(text).size
}

private val SENTENCE_END = setOf('.', '!', '?', '।', '؟', '！', '？')
private val CLOSING_QUOTE = setOf('"', '\'', '”', '’', '»', '）', ')')

private fun normalizeSnippet(text: String): String {
    val out = StringBuilder(text.length)
    var previousSpace = false
    for (char in text) {
        val space = char == ' ' || char == '\n' || char == '\r' || char == '\t' ||
            char == '\u000c' || char == '\u00A0'
        if (space) {
            if (!previousSpace) {
                out.append(' ')
                previousSpace = true
            }
        } else {
            out.append(char)
            previousSpace = false
        }
    }
    return out.toString().trim()
}

private fun splitSentences(text: String): List<String> {
    val sentences = ArrayList<String>()
    val current = StringBuilder()
    var index = 0
    while (index < text.length) {
        val char = text[index]
        current.append(char)
        if (char in SENTENCE_END) {
            var lookAhead = index + 1
            while (lookAhead < text.length && text[lookAhead] in CLOSING_QUOTE) {
                current.append(text[lookAhead])
                lookAhead++
            }
            if (lookAhead >= text.length || text[lookAhead].isWhitespace()) {
                val sentence = current.toString().trim()
                if (sentence.isNotEmpty()) sentences.add(sentence)
                current.setLength(0)
                index = lookAhead
                while (index < text.length && text[index].isWhitespace()) index++
                continue
            }
        }
        index++
    }
    val tail = current.toString().trim()
    if (tail.isNotEmpty()) sentences.add(tail)
    return sentences
}
