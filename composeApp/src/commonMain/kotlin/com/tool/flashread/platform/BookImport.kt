package com.tool.flashread.platform

import androidx.compose.runtime.Composable

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
