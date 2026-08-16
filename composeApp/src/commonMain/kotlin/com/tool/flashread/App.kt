package com.tool.flashread

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.tool.flashread.core.model.Book
import com.tool.flashread.core.model.ReadingPosition
import com.tool.flashread.core.speedread.splitBookParagraphs
import com.tool.flashread.data.repository.ReadingSessionRepository
import com.tool.flashread.data.repository.SpeedReadSettingsRepository
import com.tool.flashread.navigation.AppRoute
import com.tool.flashread.navigation.AppScreen
import com.tool.flashread.platform.BookStorage
import com.tool.flashread.ui.speedread.SpeedReadPlayerScreen
import com.tool.flashread.ui.speedread.SpeedReadSetupScreen
import com.tool.flashread.platform.ImportedBook
import com.tool.flashread.platform.rememberBookImportLauncher
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun App() {
    MaterialTheme {
        val readingSessionRepository = remember { ReadingSessionRepository() }
        val speedReadSettingsRepository = remember { SpeedReadSettingsRepository() }
        val books = remember {
            mutableStateListOf<Book>().apply {
                addAll(BookStorage.loadBooks())
            }
        }
        var selectedBookId by rememberSaveable { mutableStateOf<String?>(null) }
        val currentBook by remember(selectedBookId, books) {
            derivedStateOf { books.firstOrNull { it.id == selectedBookId } }
        }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        fun upsertBook(importedBook: ImportedBook) {
            val book = Book(
                id = importedBook.id,
                title = importedBook.title,
                content = importedBook.content,
            )
            val index = books.indexOfFirst { it.id == importedBook.id }
            if (index == -1) {
                books.add(book)
            } else {
                books[index] = book
            }
            BookStorage.saveBooks(books.toList())
        }

        fun deleteBook(bookId: String) {
            val index = books.indexOfFirst { it.id == bookId }
            if (index == -1) return
            val deletedTitle = books[index].title
            books.removeAt(index)
            BookStorage.saveBooks(books.toList())
            if (selectedBookId == bookId) {
                selectedBookId = null
            }
            scope.launch {
                snackbarHostState.showSnackbar("Deleted $deletedTitle")
            }
        }

        val launchBookImport = rememberBookImportLauncher(
            onImported = { importedBook ->
                upsertBook(importedBook)
                selectedBookId = importedBook.id
                scope.launch {
                    snackbarHostState.showSnackbar("Imported ${importedBook.title}")
                }
            },
            onError = { message ->
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            },
        )

        val backStack = remember { mutableStateListOf<AppRoute>(AppRoute.Library) }
        val currentRoute = backStack.lastOrNull() ?: AppRoute.Library
        val currentScreen = AppScreen.fromRoute(currentRoute)

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(currentScreen.title) },
                )
            },
            bottomBar = {
                NavigationBar {
                    AppScreen.entries.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = {
                                navigateToTopLevel(backStack, screen.route)
                            },
                            label = { Text(screen.title) },
                            icon = {},
                        )
                    }
                }
            },
        ) { innerPadding ->
            NavDisplay(
                modifier = Modifier.padding(innerPadding),
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                predictivePopTransitionSpec = { _ ->
                    EnterTransition.None togetherWith ExitTransition.None
                },
                entryProvider = entryProvider {
                    entry<AppRoute.Library> {
                        LibraryScreen(
                            books = books,
                            readingSessionRepository = readingSessionRepository,
                            onImportBook = launchBookImport,
                            onDeleteBook = ::deleteBook,
                            onOpenReader = { bookId ->
                                selectedBookId = bookId
                                pushIfNeeded(backStack, AppRoute.Reader)
                            },
                            onOpenSpeedRead = { pushIfNeeded(backStack, AppRoute.SpeedRead) },
                        )
                    }
                    entry<AppRoute.Reader> {
                        ReaderScreen(
                            book = currentBook,
                            readingSessionRepository = readingSessionRepository,
                            onOpenSpeedRead = { pushIfNeeded(backStack, AppRoute.SpeedRead) },
                        )
                    }
                    entry<AppRoute.SpeedRead> {
                        SpeedReadSetupScreen(
                            book = currentBook,
                            settingsRepository = speedReadSettingsRepository,
                            onContinue = { pushIfNeeded(backStack, AppRoute.SpeedReadPlayer) },
                        )
                    }
                    entry<AppRoute.SpeedReadPlayer> {
                        val book = currentBook
                        if (book == null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("Pick a book in Library first.")
                            }
                        } else {
                            val settings = remember { speedReadSettingsRepository.load() }
                            val startParagraphIndex = remember(book.id) {
                                readingSessionRepository.getPosition(book.id).paragraphIndex
                            }
                            SpeedReadPlayerScreen(
                                book = book,
                                settings = settings,
                                startParagraphIndex = startParagraphIndex,
                                onParagraphIndexChanged = { paragraphIndex ->
                                    readingSessionRepository.savePosition(
                                        ReadingPosition(
                                            bookId = book.id,
                                            paragraphIndex = paragraphIndex,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                    entry<AppRoute.Settings> {
                        SettingsScreen()
                    }
                },
            )
        }
    }
}

private fun navigateToTopLevel(
    backStack: MutableList<AppRoute>,
    route: AppRoute,
) {
    if (backStack.singleOrNull() == route) return
    backStack.clear()
    backStack.add(route)
}

private fun pushIfNeeded(
    backStack: MutableList<AppRoute>,
    route: AppRoute,
) {
    if (backStack.lastOrNull() != route) {
        backStack.add(route)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LibraryScreen(
    modifier: Modifier = Modifier,
    books: List<Book>,
    readingSessionRepository: ReadingSessionRepository,
    onImportBook: () -> Unit,
    onDeleteBook: (String) -> Unit,
    onOpenReader: (String) -> Unit,
    onOpenSpeedRead: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onImportBook) {
                Text("Import book")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = books.isNotEmpty(),
                onClick = { onOpenReader(books.first().id) },
            ) {
                Text("Open Reader")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onOpenSpeedRead) {
                Text("SpeedRead")
            }
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        if (books.isEmpty()) {
            Text("No books yet. Import a .txt file to start reading.")
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(books, key = { it.id }) { book ->
                val paragraphs = remember(book.content) { splitBookParagraphs(book.content) }
                val position = readingSessionRepository.getPosition(book.id).paragraphIndex
                val progress = if (paragraphs.isEmpty()) 0 else ((position * 100) / paragraphs.size).coerceIn(0, 100)
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onDeleteBook(book.id)
                            true
                        } else {
                            false
                        }
                    },
                )

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Text(
                                text = "Delete",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onOpenReader(book.id) }
                            .padding(vertical = 10.dp),
                    ) {
                        Text(text = book.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Progress: $progress%",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ReaderScreen(
    modifier: Modifier = Modifier,
    book: Book?,
    readingSessionRepository: ReadingSessionRepository,
    onOpenSpeedRead: () -> Unit,
) {
    if (book == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Pick a book in Library first.")
        }
        return
    }

    val paragraphs = remember(book.content) { splitBookParagraphs(book.content) }
    val initialPosition = remember(book.id) { readingSessionRepository.getPosition(book.id).paragraphIndex }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialPosition.coerceIn(0, paragraphs.lastIndex.coerceAtLeast(0)),
    )

    LaunchedEffect(book.id, listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { it.coerceAtLeast(0) }
            .distinctUntilChanged()
            .collect { paragraphIndex ->
                readingSessionRepository.savePosition(
                    ReadingPosition(
                        bookId = book.id,
                        paragraphIndex = paragraphIndex,
                    ),
                )
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = book.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        Button(onClick = onOpenSpeedRead) {
            Text("Switch to SpeedRead")
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
        ) {
            items(paragraphs) { paragraph ->
                Text(
                    text = paragraph,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Settings")
    }
}