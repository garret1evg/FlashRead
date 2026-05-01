package com.tool.flashread.platform

import android.database.Cursor
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberBookImportLauncher(
    onImported: (ImportedBook) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Unable to read selected file.")
            val title = readDisplayName(contentResolver.query(uri, null, null, null, null)) ?: "Imported Book"
            ImportedBook(
                id = uri.toString(),
                title = title,
                content = content,
            )
        }.onSuccess { imported ->
            if (imported.content.isBlank()) {
                onError("Selected file is empty.")
            } else {
                onImported(imported)
            }
        }.onFailure {
            onError(it.message ?: "Failed to import book.")
        }
    }

    return { launcher.launch("text/*") }
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
