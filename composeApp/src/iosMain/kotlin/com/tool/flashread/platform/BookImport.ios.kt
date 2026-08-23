package com.tool.flashread.platform

import androidx.compose.runtime.Composable
import com.tool.flashread.navigation.AppRoute
import com.tool.flashread.resources.Res
import com.tool.flashread.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun rememberBookImportLauncher(
    onImported: (ImportedBook) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val unsupported = stringResource(Res.string.import_ios_unsupported)
    return {
        onError(unsupported)
    }
}

actual fun launchRouteForExternalBookOpen(): AppRoute? = null

@Composable
actual fun ObserveExternalBookOpens(
    onOpenStarted: () -> Unit,
    onImported: (ImportedBook) -> Unit,
    onError: (String) -> Unit,
) = Unit
