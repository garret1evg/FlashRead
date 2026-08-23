package com.tool.flashread.platform

import androidx.compose.runtime.Composable
import com.tool.flashread.navigation.AppRoute

@Composable
actual fun rememberBookImportLauncher(
    onImported: (ImportedBook) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    return {
        onError("Book import is currently supported on Android.")
    }
}

actual fun launchRouteForExternalBookOpen(): AppRoute? = null

@Composable
actual fun ObserveExternalBookOpens(
    onOpenStarted: () -> Unit,
    onImported: (ImportedBook) -> Unit,
    onError: (String) -> Unit,
) = Unit
