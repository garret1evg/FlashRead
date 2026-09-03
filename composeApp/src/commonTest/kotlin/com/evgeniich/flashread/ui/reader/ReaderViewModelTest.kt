package com.evgeniich.flashread.ui.reader

import com.evgeniich.flashread.analytics.AnalyticsEvent
import com.evgeniich.flashread.analytics.AnalyticsLogger
import com.evgeniich.flashread.analytics.ProgressBucket
import com.evgeniich.flashread.analytics.RecordingAnalytics
import com.evgeniich.flashread.core.model.Book
import com.evgeniich.flashread.core.model.ReadingPosition
import com.evgeniich.flashread.core.reading.ReaderTextDefaults
import com.evgeniich.flashread.core.reading.ReaderTextSettings
import com.evgeniich.flashread.core.reading.ReaderTheme
import com.evgeniich.flashread.core.reading.withReadingStats
import com.evgeniich.flashread.memoryReaderTextSettingsRepository
import com.evgeniich.flashread.memoryReadingSessionRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsParagraphsAndClampsInitialPosition() {
        val positions = mutableMapOf("book-1" to 80)
        val viewModel = readerViewModel(
            content = "First paragraph.\n\nSecond paragraph.",
            positions = positions,
        )

        assertEquals(listOf("First paragraph.", "Second paragraph."), viewModel.paragraphs)
        assertEquals(1, viewModel.initialParagraphIndex)
    }

    @Test
    fun saveParagraphIndexAndSettingsPersist() {
        val positions = mutableMapOf("book-1" to 0)
        val storedSettings = arrayOf(ReaderTextSettings())
        val viewModel = readerViewModel(
            content = "Hello world.",
            positions = positions,
            storedSettings = storedSettings,
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

    @Test
    fun scrollWithoutPinChangesHighlightedWord() {
        val positions = mutableMapOf<String, Int>()
        val wordOffsets = mutableMapOf<String, Int>()
        val viewModel = readerViewModel(
            content = "First paragraph.\n\nSecond paragraph.\n\nThird paragraph.",
            positions = positions,
            wordOffsets = wordOffsets,
        )

        val initial = viewModel.startWord.value
        assertNotNull(initial)
        assertEquals(0, initial.paragraphIndex)
        assertFalse(initial.pinned)

        viewModel.saveParagraphIndex(1)
        val afterScroll = viewModel.startWord.value
        assertNotNull(afterScroll)
        assertEquals(1, afterScroll.paragraphIndex)
        assertFalse(afterScroll.pinned)
        assertEquals(1, positions["book-1"])
        assertEquals(ReadingPosition.UNSET, wordOffsets["book-1"])
    }

    @Test
    fun selectWordFixesOffsetAndBlocksScrollUpdates() {
        val positions = mutableMapOf<String, Int>()
        val wordOffsets = mutableMapOf<String, Int>()
        val viewModel = readerViewModel(
            content = "First paragraph.\n\nSecond paragraph.\n\nThird paragraph.",
            positions = positions,
            wordOffsets = wordOffsets,
        )

        viewModel.selectWord(1, 0)
        val selected = viewModel.startWord.value
        assertNotNull(selected)
        assertEquals(1, selected.paragraphIndex)
        assertTrue(selected.pinned)
        assertTrue(wordOffsets["book-1"]!! >= 0)

        val offsetBefore = selected.contentOffset
        viewModel.saveParagraphIndex(0)
        val afterScroll = viewModel.startWord.value
        assertNotNull(afterScroll)
        assertEquals(1, afterScroll.paragraphIndex)
        assertEquals(offsetBefore, afterScroll.contentOffset)
        assertTrue(afterScroll.pinned)
    }

    @Test
    fun restoresSavedWordOffsetAsPinned() {
        val positions = mutableMapOf("book-1" to 1)
        val wordOffsets = mutableMapOf("book-1" to 18)
        val viewModel = readerViewModel(
            content = "First paragraph.\n\nSecond paragraph.",
            positions = positions,
            wordOffsets = wordOffsets,
        )

        val restored = viewModel.startWord.value
        assertNotNull(restored)
        assertEquals(1, restored.paragraphIndex)
        assertTrue(restored.pinned)
        assertEquals(18, restored.contentOffset)
    }

    @Test
    fun refreshPositionUpdatesStartWordAfterExternalChange() {
        val positions = mutableMapOf("book-1" to 0)
        val wordOffsets = mutableMapOf<String, Int>()
        val viewModel = readerViewModel(
            content = "First paragraph.\n\nSecond paragraph.\n\nThird paragraph.",
            positions = positions,
            wordOffsets = wordOffsets,
        )

        val initial = viewModel.startWord.value
        assertNotNull(initial)
        assertEquals(0, initial.paragraphIndex)
        assertFalse(initial.pinned)

        // Simulate player saving a different position
        positions["book-1"] = 2
        wordOffsets["book-1"] = 37

        viewModel.refreshPosition()

        val updated = viewModel.startWord.value
        assertNotNull(updated)
        assertEquals(2, updated.paragraphIndex)
        assertTrue(updated.pinned)
        assertEquals(37, updated.contentOffset)
        assertEquals(2, viewModel.scrollToParagraph.value)
    }

    @Test
    fun refreshPositionDoesNothingIfPositionUnchanged() {
        val positions = mutableMapOf("book-1" to 0)
        val wordOffsets = mutableMapOf<String, Int>()
        val viewModel = readerViewModel(
            content = "First paragraph.\n\nSecond paragraph.",
            positions = positions,
            wordOffsets = wordOffsets,
        )

        val initial = viewModel.startWord.value
        viewModel.refreshPosition()
        val afterRefresh = viewModel.startWord.value

        assertEquals(initial, afterRefresh)
        assertEquals(null, viewModel.scrollToParagraph.value)
    }

    @Test
    fun saveParagraphIndexLogsNewlyCrossedProgressBuckets() {
        val analytics = RecordingAnalytics()
        val viewModel = readerViewModel(
            content = fourParagraphs(),
            analytics = analytics,
        )

        viewModel.saveParagraphIndex(1)
        assertEquals(
            listOf<AnalyticsEvent>(AnalyticsEvent.ReaderProgress(ProgressBucket.P25)),
            analytics.events,
        )

        viewModel.saveParagraphIndex(3)
        assertEquals(
            listOf<AnalyticsEvent>(
                AnalyticsEvent.ReaderProgress(ProgressBucket.P25),
                AnalyticsEvent.ReaderProgress(ProgressBucket.P50),
                AnalyticsEvent.ReaderProgress(ProgressBucket.P75),
            ),
            analytics.events,
        )

        viewModel.saveParagraphIndex(4)
        assertEquals(
            listOf<AnalyticsEvent>(
                AnalyticsEvent.ReaderProgress(ProgressBucket.P25),
                AnalyticsEvent.ReaderProgress(ProgressBucket.P50),
                AnalyticsEvent.ReaderProgress(ProgressBucket.P75),
                AnalyticsEvent.ReaderProgress(ProgressBucket.P100),
            ),
            analytics.events,
        )
    }

    @Test
    fun resumeDoesNotLogAlreadyReachedProgressBuckets() {
        val analytics = RecordingAnalytics()
        val viewModel = readerViewModel(
            content = fourParagraphs(),
            positions = mutableMapOf("book-1" to 2),
            analytics = analytics,
        )

        viewModel.saveParagraphIndex(2)
        assertTrue(analytics.events.isEmpty())

        viewModel.saveParagraphIndex(1)
        assertTrue(analytics.events.isEmpty())

        viewModel.saveParagraphIndex(4)
        assertEquals(
            listOf<AnalyticsEvent>(
                AnalyticsEvent.ReaderProgress(ProgressBucket.P75),
                AnalyticsEvent.ReaderProgress(ProgressBucket.P100),
            ),
            analytics.events,
        )
    }

    @Test
    fun refreshPositionUpdatesWhenPlayerAdvancesFromSameParagraph() {
        // Scenario: User scrolls to paragraph 1, goes to speed read, reads a few words, comes back
        val content = "First paragraph.\n\nSecond word here.\n\nThird paragraph."
        val positions = mutableMapOf("book-1" to 1)
        val wordOffsets = mutableMapOf<String, Int>()
        val viewModel = readerViewModel(
            content = content,
            positions = positions,
            wordOffsets = wordOffsets,
        )

        val initial = viewModel.startWord.value
        assertNotNull(initial)
        assertEquals(1, initial.paragraphIndex)
        assertEquals("Second", content.substring(initial.contentOffset, initial.contentOffset + 6))
        assertFalse(initial.pinned)

        // Player reads to "here" word (offset 25 in "Second word here.")
        // Content: "First paragraph.\n\n" = 18 chars, then "Second word " = 12 chars, "here" starts at 30
        wordOffsets["book-1"] = 30

        viewModel.refreshPosition()

        val updated = viewModel.startWord.value
        assertNotNull(updated)
        assertEquals(1, updated.paragraphIndex)
        assertEquals(30, updated.contentOffset)
        assertTrue(updated.pinned)
        // Should not scroll since same paragraph
        assertEquals(null, viewModel.scrollToParagraph.value)
    }

    private fun fourParagraphs(): String =
        (1..4).joinToString("\n\n") { "Paragraph $it." }

    private fun readerViewModel(
        content: String,
        positions: MutableMap<String, Int> = mutableMapOf(),
        wordOffsets: MutableMap<String, Int> = mutableMapOf(),
        storedSettings: Array<ReaderTextSettings> = arrayOf(ReaderTextSettings()),
        analytics: AnalyticsLogger = RecordingAnalytics(),
    ): ReaderViewModel {
        return ReaderViewModel(
            book = Book(id = "book-1", title = "Sample", content = content).withReadingStats(),
            readingSessionRepository = memoryReadingSessionRepository(positions, wordOffsets),
            textSettingsRepository = memoryReaderTextSettingsRepository(storedSettings),
            computationDispatcher = dispatcher,
            analytics = analytics,
        )
    }
}
