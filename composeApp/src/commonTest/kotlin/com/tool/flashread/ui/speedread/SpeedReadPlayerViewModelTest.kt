package com.tool.flashread.ui.speedread

import com.tool.flashread.core.model.Book
import com.tool.flashread.core.speedread.SpeedReadPlayerStatus
import com.tool.flashread.core.speedread.SpeedReadSettings
import com.tool.flashread.memoryReadingSessionRepository
import com.tool.flashread.memorySpeedReadSettingsRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
        val state = assertNotNull(viewModel.viewState.value)
        assertEquals("one", state.text)
        assertEquals(SpeedReadPlayerStatus.Paused, state.status)

        viewModel.stepForward()
        assertEquals("two", viewModel.viewState.value?.text)

        viewModel.togglePlayPause()
        assertEquals(SpeedReadPlayerStatus.Playing, viewModel.viewState.value?.status)

        viewModel.onHostStop()
        assertEquals(SpeedReadPlayerStatus.Paused, viewModel.viewState.value?.status)
        assertEquals(0, positions["book-1"])
    }

    @Test
    fun changingChunkSizeRebuildsTheSession() {
        val viewModel = playerViewModel(content = "one two three four")
        assertEquals("one", viewModel.viewState.value?.text)

        viewModel.updateSettings(SpeedReadSettings(wpm = 300, chunkSize = 3))
        val rebuilt = assertNotNull(viewModel.viewState.value)
        assertEquals("one two three", rebuilt.text)
        assertEquals(3, rebuilt.settings.chunkSize)
        assertEquals(SpeedReadPlayerStatus.Paused, rebuilt.status)
    }

    @Test
    fun emptyContentFinishesImmediately() {
        val viewModel = playerViewModel(content = "   ")
        val state = assertNotNull(viewModel.viewState.value)
        assertTrue(state.isEmpty)
        assertEquals(SpeedReadPlayerStatus.Finished, state.status)
    }

    private fun playerViewModel(
        content: String,
        positions: MutableMap<String, Int> = mutableMapOf(),
        settings: SpeedReadSettings = SpeedReadSettings(wpm = 300, chunkSize = 1),
    ): SpeedReadPlayerViewModel {
        return SpeedReadPlayerViewModel(
            book = Book(id = "book-1", title = "Sample", content = content),
            readingSessionRepository = memoryReadingSessionRepository(positions),
            settingsRepository = memorySpeedReadSettingsRepository(arrayOf(settings)),
            computationDispatcher = dispatcher,
        )
    }
}
