package com.tool.flashread.platform

actual object CoverStorage {
    private val files = mutableMapOf<String, ByteArray>()

    actual fun saveCover(bookId: String, bytes: ByteArray, mimeType: String): String {
        val fileName = coverFileNameFor(bookId, mimeType)
        files[fileName] = bytes
        return fileName
    }

    actual fun loadCover(fileName: String): ByteArray? = files[fileName]

    actual fun deleteCover(fileName: String) {
        files.remove(fileName)
    }

    actual fun coverImageModel(fileName: String): Any? = files[fileName]
}
