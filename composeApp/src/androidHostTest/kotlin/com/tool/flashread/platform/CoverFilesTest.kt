package com.tool.flashread.platform

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoverFilesTest {

    private lateinit var tempDir: File
    private lateinit var covers: CoverFiles

    @BeforeTest
    fun setUp() {
        tempDir = createTempDirectory("cover_files_test").toFile()
        covers = CoverFiles(tempDir)
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun findCoverFileNameFindsJpgOnDisk() {
        val bookId = "book-1"
        val fileName = coverFileNameFor(bookId, "image/jpeg")
        File(tempDir, fileName).writeBytes(byteArrayOf(1, 2, 3))

        assertEquals(fileName, covers.findCoverFileName(bookId))
    }

    @Test
    fun findCoverFileNameReturnsNullWhenMissing() {
        assertNull(covers.findCoverFileName("missing-book"))
    }

    @Test
    fun findCoverFileNameReturnsNullWhenDirectoryMissing() {
        val missingDir = File(tempDir, "does-not-exist")
        assertNull(CoverFiles(missingDir).findCoverFileName("book-1"))
    }

    @Test
    fun findCoverFileNamePicksNewestWhenMultipleExtensionsExist() {
        val bookId = "book-reimport"
        val hex = coverHashHex(bookId)
        val jpg = File(tempDir, "$hex.jpg").apply { writeBytes(byteArrayOf(1)) }
        val png = File(tempDir, "$hex.png").apply { writeBytes(byteArrayOf(2)) }

        jpg.setLastModified(1_000L)
        png.setLastModified(2_000L)

        assertEquals("$hex.png", covers.findCoverFileName(bookId))
    }

    @Test
    fun findCoverFileNamePicksLexicographicMinWhenLastModifiedTies() {
        val bookId = "book-tie"
        val hex = coverHashHex(bookId)
        val jpg = File(tempDir, "$hex.jpg").apply { writeBytes(byteArrayOf(1)) }
        val png = File(tempDir, "$hex.png").apply { writeBytes(byteArrayOf(2)) }
        val webp = File(tempDir, "$hex.webp").apply { writeBytes(byteArrayOf(3)) }

        jpg.setLastModified(5_000L)
        png.setLastModified(5_000L)
        webp.setLastModified(5_000L)

        assertEquals("$hex.jpg", covers.findCoverFileName(bookId))
    }

    @Test
    fun findCoverFileNameIgnoresOtherBooks() {
        val bookId = "wanted"
        val otherId = "other"
        File(tempDir, coverFileNameFor(otherId, "image/jpeg")).writeBytes(byteArrayOf(9))
        val wanted = coverFileNameFor(bookId, "image/png")
        File(tempDir, wanted).writeBytes(byteArrayOf(1))

        assertEquals(wanted, covers.findCoverFileName(bookId))
        assertEquals(coverFileNameFor(otherId, "image/jpeg"), covers.findCoverFileName(otherId))
    }

    @Test
    fun findCoverFileNameRequiresDotAfterHashPrefix() {
        val bookId = "prefix-book"
        val hex = coverHashHex(bookId)
        File(tempDir, "${hex}extra.jpg").writeBytes(byteArrayOf(1))
        File(tempDir, coverFileNameFor(bookId, "image/jpeg")).writeBytes(byteArrayOf(2))

        assertEquals(coverFileNameFor(bookId, "image/jpeg"), covers.findCoverFileName(bookId))
    }

    @Test
    fun saveCreatesDirectoryIfMissing() {
        val coversDir = File(tempDir, "covers")
        assertFalse(coversDir.exists())
        val nested = CoverFiles(coversDir)
        val fileName = nested.save("new-book", byteArrayOf(7), "image/png")
        assertTrue(coversDir.isDirectory)
        assertEquals(fileName, nested.findCoverFileName("new-book"))
    }

    @Test
    fun saveWritesBytesAndFindCoverFileNameResolvesThem() {
        val bookId = "saved-book"
        val bytes = byteArrayOf(10, 20, 30)
        val fileName = covers.save(bookId, bytes, "image/jpeg")

        assertEquals(coverFileNameFor(bookId, "image/jpeg"), fileName)
        assertTrue(File(tempDir, fileName).isFile)
        assertContentEquals(bytes, covers.load(fileName))
        assertEquals(fileName, covers.findCoverFileName(bookId))
    }

    @Test
    fun saveRemovesOtherExtensionsForTheSameBook() {
        val bookId = "reimported"
        val jpgName = covers.save(bookId, byteArrayOf(1, 1, 1), "image/jpeg")
        assertTrue(File(tempDir, jpgName).isFile)

        val pngName = covers.save(bookId, byteArrayOf(2, 2, 2), "image/png")

        assertEquals(coverFileNameFor(bookId, "image/png"), pngName)
        assertTrue(File(tempDir, pngName).isFile)
        assertFalse(File(tempDir, jpgName).exists())
        assertEquals(pngName, covers.findCoverFileName(bookId))
        assertContentEquals(byteArrayOf(2, 2, 2), covers.load(pngName))
    }

    @Test
    fun saveOverwritesSameExtension() {
        val bookId = "overwrite"
        val fileName = covers.save(bookId, byteArrayOf(1), "image/jpeg")
        covers.save(bookId, byteArrayOf(9, 9), "image/jpeg")

        val files = tempDir.listFiles()?.filter { it.isFile }.orEmpty()
        assertEquals(1, files.size)
        assertEquals(fileName, files.single().name)
        assertContentEquals(byteArrayOf(9, 9), covers.load(fileName))
    }

    @Test
    fun saveDoesNotDeleteCoversOfOtherBooks() {
        val first = covers.save("first", byteArrayOf(1), "image/jpeg")
        val second = covers.save("second", byteArrayOf(2), "image/png")

        covers.save("first", byteArrayOf(3), "image/webp")

        assertFalse(File(tempDir, first).exists())
        assertTrue(File(tempDir, second).isFile)
        assertEquals(coverFileNameFor("second", "image/png"), covers.findCoverFileName("second"))
    }

    @Test
    fun loadRejectsPathSeparators() {
        assertNull(covers.load("../secret.jpg"))
        assertNull(covers.load("dir\\secret.jpg"))
        assertNull(covers.load(""))
    }

    @Test
    fun resolveReturnsExistingFile() {
        val fileName = covers.save("resolve-book", byteArrayOf(4, 5), "image/png")
        val resolved = covers.resolve(fileName)
        assertEquals(File(tempDir, fileName), resolved)
        assertTrue(resolved!!.isFile)
    }

    @Test
    fun resolveReturnsNullWhenMissingOrUnsafe() {
        assertNull(covers.resolve("missing.jpg"))
        assertNull(covers.resolve("../secret.jpg"))
        assertNull(covers.resolve("dir\\secret.jpg"))
        assertNull(covers.resolve(""))
    }

    @Test
    fun deleteRemovesFile() {
        val fileName = covers.save("to-delete", byteArrayOf(1), "image/jpeg")
        covers.delete(fileName)
        assertFalse(File(tempDir, fileName).exists())
        assertNull(covers.findCoverFileName("to-delete"))
    }
}
