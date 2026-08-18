package com.tool.flashread.ui.speedread

import androidx.lifecycle.ViewModel
import com.tool.flashread.core.model.Book
import com.tool.flashread.core.speedread.SpeedReadPlayback
import com.tool.flashread.core.speedread.SpeedReadSettings
import com.tool.flashread.core.speedread.delayUnitsToMs
import com.tool.flashread.core.speedread.remainingMsToMinutes
import com.tool.flashread.data.repository.ReadingSessionRepository
import com.tool.flashread.data.repository.SpeedReadSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SpeedReadSetupUiState(
    val settings: SpeedReadSettings,
    val remainingMinutes: Int,
    val canStart: Boolean,
)

class SpeedReadSetupViewModel(
    private val book: Book,
    private val settingsRepository: SpeedReadSettingsRepository = SpeedReadSettingsRepository(),
    private val readingSessionRepository: ReadingSessionRepository = ReadingSessionRepository(),
) : ViewModel() {
    private val startParagraphIndex = readingSessionRepository.getPosition(book.id).paragraphIndex
    private var cachedChunkSize: Int? = null
    private var cachedRemainingDelayUnits: Double = 0.0

    private val _uiState = MutableStateFlow(stateFrom(settingsRepository.load()))
    val uiState: StateFlow<SpeedReadSetupUiState> = _uiState.asStateFlow()

    fun updateSettings(updated: SpeedReadSettings) {
        val normalized = updated.normalized()
        settingsRepository.save(normalized)
        _uiState.value = stateFrom(normalized)
    }

    fun persistSettings() {
        settingsRepository.save(_uiState.value.settings)
    }

    private fun stateFrom(settings: SpeedReadSettings): SpeedReadSetupUiState {
        val normalized = settings.normalized()
        val remainingMs = delayUnitsToMs(
            remainingDelayUnits(normalized.chunkSize),
            normalized.wpm,
        )
        return SpeedReadSetupUiState(
            settings = normalized,
            remainingMinutes = remainingMsToMinutes(remainingMs),
            canStart = book.content.isNotBlank(),
        )
    }

    private fun remainingDelayUnits(chunkSize: Int): Double {
        if (cachedChunkSize != chunkSize) {
            val playback = SpeedReadPlayback(book.content, chunkSize)
            cachedRemainingDelayUnits = playback.remainingDelayUnits(
                playback.startPosition(startParagraphIndex),
            )
            cachedChunkSize = chunkSize
        }
        return cachedRemainingDelayUnits
    }
}
