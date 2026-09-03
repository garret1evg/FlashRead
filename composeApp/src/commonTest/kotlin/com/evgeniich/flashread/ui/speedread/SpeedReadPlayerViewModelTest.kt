package com.evgeniich.flashread.ui.speedread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import com.evgeniich.flashread.ScratchSpeedReadBookId
import com.evgeniich.flashread.analytics.AnalyticsEvent
import com.evgeniich.flashread.analytics.AnalyticsLogger
import com.evgeniich.flashread.analytics.DurationBucket
import com.evgeniich.flashread.analytics.RecordingAnalytics
import com.evgeniich.flashread.analytics.WpmBucket
import com.evgeniich.flashread.core.model.Book
import com.evgeniich.flashread.core.speedread.SpeedReadPlayerStatus
import com.evgeniich.flashread.core.speedread.SpeedReadSettings
import com.evgeniich.flashread.memoryReadingSessionRepository
import com.evgeniich.flashread.memorySpeedReadSettingsRepository
import kotlin.reflect.KClass
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class SpeedReadPlayerViewModelTest {

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
    fun preparesPausedSessionAndAdvancesOnStep() {
        val positions = mutableMapOf("book-1" to 0)
        val viewModel = playerViewModel(
            content = "one two three",
            positions = positions,
        )
        val state = viewModel.viewState.value
        assertEquals("one", state.text)
        assertEquals(SpeedReadPlayerStatus.Paused, state.status)

        viewModel.stepForward()
        assertEquals("two", viewModel.viewState.value.text)

        viewModel.togglePlayPause()
        assertEquals(SpeedReadPlayerStatus.Playing, viewModel.viewState.value.status)

        viewModel.onHostStop()
        assertEquals(SpeedReadPlayerStatus.Paused, viewModel.viewState.value.status)
        assertEquals(0, positions["book-1"])
    }

    @Test
    fun changingChunkSizeRebuildsTheSession() {
        val viewModel = playerViewModel(content = "one two three four")
        assertEquals("one", viewModel.viewState.value.text)

        viewModel.updateSettings(SpeedReadSettings(wpm = 300, chunkSize = 3))
        val rebuilt = viewModel.viewState.value
        assertEquals("one two three", rebuilt.text)
        assertEquals(3, rebuilt.settings.chunkSize)
        assertEquals(SpeedReadPlayerStatus.Paused, rebuilt.status)
    }

    @Test
    fun emptyContentFinishesImmediately() {
        val viewModel = playerViewModel(content = "   ")
        val state = viewModel.viewState.value
        assertTrue(state.isEmpty)
        assertEquals(SpeedReadPlayerStatus.Finished, state.status)
    }

    @Test
    fun remainingClockUsesSessionTotals() {
        val viewModel = playerViewModel(content = "one two three")
        assertTrue(viewModel.viewState.value.remainingMs > 0L)
        assertTrue(viewModel.viewState.value.progress >= 0f)
    }

    @Test
    fun startsFromWordOffsetWhenSet() {
        val content = "first second third fourth"
        val wordOffsets = mutableMapOf("book-1" to 6) // offset of "second"
        val viewModel = playerViewModel(
            content = content,
            wordOffsets = wordOffsets,
        )
        assertEquals("second", viewModel.viewState.value.text)
    }

    @Test
    fun persistsWordOffsetOnPositionChange() {
        val content = "one two three"
        val wordOffsets = mutableMapOf<String, Int>()
        val viewModel = playerViewModel(
            content = content,
            wordOffsets = wordOffsets,
        )
        assertEquals("one", viewModel.viewState.value.text)

        viewModel.stepForward()
        assertEquals("two", viewModel.viewState.value.text)
        viewModel.persistNow()
        assertEquals(4, wordOffsets["book-1"])
    }

    @Test
    fun firstPlayLogsSpeedReadStartOnce() {
        val analytics = RecordingAnalytics()
        val viewModel = playerViewModel(
            content = "one two three",
            settings = SpeedReadSettings(wpm = 300, chunkSize = 2, spritzEnabled = false),
            analytics = analytics,
        )
        assertTrue(analytics.events.isEmpty())

        viewModel.togglePlayPause()
        assertEquals(
            listOf<AnalyticsEvent>(
                AnalyticsEvent.SpeedReadStart(
                    wpmBucket = WpmBucket.From251To400,
                    spritzEnabled = false,
                    source = AnalyticsEvent.SpeedReadStart.Source.Book,
                    chunkSize = 2,
                ),
            ),
            analytics.events,
        )

        viewModel.togglePlayPause()
        viewModel.togglePlayPause()
        assertEquals(1, analytics.ofType<AnalyticsEvent.SpeedReadStart>().size)
        assertTrue(analytics.ofType<AnalyticsEvent.SpeedReadComplete>().isEmpty())
    }

    @Test
    fun pasteSessionLogsPasteSourceOnFirstPlay() {
        val analytics = RecordingAnalytics()
        val viewModel = playerViewModel(
            content = "paste this text",
            bookId = ScratchSpeedReadBookId,
            analytics = analytics,
        )

        viewModel.togglePlayPause()

        val start = analytics.ofType<AnalyticsEvent.SpeedReadStart>().single()
        assertEquals(AnalyticsEvent.SpeedReadStart.Source.Paste, start.source)
        assertEquals("paste", start.params["source"])
    }

    @Test
    fun finishedPlaybackLogsCompleteFinishedOnce() {
        val analytics = RecordingAnalytics()
        val viewModel = playerViewModel(
            content = "one two",
            analytics = analytics,
        )

        viewModel.togglePlayPause()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SpeedReadPlayerStatus.Finished, viewModel.viewState.value.status)
        assertEquals(1, analytics.ofType<AnalyticsEvent.SpeedReadStart>().size)
        assertEquals(
            listOf(
                AnalyticsEvent.SpeedReadComplete(
                    durationBucket = DurationBucket.UpTo30s,
                    result = AnalyticsEvent.SpeedReadComplete.Result.Finished,
                ),
            ),
            analytics.ofType<AnalyticsEvent.SpeedReadComplete>(),
        )

        clearViewModel(viewModel)
        assertEquals(1, analytics.ofType<AnalyticsEvent.SpeedReadComplete>().size)
        assertEquals(
            AnalyticsEvent.SpeedReadComplete.Result.Finished,
            analytics.ofType<AnalyticsEvent.SpeedReadComplete>().single().result,
        )
    }

    @Test
    fun leavingAfterStartLogsCompleteClosed() {
        val analytics = RecordingAnalytics()
        val viewModel = playerViewModel(
            content = "one two three four five",
            analytics = analytics,
        )

        viewModel.togglePlayPause()
        assertEquals(SpeedReadPlayerStatus.Playing, viewModel.viewState.value.status)
        assertEquals(1, analytics.ofType<AnalyticsEvent.SpeedReadStart>().size)

        clearViewModel(viewModel)

        assertEquals(
            listOf(
                AnalyticsEvent.SpeedReadComplete(
                    durationBucket = DurationBucket.UpTo30s,
                    result = AnalyticsEvent.SpeedReadComplete.Result.Closed,
                ),
            ),
            analytics.ofType<AnalyticsEvent.SpeedReadComplete>(),
        )
    }

    private fun playerViewModel(
        content: String,
        positions: MutableMap<String, Int> = mutableMapOf(),
        wordOffsets: MutableMap<String, Int> = mutableMapOf(),
        settings: SpeedReadSettings = SpeedReadSettings(wpm = 300, chunkSize = 1),
        bookId: String = "book-1",
        analytics: AnalyticsLogger = RecordingAnalytics(),
    ): SpeedReadPlayerViewModel {
        return SpeedReadPlayerViewModel(
            book = Book(id = bookId, title = "Sample", content = content),
            readingSessionRepository = memoryReadingSessionRepository(positions, wordOffsets),
            settingsRepository = memorySpeedReadSettingsRepository(arrayOf(settings)),
            computationDispatcher = dispatcher,
            analytics = analytics,
        )
    }

    private fun clearViewModel(viewModel: SpeedReadPlayerViewModel) {
        val store = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return viewModel as T
            }
        }
        ViewModelProvider.create(store, factory)[SpeedReadPlayerViewModel::class]
        store.clear()
    }
}
