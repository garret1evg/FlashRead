package com.tool.flashread.platform

import java.io.File

/**
 * Cover image files stored as `{coversDir}/{hex}.{ext}`.
 *
 * [hex] is [coverHashHex]. When several extensions exist for the same book,
 * [findCoverFileName] picks the newest `lastModified`, then lexicographic min.
 */
internal class CoverFiles(private val coversDir: File) {

    fun save(bookId: String, bytes: ByteArray, mimeType: String): String {
        ensureDir()
        val fileName = coverFileNameFor(bookId, mimeType)
        File(coversDir, fileName).writeBytes(bytes)
        deleteOtherFilesForBook(bookId, keepFileName = fileName)
        return fileName
    }

    fun load(fileName: String): ByteArray? {
        if (!isSafeFileName(fileName)) return null
        val file = File(coversDir, fileName)
        if (!file.isFile) return null
        return runCatching { file.readBytes() }.getOrNull()
    }

    fun delete(fileName: String) {
        if (!isSafeFileName(fileName)) return
        File(coversDir, fileName).delete()
    }

    fun findCoverFileName(bookId: String): String? {
        val matches = filesForBook(bookId)
        if (matches.isEmpty()) return null
        val newestModified = matches.maxOf { it.lastModified() }
        return matches
            .filter { it.lastModified() == newestModified }
            .minOf { it.name }
    }

    private fun filesForBook(bookId: String): List<File> {
        if (!coversDir.isDirectory) return emptyList()
        val prefix = "${coverHashHex(bookId)}."
        return coversDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(prefix) && isSafeFileName(it.name) }
            .orEmpty()
    }

    private fun deleteOtherFilesForBook(bookId: String, keepFileName: String) {
        if (!coversDir.isDirectory) return
        val prefix = "${coverHashHex(bookId)}."
        coversDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith(prefix) && file.name != keepFileName) {
                file.delete()
            }
        }
    }

    private fun ensureDir() {
        if (!coversDir.exists()) {
            coversDir.mkdirs()
        }
    }

    private fun isSafeFileName(fileName: String): Boolean {
        return fileName.isNotBlank() && !fileName.contains('/') && !fileName.contains('\\')
    }

    companion object {
        fun create(): CoverFiles {
            val dir = File(AndroidAppContext.applicationContext.filesDir, "covers")
            return CoverFiles(dir)
        }
    }
}
