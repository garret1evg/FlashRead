package com.evgeniich.flashread.platform

import com.evgeniich.flashread.core.model.Book

actual object BookStorage {
    private val inMemoryBooks = mutableListOf<Book>()

    actual fun saveBooks(books: List<Book>) {
        inMemoryBooks.clear()
        inMemoryBooks.addAll(books)
    }

    actual fun loadBooks(): List<Book> = inMemoryBooks.toList()
}
