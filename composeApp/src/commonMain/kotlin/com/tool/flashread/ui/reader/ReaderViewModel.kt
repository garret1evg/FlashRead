package com.tool.flashread.ui.reader

import androidx.lifecycle.ViewModel
import com.tool.flashread.core.model.Book
import com.tool.flashread.core.model.ReadingPosition
import com.tool.flashread.core.reading.ReaderTextSettings
import com.tool.flashread.core.speedread.firstWordInParagraph
import com.tool.flashread.core.speedread.splitBookParagraphs
import com.tool.flashread.core.speedread.wordAtParagraphOffset
import com.tool.flashread.core.speedread.wordHighlightAtContentOffset
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

    val initialParagraphIndex: Int = run {
        val savedPosition = readingSessionRepository.getPosition(book.id)
        val wordOffset = savedPosition.wordOffset
        if (wordOffset != ReadingPosition.UNSET) {
            wordHighlightAtContentOffset(book.content, wordOffset)?.paragraphIndex
                ?: savedPosition.paragraphIndex
        } else {
            savedPosition.paragraphIndex
        }.coerceIn(0, paragraphs.lastIndex.coerceAtLeast(0))
    }

    private val _startWord = MutableStateFlow(initStartWord())
    val startWord: StateFlow<ReaderStartWord?> = _startWord.asStateFlow()

    private val _scrollToParagraph = MutableStateFlow<Int?>(null)
    val scrollToParagraph: StateFlow<Int?> = _scrollToParagraph.asStateFlow()

    private val _settings = MutableStateFlow(textSettingsRepository.load())
    val settings: StateFlow<ReaderTextSettings> = _settings.asStateFlow()

    fun refreshPosition() {
        val currentPosition = readingSessionRepository.getPosition(book.id)
        val storedOffset = currentPosition.wordOffset
        val currentHighlight = _startWord.value

        // Compare stored position with what's currently highlighted
        val currentContentOffset = currentHighlight?.contentOffset ?: -1
        val storedContentOffset = if (storedOffset != ReadingPosition.UNSET) {
            storedOffset
        } else {
            // If stored is UNSET, it means first word of stored paragraph
            firstWordInParagraph(book.content, currentPosition.paragraphIndex)?.contentOffset ?: -1
        }

        if (storedContentOffset == currentContentOffset) {
            return
        }

        val newStartWord = initStartWordFromPosition(currentPosition)
        _startWord.value = newStartWord
        if (newStartWord != null && newStartWord.paragraphIndex != currentHighlight?.paragraphIndex) {
            _scrollToParagraph.value = newStartWord.paragraphIndex
        }
    }

    fun onScrollHandled() {
        _scrollToParagraph.value = null
    }

    private fun initStartWord(): ReaderStartWord? {
        val savedPosition = readingSessionRepository.getPosition(book.id)
        return initStartWordFromPosition(savedPosition)
    }

    private fun initStartWordFromPosition(position: ReadingPosition): ReaderStartWord? {
        val wordOffset = position.wordOffset
        return if (wordOffset != ReadingPosition.UNSET) {
            wordHighlightAtContentOffset(book.content, wordOffset)?.let { loc ->
                ReaderStartWord(
                    paragraphIndex = loc.paragraphIndex,
                    localStart = loc.localStart,
                    localEnd = loc.localEnd,
                    contentOffset = loc.contentOffset,
                    pinned = true,
                )
            }
        } else {
            firstWordInParagraph(book.content, position.paragraphIndex)?.let { loc ->
                ReaderStartWord(
                    paragraphIndex = loc.paragraphIndex,
                    localStart = loc.localStart,
                    localEnd = loc.localEnd,
                    contentOffset = loc.contentOffset,
                    pinned = false,
                )
            }
        }
    }

    fun saveParagraphIndex(paragraphIndex: Int) {
        val current = _startWord.value
        if (current != null && current.pinned) {
            return
        }
        val safeParagraphIndex = paragraphIndex.coerceAtLeast(0)
        val wordLoc = firstWordInParagraph(book.content, safeParagraphIndex)
        if (wordLoc != null) {
            _startWord.value = ReaderStartWord(
                paragraphIndex = wordLoc.paragraphIndex,
                localStart = wordLoc.localStart,
                localEnd = wordLoc.localEnd,
                contentOffset = wordLoc.contentOffset,
                pinned = false,
            )
        }
        readingSessionRepository.savePosition(
            ReadingPosition(
                bookId = book.id,
                paragraphIndex = safeParagraphIndex,
                wordOffset = ReadingPosition.UNSET,
            ),
        )
    }

    fun selectWord(paragraphIndex: Int, localCharOffset: Int) {
        val wordLoc = wordAtParagraphOffset(book.content, paragraphIndex, localCharOffset) ?: return
        _startWord.value = ReaderStartWord(
            paragraphIndex = wordLoc.paragraphIndex,
            localStart = wordLoc.localStart,
            localEnd = wordLoc.localEnd,
            contentOffset = wordLoc.contentOffset,
            pinned = true,
        )
        readingSessionRepository.savePosition(
            ReadingPosition(
                bookId = book.id,
                paragraphIndex = wordLoc.paragraphIndex,
                wordOffset = wordLoc.contentOffset,
            ),
        )
    }

    fun updateSettings(updated: ReaderTextSettings) {
        val normalized = updated.normalized()
        _settings.value = normalized
        textSettingsRepository.save(normalized)
    }
}
