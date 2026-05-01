package com.tool.flashread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.tool.flashread.navigation.AppRoute
import com.tool.flashread.navigation.AppScreen

@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun App() {
    MaterialTheme {
        val backStack = remember { mutableStateListOf<AppRoute>(AppRoute.Library) }
        val currentRoute = backStack.lastOrNull() ?: AppRoute.Library
        val currentScreen = AppScreen.fromRoute(currentRoute)

        Scaffold(
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
                            onOpenReader = { pushIfNeeded(backStack, AppRoute.Reader) },
                            onOpenSpeedRead = { pushIfNeeded(backStack, AppRoute.SpeedRead) },
                        )
                    }
                    entry<AppRoute.Reader> {
                        ReaderScreen(
                            onOpenSpeedRead = { pushIfNeeded(backStack, AppRoute.SpeedRead) },
                        )
                    }
                    entry<AppRoute.SpeedRead> {
                        SpeedReadScreen(
                            onBackToReader = {
                                if (backStack.size > 1) backStack.removeLast()
                                if (backStack.lastOrNull() != AppRoute.Reader) {
                                    pushIfNeeded(backStack, AppRoute.Reader)
                                }
                            },
                        )
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
private fun LibraryScreen(
    modifier: Modifier = Modifier,
    onOpenReader: () -> Unit,
    onOpenSpeedRead: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Library")
        Button(onClick = onOpenReader) {
            Text("Open Reader")
        }
        Button(onClick = onOpenSpeedRead) {
            Text("Start SpeedRead")
        }
    }
}

@Composable
private fun ReaderScreen(
    modifier: Modifier = Modifier,
    onOpenSpeedRead: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Reader")
        Button(onClick = onOpenSpeedRead) {
            Text("Switch to SpeedRead")
        }
    }
}

@Composable
private fun SpeedReadScreen(
    modifier: Modifier = Modifier,
    onBackToReader: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("SpeedRead")
        Button(onClick = onBackToReader) {
            Text("Back to Reader")
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