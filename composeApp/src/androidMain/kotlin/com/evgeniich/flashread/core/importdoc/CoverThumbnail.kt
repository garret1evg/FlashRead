package com.evgeniich.flashread.core.importdoc

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * Shrinks an extracted cover to thumbnail resolution for library tiles.
 * Returns JPEG bytes when decoding succeeds; otherwise keeps a small original
 * or drops a large undecodable payload.
 */
internal object CoverThumbnail {
    fun prepare(bytes: ByteArray, mimeType: String?): Pair<ByteArray, String>? {
        if (bytes.isEmpty()) return null
        scaleToJpeg(bytes)?.let { return it to "image/jpeg" }
        if (bytes.size <= BookCoverLimits.MAX_UNSCALED_FALLBACK_BYTES) {
            return bytes to (mimeType?.takeIf { it.startsWith("image/") } ?: "image/jpeg")
        }
        return null
    }
}

internal fun coverInSampleSize(width: Int, height: Int, maxEdge: Int = BookCoverLimits.MAX_EDGE_PX): Int {
    if (width <= 0 || height <= 0 || maxEdge <= 0) return 1
    var sample = 1
    val longest = maxOf(width, height)
    while (longest / (sample * 2) >= maxEdge) {
        sample *= 2
    }
    return sample
}

internal fun scaledCoverSize(width: Int, height: Int, maxEdge: Int = BookCoverLimits.MAX_EDGE_PX): Pair<Int, Int> {
    if (width <= 0 || height <= 0 || maxEdge <= 0) return 1 to 1
    val longest = maxOf(width, height)
    if (longest <= maxEdge) return width to height
    val scale = maxEdge.toFloat() / longest
    return (width * scale).roundToInt().coerceAtLeast(1) to
        (height * scale).roundToInt().coerceAtLeast(1)
}

private fun scaleToJpeg(bytes: ByteArray): ByteArray? {
    var decoded: Bitmap? = null
    var scaled: Bitmap? = null
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val decode = BitmapFactory.Options().apply {
            inSampleSize = coverInSampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decode) ?: return null
        val (targetWidth, targetHeight) = scaledCoverSize(decoded.width, decoded.height)
        scaled = if (decoded.width == targetWidth && decoded.height == targetHeight) {
            decoded
        } else {
            Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true)
        }
        val out = ByteArrayOutputStream()
        if (!scaled.compress(Bitmap.CompressFormat.JPEG, BookCoverLimits.JPEG_QUALITY, out)) {
            return null
        }
        out.toByteArray().takeIf { it.isNotEmpty() }
    } catch (_: Throwable) {
        null
    } finally {
        val scaledBitmap = scaled
        val decodedBitmap = decoded
        if (scaledBitmap != null && scaledBitmap != decodedBitmap) {
            scaledBitmap.recycle()
        }
        decodedBitmap?.recycle()
    }
}
