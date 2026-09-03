package com.evgeniich.flashread.platform

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
import com.evgeniich.flashread.analytics.Analytics
import com.evgeniich.flashread.analytics.AnalyticsEvent
import com.evgeniich.flashread.core.importdoc.BookExtractException
import com.evgeniich.flashread.core.importdoc.BookFormat
import com.evgeniich.flashread.core.importdoc.BookTextExtractor
import com.evgeniich.flashread.core.importdoc.CoverThumbnail
import com.evgeniich.flashread.resources.Res
import com.evgeniich.flashread.resources.*
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
                    importBookFromUri(
                        contentResolver,
                        uri,
                        cacheDir,
                        fallbackTitle,
                        AnalyticsEvent.BookImport.Source.Picker,
                    )
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
    source: AnalyticsEvent.BookImport.Source,
): ImportedBook {
    val displayName = readDisplayName(contentResolver.query(uri, null, null, null, null))
    val mimeType = contentResolver.getType(uri)
    try {
        val extracted = BookTextExtractor.extract(
            contentResolver = contentResolver,
            uri = uri,
            cacheDir = cacheDir,
            fileName = displayName,
            mimeType = mimeType,
        )
        val result = if (extracted.content.isBlank()) {
            AnalyticsEvent.BookImport.Result.Empty
        } else {
            AnalyticsEvent.BookImport.Result.Success
        }
        logBookImport(extracted.format, result, source)
        val cover = extracted.coverBytes?.let { CoverThumbnail.prepare(it, extracted.coverMimeType) }
        return ImportedBook(
            id = uri.toString(),
            title = extracted.title?.trim()?.takeIf { it.isNotEmpty() } ?: displayName ?: fallbackTitle,
            content = extracted.content,
            coverBytes = cover?.first,
            coverMimeType = cover?.second,
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: BookExtractException) {
        logBookImport(error.format, bookImportResult(error.message), source)
        throw error
    } catch (error: Exception) {
        val format = BookFormat.detect(fileName = displayName, mimeType = mimeType)
        logBookImport(format, bookImportResult(error.message), source)
        throw error
    }
}

internal fun bookImportResult(rawMessage: String?): AnalyticsEvent.BookImport.Result {
    return when (rawMessage) {
        BookTextExtractor.UNSUPPORTED_FORMAT_MESSAGE -> AnalyticsEvent.BookImport.Result.Unsupported
        BookTextExtractor.DAMAGED_FILE_MESSAGE -> AnalyticsEvent.BookImport.Result.Damaged
        BookTextExtractor.UNABLE_TO_READ_MESSAGE -> AnalyticsEvent.BookImport.Result.UnableRead
        else -> AnalyticsEvent.BookImport.Result.Failed
    }
}

private fun logBookImport(
    format: BookFormat,
    result: AnalyticsEvent.BookImport.Result,
    source: AnalyticsEvent.BookImport.Source,
) {
    Analytics.log(
        AnalyticsEvent.BookImport(
            format = AnalyticsEvent.BookImport.Format.from(format),
            result = result,
            source = source,
        ),
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
