package com.tool.flashread.ui.speedread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tool.flashread.core.model.Book
import com.tool.flashread.core.model.ReadingPosition
import com.tool.flashread.core.speedread.SpeedReadPlayback
import com.tool.flashread.core.speedread.SpeedReadPlayerController
import com.tool.flashread.core.speedread.SpeedReadPlayerStatus
import com.tool.flashread.core.speedread.SpeedReadPlayerViewState
import com.tool.flashread.core.speedread.SpeedReadPosition
import com.tool.flashread.core.speedread.SpeedReadSessionTotals
import com.tool.flashread.core.speedread.SpeedReadSettings
import com.tool.flashread.data.repository.ReadingSessionRepository
import com.tool.flashread.data.repository.SpeedReadSettingsRepository
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
) : ViewModel() {
    private val startParagraphIndex = readingSessionRepository.getPosition(book.id).paragraphIndex
    private var savedTokenIndex = -1
    private var savedOffset = 0
    private var savedParagraph = startParagraphIndex
    private var lastSavedParagraph = startParagraphIndex
    private var resumeOnStart = false
    private var settings: SpeedReadSettings = settingsRepository.load().normalized()
    private var controller: SpeedReadPlayerController? = null
    private var tickerJob: Job? = null
    private var prepareJob: Job? = null

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
        settings = normalized
        settingsRepository.save(normalized)
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
            else -> playback.startPosition(startParagraphIndex)
        }
    }

    private fun publish() {
        val current = controller ?: return
        _viewState.value = current.viewState
        persistPosition()
        syncTicker()
    }

    private fun persistPosition(force: Boolean = false) {
        val position = controller?.viewState?.position ?: return
        savedTokenIndex = position.tokenIndex
        savedOffset = position.offset
        savedParagraph = position.paragraphIndex
        if (force || position.paragraphIndex != lastSavedParagraph) {
            lastSavedParagraph = position.paragraphIndex
            readingSessionRepository.savePosition(
                ReadingPosition(
                    bookId = book.id,
                    paragraphIndex = position.paragraphIndex,
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
