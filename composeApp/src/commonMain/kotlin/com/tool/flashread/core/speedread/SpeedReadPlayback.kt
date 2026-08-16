package com.tool.flashread.core.speedread

/**
 * Token stream for a book at a given chunk size, plus helpers the player uses
 * to step, jump by sentence, loop, and resume from a Reader paragraph.
 */
class SpeedReadPlayback(
    content: String,
    chunkSize: Int = SpeedReadDefaults.DEFAULT_CHUNK_SIZE,
) {
    val tokens: List<SpeedReadToken> = tokenizeBook(content)
    val chunks: List<SpeedReadChunk> = chunkTokens(tokens, chunkSize)

    fun startChunkIndex(paragraphIndex: Int): Int {
        if (chunks.isEmpty()) return 0
        val index = chunks.indexOfFirst { it.paragraphIndex >= paragraphIndex }
        return if (index >= 0) index else chunks.lastIndex
    }

    fun nextChunkIndex(fromChunkIndex: Int, loop: Boolean): Int? {
        if (chunks.isEmpty()) return null
        val next = fromChunkIndex + 1
        return when {
            next < chunks.size -> next
            loop -> 0
            else -> null
        }
    }

    fun previousChunkIndex(fromChunkIndex: Int): Int {
        if (chunks.isEmpty()) return 0
        return (fromChunkIndex - 1).coerceAtLeast(0)
    }

    fun nextSentenceChunkIndex(fromChunkIndex: Int): Int {
        if (chunks.isEmpty()) return 0
        val fromToken = tokenIndexForChunk(fromChunkIndex)
        val nextToken = nextSentenceTokenIndex(fromToken)
        return chunkIndexForToken(nextToken)
    }

    fun previousSentenceChunkIndex(fromChunkIndex: Int): Int {
        if (chunks.isEmpty()) return 0
        val fromToken = tokenIndexForChunk(fromChunkIndex)
        val previousToken = previousSentenceTokenIndex(fromToken)
        return chunkIndexForToken(previousToken)
    }

    fun delayMs(chunkIndex: Int, wpm: Int): Long {
        val chunk = chunks.getOrNull(chunkIndex) ?: return SpeedReadTiming.delayMs(wpm, 1.0)
        return SpeedReadTiming.chunkDelayMs(chunk, wpm)
    }

    private fun tokenIndexForChunk(chunkIndex: Int): Int {
        val chunk = chunks.getOrNull(chunkIndex.coerceIn(0, chunks.lastIndex))
        return chunk?.startTokenIndex ?: 0
    }

    private fun chunkIndexForToken(tokenIndex: Int): Int {
        if (chunks.isEmpty()) return 0
        return chunks.indexOfLast { it.startTokenIndex <= tokenIndex }.coerceAtLeast(0)
    }

    private fun sentenceStartTokenIndex(fromTokenIndex: Int): Int {
        if (tokens.isEmpty()) return 0
        val safeFrom = fromTokenIndex.coerceIn(0, tokens.lastIndex)
        for (i in safeFrom - 1 downTo 0) {
            if (tokens[i].isSentenceEnd) return i + 1
        }
        return 0
    }

    private fun nextSentenceTokenIndex(fromTokenIndex: Int): Int {
        if (tokens.isEmpty()) return 0
        val safeFrom = fromTokenIndex.coerceIn(0, tokens.lastIndex)
        for (i in safeFrom until tokens.size) {
            if (tokens[i].isSentenceEnd) {
                val next = i + 1
                return if (next < tokens.size) next else safeFrom
            }
        }
        return safeFrom
    }

    private fun previousSentenceTokenIndex(fromTokenIndex: Int): Int {
        val currentStart = sentenceStartTokenIndex(fromTokenIndex)
        if (fromTokenIndex > currentStart) return currentStart
        if (currentStart == 0) return 0
        return sentenceStartTokenIndex(currentStart - 1)
    }
}
