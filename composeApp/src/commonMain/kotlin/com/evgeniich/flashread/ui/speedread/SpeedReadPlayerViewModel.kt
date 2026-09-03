package com.evgeniich.flashread.ui.speedread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evgeniich.flashread.ScratchSpeedReadBookId
import com.evgeniich.flashread.analytics.Analytics
import com.evgeniich.flashread.analytics.AnalyticsBuckets
import com.evgeniich.flashread.analytics.AnalyticsEvent
import com.evgeniich.flashread.analytics.AnalyticsLogger
import com.evgeniich.flashread.analytics.SettingsChangeLogger
import com.evgeniich.flashread.core.model.Book
import com.evgeniich.flashread.core.model.ReadingPosition
import com.evgeniich.flashread.core.speedread.SpeedReadPlayback
import com.evgeniich.flashread.core.speedread.SpeedReadPlayerController
import com.evgeniich.flashread.core.speedread.SpeedReadPlayerStatus
import com.evgeniich.flashread.core.speedread.SpeedReadPlayerViewState
import com.evgeniich.flashread.core.speedread.SpeedReadPosition
import com.evgeniich.flashread.core.speedread.SpeedReadSessionTotals
import com.evgeniich.flashread.core.speedread.SpeedReadSettings
import com.evgeniich.flashread.data.repository.ReadingSessionRepository
import com.evgeniich.flashread.data.repository.SpeedReadSettingsRepository
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SpeedReadPlayerViewModel(
    private val book: Book,
    private val readingSessionRepository: ReadingSessionRepository = ReadingSessionRepository(),
    private val settingsRepository: SpeedReadSettingsRepository = SpeedReadSettingsRepository(),
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val analytics: AnalyticsLogger = Analytics,
) : ViewModel() {
    private val startPosition = readingSessionRepository.getPosition(book.id)
    private val startParagraphIndex = startPosition.paragraphIndex
    private val startWordOffset = startPosition.wordOffset
    private var savedTokenIndex = -1
    private var savedOffset = 0
    private var savedParagraph = startParagraphIndex
    private var savedWordOffset = startWordOffset
    private var lastSavedParagraph = startParagraphIndex
    private var lastSavedWordOffset = startWordOffset
    private var resumeOnStart = false
    private var settings: SpeedReadSettings = settingsRepository.load().normalized()
    private var controller: SpeedReadPlayerController? = null
    private var tickerJob: Job? = null
    private var prepareJob: Job? = null
    private val settingsChangeLogger = SettingsChangeLogger(analytics, viewModelScope)
    private var sessionStarted = false
    private var completeLogged = false
    private var playStartedAt: TimeMark? = null

    private val _viewState = MutableStateFlow(SpeedReadPlayerViewState.placeholder(settings))
    val viewState: StateFlow<SpeedReadPlayerViewState> = _viewState.asStateFlow()

    init {
        prepareSession()
    }

    fun togglePlayPause() = runController { it.togglePlayPause() }

    fun restart() = runController { it.restart() }

    fun stepBack() = runController { it.stepBack() }

    fun stepForward() = runController { it.stepForward() }

    fun updateSettings(updated: SpeedReadSettings) {
        val normalized = updated.normalized()
        val chunkChanged = normalized.chunkSize != settings.chunkSize
        val previous = settings
        settings = normalized
        settingsRepository.save(normalized)
        settingsChangeLogger.logSpeedReadDiff(previous, normalized)
        if (chunkChanged) {
            persistPosition(force = true)
            prepareSession()
        } else {
            controller?.updateSettings(normalized)
            publish()
        }
    }

    fun persistNow() {
        persistPosition(force = true)
    }

    fun onHostStop() {
        persistPosition(force = true)
        val current = controller ?: return
        if (current.viewState.isPlaying) {
            resumeOnStart = true
            current.pause()
            publish()
        }
    }

    fun onHostStart() {
        if (!resumeOnStart) return
        resumeOnStart = false
        controller?.play()
        publish()
    }

    override fun onCleared() {
        persistPosition(force = true)
        settingsChangeLogger.flush()
        logSpeedReadComplete(AnalyticsEvent.SpeedReadComplete.Result.Closed)
        super.onCleared()
    }

    private fun runController(action: (SpeedReadPlayerController) -> Unit) {
        val current = controller ?: return
        action(current)
        publish()
    }

    private fun prepareSession() {
        prepareJob?.cancel()
        stopTicker()
        controller = null
        val chunkSize = settings.chunkSize
        val content = book.content
        prepareJob = viewModelScope.launch {
            val prepared = withContext(computationDispatcher) {
                val playback = SpeedReadPlayback(content, chunkSize)
                playback to restoreStart(playback, chunkSize)
            }
            if (!isActive) return@launch
            val (playback, start) = prepared
            val session = SpeedReadPlayerController(
                playback = playback,
                totals = SpeedReadSessionTotals.Empty,
                initialPosition = start,
                initialSettings = settings,
            )
            controller = session
            publish()
            val (totals, elapsedAtStart) = withContext(computationDispatcher) {
                playback.sessionTotals() to playback.elapsedDelayUnits(start)
            }
            if (!isActive || controller !== session) return@launch
            session.applySessionMetrics(totals, elapsedAtStart)
            publish()
        }
    }

    private fun restoreStart(
        playback: SpeedReadPlayback,
        chunkSize: Int,
    ): SpeedReadPosition {
        val restored = SpeedReadPosition(savedTokenIndex, savedOffset, savedParagraph)
        return when {
            savedTokenIndex >= 0 &&
                savedTokenIndex % chunkSize == 0 &&
                playback.chunkAt(restored) != null -> restored
            savedTokenIndex >= 0 -> playback.startPosition(savedParagraph)
            startWordOffset >= 0 -> playback.positionAtOffset(startWordOffset)
            else -> playback.startPosition(startParagraphIndex)
        }
    }

    private fun publish() {
        val current = controller ?: return
        _viewState.value = current.viewState
        trackPlaybackAnalytics(current.viewState)
        persistPosition()
        syncTicker()
    }

    private fun trackPlaybackAnalytics(state: SpeedReadPlayerViewState) {
        if (state.isPlaying && !sessionStarted) {
            sessionStarted = true
            playStartedAt = TimeSource.Monotonic.markNow()
            analytics.log(
                AnalyticsEvent.SpeedReadStart(
                    wpmBucket = AnalyticsBuckets.wpm(state.settings.wpm),
                    spritzEnabled = state.settings.spritzEnabled,
                    source = if (book.id == ScratchSpeedReadBookId) {
                        AnalyticsEvent.SpeedReadStart.Source.Paste
                    } else {
                        AnalyticsEvent.SpeedReadStart.Source.Book
                    },
                    chunkSize = state.settings.chunkSize,
                ),
            )
        }
        if (state.isFinished) {
            logSpeedReadComplete(AnalyticsEvent.SpeedReadComplete.Result.Finished)
        }
    }

    private fun logSpeedReadComplete(result: AnalyticsEvent.SpeedReadComplete.Result) {
        if (!sessionStarted || completeLogged) return
        completeLogged = true
        val durationMs = playStartedAt?.elapsedNow()?.inWholeMilliseconds ?: 0L
        analytics.log(
            AnalyticsEvent.SpeedReadComplete(
                durationBucket = AnalyticsBuckets.duration(durationMs),
                result = result,
            ),
        )
    }

    private fun persistPosition(force: Boolean = false) {
        val position = controller?.viewState?.position ?: return
        savedTokenIndex = position.tokenIndex
        savedOffset = position.offset
        savedParagraph = position.paragraphIndex
        savedWordOffset = position.offset
        val positionChanged = position.paragraphIndex != lastSavedParagraph ||
            position.offset != lastSavedWordOffset
        if (force || positionChanged) {
            lastSavedParagraph = position.paragraphIndex
            lastSavedWordOffset = position.offset
            readingSessionRepository.savePosition(
                ReadingPosition(
                    bookId = book.id,
                    paragraphIndex = position.paragraphIndex,
                    wordOffset = position.offset,
                ),
            )
        }
    }

    private fun syncTicker() {
        val current = controller
        if (current == null || current.viewState.status != SpeedReadPlayerStatus.Playing) {
            stopTicker()
            return
        }
        stopTicker()
        tickerJob = viewModelScope.launch {
            delay(current.currentDelayMs())
            if (controller !== current) return@launch
            if (current.viewState.status != SpeedReadPlayerStatus.Playing) return@launch
            current.onTick()
            publish()
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }
}
