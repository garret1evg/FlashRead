package com.tool.flashread.platform

import com.tool.flashread.core.model.Book

expect object BookStorage {
    fun saveBooks(books: List<Book>)
    fun loadBooks(): List<Book>
}
