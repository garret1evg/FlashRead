package com.tool.flashread.platform

import androidx.core.content.edit
import com.tool.flashread.core.model.Book
import com.tool.flashread.core.model.MaterialSourceType
import com.tool.flashread.core.reading.paragraphCount as countParagraphs
import com.tool.flashread.core.reading.wordCount as countWords

actual object BookStorage {
    private const val PREFS_NAME = "flashread_books_prefs"
    private const val KEY_BOOK_IDS = "book_ids"
    private const val KEY_ID_PREFIX = "book_id_"
    private const val KEY_TITLE_PREFIX = "book_title_"
    private const val KEY_TYPE_PREFIX = "book_type_"
    private const val KEY_WORD_COUNT_PREFIX = "book_word_count_"
    private const val KEY_PARAGRAPH_COUNT_PREFIX = "book_paragraph_count_"

    actual fun saveBooks(books: List<Book>) {
        val preferences = prefs()
        val oldKeys = preferences.getStringSet(KEY_BOOK_IDS, emptySet()).orEmpty()
        val contentFiles = BookContentFiles.create()
        val newKeys = books.map { storageKey(it.id) }.toSet()

        books.forEach { book ->
            contentFiles.write(storageKey(book.id), book.content)
        }

        preferences.edit {
            oldKeys.forEach { key ->
                remove(KEY_ID_PREFIX + key)
                remove(KEY_TITLE_PREFIX + key)
                remove(KEY_TYPE_PREFIX + key)
                remove(KEY_WORD_COUNT_PREFIX + key)
                remove(KEY_PARAGRAPH_COUNT_PREFIX + key)
            }
            putStringSet(KEY_BOOK_IDS, newKeys)
            books.forEach { book ->
                val key = storageKey(book.id)
                putString(KEY_ID_PREFIX + key, book.id)
                putString(KEY_TITLE_PREFIX + key, book.title)
                putString(KEY_TYPE_PREFIX + key, book.sourceType.name)
                putInt(KEY_WORD_COUNT_PREFIX + key, book.wordCount)
                putInt(KEY_PARAGRAPH_COUNT_PREFIX + key, book.paragraphCount)
            }
        }

        contentFiles.deleteOrphans(newKeys)
    }

    actual fun loadBooks(): List<Book> {
        val preferences = prefs()
        val storageKeys = preferences.getStringSet(KEY_BOOK_IDS, emptySet()).orEmpty()
        val contentFiles = BookContentFiles.create()
        return storageKeys.mapNotNull { key ->
            val id = preferences.getString(KEY_ID_PREFIX + key, null) ?: return@mapNotNull null
            val title = preferences.getString(KEY_TITLE_PREFIX + key, null) ?: return@mapNotNull null
            val content = contentFiles.read(key) ?: return@mapNotNull null
            val sourceType = preferences.getString(KEY_TYPE_PREFIX + key, null)
                ?.let { runCatching { MaterialSourceType.valueOf(it) }.getOrNull() }
                ?: MaterialSourceType.Book
            val hasWordCount = preferences.contains(KEY_WORD_COUNT_PREFIX + key)
            val hasParagraphCount = preferences.contains(KEY_PARAGRAPH_COUNT_PREFIX + key)
            Book(
                id = id,
                title = title,
                content = content,
                sourceType = sourceType,
                wordCount = if (hasWordCount) {
                    preferences.getInt(KEY_WORD_COUNT_PREFIX + key, 0)
                } else {
                    countWords(content)
                },
                paragraphCount = if (hasParagraphCount) {
                    preferences.getInt(KEY_PARAGRAPH_COUNT_PREFIX + key, 0)
                } else {
                    countParagraphs(content)
                },
                coverFileName = CoverStorage.findCoverFileName(id),
            )
        }
    }

    private fun storageKey(bookId: String): String = BookContentFiles.storageKey(bookId)

    private fun prefs() = AndroidAppContext.applicationContext.getSharedPreferences(PREFS_NAME, 0)
}
