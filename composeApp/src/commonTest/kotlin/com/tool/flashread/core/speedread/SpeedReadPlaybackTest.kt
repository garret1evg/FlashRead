package com.tool.flashread.core.speedread

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpeedReadPlaybackTest {

    private val twoParagraphs = "The cat sat. The dog ran.\n\nYes it did!"

    @Test
    fun startsAtFirstChunkOfRequestedParagraph() {
        val playback = SpeedReadPlayback(twoParagraphs, chunkSize = 1)
        assertEquals(0, playback.startChunkIndex(paragraphIndex = 0))
        assertEquals(
            "Yes",
            playback.chunks[playback.startChunkIndex(paragraphIndex = 1)].displayText,
        )
    }

    @Test
    fun startPastLastParagraphClampsToEnd() {
        val playback = SpeedReadPlayback(twoParagraphs, chunkSize = 1)
        val index = playback.startChunkIndex(paragraphIndex = 99)
        assertEquals(playback.chunks.lastIndex, index)
    }

    @Test
    fun nextChunkAdvancesAndLoopsAtEnd() {
        val playback = SpeedReadPlayback("one two three", chunkSize = 1)
        assertEquals(1, playback.nextChunkIndex(fromChunkIndex = 0, loop = false))
        assertEquals(2, playback.nextChunkIndex(fromChunkIndex = 1, loop = false))
        assertNull(playback.nextChunkIndex(fromChunkIndex = 2, loop = false))
        assertEquals(0, playback.nextChunkIndex(fromChunkIndex = 2, loop = true))
    }

    @Test
    fun previousChunkStopsAtStart() {
        val playback = SpeedReadPlayback("one two", chunkSize = 1)
        assertEquals(0, playback.previousChunkIndex(fromChunkIndex = 0))
        assertEquals(0, playback.previousChunkIndex(fromChunkIndex = 1))
    }

    @Test
    fun sentenceJumpsLandOnSentenceBoundaries() {
        val playback = SpeedReadPlayback("The cat sat. The dog ran. Yes!", chunkSize = 1)
        assertEquals(
            listOf("The", "cat", "sat.", "The", "dog", "ran.", "Yes!"),
            playback.tokens.map { it.text },
        )

        val secondSentence = playback.nextSentenceChunkIndex(fromChunkIndex = 0)
        assertEquals("The", playback.chunks[secondSentence].displayText)
        assertEquals(3, secondSentence)

        val thirdSentence = playback.nextSentenceChunkIndex(fromChunkIndex = secondSentence)
        assertEquals("Yes!", playback.chunks[thirdSentence].displayText)

        val backToSecond = playback.previousSentenceChunkIndex(fromChunkIndex = thirdSentence)
        assertEquals(secondSentence, backToSecond)

        val midSecond = secondSentence + 1
        assertEquals("dog", playback.chunks[midSecond].displayText)
        assertEquals(
            secondSentence,
            playback.previousSentenceChunkIndex(fromChunkIndex = midSecond),
        )
    }

    @Test
    fun sentenceJumpUsesChunkThatContainsTheWord() {
        val playback = SpeedReadPlayback("The cat sat. The dog ran.", chunkSize = 2)
        assertEquals(
            listOf("The cat", "sat. The", "dog ran."),
            playback.chunks.map { it.displayText },
        )
        val next = playback.nextSentenceChunkIndex(fromChunkIndex = 0)
        assertEquals("sat. The", playback.chunks[next].displayText)
    }

    @Test
    fun emptyContentIsSafeToNavigate() {
        val playback = SpeedReadPlayback("   \n\n")
        assertEquals(0, playback.startChunkIndex(0))
        assertNull(playback.nextChunkIndex(0, loop = false))
        assertEquals(0, playback.previousChunkIndex(0))
        assertEquals(0, playback.nextSentenceChunkIndex(0))
        assertEquals(0, playback.previousSentenceChunkIndex(0))
    }
}
