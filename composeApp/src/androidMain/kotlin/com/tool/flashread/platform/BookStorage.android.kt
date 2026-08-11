package com.tool.flashread.platform

import androidx.core.content.edit
import com.tool.flashread.core.model.Book

actual object BookStorage {
    private const val PREFS_NAME = "flashread_books_prefs"
    private const val KEY_BOOK_IDS = "book_ids"
    private const val KEY_ID_PREFIX = "book_id_"
    private const val KEY_TITLE_PREFIX = "book_title_"
    private const val KEY_CONTENT_PREFIX = "book_content_"

    actual fun saveBooks(books: List<Book>) {
        val preferences = prefs()
        val oldKeys = preferences.getStringSet(KEY_BOOK_IDS, emptySet()).orEmpty()
        preferences.edit {
            oldKeys.forEach { storageKey ->
                remove(KEY_ID_PREFIX + storageKey)
                remove(KEY_TITLE_PREFIX + storageKey)
                remove(KEY_CONTENT_PREFIX + storageKey)
            }

            val newKeys = books.map { storageKey(it.id) }.toSet()
            putStringSet(KEY_BOOK_IDS, newKeys)
            books.forEach { book ->
                val key = storageKey(book.id)
                putString(KEY_ID_PREFIX + key, book.id)
                putString(KEY_TITLE_PREFIX + key, book.title)
                putString(KEY_CONTENT_PREFIX + key, book.content)
            }
        }
    }

    actual fun loadBooks(): List<Book> {
        val storageKeys = prefs().getStringSet(KEY_BOOK_IDS, emptySet()).orEmpty()
        return storageKeys.mapNotNull { key ->
            val id = prefs().getString(KEY_ID_PREFIX + key, null) ?: return@mapNotNull null
            val title = prefs().getString(KEY_TITLE_PREFIX + key, null) ?: return@mapNotNull null
            val content = prefs().getString(KEY_CONTENT_PREFIX + key, null) ?: return@mapNotNull null
            Book(id = id, title = title, content = content)
        }
    }

    private fun storageKey(bookId: String): String = bookId.hashCode().toString()

    private fun prefs() = AndroidAppContext.applicationContext.getSharedPreferences(PREFS_NAME, 0)
}
