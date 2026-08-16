package com.tool.flashread.platform

import java.io.File

actual object CoverStorage {
    actual fun saveCover(bookId: String, bytes: ByteArray, mimeType: String): String {
        val fileName = coverFileNameFor(bookId, mimeType)
        val file = File(coversDir(), fileName)
        file.writeBytes(bytes)
        return fileName
    }

    actual fun loadCover(fileName: String): ByteArray? {
        if (fileName.isBlank() || fileName.contains('/') || fileName.contains('\\')) return null
        val file = File(coversDir(), fileName)
        if (!file.isFile) return null
        return runCatching { file.readBytes() }.getOrNull()
    }

    actual fun deleteCover(fileName: String) {
        if (fileName.isBlank() || fileName.contains('/') || fileName.contains('\\')) return
        File(coversDir(), fileName).delete()
    }

    private fun coversDir(): File {
        val dir = File(AndroidAppContext.applicationContext.filesDir, "covers")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
