package com.evgeniich.flashread.data.repository

import com.evgeniich.flashread.core.model.Book
import com.evgeniich.flashread.platform.BookStorage

class BookRepository(
    private val onLoad: () -> List<Book> = { BookStorage.loadBooks() },
    private val onSave: (List<Book>) -> Unit = { BookStorage.saveBooks(it) },
) {
    fun loadBooks(): List<Book> = onLoad()

    fun saveBooks(books: List<Book>) = onSave(books)
}
