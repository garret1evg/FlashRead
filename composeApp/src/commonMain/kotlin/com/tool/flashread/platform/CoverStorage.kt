package com.tool.flashread.platform

expect object CoverStorage {
    fun saveCover(bookId: String, bytes: ByteArray, mimeType: String): String
    fun loadCover(fileName: String): ByteArray?
    fun deleteCover(fileName: String)

    /** Coil model for [fileName], or null if the cover is missing. */
    fun coverImageModel(fileName: String): Any?
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

internal fun coverHashHex(bookId: String): String = bookId.hashCode().toUInt().toString(16)

internal fun coverFileNameFor(bookId: String, mimeType: String): String {
    return "${coverHashHex(bookId)}.${coverFileExtension(mimeType)}"
}
