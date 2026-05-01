package com.tool.flashread.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberBookImportLauncher(
    onImported: (ImportedBook) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    return {
        onError("Book import is currently supported on Android.")
    }
}
