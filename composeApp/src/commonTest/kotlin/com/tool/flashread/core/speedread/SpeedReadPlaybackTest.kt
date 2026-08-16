package com.tool.flashread.core.speedread

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpeedReadPlaybackTest {

    private val twoParagraphs = "The cat sat. The dog ran.\n\nYes it did!"

    @Test
    fun startsAtFirstChunkOfRequestedParagraph() {
        val playback = SpeedReadPlayback(twoParagraphs, chunkSize = 1)
        val first = playback.chunkAt(playback.startPosition(0))
        val second = playback.chunkAt(playback.startPosition(1))
        assertEquals("The", first?.displayText)
        assertEquals("Yes", second?.displayText)
        assertEquals(1, second?.tokens?.first()?.paragraphIndex)
    }

    @Test
    fun startPastLastParagraphClampsToEnd() {
        val playback = SpeedReadPlayback(twoParagraphs, chunkSize = 1)
        val position = playback.startPosition(99)
        assertTrue(playback.isLastChunk(position))
        assertEquals("did!", playback.chunkAt(position)?.displayText)
    }

    @Test
    fun nextChunkAdvancesAndLoopsAtEnd() {
        val playback = SpeedReadPlayback("one two three", chunkSize = 1)
        val first = playback.startPosition(0)
        val second = playback.next(first, loop = false)!!
        val third = playback.next(second, loop = false)!!
        assertEquals("two", playback.chunkAt(second)?.displayText)
        assertEquals("three", playback.chunkAt(third)?.displayText)
        assertNull(playback.next(third, loop = false))
        assertEquals("one", playback.chunkAt(playback.next(third, loop = true)!!)?.displayText)
    }

    @Test
    fun previousChunkStopsAtStart() {
        val playback = SpeedReadPlayback("one two", chunkSize = 1)
        val first = playback.startPosition(0)
        val second = playback.next(first, loop = false)!!
        assertEquals("one", playback.chunkAt(playback.previous(first))?.displayText)
        assertEquals("one", playback.chunkAt(playback.previous(second))?.displayText)
    }

    @Test
    fun sentenceJumpsLandOnSentenceBoundaries() {
        val playback = SpeedReadPlayback("The cat sat. The dog ran. Yes!", chunkSize = 1)
        val tokens = tokenizeBook("The cat sat. The dog ran. Yes!")
        assertEquals(
            listOf("The", "cat", "sat.", "The", "dog", "ran.", "Yes!"),
            tokens.map { it.text },
        )

        val first = playback.startPosition(0)
        val secondSentence = playback.nextSentence(first)
        assertEquals("The", playback.chunkAt(secondSentence)?.displayText)
        assertEquals(3, secondSentence.tokenIndex)

        val thirdSentence = playback.nextSentence(secondSentence)
        assertEquals("Yes!", playback.chunkAt(thirdSentence)?.displayText)

        val backToSecond = playback.previousSentence(thirdSentence)
        assertEquals(secondSentence, backToSecond)

        val midSecond = playback.next(secondSentence, loop = false)!!
        assertEquals("dog", playback.chunkAt(midSecond)?.displayText)
        assertEquals(
            secondSentence,
            playback.previousSentence(midSecond),
        )
    }

    @Test
    fun sentenceJumpUsesChunkThatContainsTheWord() {
        val playback = SpeedReadPlayback("The cat sat. The dog ran.", chunkSize = 2)
        assertEquals(
            listOf("The cat", "sat. The", "dog ran."),
            playback.chunkTexts(),
        )
        val next = playback.nextSentence(playback.startPosition(0))
        assertEquals("sat. The", playback.chunkAt(next)?.displayText)
    }

    @Test
    fun emptyContentIsSafeToNavigate() {
        val playback = SpeedReadPlayback("   \n\n")
        val position = playback.startPosition(0)
        assertTrue(playback.isEmpty)
        assertNull(playback.chunkAt(position))
        assertNull(playback.next(position, loop = false))
        assertEquals(position, playback.previous(position))
        assertEquals(position, playback.nextSentence(position))
        assertEquals(position, playback.previousSentence(position))
    }

    @Test
    fun walkingChunksMatchesEagerTokenization() {
        val content = "The cat sat. The dog ran.\n\nYes it did!"
        val playback = SpeedReadPlayback(content, chunkSize = 2)
        val expected = chunkTokens(tokenizeBook(content), chunkSize = 2)
        val walked = playback.chunks()
        assertEquals(expected.map { it.displayText }, walked.map { it.displayText })
        assertEquals(expected.map { it.startTokenIndex }, walked.map { it.startTokenIndex })
        assertEquals(expected.map { it.paragraphIndex }, walked.map { it.paragraphIndex })
    }

    @Test
    fun startPositionAlignsToChunkThatBeginsInRequestedParagraph() {
        val content = (0..400).joinToString("\n\n") { "Word$it next extra." }
        val playback = SpeedReadPlayback(content, chunkSize = 2)
        val aligned = playback.startPosition(1)
        assertEquals(1, playback.chunkAt(aligned)?.paragraphIndex)
        assertEquals("next extra.", playback.chunkAt(aligned)?.displayText)

        val later = playback.startPosition(250)
        assertEquals(250, playback.chunkAt(later)?.paragraphIndex)
        assertEquals("Word250 next", playback.chunkAt(later)?.displayText)
    }
}

private fun SpeedReadPlayback.chunkTexts(): List<String> = chunks().map { it.displayText }

private fun SpeedReadPlayback.chunks(): List<SpeedReadChunk> {
    if (isEmpty) return emptyList()
    val chunks = ArrayList<SpeedReadChunk>()
    var position = startPosition(0)
    while (true) {
        chunks.add(chunkAt(position) ?: break)
        position = next(position, loop = false) ?: break
    }
    return chunks
}
