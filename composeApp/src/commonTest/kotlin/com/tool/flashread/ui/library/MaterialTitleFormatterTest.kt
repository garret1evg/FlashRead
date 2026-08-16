package com.tool.flashread.ui.library

import com.tool.flashread.core.model.Book
import kotlin.test.Test
import kotlin.test.assertEquals

class MaterialTitleFormatterTest {

    @Test
    fun stripsFileExtension() {
        assertEquals("War and Peace", MaterialTitleFormatter.displayTitle("War and Peace.txt"))
        assertEquals("Chapter one", MaterialTitleFormatter.displayTitle("Chapter one.EPUB"))
        assertEquals("notes", MaterialTitleFormatter.displayTitle("notes.fb2"))
    }

    @Test
    fun replacesUnderscoresWithSpaces() {
        assertEquals(
            "war and peace",
            MaterialTitleFormatter.displayTitle("war_and_peace.txt"),
        )
        assertEquals(
            "My long book title",
            MaterialTitleFormatter.displayTitle("My_long_book_title"),
        )
    }

    @Test
    fun stripsTechnicalNumericSuffixes() {
        assertEquals(
            "My Book",
            MaterialTitleFormatter.displayTitle("My_Book_12345.epub"),
        )
        assertEquals(
            "lecture notes",
            MaterialTitleFormatter.displayTitle("lecture_notes-1698765432.txt"),
        )
        assertEquals(
            "draft",
            MaterialTitleFormatter.displayTitle("draft (48291)"),
        )
    }

    @Test
    fun keepsHumanReadableNumbers() {
        assertEquals("1984", MaterialTitleFormatter.displayTitle("1984.txt"))
        assertEquals("Catch-22", MaterialTitleFormatter.displayTitle("Catch-22"))
        assertEquals("chapter 01", MaterialTitleFormatter.displayTitle("chapter_01.txt"))
        assertEquals("Volume 2", MaterialTitleFormatter.displayTitle("Volume 2"))
    }

    @Test
    fun doesNotMutateModelTitle() {
        val book = Book(
            id = "1",
            title = "raw_file_name_999.txt",
            content = "hello",
        )
        assertEquals("raw file name", MaterialTitleFormatter.displayTitle(book.title))
        assertEquals("raw_file_name_999.txt", book.title)
    }
}
