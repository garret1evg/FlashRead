package com.tool.flashread

import androidx.lifecycle.ViewModel
import com.tool.flashread.core.model.Book
import com.tool.flashread.core.model.MaterialSourceType
import com.tool.flashread.core.reading.bookProgressPercent
import com.tool.flashread.core.reading.withReadingStats
import com.tool.flashread.data.repository.BookRepository
import com.tool.flashread.data.repository.CoverRepository
import com.tool.flashread.data.repository.ReadingSessionRepository
import com.tool.flashread.platform.ImportedBook
import com.tool.flashread.ui.library.MaterialTitleFormatter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

data class AppUiState(
    val books: List<Book> = emptyList(),
    val selectedBookId: String? = null,
) {
    val currentBook: Book?
        get() = books.firstOrNull { it.id == selectedBookId }
}

class AppViewModel(
    private val bookRepository: BookRepository = BookRepository(),
    private val readingSessionRepository: ReadingSessionRepository = ReadingSessionRepository(),
    private val coverRepository: CoverRepository = CoverRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AppUiState(books = bookRepository.loadBooks()),
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun progressPercent(book: Book): Int {
        return bookProgressPercent(
            paragraphIndex = readingSessionRepository.getPosition(book.id).paragraphIndex,
            paragraphCount = book.paragraphCount,
        )
    }

    fun selectBook(bookId: String) {
        _uiState.update { it.copy(selectedBookId = bookId) }
    }

    fun upsertImportedBook(importedBook: ImportedBook) {
        val existing = _uiState.value.books.firstOrNull { it.id == importedBook.id }
        val book = Book(
            id = importedBook.id,
            title = importedBook.title,
            content = importedBook.content,
            sourceType = MaterialSourceType.Book,
            coverFileName = persistImportedCover(importedBook, existing?.coverFileName),
        ).withReadingStats()
        upsert(book)
        selectBook(book.id)
        showMessage("Imported ${MaterialTitleFormatter.displayTitle(importedBook.title)}")
    }

    fun onImportError(message: String) {
        showMessage(message)
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
        showMessage("Added ${MaterialTitleFormatter.displayTitle(resolvedTitle)}")
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
        _uiState.value = current.copy(
            books = updatedBooks,
            selectedBookId = if (current.selectedBookId == bookId) null else current.selectedBookId,
        )
        showMessage("Deleted ${MaterialTitleFormatter.displayTitle(deleted.title)}")
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

    private fun showMessage(message: String) {
        _messages.trySend(message)
    }
}
