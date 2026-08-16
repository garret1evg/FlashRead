package com.tool.flashread.core.speedread

private val Whitespace = Regex("\\s+")

/**
 * Splits book content the same way the Reader does, so SpeedRead paragraph
 * indices line up with [com.tool.flashread.core.model.ReadingPosition].
 */
fun splitBookParagraphs(content: String): List<String> {
    return content
        .replace("\r\n", "\n")
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

fun tokenizeBook(content: String): List<SpeedReadToken> {
    return splitBookParagraphs(content).flatMapIndexed { paragraphIndex, paragraph ->
        paragraph.split(Whitespace).filter { it.isNotEmpty() }.map { word ->
            SpeedReadToken(
                text = word,
                paragraphIndex = paragraphIndex,
                pauseMultiplier = SpeedReadTiming.pauseMultiplier(word),
                isSentenceEnd = SpeedReadTiming.isSentenceEnd(word),
            )
        }
    }
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
