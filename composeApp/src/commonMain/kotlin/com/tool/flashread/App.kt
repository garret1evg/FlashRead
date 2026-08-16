package com.tool.flashread

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.tool.flashread.core.model.Book
import com.tool.flashread.core.model.MaterialSourceType
import com.tool.flashread.core.model.ReadingPosition
import com.tool.flashread.core.reading.bookProgressPercent
import com.tool.flashread.core.speedread.splitBookParagraphs
import com.tool.flashread.data.repository.ReadingSessionRepository
import com.tool.flashread.data.repository.SpeedReadSettingsRepository
import com.tool.flashread.navigation.AppRoute
import com.tool.flashread.navigation.AppScreen
import com.tool.flashread.navigation.isTopLevel
import com.tool.flashread.navigation.navigateToTopLevel
import com.tool.flashread.navigation.popBack
import com.tool.flashread.navigation.pushIfNeeded
import com.tool.flashread.navigation.title
import com.tool.flashread.platform.BookStorage
import com.tool.flashread.platform.ImportedBook
import com.tool.flashread.platform.rememberBookImportLauncher
import com.tool.flashread.ui.library.LibraryScreen
import com.tool.flashread.ui.library.MaterialTitleFormatter
import com.tool.flashread.ui.speedread.SpeedReadPlayerScreen
import com.tool.flashread.ui.speedread.SpeedReadSetupScreen
import com.tool.flashread.ui.theme.FlashReadDimens
import com.tool.flashread.ui.theme.FlashReadShapes
import com.tool.flashread.ui.theme.FlashReadTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun App() {
    FlashReadTheme {
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

        fun persistBooks() {
            BookStorage.saveBooks(books.toList())
        }

        fun upsertBook(importedBook: ImportedBook) {
            val book = Book(
                id = importedBook.id,
                title = importedBook.title,
                content = importedBook.content,
                sourceType = MaterialSourceType.Book,
            )
            val index = books.indexOfFirst { it.id == importedBook.id }
            if (index == -1) {
                books.add(book)
            } else {
                books[index] = book
            }
            persistBooks()
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
            )
            val index = books.indexOfFirst { it.id == book.id }
            if (index == -1) {
                books.add(book)
            } else {
                books[index] = book
            }
            persistBooks()
            selectedBookId = book.id
            scope.launch {
                snackbarHostState.showSnackbar("Added ${MaterialTitleFormatter.displayTitle(resolvedTitle)}")
            }
        }

        fun renameBook(bookId: String, newTitle: String) {
            val index = books.indexOfFirst { it.id == bookId }
            if (index == -1) return
            val trimmed = newTitle.trim()
            if (trimmed.isBlank()) return
            books[index] = books[index].copy(title = trimmed)
            persistBooks()
        }

        fun deleteBook(bookId: String) {
            val index = books.indexOfFirst { it.id == bookId }
            if (index == -1) return
            val deletedTitle = books[index].title
            books.removeAt(index)
            persistBooks()
            if (selectedBookId == bookId) {
                selectedBookId = null
            }
            scope.launch {
                snackbarHostState.showSnackbar("Deleted ${MaterialTitleFormatter.displayTitle(deletedTitle)}")
            }
        }

        val launchBookImport = rememberBookImportLauncher(
            onImported = { importedBook ->
                upsertBook(importedBook)
                selectedBookId = importedBook.id
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Imported ${MaterialTitleFormatter.displayTitle(importedBook.title)}",
                    )
                }
            },
            onError = { message ->
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            },
        )

        val backStack = remember { mutableStateListOf<AppRoute>(AppRoute.Home) }
        val currentRoute = backStack.lastOrNull() ?: AppRoute.Home
        val currentScreen = AppScreen.fromRoute(currentRoute)
        val showBottomBar = currentRoute.isTopLevel
        val showTopBar = !currentRoute.isTopLevel && currentRoute !is AppRoute.SpeedReadPlayer

        fun openReader(bookId: String) {
            selectedBookId = bookId
            backStack.pushIfNeeded(AppRoute.Reader)
        }

        fun redirectToLibraryIfNoBook() {
            backStack.navigateToTopLevel(AppRoute.Library)
        }

        fun progressFor(book: Book): Int {
            return bookProgressPercent(
                content = book.content,
                paragraphIndex = readingSessionRepository.getPosition(book.id).paragraphIndex,
            )
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (showTopBar) {
                    TopAppBar(
                        title = {
                            Text(
                                text = currentRoute.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        navigationIcon = {
                            if (!currentRoute.isTopLevel) {
                                IconButton(onClick = { backStack.popBack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                    )
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        AppScreen.entries.forEach { screen ->
                            val selected = currentScreen == screen
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    backStack.navigateToTopLevel(screen.route)
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon(selected),
                                        contentDescription = screen.title,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavDisplay(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                backStack = backStack,
                onBack = { backStack.popBack() },
                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                predictivePopTransitionSpec = { _ ->
                    EnterTransition.None togetherWith ExitTransition.None
                },
                entryProvider = entryProvider {
                    entry<AppRoute.Home> {
                        HomeScreen(
                            book = currentBook,
                            progressPercent = currentBook?.let(::progressFor) ?: 0,
                            onImportBook = launchBookImport,
                            onContinueReading = { bookId -> openReader(bookId) },
                        )
                    }
                    entry<AppRoute.Library> {
                        LibraryScreen(
                            books = books,
                            progressPercent = { progressFor(it) },
                            onImportBook = launchBookImport,
                            onAddYouTubeVideo = ::addYouTubeVideo,
                            onRenameBook = ::renameBook,
                            onDeleteBook = ::deleteBook,
                            onContinueReading = { bookId -> openReader(bookId) },
                        )
                    }
                    entry<AppRoute.Reader> {
                        SelectedBookRoute(
                            book = currentBook,
                            onMissingBook = ::redirectToLibraryIfNoBook,
                        ) { book ->
                            ReaderScreen(
                                book = book,
                                readingSessionRepository = readingSessionRepository,
                                onOpenSpeedRead = { backStack.pushIfNeeded(AppRoute.SpeedRead) },
                            )
                        }
                    }
                    entry<AppRoute.SpeedRead> {
                        SelectedBookRoute(
                            book = currentBook,
                            onMissingBook = ::redirectToLibraryIfNoBook,
                        ) { book ->
                            SpeedReadSetupScreen(
                                book = book,
                                settingsRepository = speedReadSettingsRepository,
                                onContinue = { backStack.pushIfNeeded(AppRoute.SpeedReadPlayer) },
                            )
                        }
                    }
                    entry<AppRoute.SpeedReadPlayer> {
                        SelectedBookRoute(
                            book = currentBook,
                            onMissingBook = ::redirectToLibraryIfNoBook,
                        ) { book ->
                            val settings = remember { speedReadSettingsRepository.load() }
                            val startParagraphIndex = remember(book.id) {
                                readingSessionRepository.getPosition(book.id).paragraphIndex
                            }
                            Box(Modifier.fillMaxSize()) {
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
                                IconButton(
                                    onClick = { backStack.popBack() },
                                    modifier = Modifier.align(Alignment.TopStart),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                    )
                                }
                            }
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

private fun AppScreen.icon(selected: Boolean): ImageVector = when (this) {
    AppScreen.Home -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    AppScreen.Library -> if (selected) {
        Icons.AutoMirrored.Filled.MenuBook
    } else {
        Icons.AutoMirrored.Outlined.MenuBook
    }
    AppScreen.Settings -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
}

@Composable
private fun SelectedBookRoute(
    book: Book?,
    onMissingBook: () -> Unit,
    content: @Composable (Book) -> Unit,
) {
    if (book == null) {
        LaunchedEffect(Unit) {
            onMissingBook()
        }
        return
    }
    content(book)
}

@Composable
private fun HomeScreen(
    book: Book?,
    progressPercent: Int,
    onImportBook: () -> Unit,
    onContinueReading: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (book == null) {
        EmptyBookState(
            modifier = modifier,
            onImportBook = onImportBook,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = FlashReadDimens.screenHorizontalPadding)
            .padding(top = FlashReadDimens.space8),
    ) {
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(FlashReadDimens.space16))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = FlashReadShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FlashReadDimens.space16),
            ) {
                Text(
                    text = "Continue reading",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(FlashReadDimens.space8))
                Text(
                    text = MaterialTitleFormatter.displayTitle(book.title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(FlashReadDimens.space8))
                Text(
                    text = "Progress: $progressPercent%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(FlashReadDimens.space16))
                Button(
                    onClick = { onContinueReading(book.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = FlashReadDimens.minTouchTarget),
                    shape = FlashReadShapes.button,
                ) {
                    Text(
                        text = "Open Reader",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBookState(
    onImportBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = FlashReadDimens.screenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No book selected",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FlashReadDimens.space8))
        Text(
            text = "Import a .txt file or pick one in Library.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FlashReadDimens.space16))
        Button(
            onClick = onImportBook,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FlashReadDimens.minTouchTarget),
            shape = FlashReadShapes.button,
        ) {
            Text(
                text = "Import book",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ReaderScreen(
    book: Book,
    readingSessionRepository: ReadingSessionRepository,
    onOpenSpeedRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            .padding(horizontal = FlashReadDimens.screenHorizontalPadding),
    ) {
        Text(
            text = MaterialTitleFormatter.displayTitle(book.title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = FlashReadDimens.space12),
        )
        Button(
            onClick = onOpenSpeedRead,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FlashReadDimens.minTouchTarget),
            shape = FlashReadShapes.button,
            contentPadding = PaddingValues(horizontal = FlashReadDimens.space16),
        ) {
            Text(
                text = "Switch to SpeedRead",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = FlashReadDimens.space12),
            color = MaterialTheme.colorScheme.outline,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
        ) {
            items(paragraphs) { paragraph ->
                Text(
                    text = paragraph,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = FlashReadDimens.space12),
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
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = FlashReadDimens.screenHorizontalPadding)
            .padding(top = FlashReadDimens.space8),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(FlashReadDimens.space12))
        Text(
            text = "Reading preferences will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
