package com.evgeniich.flashread.platform

import com.evgeniich.flashread.core.model.Book

expect object BookStorage {
    fun saveBooks(books: List<Book>)
    fun loadBooks(): List<Book>
}
