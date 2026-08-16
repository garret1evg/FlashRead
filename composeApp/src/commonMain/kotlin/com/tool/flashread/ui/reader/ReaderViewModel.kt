package com.tool.flashread.ui.reader

import androidx.lifecycle.ViewModel
import com.tool.flashread.core.model.Book
import com.tool.flashread.core.model.ReadingPosition
import com.tool.flashread.core.reading.ReaderTextSettings
import com.tool.flashread.core.speedread.splitBookParagraphs
import com.tool.flashread.data.repository.ReaderTextSettingsRepository
import com.tool.flashread.data.repository.ReadingSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReaderViewModel(
    val book: Book,
    private val readingSessionRepository: ReadingSessionRepository = ReadingSessionRepository(),
    private val textSettingsRepository: ReaderTextSettingsRepository = ReaderTextSettingsRepository(),
) : ViewModel() {
    val paragraphs: List<String> = splitBookParagraphs(book.content)
    val initialParagraphIndex: Int = readingSessionRepository
        .getPosition(book.id)
        .paragraphIndex
        .coerceIn(0, paragraphs.lastIndex.coerceAtLeast(0))

    private val _settings = MutableStateFlow(textSettingsRepository.load())
    val settings: StateFlow<ReaderTextSettings> = _settings.asStateFlow()

    fun saveParagraphIndex(paragraphIndex: Int) {
        readingSessionRepository.savePosition(
            ReadingPosition(
                bookId = book.id,
                paragraphIndex = paragraphIndex.coerceAtLeast(0),
            ),
        )
    }

    fun updateSettings(updated: ReaderTextSettings) {
        val normalized = updated.normalized()
        _settings.value = normalized
        textSettingsRepository.save(normalized)
    }
}
