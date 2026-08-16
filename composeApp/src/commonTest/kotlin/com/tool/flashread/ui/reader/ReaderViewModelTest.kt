package com.tool.flashread.ui.reader

import com.tool.flashread.core.model.Book
import com.tool.flashread.core.reading.ReaderTextDefaults
import com.tool.flashread.core.reading.ReaderTextSettings
import com.tool.flashread.core.reading.ReaderTheme
import com.tool.flashread.memoryReaderTextSettingsRepository
import com.tool.flashread.memoryReadingSessionRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderViewModelTest {

    @Test
    fun loadsParagraphsAndClampsInitialPosition() {
        val positions = mutableMapOf("book-1" to 80)
        val viewModel = ReaderViewModel(
            book = Book(
                id = "book-1",
                title = "Sample",
                content = "First paragraph.\n\nSecond paragraph.",
            ),
            readingSessionRepository = memoryReadingSessionRepository(positions),
            textSettingsRepository = memoryReaderTextSettingsRepository(),
        )

        assertEquals(listOf("First paragraph.", "Second paragraph."), viewModel.paragraphs)
        assertEquals(1, viewModel.initialParagraphIndex)
    }

    @Test
    fun saveParagraphIndexAndSettingsPersist() {
        val positions = mutableMapOf("book-1" to 0)
        val storedSettings = arrayOf(ReaderTextSettings())
        val viewModel = ReaderViewModel(
            book = Book(id = "book-1", title = "Sample", content = "Hello world."),
            readingSessionRepository = memoryReadingSessionRepository(positions),
            textSettingsRepository = memoryReaderTextSettingsRepository(storedSettings),
        )

        viewModel.saveParagraphIndex(4)
        assertEquals(4, positions["book-1"])

        viewModel.updateSettings(
            ReaderTextSettings(fontSizeSp = 99, theme = ReaderTheme.Dark),
        )
        assertEquals(ReaderTextDefaults.MAX_FONT_SIZE_SP, viewModel.settings.value.fontSizeSp)
        assertEquals(ReaderTheme.Dark, storedSettings[0].theme)
        assertEquals(ReaderTextDefaults.MAX_FONT_SIZE_SP, storedSettings[0].fontSizeSp)
    }
}
