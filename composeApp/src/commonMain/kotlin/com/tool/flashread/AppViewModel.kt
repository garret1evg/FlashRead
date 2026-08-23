package com.tool.flashread

import androidx.lifecycle.ViewModel
import com.tool.flashread.core.model.Book
import com.tool.flashread.core.model.MaterialSourceType
import com.tool.flashread.core.model.ReadingPosition
import com.tool.flashread.core.reading.bookProgressPercent
import com.tool.flashread.core.reading.withReadingStats
import com.tool.flashread.data.repository.BookRepository
import com.tool.flashread.data.repository.CoverRepository
import com.tool.flashread.data.repository.ReadingSessionRepository
import com.tool.flashread.data.repository.RecentBookRepository
import com.tool.flashread.platform.ImportedBook
import com.tool.flashread.ui.library.MaterialTitleFormatter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal const val ScratchSpeedReadBookId = "scratch:speed-read"
internal const val DefaultNewBookTitle = "New book"
internal const val DefaultSpeedReadTitle = "Speed read"

sealed interface AppMessage {
    data class Imported(val title: String) : AppMessage
    data class Added(val title: String) : AppMessage
    data class Deleted(val title: String) : AppMessage
    data class Error(val text: String) : AppMessage
}

data class AppUiState(
    val books: List<Book> = emptyList(),
    val selectedBookId: String? = null,
    val editorBookId: String? = null,
    val scratchBook: Book? = null,
    val isImportingExternalBook: Boolean = false,
    val pendingReaderBookId: String? = null,
) {
    val currentBook: Book?
        get() = books.firstOrNull { it.id == selectedBookId }

    val speedReadBook: Book?
        get() = scratchBook ?: currentBook
}

class AppViewModel(
    private val bookRepository: BookRepository = BookRepository(),
    private val readingSessionRepository: ReadingSessionRepository = ReadingSessionRepository(),
    private val coverRepository: CoverRepository = CoverRepository(),
    private val recentBookRepository: RecentBookRepository = RecentBookRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _messages = Channel<AppMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun progressPercent(book: Book): Int {
        return bookProgressPercent(
            paragraphIndex = readingSessionRepository.getPosition(book.id).paragraphIndex,
            paragraphCount = book.paragraphCount,
        )
    }

    fun selectBook(bookId: String) {
        _uiState.update { it.copy(selectedBookId = bookId, scratchBook = null) }
        persistRecentBookId(bookId)
    }

    fun onExternalBookOpenStarted() {
        _uiState.update { it.copy(isImportingExternalBook = true, pendingReaderBookId = null) }
    }

    fun upsertImportedBook(importedBook: ImportedBook, openInReader: Boolean = false) {
        val existing = _uiState.value.books.firstOrNull { it.id == importedBook.id }
        val book = Book(
            id = importedBook.id,
            title = importedBook.title,
            content = importedBook.content,
            sourceType = MaterialSourceType.Book,
            coverFileName = persistImportedCover(importedBook, existing?.coverFileName),
        ).withReadingStats()
        upsert(book)
        _uiState.update {
            it.copy(
                selectedBookId = book.id,
                isImportingExternalBook = false,
                pendingReaderBookId = if (openInReader) book.id else null,
            )
        }
        persistRecentBookId(book.id)
        showMessage(AppMessage.Imported(MaterialTitleFormatter.displayTitle(importedBook.title)))
    }

    fun consumePendingReaderNavigation() {
        _uiState.update { it.copy(pendingReaderBookId = null) }
    }

    fun onImportError(message: String) {
        _uiState.update { it.copy(isImportingExternalBook = false, pendingReaderBookId = null) }
        showMessage(AppMessage.Error(message))
    }

    fun addYouTubeVideo(title: String, url: String) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) return
        val resolvedTitle = title.trim().ifBlank { trimmedUrl }
        val book = Book(
            id = "youtube:$trimmedUrl",
            title = resolvedTitle,
            content = trimmedUrl,
            sourceType = MaterialSourceType.YouTube,
        ).withReadingStats()
        upsert(book)
        selectBook(book.id)
        showMessage(AppMessage.Added(MaterialTitleFormatter.displayTitle(resolvedTitle)))
    }

    fun startBookEditor(bookId: String?) {
        _uiState.update { it.copy(editorBookId = bookId, scratchBook = null) }
    }

    fun startScratchSpeedRead(
        content: String,
        title: String = DefaultSpeedReadTitle,
    ): Boolean {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) return false
        val book = Book(
            id = ScratchSpeedReadBookId,
            title = title.trim().ifBlank { DefaultSpeedReadTitle },
            content = trimmedContent,
            sourceType = MaterialSourceType.Book,
        ).withReadingStats()
        readingSessionRepository.savePosition(
            ReadingPosition(
                bookId = ScratchSpeedReadBookId,
                paragraphIndex = 0,
                wordOffset = ReadingPosition.UNSET,
            ),
        )
        _uiState.update { it.copy(scratchBook = book) }
        return true
    }

    fun clearScratchBook() {
        if (_uiState.value.scratchBook == null) return
        _uiState.update { it.copy(scratchBook = null) }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun createBook(
        title: String,
        content: String,
        defaultTitle: String = DefaultNewBookTitle,
    ) {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) return
        val book = Book(
            id = "created:${Uuid.random()}",
            title = authoredBookTitle(title, defaultTitle),
            content = trimmedContent,
            sourceType = MaterialSourceType.Book,
        ).withReadingStats()
        upsert(book)
        selectBook(book.id)
    }

    fun updateCreatedBook(
        bookId: String,
        title: String,
        content: String,
        defaultTitle: String = DefaultNewBookTitle,
    ) {
        if (!bookId.startsWith("created:")) return
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) return
        val existing = _uiState.value.books.firstOrNull { it.id == bookId } ?: return
        upsert(
            existing.copy(
                title = authoredBookTitle(title, defaultTitle),
                content = trimmedContent,
            ).withReadingStats(),
        )
    }

    fun renameBook(bookId: String, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isBlank()) return
        val current = _uiState.value
        val index = current.books.indexOfFirst { it.id == bookId }
        if (index == -1) return
        val updatedBooks = current.books.toMutableList()
        updatedBooks[index] = updatedBooks[index].copy(title = trimmed)
        persist(updatedBooks)
        _uiState.value = current.copy(books = updatedBooks)
    }

    fun deleteBook(bookId: String) {
        val current = _uiState.value
        val deleted = current.books.firstOrNull { it.id == bookId } ?: return
        deleted.coverFileName?.let { coverRepository.deleteCover(it) }
        val updatedBooks = current.books.filterNot { it.id == bookId }
        persist(updatedBooks)
        val selectedBookId = if (current.selectedBookId == bookId) null else current.selectedBookId
        _uiState.value = current.copy(
            books = updatedBooks,
            selectedBookId = selectedBookId,
        )
        if (selectedBookId != current.selectedBookId) {
            persistRecentBookId(selectedBookId)
        }
        showMessage(AppMessage.Deleted(MaterialTitleFormatter.displayTitle(deleted.title)))
    }

    private fun upsert(book: Book) {
        val current = _uiState.value
        val books = current.books.toMutableList()
        val index = books.indexOfFirst { it.id == book.id }
        if (index == -1) {
            books.add(book)
        } else {
            books[index] = book
        }
        persist(books)
        _uiState.value = current.copy(books = books)
    }

    private fun persistImportedCover(
        importedBook: ImportedBook,
        existingCoverFileName: String?,
    ): String? {
        val bytes = importedBook.coverBytes?.takeIf { it.isNotEmpty() } ?: return existingCoverFileName
        val saved = runCatching {
            coverRepository.saveCover(
                bookId = importedBook.id,
                bytes = bytes,
                mimeType = importedBook.coverMimeType ?: "image/jpeg",
            )
        }.getOrNull() ?: return existingCoverFileName
        if (existingCoverFileName != null && existingCoverFileName != saved) {
            coverRepository.deleteCover(existingCoverFileName)
        }
        return saved
    }

    private fun persist(books: List<Book>) {
        bookRepository.saveBooks(books)
    }

    private fun persistRecentBookId(bookId: String?) {
        recentBookRepository.save(bookId)
    }

    private fun loadInitialState(): AppUiState {
        val books = bookRepository.loadBooks()
        val savedId = recentBookRepository.load()
        val selectedBookId = savedId?.takeIf { id -> books.any { it.id == id } }
        if (savedId != null && selectedBookId == null) {
            persistRecentBookId(null)
        }
        return AppUiState(books = books, selectedBookId = selectedBookId)
    }

    private fun authoredBookTitle(title: String, defaultTitle: String): String {
        return title.trim().ifBlank { defaultTitle.trim().ifBlank { DefaultNewBookTitle } }
    }

    private fun showMessage(message: AppMessage) {
        _messages.trySend(message)
    }
}
