package com.evgeniich.flashread

import com.evgeniich.flashread.core.locale.AppLanguage
import com.evgeniich.flashread.core.model.Book
import com.evgeniich.flashread.core.model.MaterialSourceType
import com.evgeniich.flashread.core.model.ReadingPosition
import com.evgeniich.flashread.core.youtube.YouTubeCaptionTracks
import com.evgeniich.flashread.core.youtube.YouTubeTranscript
import com.evgeniich.flashread.core.youtube.YouTubeTranscriptException
import com.evgeniich.flashread.core.youtube.YouTubeTranscriptFailureKind
import com.evgeniich.flashread.core.youtube.YouTubeTranscriptFetcher
import com.evgeniich.flashread.data.repository.AppLanguageRepository
import com.evgeniich.flashread.data.repository.BookRepository
import com.evgeniich.flashread.data.repository.CoverRepository
import com.evgeniich.flashread.data.repository.ReadingSessionRepository
import com.evgeniich.flashread.data.repository.RecentBookRepository
import com.evgeniich.flashread.platform.ImportedBook
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun upsertImportedBookSelectsAndPersistsIt() = runTest {
        val stored = mutableListOf<Book>()
        val viewModel = appViewModel(
            bookRepository = memoryBookRepository(stored),
            readingSessionRepository = memoryReadingSessionRepository(),
            recentBookRepository = memoryRecentBookRepository(),
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
        assertEquals(AppMessage.Imported("notes"), message.await())
    }

    @Test
    fun addYouTubeVideoIgnoresBlankUrl() = runTest {
        val fetcher = FakeYouTubeTranscriptFetcher()
        val viewModel = appViewModel(youTubeTranscriptFetcher = fetcher)
        val pending = async { viewModel.messages.first() }
        testScheduler.runCurrent()

        viewModel.addYouTubeVideo(title = "Ignored", url = "   ")

        assertTrue(viewModel.uiState.value.books.isEmpty())
        assertFalse(viewModel.uiState.value.isFetchingYouTubeTranscript)
        assertTrue(fetcher.recordedVideoIds.isEmpty())
        assertFalse(pending.isCompleted)
        pending.cancel()
    }

    @Test
    fun addYouTubeVideoRejectsInvalidAndNonYouTubeUrls() = runTest {
        val fetcher = FakeYouTubeTranscriptFetcher()
        val viewModel = appViewModel(youTubeTranscriptFetcher = fetcher)

        val invalid = async { viewModel.messages.first() }
        testScheduler.runCurrent()
        viewModel.addYouTubeVideo(title = "Ignored", url = "https://youtu.be/abc")
        assertEquals(
            AppMessage.YouTubeTranscriptFailed(YouTubeTranscriptFailureKind.InvalidLink),
            invalid.await(),
        )
        assertTrue(viewModel.uiState.value.books.isEmpty())

        val nonYouTube = async { viewModel.messages.first() }
        testScheduler.runCurrent()
        viewModel.addYouTubeVideo(title = "Ignored", url = "https://example.com/watch")
        assertEquals(
            AppMessage.YouTubeTranscriptFailed(YouTubeTranscriptFailureKind.InvalidLink),
            nonYouTube.await(),
        )
        assertTrue(viewModel.uiState.value.books.isEmpty())
        assertTrue(fetcher.recordedVideoIds.isEmpty())
        assertFalse(viewModel.uiState.value.isFetchingYouTubeTranscript)
    }

    @Test
    fun addYouTubeVideoStoresTranscriptAndUpsertsByVideoId() = runTest {
        val stored = mutableListOf<Book>()
        val fetcher = FakeYouTubeTranscriptFetcher()
        val viewModel = appViewModel(
            bookRepository = memoryBookRepository(stored),
            youTubeTranscriptFetcher = fetcher,
        )

        val added = async { viewModel.messages.first() }
        testScheduler.runCurrent()
        viewModel.addYouTubeVideo(title = "My video", url = "https://youtu.be/dQw4w9WgXcQ")
        assertEquals(AppMessage.Added("My video"), added.await())

        val book = viewModel.uiState.value.currentBook
        assertEquals("youtube:dQw4w9WgXcQ", book?.id)
        assertEquals("My video", book?.title)
        assertEquals("Never gonna give you up", book?.content)
        assertEquals(MaterialSourceType.YouTube, book?.sourceType)
        assertEquals("youtube:dQw4w9WgXcQ", viewModel.uiState.value.selectedBookId)
        assertEquals(1, stored.size)

        val upserted = async { viewModel.messages.first() }
        testScheduler.runCurrent()
        viewModel.addYouTubeVideo(
            title = "Same video",
            url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        )
        assertEquals(AppMessage.Added("Same video"), upserted.await())
        assertEquals(1, viewModel.uiState.value.books.size)
        assertEquals("Same video", viewModel.uiState.value.books.single().title)
        assertEquals("youtube:dQw4w9WgXcQ", viewModel.uiState.value.books.single().id)
        assertEquals(listOf("dQw4w9WgXcQ", "dQw4w9WgXcQ"), fetcher.recordedVideoIds)
        assertFalse(viewModel.uiState.value.isFetchingYouTubeTranscript)
    }

    @Test
    fun addYouTubeVideoFallsBackToTranscriptTitleThenVideoId() = runTest {
        var transcriptTitle: String? = "From captions"
        val fetcher = FakeYouTubeTranscriptFetcher { videoId, _ ->
            YouTubeTranscript(
                videoId = videoId,
                text = "caption text",
                title = transcriptTitle,
            )
        }
        val viewModel = appViewModel(youTubeTranscriptFetcher = fetcher)

        viewModel.addYouTubeVideo(title = "  User title  ", url = "https://youtu.be/dQw4w9WgXcQ")
        assertEquals("User title", viewModel.uiState.value.currentBook?.title)

        viewModel.addYouTubeVideo(title = "  ", url = "https://youtu.be/dQw4w9WgXcQ")
        assertEquals("From captions", viewModel.uiState.value.currentBook?.title)

        transcriptTitle = "  "
        viewModel.addYouTubeVideo(title = "", url = "https://youtu.be/dQw4w9WgXcQ")
        assertEquals("dQw4w9WgXcQ", viewModel.uiState.value.currentBook?.title)

        transcriptTitle = null
        viewModel.addYouTubeVideo(title = "   ", url = "https://youtu.be/dQw4w9WgXcQ")
        assertEquals("dQw4w9WgXcQ", viewModel.uiState.value.currentBook?.title)
        assertEquals(1, viewModel.uiState.value.books.size)
    }

    @Test
    fun addYouTubeVideoReportsFetcherFailureWithoutAddingABook() = runTest {
        val fetcher = FakeYouTubeTranscriptFetcher { videoId, _ ->
            throw YouTubeTranscriptException(videoId, YouTubeTranscriptFailureKind.NoTranscript)
        }
        val viewModel = appViewModel(youTubeTranscriptFetcher = fetcher)
        val message = async { viewModel.messages.first() }
        testScheduler.runCurrent()

        viewModel.addYouTubeVideo(title = "Video", url = "https://youtu.be/dQw4w9WgXcQ")

        assertEquals(
            AppMessage.YouTubeTranscriptFailed(YouTubeTranscriptFailureKind.NoTranscript),
            message.await(),
        )
        assertTrue(viewModel.uiState.value.books.isEmpty())
        assertFalse(viewModel.uiState.value.isFetchingYouTubeTranscript)
    }

    @Test
    fun addYouTubeVideoMapsUnknownErrorsToGenericFailure() = runTest {
        val fetcher = FakeYouTubeTranscriptFetcher { _, _ ->
            throw IllegalStateException("boom")
        }
        val viewModel = appViewModel(youTubeTranscriptFetcher = fetcher)
        val message = async { viewModel.messages.first() }
        testScheduler.runCurrent()

        viewModel.addYouTubeVideo(title = "Video", url = "https://youtu.be/dQw4w9WgXcQ")

        assertEquals(
            AppMessage.YouTubeTranscriptFailed(YouTubeTranscriptFailureKind.Generic),
            message.await(),
        )
        assertTrue(viewModel.uiState.value.books.isEmpty())
        assertFalse(viewModel.uiState.value.isFetchingYouTubeTranscript)
    }

    @Test
    fun addYouTubeVideoSetsFetchingFlagWhileFetcherIsSuspended() = runTest {
        val gate = CompletableDeferred<YouTubeTranscript>()
        val viewModel = appViewModel(
            youTubeTranscriptFetcher = FakeYouTubeTranscriptFetcher { videoId, _ ->
                gate.await().copy(videoId = videoId)
            },
        )

        viewModel.addYouTubeVideo(title = "Video", url = "https://youtu.be/dQw4w9WgXcQ")
        assertTrue(viewModel.uiState.value.isFetchingYouTubeTranscript)
        assertTrue(viewModel.uiState.value.books.isEmpty())

        gate.complete(
            YouTubeTranscript(
                videoId = "dQw4w9WgXcQ",
                text = "Never gonna give you up",
                title = "Rick Astley",
            ),
        )
        assertFalse(viewModel.uiState.value.isFetchingYouTubeTranscript)
        assertEquals("youtube:dQw4w9WgXcQ", viewModel.uiState.value.currentBook?.id)

        val failGate = CompletableDeferred<YouTubeTranscript>()
        val failing = appViewModel(
            youTubeTranscriptFetcher = FakeYouTubeTranscriptFetcher { _, _ -> failGate.await() },
        )
        failing.addYouTubeVideo(title = "Video", url = "https://youtu.be/dQw4w9WgXcQ")
        assertTrue(failing.uiState.value.isFetchingYouTubeTranscript)
        failGate.completeExceptionally(
            YouTubeTranscriptException("dQw4w9WgXcQ", YouTubeTranscriptFailureKind.NoTranscript),
        )
        assertFalse(failing.uiState.value.isFetchingYouTubeTranscript)
        assertTrue(failing.uiState.value.books.isEmpty())
    }

    @Test
    fun addYouTubeVideoUsesAppLanguageThenEnglishForCaptions() = runTest {
        val germanApp = FakeYouTubeTranscriptFetcher()
        appViewModel(
            youTubeTranscriptFetcher = germanApp,
            appLanguageRepository = memoryAppLanguageRepository(
                arrayOf(AppLanguage.Language("de")),
            ),
        ).addYouTubeVideo(title = "Video", url = "https://youtu.be/dQw4w9WgXcQ")
        assertEquals(listOf("de", "en"), germanApp.recordedLanguages.single())

        val germanSystem = FakeYouTubeTranscriptFetcher()
        appViewModel(
            youTubeTranscriptFetcher = germanSystem,
            appLanguageRepository = memoryAppLanguageRepository(arrayOf(AppLanguage.System)),
            systemLanguageTag = { "de-DE" },
        ).addYouTubeVideo(title = "Video", url = "https://youtu.be/dQw4w9WgXcQ")
        assertEquals(listOf("de", "en"), germanSystem.recordedLanguages.single())
        assertEquals(
            YouTubeCaptionTracks.languagePriority("de"),
            germanSystem.recordedLanguages.single(),
        )
    }

    @Test
    fun renameAndDeleteUpdateLibraryAndClearSelection() = runTest {
        val viewModel = appViewModel(
            bookRepository = memoryBookRepository(),
            readingSessionRepository = memoryReadingSessionRepository(),
            recentBookRepository = memoryRecentBookRepository(),
        )
        val deleted = async { viewModel.messages.first { it is AppMessage.Deleted } }
        testScheduler.runCurrent()
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
        assertEquals(AppMessage.Deleted("Gone"), deleted.await())

        viewModel.selectBook("keep")
        viewModel.deleteBook("keep")
        assertTrue(viewModel.uiState.value.books.isEmpty())
        assertNull(viewModel.uiState.value.selectedBookId)
    }

    @Test
    fun upsertImportedBookPersistsCoverAndDeletesItWithTheBook() = runTest {
        val stored = mutableListOf<Book>()
        val covers = mutableMapOf<String, ByteArray>()
        val viewModel = appViewModel(
            bookRepository = memoryBookRepository(stored),
            readingSessionRepository = memoryReadingSessionRepository(),
            coverRepository = memoryCoverRepository(covers),
            recentBookRepository = memoryRecentBookRepository(),
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
        val viewModel = appViewModel(
            bookRepository = memoryBookRepository(),
            readingSessionRepository = memoryReadingSessionRepository(),
            recentBookRepository = memoryRecentBookRepository(),
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
        assertEquals(AppMessage.Imported("War"), message.await())

        viewModel.consumePendingReaderNavigation()
        assertNull(viewModel.uiState.value.pendingReaderBookId)
        assertEquals("content://books/war.epub", viewModel.uiState.value.selectedBookId)
    }

    @Test
    fun pickerImportDoesNotOpenReader() = runTest {
        val viewModel = appViewModel(
            bookRepository = memoryBookRepository(),
            readingSessionRepository = memoryReadingSessionRepository(),
            recentBookRepository = memoryRecentBookRepository(),
        )
        viewModel.upsertImportedBook(
            ImportedBook(id = "book-1", title = "notes.txt", content = "one"),
        )
        assertNull(viewModel.uiState.value.pendingReaderBookId)
    }

    @Test
    fun importErrorClearsExternalImportingState() = runTest {
        val viewModel = appViewModel(
            bookRepository = memoryBookRepository(),
            readingSessionRepository = memoryReadingSessionRepository(),
            recentBookRepository = memoryRecentBookRepository(),
        )
        viewModel.onExternalBookOpenStarted()
        val message = async { viewModel.messages.first() }
        testScheduler.runCurrent()
        viewModel.onImportError("Failed to import book.")

        assertFalse(viewModel.uiState.value.isImportingExternalBook)
        assertNull(viewModel.uiState.value.pendingReaderBookId)
        assertEquals(AppMessage.Error("Failed to import book."), message.await())
    }

    @Test
    fun startBookEditorStoresEditorBookId() = runTest {
        val viewModel = appViewModel(
            bookRepository = memoryBookRepository(),
            readingSessionRepository = memoryReadingSessionRepository(),
            recentBookRepository = memoryRecentBookRepository(),
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
        val viewModel = appViewModel(
            bookRepository = memoryBookRepository(stored),
            readingSessionRepository = memoryReadingSessionRepository(),
            recentBookRepository = memoryRecentBookRepository(),
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
        val viewModel = appViewModel(
            bookRepository = memoryBookRepository(),
            readingSessionRepository = memoryReadingSessionRepository(),
            recentBookRepository = memoryRecentBookRepository(),
        )

        viewModel.createBook(title = "Ignored", content = "   \n  ")
        assertTrue(viewModel.uiState.value.books.isEmpty())
        assertNull(viewModel.uiState.value.selectedBookId)

        viewModel.createBook(title = "  ", content = "hello")
        val book = viewModel.uiState.value.currentBook
        assertNotNull(book)
        assertEquals(DefaultNewBookTitle, book.title)
        assertEquals("New book", book.title)
        assertEquals("hello", book.content)
        assertTrue(book.id.startsWith("created:"))
        assertEquals(MaterialSourceType.Book, book.sourceType)

        viewModel.createBook(
            title = "  ",
            content = "bonjour",
            defaultTitle = "Nouveau livre",
        )
        assertEquals("Nouveau livre", viewModel.uiState.value.currentBook?.title)

        viewModel.createBook(title = "  ", content = "fallback", defaultTitle = "   ")
        assertEquals("New book", viewModel.uiState.value.currentBook?.title)
    }

    @Test
    fun updateCreatedBookChangesContentAndIgnoresImportedIds() = runTest {
        val stored = mutableListOf<Book>()
        val viewModel = appViewModel(
            bookRepository = memoryBookRepository(stored),
            readingSessionRepository = memoryReadingSessionRepository(),
            recentBookRepository = memoryRecentBookRepository(),
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

        viewModel.updateCreatedBook(
            bookId = createdId,
            title = "  ",
            content = "one two three four five",
        )
        assertEquals("New book", viewModel.uiState.value.books.first { it.id == createdId }.title)

        viewModel.updateCreatedBook(
            bookId = createdId,
            title = "  ",
            content = "one two three four five",
            defaultTitle = "Новая книга",
        )
        assertEquals("Новая книга", viewModel.uiState.value.books.first { it.id == createdId }.title)
    }

    @Test
    fun startScratchSpeedReadKeepsTextOutOfLibrary() = runTest {
        val stored = mutableListOf<Book>()
        val positions = mutableMapOf<String, Int>()
        val wordOffsets = mutableMapOf<String, Int>()
        val viewModel = appViewModel(
            bookRepository = memoryBookRepository(stored),
            readingSessionRepository = memoryReadingSessionRepository(positions, wordOffsets),
            recentBookRepository = memoryRecentBookRepository(),
        )
        viewModel.upsertImportedBook(
            ImportedBook(id = "book-1", title = "notes.txt", content = "library text"),
        )
        viewModel.selectBook("book-1")

        assertFalse(viewModel.startScratchSpeedRead("   \n  "))
        assertNull(viewModel.uiState.value.scratchBook)
        assertEquals("book-1", viewModel.uiState.value.selectedBookId)
        assertEquals("book-1", viewModel.uiState.value.currentBook?.id)
        assertEquals("book-1", viewModel.uiState.value.speedReadBook?.id)

        assertTrue(viewModel.startScratchSpeedRead("  paste this  \n\nnow  "))
        val state = viewModel.uiState.value
        assertEquals(ScratchSpeedReadBookId, state.scratchBook?.id)
        assertEquals(DefaultSpeedReadTitle, state.scratchBook?.title)
        assertEquals("Speed read", state.scratchBook?.title)
        assertEquals("paste this  \n\nnow", state.scratchBook?.content)
        assertEquals(MaterialSourceType.Book, state.scratchBook?.sourceType)
        assertEquals(ScratchSpeedReadBookId, state.speedReadBook?.id)
        assertEquals("book-1", state.selectedBookId)
        assertEquals("book-1", state.currentBook?.id)
        assertEquals(1, state.books.size)
        assertEquals(1, stored.size)
        assertEquals("notes.txt", stored.single().title)
        assertEquals(0, positions[ScratchSpeedReadBookId])
        assertEquals(ReadingPosition.UNSET, wordOffsets[ScratchSpeedReadBookId])
    }

    @Test
    fun scratchSpeedReadResetsPositionAndClearsWithoutTouchingLibrary() = runTest {
        val stored = mutableListOf<Book>()
        val positions = mutableMapOf(ScratchSpeedReadBookId to 4)
        val wordOffsets = mutableMapOf(ScratchSpeedReadBookId to 12)
        val viewModel = appViewModel(
            bookRepository = memoryBookRepository(stored),
            readingSessionRepository = memoryReadingSessionRepository(positions, wordOffsets),
            recentBookRepository = memoryRecentBookRepository(),
        )
        viewModel.upsertImportedBook(
            ImportedBook(id = "keep", title = "Keep.txt", content = "alpha"),
        )
        viewModel.selectBook("keep")

        assertTrue(viewModel.startScratchSpeedRead("one two three"))
        assertEquals(0, positions[ScratchSpeedReadBookId])
        assertEquals(ReadingPosition.UNSET, wordOffsets[ScratchSpeedReadBookId])
        assertEquals(ScratchSpeedReadBookId, viewModel.uiState.value.speedReadBook?.id)

        viewModel.clearScratchBook()
        assertNull(viewModel.uiState.value.scratchBook)
        assertEquals("keep", viewModel.uiState.value.speedReadBook?.id)
        assertEquals("keep", viewModel.uiState.value.selectedBookId)
        assertEquals(1, stored.size)

        viewModel.startScratchSpeedRead("fresh text")
        viewModel.selectBook("keep")
        assertNull(viewModel.uiState.value.scratchBook)
        assertEquals("keep", viewModel.uiState.value.currentBook?.id)

        assertTrue(viewModel.startScratchSpeedRead("localized", title = "Скорочтение"))
        assertEquals("Скорочтение", viewModel.uiState.value.scratchBook?.title)
        assertTrue(viewModel.startScratchSpeedRead("english fallback", title = "  "))
        assertEquals("Speed read", viewModel.uiState.value.scratchBook?.title)
    }

    @Test
    fun restoresPersistedRecentBookWhenItStillExists() = runTest {
        val stored = mutableListOf<Book>()
        val recentBookId = arrayOf<String?>(null)
        val first = appViewModel(
            bookRepository = memoryBookRepository(stored),
            readingSessionRepository = memoryReadingSessionRepository(),
            recentBookRepository = memoryRecentBookRepository(recentBookId),
        )

        first.upsertImportedBook(
            ImportedBook(id = "book-1", title = "notes.txt", content = "one two"),
        )
        assertEquals("book-1", recentBookId[0])

        val restored = appViewModel(
            bookRepository = memoryBookRepository(stored),
            readingSessionRepository = memoryReadingSessionRepository(),
            recentBookRepository = memoryRecentBookRepository(recentBookId),
        )
        assertEquals("book-1", restored.uiState.value.selectedBookId)
        assertEquals("notes.txt", restored.uiState.value.currentBook?.title)
    }

    @Test
    fun selectBookPersistsRecentBookAndDeleteClearsIt() = runTest {
        val recentBookId = arrayOf<String?>(null)
        val viewModel = appViewModel(
            bookRepository = memoryBookRepository(),
            readingSessionRepository = memoryReadingSessionRepository(),
            recentBookRepository = memoryRecentBookRepository(recentBookId),
        )
        viewModel.upsertImportedBook(
            ImportedBook(id = "keep", title = "Keep.txt", content = "alpha"),
        )
        viewModel.upsertImportedBook(
            ImportedBook(id = "gone", title = "Gone.txt", content = "beta"),
        )
        assertEquals("gone", recentBookId[0])

        viewModel.selectBook("keep")
        assertEquals("keep", recentBookId[0])

        viewModel.deleteBook("gone")
        assertEquals("keep", recentBookId[0])

        viewModel.deleteBook("keep")
        assertNull(recentBookId[0])
        assertNull(viewModel.uiState.value.selectedBookId)
    }

    @Test
    fun ignoresStaleRecentBookIdOnLoad() = runTest {
        val recentBookId = arrayOf<String?>("missing")
        val viewModel = appViewModel(
            bookRepository = memoryBookRepository(),
            readingSessionRepository = memoryReadingSessionRepository(),
            recentBookRepository = memoryRecentBookRepository(recentBookId),
        )

        assertNull(viewModel.uiState.value.selectedBookId)
        assertNull(viewModel.uiState.value.currentBook)
        assertNull(recentBookId[0])
    }

    private fun appViewModel(
        bookRepository: BookRepository = memoryBookRepository(),
        readingSessionRepository: ReadingSessionRepository = memoryReadingSessionRepository(),
        coverRepository: CoverRepository = CoverRepository(),
        recentBookRepository: RecentBookRepository = memoryRecentBookRepository(),
        youTubeTranscriptFetcher: YouTubeTranscriptFetcher = FakeYouTubeTranscriptFetcher(),
        appLanguageRepository: AppLanguageRepository = memoryAppLanguageRepository(),
        systemLanguageTag: () -> String = { "en-US" },
    ): AppViewModel {
        return AppViewModel(
            bookRepository = bookRepository,
            readingSessionRepository = readingSessionRepository,
            coverRepository = coverRepository,
            recentBookRepository = recentBookRepository,
            youTubeTranscriptFetcher = youTubeTranscriptFetcher,
            ioDispatcher = dispatcher,
            appLanguageRepository = appLanguageRepository,
            systemLanguageTag = systemLanguageTag,
        )
    }
}
