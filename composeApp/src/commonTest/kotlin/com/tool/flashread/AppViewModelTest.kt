package com.tool.flashread

import com.tool.flashread.core.model.Book
import com.tool.flashread.core.model.MaterialSourceType
import com.tool.flashread.platform.ImportedBook
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class AppViewModelTest {

    @Test
    fun upsertImportedBookSelectsAndPersistsIt() = runTest {
        val stored = mutableListOf<Book>()
        val viewModel = AppViewModel(
            bookRepository = memoryBookRepository(stored),
            readingSessionRepository = memoryReadingSessionRepository(),
        )
        val message = async { viewModel.messages.first() }
        testScheduler.runCurrent()

        viewModel.upsertImportedBook(
            ImportedBook(id = "book-1", title = "notes.txt", content = "one two three"),
        )

        val state = viewModel.uiState.value
        assertEquals("book-1", state.selectedBookId)
        assertEquals(1, state.books.size)
        assertEquals("notes.txt", state.books.single().title)
        assertEquals(MaterialSourceType.Book, state.books.single().sourceType)
        assertEquals(1, stored.size)
        assertEquals("Imported notes", message.await())
    }

    @Test
    fun addYouTubeVideoIgnoresBlankUrlAndUsesUrlAsTitleWhenNeeded() = runTest {
        val viewModel = AppViewModel(
            bookRepository = memoryBookRepository(),
            readingSessionRepository = memoryReadingSessionRepository(),
        )

        viewModel.addYouTubeVideo(title = "Ignored", url = "   ")
        assertTrue(viewModel.uiState.value.books.isEmpty())

        val message = async { viewModel.messages.first() }
        testScheduler.runCurrent()
        viewModel.addYouTubeVideo(title = "  ", url = "https://youtu.be/abc")

        val book = viewModel.uiState.value.currentBook
        assertEquals("youtube:https://youtu.be/abc", book?.id)
        assertEquals("https://youtu.be/abc", book?.title)
        assertEquals(MaterialSourceType.YouTube, book?.sourceType)
        assertEquals("Added https://youtu.be/abc", message.await())
    }

    @Test
    fun renameAndDeleteUpdateLibraryAndClearSelection() = runTest {
        val viewModel = AppViewModel(
            bookRepository = memoryBookRepository(),
            readingSessionRepository = memoryReadingSessionRepository(),
        )
        viewModel.upsertImportedBook(
            ImportedBook(id = "keep", title = "Keep.txt", content = "alpha"),
        )
        viewModel.upsertImportedBook(
            ImportedBook(id = "gone", title = "Gone.txt", content = "beta"),
        )
        viewModel.renameBook("keep", "  Renamed keep  ")
        assertEquals("Renamed keep", viewModel.uiState.value.books.first { it.id == "keep" }.title)

        viewModel.renameBook("keep", "   ")
        assertEquals("Renamed keep", viewModel.uiState.value.books.first { it.id == "keep" }.title)

        viewModel.deleteBook("gone")
        assertNull(viewModel.uiState.value.books.firstOrNull { it.id == "gone" })
        assertNull(viewModel.uiState.value.selectedBookId)
        assertEquals("keep", viewModel.uiState.value.books.single().id)

        viewModel.selectBook("keep")
        viewModel.deleteBook("keep")
        assertTrue(viewModel.uiState.value.books.isEmpty())
        assertNull(viewModel.uiState.value.selectedBookId)
    }

    @Test
    fun upsertImportedBookPersistsCoverAndDeletesItWithTheBook() = runTest {
        val stored = mutableListOf<Book>()
        val covers = mutableMapOf<String, ByteArray>()
        val viewModel = AppViewModel(
            bookRepository = memoryBookRepository(stored),
            readingSessionRepository = memoryReadingSessionRepository(),
            coverRepository = memoryCoverRepository(covers),
        )
        val cover = byteArrayOf(1, 2, 3, 4)

        viewModel.upsertImportedBook(
            ImportedBook(
                id = "book-1",
                title = "novel.epub",
                content = "chapter",
                coverBytes = cover,
                coverMimeType = "image/jpeg",
            ),
        )

        val book = viewModel.uiState.value.books.single()
        assertEquals(1, covers.size)
        assertEquals(covers.keys.single(), book.coverFileName)
        assertContentEquals(cover, covers.values.single())
        assertEquals(book.coverFileName, stored.single().coverFileName)

        viewModel.deleteBook("book-1")
        assertTrue(covers.isEmpty())
        assertTrue(stored.isEmpty())
    }

    @Test
    fun externalOpenShowsLibraryThenOpensReaderAfterImport() = runTest {
        val viewModel = AppViewModel(
            bookRepository = memoryBookRepository(),
            readingSessionRepository = memoryReadingSessionRepository(),
        )

        viewModel.onExternalBookOpenStarted()
        assertTrue(viewModel.uiState.value.isImportingExternalBook)
        assertNull(viewModel.uiState.value.pendingReaderBookId)

        val message = async { viewModel.messages.first() }
        testScheduler.runCurrent()
        viewModel.upsertImportedBook(
            ImportedBook(id = "content://books/war.epub", title = "War.epub", content = "chapter"),
            openInReader = true,
        )

        val state = viewModel.uiState.value
        assertEquals("content://books/war.epub", state.selectedBookId)
        assertEquals("content://books/war.epub", state.pendingReaderBookId)
        assertFalse(state.isImportingExternalBook)
        assertEquals("Imported War", message.await())

        viewModel.consumePendingReaderNavigation()
        assertNull(viewModel.uiState.value.pendingReaderBookId)
        assertEquals("content://books/war.epub", viewModel.uiState.value.selectedBookId)
    }

    @Test
    fun pickerImportDoesNotOpenReader() = runTest {
        val viewModel = AppViewModel(
            bookRepository = memoryBookRepository(),
            readingSessionRepository = memoryReadingSessionRepository(),
        )
        viewModel.upsertImportedBook(
            ImportedBook(id = "book-1", title = "notes.txt", content = "one"),
        )
        assertNull(viewModel.uiState.value.pendingReaderBookId)
    }

    @Test
    fun importErrorClearsExternalImportingState() = runTest {
        val viewModel = AppViewModel(
            bookRepository = memoryBookRepository(),
            readingSessionRepository = memoryReadingSessionRepository(),
        )
        viewModel.onExternalBookOpenStarted()
        val message = async { viewModel.messages.first() }
        testScheduler.runCurrent()
        viewModel.onImportError("Failed to import book.")

        assertFalse(viewModel.uiState.value.isImportingExternalBook)
        assertNull(viewModel.uiState.value.pendingReaderBookId)
        assertEquals("Failed to import book.", message.await())
    }

    @Test
    fun startBookEditorStoresEditorBookId() = runTest {
        val viewModel = AppViewModel(
            bookRepository = memoryBookRepository(),
            readingSessionRepository = memoryReadingSessionRepository(),
        )
        assertNull(viewModel.uiState.value.editorBookId)

        viewModel.startBookEditor("created:draft")
        assertEquals("created:draft", viewModel.uiState.value.editorBookId)

        viewModel.startBookEditor(null)
        assertNull(viewModel.uiState.value.editorBookId)
    }

    @Test
    fun createBookPersistsTitleContentAndSelectsIt() = runTest {
        val stored = mutableListOf<Book>()
        val viewModel = AppViewModel(
            bookRepository = memoryBookRepository(stored),
            readingSessionRepository = memoryReadingSessionRepository(),
        )

        viewModel.createBook(title = "  My notes  ", content = "one two\n\nthree")

        val book = viewModel.uiState.value.books.single()
        assertTrue(book.id.startsWith("created:"))
        assertEquals("My notes", book.title)
        assertEquals("one two\n\nthree", book.content)
        assertEquals(MaterialSourceType.Book, book.sourceType)
        assertEquals(book.id, viewModel.uiState.value.selectedBookId)
        assertEquals(1, stored.size)
        assertEquals(book, stored.single())
    }

    @Test
    fun createBookIgnoresBlankContentAndDefaultsBlankTitle() = runTest {
        val viewModel = AppViewModel(
            bookRepository = memoryBookRepository(),
            readingSessionRepository = memoryReadingSessionRepository(),
        )

        viewModel.createBook(title = "Ignored", content = "   \n  ")
        assertTrue(viewModel.uiState.value.books.isEmpty())
        assertNull(viewModel.uiState.value.selectedBookId)

        viewModel.createBook(title = "  ", content = "hello")
        val book = viewModel.uiState.value.currentBook
        assertNotNull(book)
        assertEquals("Новая книга", book.title)
        assertEquals("hello", book.content)
        assertTrue(book.id.startsWith("created:"))
        assertEquals(MaterialSourceType.Book, book.sourceType)
    }

    @Test
    fun updateCreatedBookChangesContentAndIgnoresImportedIds() = runTest {
        val stored = mutableListOf<Book>()
        val viewModel = AppViewModel(
            bookRepository = memoryBookRepository(stored),
            readingSessionRepository = memoryReadingSessionRepository(),
        )
        viewModel.createBook(title = "Draft", content = "one two")
        val createdId = viewModel.uiState.value.books.single().id
        assertEquals(2, viewModel.uiState.value.books.single().wordCount)

        viewModel.updateCreatedBook(
            bookId = createdId,
            title = "  Revised  ",
            content = "one two three\n\nfour five",
        )
        val updated = viewModel.uiState.value.books.single()
        assertEquals("Revised", updated.title)
        assertEquals("one two three\n\nfour five", updated.content)
        assertEquals(5, updated.wordCount)
        assertEquals(2, updated.paragraphCount)
        assertEquals(updated, stored.single())

        viewModel.upsertImportedBook(
            ImportedBook(id = "imported-1", title = "novel.epub", content = "chapter one"),
        )
        viewModel.updateCreatedBook("imported-1", title = "Hacked", content = "changed")
        val imported = viewModel.uiState.value.books.first { it.id == "imported-1" }
        assertEquals("novel.epub", imported.title)
        assertEquals("chapter one", imported.content)
    }
}
