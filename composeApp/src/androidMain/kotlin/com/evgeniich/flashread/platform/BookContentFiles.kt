package com.evgeniich.flashread.platform

import java.io.File

/**
 * Internal helper for file-based book content storage on Android.
 *
 * Files are stored as `{booksDir}/{storageKey}.txt` in UTF-8.
 * Atomic writes use a `.tmp` suffix and rename.
 */
internal class BookContentFiles(private val booksDir: File) {

    /**
     * Writes [content] to `{storageKey}.txt` atomically.
     *
     * @throws IllegalArgumentException if [storageKey] contains `/` or `\`
     */
    fun write(storageKey: String, content: String) {
        requireValidKey(storageKey)
        ensureDir()
        val target = File(booksDir, "$storageKey.txt")
        val tmp = File(booksDir, "$storageKey.txt.tmp")
        tmp.writeText(content, Charsets.UTF_8)
        val renamed = tmp.renameTo(target)
        if (!renamed) {
            target.delete()
            if (!tmp.renameTo(target)) {
                tmp.delete()
                throw IllegalStateException("Failed to atomically write $storageKey.txt")
            }
        }
    }

    /**
     * Reads content from `{storageKey}.txt`.
     *
     * @return file content or `null` if file doesn't exist or key is invalid
     */
    fun read(storageKey: String): String? {
        if (!isValidKey(storageKey)) return null
        val file = File(booksDir, "$storageKey.txt")
        if (!file.isFile) return null
        return runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
    }

    /**
     * Deletes `{storageKey}.txt` if it exists.
     */
    fun delete(storageKey: String) {
        if (!isValidKey(storageKey)) return
        File(booksDir, "$storageKey.txt").delete()
    }

    /**
     * Deletes all `.txt` files whose base name is not in [keepKeys].
     *
     * @return set of deleted storage keys
     */
    fun deleteOrphans(keepKeys: Set<String>): Set<String> {
        if (!booksDir.isDirectory) return emptySet()
        val deleted = mutableSetOf<String>()
        booksDir.listFiles()?.forEach { file ->
            if (file.isFile && file.extension == "txt") {
                val key = file.nameWithoutExtension
                if (key !in keepKeys && !key.endsWith(".tmp")) {
                    if (file.delete()) {
                        deleted.add(key)
                    }
                }
            }
        }
        return deleted
    }

    /**
     * Returns all storage keys for existing `.txt` files.
     */
    fun listKeys(): Set<String> {
        if (!booksDir.isDirectory) return emptySet()
        return booksDir.listFiles()
            ?.filter { it.isFile && it.extension == "txt" && !it.nameWithoutExtension.endsWith(".tmp") }
            ?.map { it.nameWithoutExtension }
            ?.toSet()
            ?: emptySet()
    }

    /**
     * Cleans up any leftover `.tmp` files.
     */
    fun cleanupTempFiles() {
        if (!booksDir.isDirectory) return
        booksDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".txt.tmp")) {
                file.delete()
            }
        }
    }

    private fun ensureDir() {
        if (!booksDir.exists()) {
            booksDir.mkdirs()
        }
    }

    private fun requireValidKey(key: String) {
        require(isValidKey(key)) { "Storage key must not contain '/' or '\\': $key" }
    }

    private fun isValidKey(key: String): Boolean {
        return key.isNotBlank() && !key.contains('/') && !key.contains('\\')
    }

    companion object {
        /**
         * Creates a [BookContentFiles] instance using the app's internal files directory.
         */
        fun create(): BookContentFiles {
            val dir = File(AndroidAppContext.applicationContext.filesDir, "books")
            return BookContentFiles(dir)
        }

        /**
         * Computes storage key from book ID (same algorithm as prefs keys).
         */
        fun storageKey(bookId: String): String = bookId.hashCode().toString()
    }
}
