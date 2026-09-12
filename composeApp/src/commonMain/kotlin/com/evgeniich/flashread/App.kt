package com.evgeniich.flashread

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
import com.evgeniich.flashread.ads.BannerAdHost
import com.evgeniich.flashread.ads.InterstitialAdHost
import com.evgeniich.flashread.ads.InterstitialResult
import com.evgeniich.flashread.ads.RewardedAdHost
import com.evgeniich.flashread.ads.canShowBannerAds
import com.evgeniich.flashread.ads.canShowInterstitialAds
import com.evgeniich.flashread.analytics.Analytics
import com.evgeniich.flashread.analytics.AnalyticsEvent
import com.evgeniich.flashread.consent.showPrivacyOptionsForm
import com.evgeniich.flashread.core.locale.resolveLocaleOverride
import com.evgeniich.flashread.core.model.Book
import com.evgeniich.flashread.data.repository.AppLanguageRepository
import com.evgeniich.flashread.data.repository.AppThemeRepository
import com.evgeniich.flashread.data.repository.KeepScreenOnRepository
import com.evgeniich.flashread.locale.AppEnvironment
import com.evgeniich.flashread.monetization.AdLevel
import com.evgeniich.flashread.monetization.MonetizationManager
import com.evgeniich.flashread.monetization.MonetizationPolicy
import com.evgeniich.flashread.monetization.UsageTracker
import com.evgeniich.flashread.navigation.AppRoute
import com.evgeniich.flashread.navigation.AppScreen
import com.evgeniich.flashread.navigation.instantNavContentTransform
import com.evgeniich.flashread.navigation.isTopLevel
import com.evgeniich.flashread.navigation.navigateToTopLevel
import com.evgeniich.flashread.navigation.openReaderFromLibrary
import com.evgeniich.flashread.navigation.popBack
import com.evgeniich.flashread.navigation.pushIfNeeded
import com.evgeniich.flashread.platform.ObserveExternalBookOpens
import com.evgeniich.flashread.platform.applyPlatformTheme
import com.evgeniich.flashread.platform.currentSystemLanguageTag
import com.evgeniich.flashread.platform.launchRouteForExternalBookOpen
import com.evgeniich.flashread.platform.rememberBookImportLauncher
import com.evgeniich.flashread.resources.Res
import com.evgeniich.flashread.resources.*
import com.evgeniich.flashread.ui.ads.RewardHintDialog
import com.evgeniich.flashread.ui.components.AppLogo
import com.evgeniich.flashread.ui.components.ScreenTitle
import com.evgeniich.flashread.ui.library.BookEditorScreen
import com.evgeniich.flashread.ui.library.LibraryScreen
import com.evgeniich.flashread.ui.library.MaterialTitleFormatter
import com.evgeniich.flashread.ui.reader.ReaderScreen
import com.evgeniich.flashread.ui.settings.LegalDocumentScreen
import com.evgeniich.flashread.ui.settings.LegalDocuments
import com.evgeniich.flashread.ui.settings.SettingsScreen
import com.evgeniich.flashread.ui.speedread.QuickSpeedReadScreen
import com.evgeniich.flashread.ui.speedread.SpeedReadPlayerScreen
import com.evgeniich.flashread.ui.speedread.SpeedReadSetupScreen
import com.evgeniich.flashread.ui.theme.FlashReadDimens
import com.evgeniich.flashread.ui.theme.FlashReadShapes
import com.evgeniich.flashread.ui.theme.FlashReadTheme
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun App() {
    val languageRepository = remember { AppLanguageRepository() }
    var appLanguage by remember { mutableStateOf(languageRepository.load()) }
    val themeRepository = remember { AppThemeRepository() }
    var appTheme by remember { mutableStateOf(themeRepository.load()) }
    val keepScreenOnRepository = remember { KeepScreenOnRepository() }
    var keepScreenOn by remember { mutableStateOf(keepScreenOnRepository.load()) }
    val systemLanguageTag = remember { currentSystemLanguageTag() }
    val localeOverride = resolveLocaleOverride(appLanguage, systemLanguageTag)
    val backStack = remember {
        mutableStateListOf(launchRouteForExternalBookOpen() ?: AppRoute.Home)
    }

    AppEnvironment(localeOverride) {
        FlashReadTheme(theme = appTheme) {
        val appViewModel: AppViewModel = viewModel { AppViewModel() }
        val uiState by appViewModel.uiState.collectAsStateWithLifecycle()
        val currentBook = uiState.currentBook
        val snackbarHostState = remember { SnackbarHostState() }
        val defaultNewBookTitle = stringResource(Res.string.default_new_book_title)
        val defaultSpeedReadTitle = stringResource(Res.string.default_speed_read_title)
        val quickStartTitle = stringResource(Res.string.quick_start_sample_title)
        val quickStartContent = stringResource(Res.string.quick_start_sample_body)
        val libraryBusyMessage = if (uiState.isImportingExternalBook) {
            stringResource(Res.string.library_opening_book)
        } else {
            null
        }

        LaunchedEffect(appTheme) {
            applyPlatformTheme(appTheme)
        }

        LaunchedEffect(appViewModel) {
            appViewModel.messages.collect { message ->
                snackbarHostState.showSnackbar(message.toSnackbarText())
            }
        }

        val launchBookImport = rememberBookImportLauncher(
            onImported = appViewModel::upsertImportedBook,
            onError = appViewModel::onImportError,
        )

        val currentRoute = backStack.lastOrNull() ?: AppRoute.Home
        val currentScreen = AppScreen.fromRoute(currentRoute)
        val showBottomBar = currentRoute.isTopLevel

        fun openReader(bookId: String, source: AnalyticsEvent.ReaderStart.Source) {
            appViewModel.selectBook(bookId)
            logReaderStart(bookId, source)
            backStack.pushIfNeeded(AppRoute.Reader)
        }

        fun openBookEditor(bookId: String?) {
            appViewModel.startBookEditor(bookId)
            backStack.pushIfNeeded(AppRoute.BookEditor)
        }

        fun openQuickSpeedRead() {
            backStack.pushIfNeeded(AppRoute.QuickSpeedRead)
        }

        fun addQuickStartSample(source: AnalyticsEvent.ReaderStart.Source) {
            val bookId = appViewModel.createBook(
                title = quickStartTitle,
                content = quickStartContent,
                defaultTitle = defaultNewBookTitle,
            )
            if (bookId != null) {
                openReader(bookId, source)
            }
        }

        fun startScratchSpeedRead(content: String) {
            if (!appViewModel.startScratchSpeedRead(content, defaultSpeedReadTitle)) return
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
            logReaderStart(bookId, AnalyticsEvent.ReaderStart.Source.Share)
            appViewModel.consumePendingReaderNavigation()
        }

        // Collect state to trigger recomposition when monetization state changes
        val monetizationState by MonetizationManager.state.collectAsStateWithLifecycle()

        // Track previous route to detect library arrivals from reading screens
        var previousRoute by remember { mutableStateOf<AppRoute?>(null) }

        // Guard to prevent duplicate hint/interstitial handling for the same Library arrival
        var libraryArrivalHandled by remember { mutableStateOf(false) }

        // Single flag so the Level 2 rewarded-ad hint cannot be shown twice at once
        var showRewardHint by rememberSaveable { mutableStateOf(false) }

        // Preload interstitial when Level2 and no reward active
        LaunchedEffect(monetizationState.appliedLevel, monetizationState.isRewardActive) {
            val shouldPreload = monetizationState.appliedLevel == AdLevel.Level2 &&
                !monetizationState.isRewardActive &&
                canShowInterstitialAds()
            if (shouldPreload) {
                InterstitialAdHost.getInstance().preload()
            }
        }

        // Apply a pending ad level at a safe UI boundary (Settings, not during playback)
        LaunchedEffect(currentRoute) {
            if (currentRoute is AppRoute.Settings) {
                MonetizationManager.applyLayoutLevel()
            }
        }

        // If a reward is earned before a deferred hint is displayed, skip it
        LaunchedEffect(showRewardHint, monetizationState.hasEverEarnedReward) {
            if (showRewardHint && monetizationState.hasEverEarnedReward) {
                showRewardHint = false
            }
        }

        // Detect Library arrival from reading screens: apply layout, then hint or interstitial
        LaunchedEffect(currentRoute) {
            val previous = previousRoute
            previousRoute = currentRoute

            // Only trigger when arriving at Library
            if (currentRoute !is AppRoute.Library) {
                if (showRewardHint) {
                    RewardedAdHost.getInstance().cancelPendingShow()
                    showRewardHint = false
                }
                // Reset guard when leaving Library
                if (previous is AppRoute.Library) {
                    libraryArrivalHandled = false
                }
                return@LaunchedEffect
            }

            // Check if coming from a reading-related screen
            val isFromReadingScreen = when (previous) {
                is AppRoute.Reader,
                is AppRoute.SpeedRead,
                is AppRoute.SpeedReadPlayer,
                -> true
                else -> false
            }

            // Skip if not from reading screen or already handled this arrival
            if (!isFromReadingScreen || libraryArrivalHandled) {
                return@LaunchedEffect
            }

            // Mark this arrival as handled to prevent duplicates
            libraryArrivalHandled = true

            MonetizationManager.applyLayoutLevel()

            if (MonetizationManager.shouldShowRewardHint()) {
                showRewardHint = true
                MonetizationManager.clearReadingActivity()
            } else {
                maybeShowInterstitialAfterReading(isHintBeingShown = showRewardHint)
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    val canShowBanner = canShowBannerAds() &&
                        MonetizationManager.shouldShowBanner(currentRoute)
                    Column {
                        if (canShowBanner) {
                            BannerAdHost(modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(FlashReadDimens.space12))
                        }
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ) {
                            AppScreen.entries.forEach { screen ->
                                val selected = currentScreen == screen
                                val label = screen.label()
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        backStack.navigateToTopLevel(screen.route)
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon(selected),
                                            contentDescription = label,
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
                }
            },
        ) { innerPadding ->
            NavDisplay(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .clipToBounds()
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
                sizeTransform = null,
                transitionSpec = { instantNavContentTransform() },
                popTransitionSpec = { instantNavContentTransform() },
                predictivePopTransitionSpec = { _ -> instantNavContentTransform() },
                entryProvider = entryProvider {
                    entry<AppRoute.Home> {
                        HomeScreen(
                            book = currentBook,
                            progressPercent = currentBook?.let(appViewModel::progressPercent) ?: 0,
                            onImportBook = launchBookImport,
                            onCreateBook = { openBookEditor(null) },
                            onSpeedReadText = ::openQuickSpeedRead,
                            onQuickStart = {
                                addQuickStartSample(AnalyticsEvent.ReaderStart.Source.Home)
                            },
                            onContinueReading = { bookId ->
                                openReader(bookId, AnalyticsEvent.ReaderStart.Source.Home)
                            },
                        )
                    }
                    entry<AppRoute.Library> {
                        LibraryScreen(
                            books = uiState.books,
                            progressPercent = appViewModel::progressPercent,
                            onImportBook = launchBookImport,
                            onCreateBook = { openBookEditor(null) },
                            onSpeedReadText = ::openQuickSpeedRead,
                            onQuickStart = {
                                addQuickStartSample(AnalyticsEvent.ReaderStart.Source.Library)
                            },
                            onRenameBook = appViewModel::renameBook,
                            onDeleteBook = appViewModel::deleteBook,
                            onContinueReading = { bookId ->
                                openReader(bookId, AnalyticsEvent.ReaderStart.Source.Library)
                            },
                            onEditBook = { bookId -> openBookEditor(bookId) },
                            busyMessage = libraryBusyMessage,
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
                                onBack = { backStack.popBack() },
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
                                keepScreenOn = keepScreenOn,
                                onClose = { backStack.popBack() },
                            )
                        }
                    }
                    entry<AppRoute.Settings> {
                        SettingsScreen(
                            selectedLanguage = appLanguage,
                            onLanguageSelected = { language ->
                                if (language != appLanguage) {
                                    Analytics.log(
                                        AnalyticsEvent.SettingsChange(
                                            settingName = AnalyticsEvent.SettingsChange.SettingName.Language,
                                            settingValue = language.toStorage(),
                                        ),
                                    )
                                }
                                languageRepository.save(language)
                                appLanguage = language
                            },
                            selectedTheme = appTheme,
                            onThemeSelected = { theme ->
                                if (theme != appTheme) {
                                    Analytics.log(
                                        AnalyticsEvent.SettingsChange(
                                            settingName = AnalyticsEvent.SettingsChange.SettingName.Theme,
                                            settingValue = theme.toStorage(),
                                        ),
                                    )
                                }
                                themeRepository.save(theme)
                                appTheme = theme
                                applyPlatformTheme(theme)
                            },
                            keepScreenOn = keepScreenOn,
                            onKeepScreenOnChange = { enabled ->
                                if (enabled != keepScreenOn) {
                                    Analytics.log(
                                        AnalyticsEvent.SettingsChange(
                                            settingName = AnalyticsEvent.SettingsChange.SettingName.KeepScreenOn,
                                            settingValue = enabled.toString(),
                                        ),
                                    )
                                }
                                keepScreenOnRepository.save(enabled)
                                keepScreenOn = enabled
                            },
                            onManagePrivacy = { showPrivacyOptionsForm() },
                            onOpenPrivacyPolicy = { backStack.pushIfNeeded(AppRoute.PrivacyPolicy) },
                            onOpenTerms = { backStack.pushIfNeeded(AppRoute.Terms) },
                            isDeveloperModeUnlocked = monetizationState.developerModeUnlocked,
                            monetizationState = monetizationState,
                        )
                    }
                    entry<AppRoute.PrivacyPolicy> {
                        LegalDocumentScreen(
                            document = LegalDocuments.privacyPolicy,
                            onBack = { backStack.popBack() },
                        )
                    }
                    entry<AppRoute.Terms> {
                        LegalDocumentScreen(
                            document = LegalDocuments.termsAndConditions,
                            onBack = { backStack.popBack() },
                        )
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
                                    appViewModel.createBook(title, content, defaultNewBookTitle)
                                } else {
                                    appViewModel.updateCreatedBook(
                                        editorBookId,
                                        title,
                                        content,
                                        defaultNewBookTitle,
                                    )
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

        if (showRewardHint && !monetizationState.hasEverEarnedReward) {
            RewardHintDialog(
                onDismiss = { showRewardHint = false },
                onRewardEarned = { showRewardHint = false },
            )
        }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
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
    onQuickStart: () -> Unit,
    onContinueReading: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (book == null) {
        EmptyBookState(
            modifier = modifier,
            onImportBook = onImportBook,
            onCreateBook = onCreateBook,
            onSpeedReadText = onSpeedReadText,
            onQuickStart = onQuickStart,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FlashReadDimens.screenHorizontalPadding)
            .padding(top = FlashReadDimens.space8)
            .padding(bottom = FlashReadDimens.space24),
    ) {
        ScreenTitle(title = stringResource(Res.string.screen_home))
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
                    text = stringResource(Res.string.home_continue_reading),
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
                    text = stringResource(Res.string.home_progress, progressPercent),
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
                        text = stringResource(Res.string.home_open_reader),
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
    onQuickStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FlashReadDimens.screenHorizontalPadding)
            .padding(vertical = FlashReadDimens.space24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppLogo(size = 80.dp)
        Spacer(Modifier.height(FlashReadDimens.space16))
        Text(
            text = stringResource(Res.string.home_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FlashReadDimens.space8))
        Text(
            text = stringResource(Res.string.home_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FlashReadDimens.space16))
        HomeActionButton(text = stringResource(Res.string.library_quick_start), onClick = onQuickStart)
        Spacer(Modifier.height(FlashReadDimens.space12))
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
    HomeActionButton(text = stringResource(Res.string.action_import_book), onClick = onImportBook)
    Spacer(Modifier.height(FlashReadDimens.space12))
    HomeActionButton(text = stringResource(Res.string.action_create_book), onClick = onCreateBook)
    Spacer(Modifier.height(FlashReadDimens.space12))
    HomeActionButton(text = stringResource(Res.string.action_speed_read_text), onClick = onSpeedReadText)
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

@Composable
private fun AppScreen.label(): String = when (this) {
    AppScreen.Home -> stringResource(Res.string.nav_home)
    AppScreen.Library -> stringResource(Res.string.nav_library)
    AppScreen.Settings -> stringResource(Res.string.nav_settings)
}

private fun logReaderStart(bookId: String, source: AnalyticsEvent.ReaderStart.Source) {
    val material = if (bookId.startsWith(CreatedBookIdPrefix)) {
        AnalyticsEvent.ReaderStart.Material.Created
    } else {
        AnalyticsEvent.ReaderStart.Material.Imported
    }
    Analytics.log(AnalyticsEvent.ReaderStart(source = source, material = material))
}

private suspend fun AppMessage.toSnackbarText(): String = when (this) {
    is AppMessage.Imported -> getString(Res.string.snackbar_imported, title)
    is AppMessage.Deleted -> getString(Res.string.snackbar_deleted, title)
    is AppMessage.Error -> text
}

/**
 * Attempts to show an interstitial ad after a reading session ends
 * (when user returns to Library from a reading screen).
 *
 * Shows the ad only if:
 * - Reading session had actual activity (scroll/tap in reader, play in speed read)
 * - Level 2 applied with no active reward
 * - Interstitial ads can be shown (consent, initialization)
 * - Ad is preloaded and ready
 * - All policy conditions are met (cooldown, caps, foreground)
 *
 * If ad cannot be shown (not ready, not eligible), the reading activity flag
 * is cleared immediately to prevent accumulation of skipped opportunities.
 */
private fun maybeShowInterstitialAfterReading(isHintBeingShown: Boolean) {
    val state = MonetizationManager.currentState

    // Skip if no reading activity occurred
    if (!state.readingSessionHadActivity) {
        return
    }

    // Skip if interstitial ads cannot be shown on this platform
    if (!canShowInterstitialAds()) {
        MonetizationManager.clearReadingActivity()
        return
    }

    val host = InterstitialAdHost.getInstance()

    // Skip if ad is not ready (don't delay navigation, don't accumulate)
    if (!host.isAdReady()) {
        MonetizationManager.clearReadingActivity()
        return
    }

    // Check full eligibility with all runtime context
    val context = MonetizationPolicy.InterstitialContext(
        isAdReady = true,
        isAppInForeground = UsageTracker.isInForeground.value,
        isHintBeingShown = isHintBeingShown,
    )

    if (!MonetizationManager.isInterstitialEligible(context)) {
        MonetizationManager.clearReadingActivity()
        return
    }

    // Show the ad
    host.show { result ->
        when (result) {
            InterstitialResult.Shown -> {
                // Record shown AFTER confirmed presentation
                MonetizationManager.recordInterstitialShown()
            }
            else -> {
                // Failed, NotReady, or Cancelled - don't record as shown
            }
        }
        // Always clear activity after the attempt
        MonetizationManager.clearReadingActivity()
    }
}
