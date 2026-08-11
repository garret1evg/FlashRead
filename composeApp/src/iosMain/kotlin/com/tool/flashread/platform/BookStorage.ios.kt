package com.tool.flashread.platform

import com.tool.flashread.core.model.Book

actual object BookStorage {
    private val inMemoryBooks = mutableListOf<Book>()

    actual fun saveBooks(books: List<Book>) {
        inMemoryBooks.clear()
        inMemoryBooks.addAll(books)
    }

    actual fun loadBooks(): List<Book> = inMemoryBooks.toList()
}
