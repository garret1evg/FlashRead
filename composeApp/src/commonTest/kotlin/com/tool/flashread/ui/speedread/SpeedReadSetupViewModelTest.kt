package com.tool.flashread.ui.speedread

import com.tool.flashread.core.model.Book
import com.tool.flashread.core.speedread.SpeedReadSettings
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
    fun blankContentCannotStart() {
        val viewModel = SpeedReadSetupViewModel(
            book = Book(id = "empty", title = "Empty", content = "   "),
            settingsRepository = memorySpeedReadSettingsRepository(),
            readingSessionRepository = memoryReadingSessionRepository(),
        )
        assertFalse(viewModel.uiState.value.canStart)
    }
}
