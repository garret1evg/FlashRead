package com.tool.flashread.platform

expect object CoverStorage {
    fun saveCover(bookId: String, bytes: ByteArray, mimeType: String): String
    fun loadCover(fileName: String): ByteArray?
    fun deleteCover(fileName: String)
}

internal fun coverFileExtension(mimeType: String): String {
    return when (mimeType.substringBefore(';').trim().lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "image/bmp" -> "bmp"
        else -> "jpg"
    }
}

internal fun coverFileNameFor(bookId: String, mimeType: String): String {
    val key = bookId.hashCode().toUInt().toString(16)
    return "$key.${coverFileExtension(mimeType)}"
}
