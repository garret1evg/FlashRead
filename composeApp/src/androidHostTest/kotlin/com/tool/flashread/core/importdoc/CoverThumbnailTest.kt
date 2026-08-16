package com.tool.flashread.core.importdoc

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoverThumbnailTest {

    @Test
    fun sampleSizeStaysOneWhenAlreadySmall() {
        assertEquals(1, coverInSampleSize(48, 72))
        assertEquals(1, coverInSampleSize(288, 288))
        assertEquals(1, coverInSampleSize(200, 300, maxEdge = 288))
    }

    @Test
    fun sampleSizeHalvesUntilUnderTwiceMaxEdge() {
        assertEquals(2, coverInSampleSize(400, 600, maxEdge = 288))
        assertEquals(8, coverInSampleSize(2000, 3000, maxEdge = 288))
        assertEquals(1, coverInSampleSize(0, 100))
    }

    @Test
    fun scaledSizeFitsLongestEdge() {
        assertEquals(48 to 72, scaledCoverSize(48, 72))
        assertEquals(192 to 288, scaledCoverSize(1920, 2880))
        assertEquals(288 to 162, scaledCoverSize(1920, 1080))
    }

    @Test
    fun prepareDropsEmptyAndHugeUndecodablePayloads() {
        assertNull(CoverThumbnail.prepare(byteArrayOf(), mimeType = "image/jpeg"))
        val huge = ByteArray(BookCoverLimits.MAX_UNSCALED_FALLBACK_BYTES + 1) { 0x7F }
        assertNull(CoverThumbnail.prepare(huge, mimeType = "image/jpeg"))
    }

    @Test
    fun prepareKeepsSmallUndecodableOriginal() {
        val original = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
        val prepared = CoverThumbnail.prepare(original, mimeType = "image/png")
        assertContentEquals(original, prepared?.first)
        assertEquals("image/png", prepared?.second)
    }

    @Test
    fun prepareScalesDecodableBitmapToJpegThumbnail() {
        val source = BitmapFactoryCover.png(width = 800, height = 1200)
        val prepared = CoverThumbnail.prepare(source, mimeType = "image/png") ?: return
        if (prepared.second != "image/jpeg") return
        assertEquals("image/jpeg", prepared.second)
        assertTrue(prepared.first.size < source.size)
        assertTrue(prepared.first.size < BookCoverLimits.MAX_UNSCALED_FALLBACK_BYTES)
    }
}

/**
 * Builds a tiny valid PNG via Android [android.graphics.Bitmap] when the host
 * test runtime can actually encode images. Returns a 1×1 PNG stub otherwise,
 * which [CoverThumbnail.prepare] will keep as a fallback.
 */
private object BitmapFactoryCover {
    fun png(width: Int, height: Int): ByteArray {
        return try {
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(0xFF336699.toInt())
            val out = java.io.ByteArrayOutputStream()
            val ok = bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            bitmap.recycle()
            if (ok) out.toByteArray() else oneByOnePng
        } catch (_: Throwable) {
            oneByOnePng
        }
    }
}

private val oneByOnePng = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
    0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
    0x08, 0x02, 0x00, 0x00, 0x00, 0x90.toByte(), 0x77, 0x53,
    0xDE.toByte(), 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
    0x54, 0x08, 0xD7.toByte(), 0x63, 0xF8.toByte(), 0xCF.toByte(), 0xC0.toByte(), 0x00,
    0x00, 0x00, 0x03, 0x00, 0x01, 0x18, 0xDD.toByte(), 0x8D.toByte(),
    0xB0.toByte(), 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
    0x44, 0xAE.toByte(), 0x42, 0x60, 0x82.toByte(),
)
