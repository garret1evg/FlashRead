package com.evgeniich.flashread.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evgeniich.flashread.core.model.Book
import com.evgeniich.flashread.core.model.ReadingPosition
import com.evgeniich.flashread.core.reading.ReaderTextSettings
import com.evgeniich.flashread.core.speedread.firstWordInParagraph
import com.evgeniich.flashread.core.speedread.splitBookParagraphs
import com.evgeniich.flashread.core.speedread.wordAtParagraphOffset
import com.evgeniich.flashread.core.speedread.wordHighlightAtContentOffset
import com.evgeniich.flashread.data.repository.ReaderTextSettingsRepository
import com.evgeniich.flashread.data.repository.ReadingSessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReaderDocument(
    val paragraphs: List<String>,
    val initialParagraphIndex: Int,
)

class ReaderViewModel(
    val book: Book,
    private val readingSessionRepository: ReadingSessionRepository = ReadingSessionRepository(),
    private val textSettingsRepository: ReaderTextSettingsRepository = ReaderTextSettingsRepository(),
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _document = MutableStateFlow<ReaderDocument?>(null)
    val document: StateFlow<ReaderDocument?> = _document.asStateFlow()

    val paragraphs: List<String>
        get() = _document.value?.paragraphs.orEmpty()

    val initialParagraphIndex: Int
        get() = _document.value?.initialParagraphIndex ?: 0

    private val _startWord = MutableStateFlow<ReaderStartWord?>(null)
    val startWord: StateFlow<ReaderStartWord?> = _startWord.asStateFlow()

    private val _scrollToParagraph = MutableStateFlow<Int?>(null)
    val scrollToParagraph: StateFlow<Int?> = _scrollToParagraph.asStateFlow()

    private val _settings = MutableStateFlow(textSettingsRepository.load())
    val settings: StateFlow<ReaderTextSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            val prepared = withContext(computationDispatcher) { prepareDocument() }
            _document.value = prepared.document
            _startWord.value = prepared.startWord
        }
    }

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

    private fun prepareDocument(): PreparedReader {
        val paragraphs = splitBookParagraphs(book.content)
        val savedPosition = readingSessionRepository.getPosition(book.id)
        val wordOffset = savedPosition.wordOffset
        val paragraphIndex = if (wordOffset != ReadingPosition.UNSET) {
            wordHighlightAtContentOffset(book.content, wordOffset)?.paragraphIndex
                ?: savedPosition.paragraphIndex
        } else {
            savedPosition.paragraphIndex
        }.coerceIn(0, paragraphs.lastIndex.coerceAtLeast(0))
        return PreparedReader(
            document = ReaderDocument(
                paragraphs = paragraphs,
                initialParagraphIndex = paragraphIndex,
            ),
            startWord = initStartWordFromPosition(savedPosition),
        )
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

    private data class PreparedReader(
        val document: ReaderDocument,
        val startWord: ReaderStartWord?,
    )
}
