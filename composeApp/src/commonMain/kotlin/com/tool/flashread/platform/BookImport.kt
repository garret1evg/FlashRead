package com.tool.flashread.platform

import androidx.compose.runtime.Composable
import com.tool.flashread.navigation.AppRoute

data class ImportedBook(
    val id: String,
    val title: String,
    val content: String,
    val coverBytes: ByteArray? = null,
    val coverMimeType: String? = null,
)

@Composable
expect fun rememberBookImportLauncher(
    onImported: (ImportedBook) -> Unit,
    onError: (String) -> Unit,
): () -> Unit

/**
 * Starting destination when the process was launched by opening a book
 * from a file manager. Null means the regular home launch.
 */
expect fun launchRouteForExternalBookOpen(): AppRoute?

@Composable
expect fun ObserveExternalBookOpens(
    onOpenStarted: () -> Unit,
    onImported: (ImportedBook) -> Unit,
    onError: (String) -> Unit,
)
