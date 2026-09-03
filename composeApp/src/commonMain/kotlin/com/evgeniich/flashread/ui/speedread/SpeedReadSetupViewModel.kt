package com.evgeniich.flashread.ui.speedread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evgeniich.flashread.analytics.Analytics
import com.evgeniich.flashread.analytics.AnalyticsLogger
import com.evgeniich.flashread.analytics.SettingsChangeLogger
import com.evgeniich.flashread.core.model.Book
import com.evgeniich.flashread.core.speedread.SpeedReadPlayback
import com.evgeniich.flashread.core.speedread.SpeedReadSettings
import com.evgeniich.flashread.core.speedread.delayUnitsToMs
import com.evgeniich.flashread.core.speedread.remainingMsToMinutes
import com.evgeniich.flashread.data.repository.ReadingSessionRepository
import com.evgeniich.flashread.data.repository.SpeedReadSettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SpeedReadSetupUiState(
    val settings: SpeedReadSettings,
    val remainingMinutes: Int?,
    val canStart: Boolean,
)

class SpeedReadSetupViewModel(
    private val book: Book,
    private val settingsRepository: SpeedReadSettingsRepository = SpeedReadSettingsRepository(),
    private val readingSessionRepository: ReadingSessionRepository = ReadingSessionRepository(),
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val analytics: AnalyticsLogger = Analytics,
) : ViewModel() {
    private val startPosition = readingSessionRepository.getPosition(book.id)
    private val startParagraphIndex = startPosition.paragraphIndex
    private val startWordOffset = startPosition.wordOffset
    private val canStart = book.content.isNotBlank()
    private var cachedChunkSize: Int? = null
    private var cachedRemainingDelayUnits: Double = 0.0
    private var remainingUnitsJob: Job? = null

    private val _uiState = MutableStateFlow(
        SpeedReadSetupUiState(
            settings = settingsRepository.load().normalized(),
            remainingMinutes = null,
            canStart = canStart,
        ),
    )
    val uiState: StateFlow<SpeedReadSetupUiState> = _uiState.asStateFlow()

    private val settingsChangeLogger = SettingsChangeLogger(analytics, viewModelScope)

    init {
        refreshRemainingTime()
    }

    fun updateSettings(updated: SpeedReadSettings) {
        val previous = _uiState.value.settings
        val normalized = updated.normalized()
        settingsRepository.save(normalized)
        settingsChangeLogger.logSpeedReadDiff(previous, normalized)
        val cacheReady = cachedChunkSize == normalized.chunkSize
        _uiState.value = SpeedReadSetupUiState(
            settings = normalized,
            remainingMinutes = if (cacheReady) {
                remainingMsToMinutes(delayUnitsToMs(cachedRemainingDelayUnits, normalized.wpm))
            } else {
                null
            },
            canStart = canStart,
        )
        if (!cacheReady) {
            refreshRemainingTime()
        }
    }

    fun persistSettings() {
        settingsRepository.save(_uiState.value.settings)
        settingsChangeLogger.flush()
    }

    override fun onCleared() {
        settingsChangeLogger.flush()
        super.onCleared()
    }

    private fun refreshRemainingTime() {
        val chunkSize = _uiState.value.settings.chunkSize
        remainingUnitsJob?.cancel()
        remainingUnitsJob = viewModelScope.launch {
            val units = withContext(computationDispatcher) {
                remainingDelayUnits(chunkSize)
            }
            if (!isActive) return@launch
            cachedChunkSize = chunkSize
            cachedRemainingDelayUnits = units
            val settings = _uiState.value.settings
            if (settings.chunkSize != chunkSize) return@launch
            _uiState.value = _uiState.value.copy(
                remainingMinutes = remainingMsToMinutes(delayUnitsToMs(units, settings.wpm)),
            )
        }
    }

    private fun remainingDelayUnits(chunkSize: Int): Double {
        val playback = SpeedReadPlayback(book.content, chunkSize)
        val startPlaybackPosition = if (startWordOffset >= 0) {
            playback.positionAtOffset(startWordOffset)
        } else {
            playback.startPosition(startParagraphIndex)
        }
        return playback.remainingDelayUnits(startPlaybackPosition)
    }
}
