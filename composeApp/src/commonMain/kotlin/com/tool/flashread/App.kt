package com.tool.flashread

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.tool.flashread.core.model.Book
import com.tool.flashread.navigation.AppRoute
import com.tool.flashread.navigation.AppScreen
import com.tool.flashread.navigation.isTopLevel
import com.tool.flashread.navigation.navigateToTopLevel
import com.tool.flashread.navigation.openReaderFromLibrary
import com.tool.flashread.navigation.popBack
import com.tool.flashread.navigation.pushIfNeeded
import com.tool.flashread.navigation.showsScaffoldTopBar
import com.tool.flashread.navigation.title
import com.tool.flashread.platform.ObserveExternalBookOpens
import com.tool.flashread.platform.launchRouteForExternalBookOpen
import com.tool.flashread.platform.rememberBookImportLauncher
import com.tool.flashread.ui.components.AppLogo
import com.tool.flashread.ui.components.ScreenTitle
import com.tool.flashread.ui.library.BookEditorScreen
import com.tool.flashread.ui.library.LibraryScreen
import com.tool.flashread.ui.library.MaterialTitleFormatter
import com.tool.flashread.ui.reader.ReaderScreen
import com.tool.flashread.ui.settings.LegalDocumentScreen
import com.tool.flashread.ui.settings.LegalDocuments
import com.tool.flashread.ui.settings.SettingsScreen
import com.tool.flashread.ui.speedread.QuickSpeedReadScreen
import com.tool.flashread.ui.speedread.SpeedReadPlayerScreen
import com.tool.flashread.ui.speedread.SpeedReadSetupScreen
import com.tool.flashread.ui.theme.FlashReadDimens
import com.tool.flashread.ui.theme.FlashReadShapes
import com.tool.flashread.ui.theme.FlashReadTheme

@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun App() {
    FlashReadTheme {
        val appViewModel: AppViewModel = viewModel { AppViewModel() }
        val uiState by appViewModel.uiState.collectAsStateWithLifecycle()
        val currentBook = uiState.currentBook
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(appViewModel) {
            appViewModel.messages.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }

        val launchBookImport = rememberBookImportLauncher(
            onImported = appViewModel::upsertImportedBook,
            onError = appViewModel::onImportError,
        )

        val backStack = remember {
            mutableStateListOf(launchRouteForExternalBookOpen() ?: AppRoute.Home)
        }
        val currentRoute = backStack.lastOrNull() ?: AppRoute.Home
        val currentScreen = AppScreen.fromRoute(currentRoute)
        val showBottomBar = currentRoute.isTopLevel
        val showTopBar = currentRoute.showsScaffoldTopBar

        fun openReader(bookId: String) {
            appViewModel.selectBook(bookId)
            backStack.pushIfNeeded(AppRoute.Reader)
        }

        fun openBookEditor(bookId: String?) {
            appViewModel.startBookEditor(bookId)
            backStack.pushIfNeeded(AppRoute.BookEditor)
        }

        fun openQuickSpeedRead() {
            backStack.pushIfNeeded(AppRoute.QuickSpeedRead)
        }

        fun startScratchSpeedRead(content: String) {
            if (!appViewModel.startScratchSpeedRead(content)) return
            backStack.pushIfNeeded(AppRoute.SpeedRead)
        }

        fun redirectToLibraryIfNoBook() {
            backStack.navigateToTopLevel(AppRoute.Library)
        }

        ObserveExternalBookOpens(
            onOpenStarted = {
                appViewModel.onExternalBookOpenStarted()
                backStack.navigateToTopLevel(AppRoute.Library)
            },
            onImported = { imported ->
                appViewModel.upsertImportedBook(imported, openInReader = true)
            },
            onError = appViewModel::onImportError,
        )

        val pendingReaderBookId = uiState.pendingReaderBookId
        LaunchedEffect(pendingReaderBookId) {
            val bookId = pendingReaderBookId ?: return@LaunchedEffect
            appViewModel.selectBook(bookId)
            backStack.openReaderFromLibrary()
            appViewModel.consumePendingReaderNavigation()
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
                    .then(
                        if (currentRoute is AppRoute.SpeedReadPlayer) {
                            Modifier
                        } else {
                            Modifier.padding(innerPadding)
                        },
                    ),
                backStack = backStack,
                onBack = {
                    if (backStack.lastOrNull() is AppRoute.QuickSpeedRead) {
                        appViewModel.clearScratchBook()
                    }
                    backStack.popBack()
                },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                predictivePopTransitionSpec = { _ ->
                    EnterTransition.None togetherWith ExitTransition.None
                },
                entryProvider = entryProvider {
                    entry<AppRoute.Home> {
                        HomeScreen(
                            book = currentBook,
                            progressPercent = currentBook?.let(appViewModel::progressPercent) ?: 0,
                            onImportBook = launchBookImport,
                            onCreateBook = { openBookEditor(null) },
                            onSpeedReadText = ::openQuickSpeedRead,
                            onContinueReading = { bookId -> openReader(bookId) },
                        )
                    }
                    entry<AppRoute.Library> {
                        LibraryScreen(
                            books = uiState.books,
                            progressPercent = appViewModel::progressPercent,
                            onImportBook = launchBookImport,
                            onCreateBook = { openBookEditor(null) },
                            onSpeedReadText = ::openQuickSpeedRead,
                            onAddYouTubeVideo = appViewModel::addYouTubeVideo,
                            onRenameBook = appViewModel::renameBook,
                            onDeleteBook = appViewModel::deleteBook,
                            onContinueReading = { bookId -> openReader(bookId) },
                            onEditBook = { bookId -> openBookEditor(bookId) },
                            isImporting = uiState.isImportingExternalBook,
                        )
                    }
                    entry<AppRoute.Reader> {
                        SelectedBookRoute(
                            book = currentBook,
                            onMissingBook = ::redirectToLibraryIfNoBook,
                        ) { book ->
                            ReaderScreen(
                                book = book,
                                onBack = { backStack.popBack() },
                                onOpenSpeedRead = {
                                    appViewModel.clearScratchBook()
                                    backStack.pushIfNeeded(AppRoute.SpeedRead)
                                },
                                isActiveRoute = currentRoute is AppRoute.Reader,
                            )
                        }
                    }
                    entry<AppRoute.SpeedRead> {
                        SelectedBookRoute(
                            book = uiState.speedReadBook,
                            onMissingBook = ::redirectToLibraryIfNoBook,
                        ) { book ->
                            SpeedReadSetupScreen(
                                book = book,
                                onContinue = { backStack.pushIfNeeded(AppRoute.SpeedReadPlayer) },
                            )
                        }
                    }
                    entry<AppRoute.SpeedReadPlayer> {
                        SelectedBookRoute(
                            book = uiState.speedReadBook,
                            onMissingBook = ::redirectToLibraryIfNoBook,
                        ) { book ->
                            SpeedReadPlayerScreen(
                                book = book,
                                onClose = { backStack.popBack() },
                            )
                        }
                    }
                    entry<AppRoute.Settings> {
                        SettingsScreen(
                            onOpenPrivacyPolicy = { backStack.pushIfNeeded(AppRoute.PrivacyPolicy) },
                            onOpenTerms = { backStack.pushIfNeeded(AppRoute.Terms) },
                        )
                    }
                    entry<AppRoute.PrivacyPolicy> {
                        LegalDocumentScreen(document = LegalDocuments.privacyPolicy)
                    }
                    entry<AppRoute.Terms> {
                        LegalDocumentScreen(document = LegalDocuments.termsAndConditions)
                    }
                    entry<AppRoute.BookEditor> {
                        val editorBookId = uiState.editorBookId
                        val editorBook = uiState.books.firstOrNull { it.id == editorBookId }
                        BookEditorScreen(
                            initialTitle = editorBook?.title.orEmpty(),
                            initialContent = editorBook?.content.orEmpty(),
                            onBack = { backStack.popBack() },
                            onSave = { title, content ->
                                if (editorBookId == null) {
                                    appViewModel.createBook(title, content)
                                } else {
                                    appViewModel.updateCreatedBook(editorBookId, title, content)
                                }
                                backStack.popBack()
                            },
                        )
                    }
                    entry<AppRoute.QuickSpeedRead> {
                        QuickSpeedReadScreen(
                            onBack = {
                                appViewModel.clearScratchBook()
                                backStack.popBack()
                            },
                            onContinue = ::startScratchSpeedRead,
                        )
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
    onCreateBook: () -> Unit,
    onSpeedReadText: () -> Unit,
    onContinueReading: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (book == null) {
        EmptyBookState(
            modifier = modifier,
            onImportBook = onImportBook,
            onCreateBook = onCreateBook,
            onSpeedReadText = onSpeedReadText,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FlashReadDimens.screenHorizontalPadding)
            .padding(top = FlashReadDimens.space8)
            .padding(bottom = FlashReadDimens.space24),
    ) {
        ScreenTitle(title = "Home")
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
        Spacer(Modifier.height(FlashReadDimens.space16))
        HomeActionButtons(
            onImportBook = onImportBook,
            onCreateBook = onCreateBook,
            onSpeedReadText = onSpeedReadText,
        )
    }
}

@Composable
private fun EmptyBookState(
    onImportBook: () -> Unit,
    onCreateBook: () -> Unit,
    onSpeedReadText: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = FlashReadDimens.screenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppLogo(size = 80.dp)
        Spacer(Modifier.height(FlashReadDimens.space16))
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
            text = "Import a .txt, .fb2, or .epub file, write your own book, paste text for speed reading, or pick one in Library.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FlashReadDimens.space16))
        HomeActionButtons(
            onImportBook = onImportBook,
            onCreateBook = onCreateBook,
            onSpeedReadText = onSpeedReadText,
        )
    }
}

@Composable
private fun HomeActionButtons(
    onImportBook: () -> Unit,
    onCreateBook: () -> Unit,
    onSpeedReadText: () -> Unit,
) {
    HomeActionButton(text = "Import book", onClick = onImportBook)
    Spacer(Modifier.height(FlashReadDimens.space12))
    HomeActionButton(text = "Создать книгу", onClick = onCreateBook)
    Spacer(Modifier.height(FlashReadDimens.space12))
    HomeActionButton(text = "Скорочтение текста", onClick = onSpeedReadText)
}

@Composable
private fun HomeActionButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FlashReadDimens.minTouchTarget),
        shape = FlashReadShapes.button,
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
