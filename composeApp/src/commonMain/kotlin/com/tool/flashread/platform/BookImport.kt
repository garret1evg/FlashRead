package com.tool.flashread.platform

import androidx.compose.runtime.Composable

data class ImportedBook(
    val id: String,
    val title: String,
    val content: String,
)

@Composable
expect fun rememberBookImportLauncher(
    onImported: (ImportedBook) -> Unit,
    onError: (String) -> Unit,
): () -> Unit
