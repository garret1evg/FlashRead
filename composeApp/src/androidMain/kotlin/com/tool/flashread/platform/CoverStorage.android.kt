package com.tool.flashread.platform

actual object CoverStorage {
    actual fun saveCover(bookId: String, bytes: ByteArray, mimeType: String): String {
        return CoverFiles.create().save(bookId, bytes, mimeType)
    }

    actual fun loadCover(fileName: String): ByteArray? {
        return CoverFiles.create().load(fileName)
    }

    actual fun deleteCover(fileName: String) {
        CoverFiles.create().delete(fileName)
    }

    fun findCoverFileName(bookId: String): String? {
        return CoverFiles.create().findCoverFileName(bookId)
    }

    actual fun coverImageModel(fileName: String): Any? {
        return CoverFiles.create().resolve(fileName)
    }
}
