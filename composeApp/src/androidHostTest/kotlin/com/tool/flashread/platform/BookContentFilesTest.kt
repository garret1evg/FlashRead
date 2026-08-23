package com.tool.flashread.platform

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BookContentFilesTest {

    private lateinit var tempDir: File
    private lateinit var files: BookContentFiles

    @BeforeTest
    fun setUp() {
        tempDir = createTempDirectory("book_content_test").toFile()
        files = BookContentFiles(tempDir)
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun roundTripUtf8SingleLine() {
        val key = "123456"
        val content = "Hello, World!"
        files.write(key, content)
        assertEquals(content, files.read(key))
    }

    @Test
    fun roundTripUtf8Multiline() {
        val key = "multiline"
        val content = """
            Перший рядок
            Second line with emoji 🎉
            Třetí řádek s diakritikou
            第四行中文
            
            Empty line above
        """.trimIndent()
        files.write(key, content)
        assertEquals(content, files.read(key))
    }

    @Test
    fun roundTripEmptyContent() {
        val key = "empty"
        files.write(key, "")
        assertEquals("", files.read(key))
    }

    @Test
    fun roundTripLargeContent() {
        val key = "large"
        val content = "Lorem ipsum ".repeat(10_000)
        files.write(key, content)
        assertEquals(content, files.read(key))
    }

    @Test
    fun overwriteSameBook() {
        val key = "overwrite"
        files.write(key, "Original content")
        assertEquals("Original content", files.read(key))

        files.write(key, "Updated content")
        assertEquals("Updated content", files.read(key))

        files.write(key, "Final version\nWith newline")
        assertEquals("Final version\nWith newline", files.read(key))
    }

    @Test
    fun readNonExistentReturnsNull() {
        assertNull(files.read("does_not_exist"))
    }

    @Test
    fun readMissingDirectoryReturnsNull() {
        val missing = File(tempDir, "no-books-dir")
        assertNull(BookContentFiles(missing).read("any"))
    }

    @Test
    fun writeCreatesDirectoryIfMissing() {
        val booksDir = File(tempDir, "books")
        assertFalse(booksDir.exists())
        val nested = BookContentFiles(booksDir)
        nested.write("first", "hello")
        assertTrue(booksDir.isDirectory)
        assertEquals("hello", nested.read("first"))
    }

    @Test
    fun readInvalidKeyReturnsNull() {
        assertNull(files.read("invalid/key"))
        assertNull(files.read("invalid\\key"))
        assertNull(files.read(""))
        assertNull(files.read("   "))
    }

    @Test
    fun writeInvalidKeyThrows() {
        assertFailsWith<IllegalArgumentException> {
            files.write("invalid/key", "content")
        }
        assertFailsWith<IllegalArgumentException> {
            files.write("invalid\\key", "content")
        }
        assertFailsWith<IllegalArgumentException> {
            files.write("", "content")
        }
        assertFailsWith<IllegalArgumentException> {
            files.write("   ", "content")
        }
    }

    @Test
    fun deleteRemovesFile() {
        val key = "to_delete"
        files.write(key, "content")
        assertTrue(File(tempDir, "$key.txt").exists())

        files.delete(key)
        assertFalse(File(tempDir, "$key.txt").exists())
        assertNull(files.read(key))
    }

    @Test
    fun deleteNonExistentDoesNotThrow() {
        files.delete("does_not_exist")
    }

    @Test
    fun deleteOrphansRemovesUnwantedFiles() {
        files.write("keep1", "content1")
        files.write("keep2", "content2")
        files.write("orphan1", "orphan content")
        files.write("orphan2", "more orphan content")

        val deleted = files.deleteOrphans(setOf("keep1", "keep2"))

        assertEquals(setOf("orphan1", "orphan2"), deleted)
        assertEquals("content1", files.read("keep1"))
        assertEquals("content2", files.read("keep2"))
        assertNull(files.read("orphan1"))
        assertNull(files.read("orphan2"))
    }

    @Test
    fun deleteOrphansWithEmptyKeepSetDeletesAll() {
        files.write("file1", "content1")
        files.write("file2", "content2")

        val deleted = files.deleteOrphans(emptySet())

        assertEquals(setOf("file1", "file2"), deleted)
        assertNull(files.read("file1"))
        assertNull(files.read("file2"))
    }

    @Test
    fun deleteOrphansIgnoresNonTxtFiles() {
        files.write("keep", "content")
        File(tempDir, "other.json").writeText("{}")
        File(tempDir, "leftover.txt.tmp").writeText("partial")

        val deleted = files.deleteOrphans(setOf("keep"))

        assertTrue(deleted.isEmpty())
        assertTrue(File(tempDir, "other.json").exists())
        assertTrue(File(tempDir, "leftover.txt.tmp").exists())
    }

    @Test
    fun deleteOrphansUsesHashCodeStorageKeys() {
        val keep = BookContentFiles.storageKey("keep-book")
        val orphan = BookContentFiles.storageKey("removed-book")
        assertNotEquals(keep, orphan)
        files.write(keep, "keep body")
        files.write(orphan, "orphan body")

        val deleted = files.deleteOrphans(setOf(keep))

        assertEquals(setOf(orphan), deleted)
        assertEquals("keep body", files.read(keep))
        assertNull(files.read(orphan))
    }

    @Test
    fun deleteOrphansOnEmptyDirectory() {
        val deleted = files.deleteOrphans(setOf("any"))
        assertTrue(deleted.isEmpty())
    }

    @Test
    fun listKeysReturnsAllStoredKeys() {
        files.write("key1", "c1")
        files.write("key2", "c2")
        files.write("key3", "c3")

        val keys = files.listKeys()

        assertEquals(setOf("key1", "key2", "key3"), keys)
    }

    @Test
    fun listKeysIgnoresTmpFiles() {
        files.write("real", "content")
        File(tempDir, "leftover.txt.tmp").writeText("temp")

        val keys = files.listKeys()

        assertEquals(setOf("real"), keys)
    }

    @Test
    fun listKeysOnEmptyDirectory() {
        val keys = files.listKeys()
        assertTrue(keys.isEmpty())
    }

    @Test
    fun cleanupTempFilesRemovesTmpFiles() {
        files.write("keep", "content")
        File(tempDir, "orphan.txt.tmp").writeText("temp1")
        File(tempDir, "another.txt.tmp").writeText("temp2")

        files.cleanupTempFiles()

        assertTrue(File(tempDir, "keep.txt").exists())
        assertFalse(File(tempDir, "orphan.txt.tmp").exists())
        assertFalse(File(tempDir, "another.txt.tmp").exists())
    }

    @Test
    fun atomicWriteDoesNotLeavePartialFile() {
        val key = "atomic"
        files.write(key, "complete content")

        assertFalse(File(tempDir, "$key.txt.tmp").exists())
        assertTrue(File(tempDir, "$key.txt").exists())
    }

    @Test
    fun storageKeyComputation() {
        assertEquals("0", BookContentFiles.storageKey(""))
        assertEquals("book-123".hashCode().toString(), BookContentFiles.storageKey("book-123"))
        assertEquals("my-unique-id".hashCode().toString(), BookContentFiles.storageKey("my-unique-id"))
    }

    @Test
    fun negativeHashCodeStorageKey() {
        val id = (0..10_000).map { "book-$it" }.first { it.hashCode() < 0 }
        val key = BookContentFiles.storageKey(id)
        assertTrue(key.startsWith("-"))
        files.write(key, "content for negative hash")
        assertEquals("content for negative hash", files.read(key))
    }
}
