package com.tool.flashread.platform

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tool.flashread.core.importdoc.BookTextExtractor
import com.tool.flashread.core.importdoc.CoverThumbnail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val BOOK_IMPORT_MIME_TYPES = arrayOf(
    "text/plain",
    "text/*",
    "application/epub+zip",
    "application/x-fictionbook+xml",
    "application/x-fictionbook",
    "application/xml",
    "application/zip",
    "application/octet-stream",
)

@Composable
actual fun rememberBookImportLauncher(
    onImported: (ImportedBook) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val cacheDir = context.cacheDir
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val imported = withContext(Dispatchers.IO) {
                    importBookFromUri(contentResolver, uri, cacheDir)
                }
                if (imported.content.isBlank()) {
                    onError("Selected file is empty.")
                } else {
                    onImported(imported)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onError(error.message?.trim()?.takeIf { it.isNotEmpty() } ?: "Failed to import book.")
            }
        }
    }

    return { launcher.launch(BOOK_IMPORT_MIME_TYPES) }
}

@Composable
actual fun ObserveExternalBookOpens(
    onOpenStarted: () -> Unit,
    onImported: (ImportedBook) -> Unit,
    onError: (String) -> Unit,
) {
    val state by ExternalBookImporter.state.collectAsStateWithLifecycle()
    val onOpenStartedState by rememberUpdatedState(onOpenStarted)
    val onImportedState by rememberUpdatedState(onImported)
    val onErrorState by rememberUpdatedState(onError)

    LaunchedEffect(state.sessionId, state.phase) {
        when (state.phase) {
            ExternalBookImportState.Phase.Idle -> Unit
            ExternalBookImportState.Phase.Importing -> onOpenStartedState()
            ExternalBookImportState.Phase.Success -> {
                state.book?.let(onImportedState)
                ExternalBookImporter.consume(state.sessionId)
            }
            ExternalBookImportState.Phase.Error -> {
                onErrorState(state.error ?: "Failed to import book.")
                ExternalBookImporter.consume(state.sessionId)
            }
        }
    }
}

internal fun importBookFromUri(
    contentResolver: ContentResolver,
    uri: Uri,
    cacheDir: File,
): ImportedBook {
    val displayName = readDisplayName(contentResolver.query(uri, null, null, null, null))
    val extracted = BookTextExtractor.extract(
        contentResolver = contentResolver,
        uri = uri,
        cacheDir = cacheDir,
        fileName = displayName,
        mimeType = contentResolver.getType(uri),
    )
    val cover = extracted.coverBytes?.let { CoverThumbnail.prepare(it, extracted.coverMimeType) }
    return ImportedBook(
        id = uri.toString(),
        title = extracted.title?.trim()?.takeIf { it.isNotEmpty() } ?: displayName ?: "Imported Book",
        content = extracted.content,
        coverBytes = cover?.first,
        coverMimeType = cover?.second,
    )
}

private fun readDisplayName(cursor: Cursor?): String? {
    cursor ?: return null
    cursor.use {
        if (!it.moveToFirst()) return null
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index < 0) return null
        return it.getString(index)
    }
}
