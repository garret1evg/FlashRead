package com.evgeniich.flashread.core.speedread

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeedReadPlayerControllerTest {

    @Test
    fun longWordStaysInViewState() {
        val controller = controller("supercalifragilistic extra")
        assertEquals("supercalifragilistic", controller.viewState.text)
        assertEquals(SpeedReadPlayerStatus.Paused, controller.viewState.status)
        val parts = orpParts(controller.viewState.text)
        assertEquals(4, parts.pivotIndex)
        assertEquals("r", parts.pivot)
    }

    @Test
    fun multiWordChunkShowsTheWholeGroup() {
        val controller = controller("one two three four", chunkSize = 3)
        assertEquals("one two three", controller.viewState.text)
        controller.stepForward()
        assertEquals("four", controller.viewState.text)
    }

    @Test
    fun pauseIgnoresTicks() {
        val controller = controller("one two three")
        controller.play()
        assertEquals(SpeedReadPlayerStatus.Playing, controller.viewState.status)
        controller.pause()
        assertEquals(SpeedReadPlayerStatus.Paused, controller.viewState.status)
        val pausedText = controller.viewState.text
        controller.onTick()
        assertEquals(pausedText, controller.viewState.text)
        assertEquals(SpeedReadPlayerStatus.Paused, controller.viewState.status)
    }

    @Test
    fun playingAdvancesUntilTheTextEnds() {
        val controller = controller("one two")
        controller.play()
        assertEquals("one", controller.viewState.text)
        controller.onTick()
        assertEquals("two", controller.viewState.text)
        assertEquals(SpeedReadPlayerStatus.Playing, controller.viewState.status)
        controller.onTick()
        assertEquals("two", controller.viewState.text)
        assertEquals(SpeedReadPlayerStatus.Finished, controller.viewState.status)
        assertEquals(1f, controller.viewState.progress)
        assertEquals(0L, controller.viewState.remainingMs)
    }

    @Test
    fun playAfterFinishRestartsFromTheBeginning() {
        val controller = controller("one two")
        controller.play()
        controller.onTick()
        controller.onTick()
        assertEquals(SpeedReadPlayerStatus.Finished, controller.viewState.status)
        controller.play()
        assertEquals("one", controller.viewState.text)
        assertEquals(SpeedReadPlayerStatus.Playing, controller.viewState.status)
        assertEquals(0f, controller.viewState.progress)
    }

    @Test
    fun restartReturnsToFirstChunkAndPauses() {
        val controller = controller("one two three")
        controller.play()
        controller.onTick()
        controller.restart()
        assertEquals("one", controller.viewState.text)
        assertEquals(SpeedReadPlayerStatus.Paused, controller.viewState.status)
        assertEquals(0L, controller.viewState.elapsedMs)
    }

    @Test
    fun sentenceEndUsesALongerDelayThanAPlainWord() {
        val controller = controller("Wait. Go")
        val sentenceDelay = controller.currentDelayMs()
        controller.stepForward()
        val plainDelay = controller.currentDelayMs()
        assertTrue(sentenceDelay > plainDelay)
        assertEquals(SpeedReadTiming.delayMs(300, 2.2), sentenceDelay)
        assertEquals(SpeedReadTiming.delayMs(300, 1.0), plainDelay)
    }

    @Test
    fun commaUsesAShorterPauseThanSentenceEnd() {
        val sentence = controller("Done.")
        val comma = controller("wait,")
        assertTrue(sentence.currentDelayMs() > comma.currentDelayMs())
        assertEquals(SpeedReadTiming.delayMs(300, 1.5), comma.currentDelayMs())
    }

    @Test
    fun applySessionMetricsFillsElapsedWhenStartingMidBook() {
        val playback = SpeedReadPlayback("one two three four", chunkSize = 1)
        val second = playback.next(playback.startPosition(0), loop = false)!!
        val controller = SpeedReadPlayerController(
            playback = playback,
            totals = SpeedReadSessionTotals.Empty,
            initialPosition = second,
            initialSettings = SpeedReadSettings(wpm = 300, chunkSize = 1),
        )
        assertEquals(0L, controller.viewState.elapsedMs)
        assertEquals(0f, controller.viewState.progress)

        controller.applySessionMetrics(
            totals = playback.sessionTotals(),
            elapsedAtInitialPosition = playback.elapsedDelayUnits(second),
        )
        assertTrue(controller.viewState.elapsedMs > 0L)
        assertTrue(controller.viewState.remainingMs > 0L)
        assertTrue(controller.viewState.progress > 0f)
    }

    @Test
    fun applySessionMetricsKeepsForwardProgressFromStart() {
        val playback = SpeedReadPlayback("one two three", chunkSize = 1)
        val controller = SpeedReadPlayerController(
            playback = playback,
            totals = SpeedReadSessionTotals.Empty,
            initialPosition = playback.startPosition(0),
            initialSettings = SpeedReadSettings(wpm = 300, chunkSize = 1),
        )
        controller.stepForward()
        val elapsedAfterStep = controller.viewState.elapsedMs
        assertTrue(elapsedAfterStep > 0L)

        controller.applySessionMetrics(
            totals = playback.sessionTotals(),
            elapsedAtInitialPosition = 0.0,
        )
        assertEquals(elapsedAfterStep, controller.viewState.elapsedMs)
        assertTrue(controller.viewState.remainingMs > 0L)
    }

    @Test
    fun remainingTimeShrinksAsChunksAdvance() {
        val controller = controller("one two three four")
        val initialRemaining = controller.viewState.remainingMs
        controller.stepForward()
        assertTrue(controller.viewState.remainingMs < initialRemaining)
        assertTrue(controller.viewState.elapsedMs > 0L)
    }

    @Test
    fun remainingMsToMinutesRoundsUpPartialMinutes() {
        assertEquals(0, remainingMsToMinutes(0))
        assertEquals(1, remainingMsToMinutes(1_500))
        assertEquals(1, remainingMsToMinutes(60_000))
        assertEquals(2, remainingMsToMinutes(60_001))
    }
}

private fun controller(
    content: String,
    chunkSize: Int = 1,
    wpm: Int = 300,
): SpeedReadPlayerController {
    val playback = SpeedReadPlayback(content, chunkSize)
    return SpeedReadPlayerController(
        playback = playback,
        totals = playback.sessionTotals(),
        initialPosition = playback.startPosition(0),
        initialSettings = SpeedReadSettings(wpm = wpm, chunkSize = chunkSize),
    )
}
