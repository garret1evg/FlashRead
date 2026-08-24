package com.evgeniich.flashread.core.speedread

data class SpeedReadPosition(
    val tokenIndex: Int,
    val offset: Int,
    val paragraphIndex: Int,
) {
    companion object {
        val Empty = SpeedReadPosition(tokenIndex = 0, offset = 0, paragraphIndex = 0)
    }
}

/**
 * Token stream for a book at a given chunk size, plus helpers the player uses
 * to step, jump by sentence, loop, and resume from a Reader paragraph.
 *
 * Large books are walked in place: only the current chunk is materialized.
 */
class SpeedReadPlayback(
    content: String,
    chunkSize: Int = SpeedReadDefaults.DEFAULT_CHUNK_SIZE,
) {
    private val source = SpeedReadSource(content)
    private val size = chunkSize.coerceIn(
        SpeedReadDefaults.MIN_CHUNK_SIZE,
        SpeedReadDefaults.MAX_CHUNK_SIZE,
    )

    val isEmpty: Boolean = source.first() == null

    fun startPosition(paragraphIndex: Int): SpeedReadPosition {
        if (isEmpty) return SpeedReadPosition.Empty
        var tokenIndex = 0
        var lastChunk = SpeedReadPosition.Empty
        var found: SpeedReadPosition? = null
        source.forEachToken { start, _, paragraph ->
            if (tokenIndex % size == 0) {
                lastChunk = SpeedReadPosition(tokenIndex, start, paragraph)
                if (paragraph >= paragraphIndex) {
                    found = lastChunk
                    return@forEachToken false
                }
            }
            tokenIndex++
            true
        }
        return found ?: lastChunk
    }

    /**
     * Returns a position starting at the word containing [contentOffset].
     * If the offset falls in whitespace, returns the next word.
     * Does NOT align back to chunk boundary: the first flash starts exactly
     * at the selected word.
     */
    fun positionAtOffset(contentOffset: Int): SpeedReadPosition {
        if (isEmpty) return SpeedReadPosition.Empty
        var tokenIndex = 0
        var lastPosition = SpeedReadPosition.Empty
        source.forEachToken { start, end, paragraph ->
            lastPosition = SpeedReadPosition(tokenIndex, start, paragraph)
            if (contentOffset in start until end) {
                return@forEachToken false
            }
            if (start > contentOffset) {
                return@forEachToken false
            }
            tokenIndex++
            true
        }
        return lastPosition
    }

    fun chunkAt(position: SpeedReadPosition): SpeedReadChunk? {
        var current = source.tokenAt(position.offset, position.paragraphIndex) ?: return null
        val tokens = ArrayList<SpeedReadToken>(size)
        tokens.add(source.token(current))
        repeat(size - 1) {
            current = source.next(current) ?: return@repeat
            tokens.add(source.token(current))
        }
        return SpeedReadChunk(tokens = tokens, startTokenIndex = position.tokenIndex)
    }

    fun next(position: SpeedReadPosition, loop: Boolean): SpeedReadPosition? {
        if (isEmpty) return null
        val next = nextChunkStart(position)
        return when {
            next != null -> next
            loop -> startPosition(0)
            else -> null
        }
    }

    fun previous(position: SpeedReadPosition): SpeedReadPosition {
        if (isEmpty || position.tokenIndex == 0) {
            return startPosition(0)
        }
        val newIndex = (position.tokenIndex - size).coerceAtLeast(0)
        var current = source.tokenAt(position.offset, position.paragraphIndex) ?: return startPosition(0)
        repeat(position.tokenIndex - newIndex) {
            current = source.previous(current) ?: return startPosition(0)
        }
        return SpeedReadPosition(newIndex, current.start, current.paragraphIndex)
    }

    fun nextSentence(position: SpeedReadPosition): SpeedReadPosition {
        if (isEmpty) return SpeedReadPosition.Empty
        var current = source.tokenAt(position.offset, position.paragraphIndex) ?: return position
        var tokenIndex = position.tokenIndex
        while (true) {
            if (source.token(current).isSentenceEnd) {
                val next = source.next(current) ?: return position
                return alignToChunk(tokenIndex + 1, next)
            }
            current = source.next(current) ?: return position
            tokenIndex++
        }
    }

    fun previousSentence(position: SpeedReadPosition): SpeedReadPosition {
        if (isEmpty) return SpeedReadPosition.Empty
        val currentStart = sentenceStart(position)
        if (position.tokenIndex > currentStart.tokenIndex) {
            return alignToChunk(currentStart)
        }
        if (currentStart.tokenIndex == 0) {
            return alignToChunk(currentStart)
        }
        val startRef = source.tokenAt(currentStart.offset, currentStart.paragraphIndex)
            ?: return currentStart
        val previousRef = source.previous(startRef) ?: return currentStart
        val previousToken = SpeedReadPosition(
            tokenIndex = currentStart.tokenIndex - 1,
            offset = previousRef.start,
            paragraphIndex = previousRef.paragraphIndex,
        )
        return alignToChunk(sentenceStart(previousToken))
    }

    fun delayMs(position: SpeedReadPosition, wpm: Int): Long {
        val chunk = chunkAt(position) ?: return SpeedReadTiming.delayMs(wpm, 1.0)
        return SpeedReadTiming.chunkDelayMs(chunk, wpm)
    }

    fun isLastChunk(position: SpeedReadPosition): Boolean {
        return nextChunkStart(position) == null
    }

    fun sessionTotals(): SpeedReadSessionTotals {
        if (isEmpty) return SpeedReadSessionTotals.Empty
        var tokenCount = 0
        var delayUnits = 0.0
        source.forEachToken { start, end, _ ->
            delayUnits += SpeedReadTiming.pauseMultiplier(source.text.substring(start, end))
            tokenCount++
            true
        }
        return SpeedReadSessionTotals(tokenCount = tokenCount, delayUnits = delayUnits)
    }

    fun delayUnitsAt(position: SpeedReadPosition): Double {
        val chunk = chunkAt(position) ?: return 0.0
        return chunk.tokens.sumOf { it.pauseMultiplier }
    }

    fun elapsedDelayUnits(position: SpeedReadPosition): Double {
        if (isEmpty || position.tokenIndex <= 0) return 0.0
        var units = 0.0
        var current = startPosition(0)
        while (current.tokenIndex < position.tokenIndex) {
            units += delayUnitsAt(current)
            current = next(current, loop = false) ?: break
        }
        return units
    }

    fun remainingDelayUnits(position: SpeedReadPosition): Double {
        return (sessionTotals().delayUnits - elapsedDelayUnits(position)).coerceAtLeast(0.0)
    }

    private fun nextChunkStart(position: SpeedReadPosition): SpeedReadPosition? {
        var current = source.tokenAt(position.offset, position.paragraphIndex) ?: return null
        repeat(size) {
            current = source.next(current) ?: return null
        }
        return SpeedReadPosition(
            tokenIndex = position.tokenIndex + size,
            offset = current.start,
            paragraphIndex = current.paragraphIndex,
        )
    }

    private fun sentenceStart(position: SpeedReadPosition): SpeedReadPosition {
        var current = source.tokenAt(position.offset, position.paragraphIndex) ?: return position
        var tokenIndex = position.tokenIndex
        while (tokenIndex > 0) {
            val previous = source.previous(current) ?: break
            if (source.token(previous).isSentenceEnd) {
                return SpeedReadPosition(tokenIndex, current.start, current.paragraphIndex)
            }
            current = previous
            tokenIndex--
        }
        val first = source.first() ?: return SpeedReadPosition.Empty
        return SpeedReadPosition(0, first.start, first.paragraphIndex)
    }

    private fun alignToChunk(position: SpeedReadPosition): SpeedReadPosition {
        val ref = source.tokenAt(position.offset, position.paragraphIndex) ?: return position
        return alignToChunk(position.tokenIndex, ref)
    }

    private fun alignToChunk(tokenIndex: Int, ref: SpeedReadTokenRef): SpeedReadPosition {
        val chunkStart = (tokenIndex / size) * size
        var current = ref
        var index = tokenIndex
        while (index > chunkStart) {
            current = source.previous(current) ?: break
            index--
        }
        return SpeedReadPosition(chunkStart, current.start, current.paragraphIndex)
    }
}
