package com.tool.flashread.ui.speedread

import androidx.lifecycle.ViewModel
import com.tool.flashread.core.model.Book
import com.tool.flashread.core.reading.estimatedRemainingMinutes
import com.tool.flashread.core.reading.remainingWordCount
import com.tool.flashread.core.speedread.SpeedReadSettings
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
    private val remainingWords = remainingWordCount(
        book.content,
        readingSessionRepository.getPosition(book.id).paragraphIndex,
    )

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
        return SpeedReadSetupUiState(
            settings = normalized,
            remainingMinutes = estimatedRemainingMinutes(remainingWords, normalized.wpm),
            canStart = book.content.isNotBlank(),
        )
    }
}
