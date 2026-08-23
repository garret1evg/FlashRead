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
import com.tool.flashread.resources.Res
import com.tool.flashread.resources.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
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
    val emptyFileMessage = stringResource(Res.string.import_file_empty)
    val fallbackTitle = stringResource(Res.string.import_fallback_title)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val imported = withContext(Dispatchers.IO) {
                    importBookFromUri(contentResolver, uri, cacheDir, fallbackTitle)
                }
                if (imported.content.isBlank()) {
                    onError(emptyFileMessage)
                } else {
                    onImported(imported)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onError(localizedImportError(error.message))
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
                onErrorState(state.error ?: getString(Res.string.import_failed))
                ExternalBookImporter.consume(state.sessionId)
            }
        }
    }
}

internal fun importBookFromUri(
    contentResolver: ContentResolver,
    uri: Uri,
    cacheDir: File,
    fallbackTitle: String,
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
        title = extracted.title?.trim()?.takeIf { it.isNotEmpty() } ?: displayName ?: fallbackTitle,
        content = extracted.content,
        coverBytes = cover?.first,
        coverMimeType = cover?.second,
    )
}

internal suspend fun localizedImportError(rawMessage: String?): String {
    return when (rawMessage) {
        BookTextExtractor.UNSUPPORTED_FORMAT_MESSAGE -> getString(Res.string.import_unsupported_format)
        BookTextExtractor.DAMAGED_FILE_MESSAGE -> getString(Res.string.import_damaged_file)
        BookTextExtractor.UNABLE_TO_READ_MESSAGE -> getString(Res.string.import_unable_to_read)
        else -> rawMessage?.trim()?.takeIf { it.isNotEmpty() } ?: getString(Res.string.import_failed)
    }
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
