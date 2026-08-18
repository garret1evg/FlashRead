package com.tool.flashread.ui.speedread

import com.tool.flashread.core.model.Book
import com.tool.flashread.core.speedread.SpeedReadPlayback
import com.tool.flashread.core.speedread.SpeedReadSettings
import com.tool.flashread.core.speedread.delayUnitsToMs
import com.tool.flashread.core.speedread.remainingMsToMinutes
import com.tool.flashread.memoryReadingSessionRepository
import com.tool.flashread.memorySpeedReadSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeedReadSetupViewModelTest {

    @Test
    fun remainingMinutesFollowsWpmAndStartRequiresContent() {
        val stored = arrayOf(SpeedReadSettings(wpm = 300))
        val viewModel = SpeedReadSetupViewModel(
            book = Book(
                id = "book-1",
                title = "Sample",
                content = List(600) { "word" }.joinToString(" "),
            ),
            settingsRepository = memorySpeedReadSettingsRepository(stored),
            readingSessionRepository = memoryReadingSessionRepository(),
        )

        assertEquals(2, viewModel.uiState.value.remainingMinutes)
        assertTrue(viewModel.uiState.value.canStart)

        viewModel.updateSettings(viewModel.uiState.value.settings.copy(wpm = 600))
        assertEquals(1, viewModel.uiState.value.remainingMinutes)
        assertEquals(600, stored[0].wpm)
    }

    @Test
    fun remainingMinutesMatchesPlayerDelayUnitsIncludingPunctuation() {
        val content = "Wait. Go now, please! " + List(300) { "word" }.joinToString(" ")
        val settings = SpeedReadSettings(wpm = 300, chunkSize = 2)
        val viewModel = SpeedReadSetupViewModel(
            book = Book(id = "book-1", title = "Sample", content = content),
            settingsRepository = memorySpeedReadSettingsRepository(arrayOf(settings)),
            readingSessionRepository = memoryReadingSessionRepository(mutableMapOf("book-1" to 0)),
        )

        assertEquals(
            expectedRemainingMinutes(content, paragraphIndex = 0, settings),
            viewModel.uiState.value.remainingMinutes,
        )
    }

    @Test
    fun remainingMinutesStartsAtSavedParagraphLikePlayer() {
        val content = List(200) { "Hello." }.joinToString(" ") +
            "\n\n" +
            List(50) { "word" }.joinToString(" ")
        val settings = SpeedReadSettings(wpm = 250, chunkSize = 1)
        val viewModel = SpeedReadSetupViewModel(
            book = Book(id = "book-1", title = "Sample", content = content),
            settingsRepository = memorySpeedReadSettingsRepository(arrayOf(settings)),
            readingSessionRepository = memoryReadingSessionRepository(mutableMapOf("book-1" to 1)),
        )

        assertEquals(
            expectedRemainingMinutes(content, paragraphIndex = 1, settings),
            viewModel.uiState.value.remainingMinutes,
        )
    }

    @Test
    fun remainingMinutesStartsAtWordOffsetWhenSet() {
        val content = List(600) { "word" }.joinToString(" ")
        val settings = SpeedReadSettings(wpm = 300, chunkSize = 1)
        val wordOffset = 300 * 5 // 300 words in, each word is 5 chars ("word ")
        val viewModel = SpeedReadSetupViewModel(
            book = Book(id = "book-1", title = "Sample", content = content),
            settingsRepository = memorySpeedReadSettingsRepository(arrayOf(settings)),
            readingSessionRepository = memoryReadingSessionRepository(
                positions = mutableMapOf("book-1" to 0),
                wordOffsets = mutableMapOf("book-1" to wordOffset),
            ),
        )

        assertEquals(
            expectedRemainingMinutesFromOffset(content, wordOffset, settings),
            viewModel.uiState.value.remainingMinutes,
        )
    }

    private fun expectedRemainingMinutesFromOffset(
        content: String,
        wordOffset: Int,
        settings: SpeedReadSettings,
    ): Int {
        val playback = SpeedReadPlayback(content, settings.chunkSize)
        val remainingMs = delayUnitsToMs(
            playback.remainingDelayUnits(playback.positionAtOffset(wordOffset)),
            settings.wpm,
        )
        return remainingMsToMinutes(remainingMs)
    }

    private fun expectedRemainingMinutes(
        content: String,
        paragraphIndex: Int,
        settings: SpeedReadSettings,
    ): Int {
        val playback = SpeedReadPlayback(content, settings.chunkSize)
        val remainingMs = delayUnitsToMs(
            playback.remainingDelayUnits(playback.startPosition(paragraphIndex)),
            settings.wpm,
        )
        return remainingMsToMinutes(remainingMs)
    }

    @Test
    fun blankContentCannotStart() {
        val viewModel = SpeedReadSetupViewModel(
            book = Book(id = "empty", title = "Empty", content = "   "),
            settingsRepository = memorySpeedReadSettingsRepository(),
            readingSessionRepository = memoryReadingSessionRepository(),
        )
        assertFalse(viewModel.uiState.value.canStart)
    }
}
