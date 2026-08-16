package com.tool.flashread.data.repository

import com.tool.flashread.platform.CoverStorage

class CoverRepository(
    private val onSave: (bookId: String, bytes: ByteArray, mimeType: String) -> String =
        { bookId, bytes, mimeType -> CoverStorage.saveCover(bookId, bytes, mimeType) },
    private val onDelete: (fileName: String) -> Unit = { CoverStorage.deleteCover(it) },
) {
    fun saveCover(bookId: String, bytes: ByteArray, mimeType: String): String =
        onSave(bookId, bytes, mimeType)

    fun deleteCover(fileName: String) = onDelete(fileName)
}
