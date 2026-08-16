package com.tool.flashread.core.importdoc

/**
 * Covers are only shown as a 48×72 dp library thumbnail.
 * Source files may be several megabytes; we keep a 5 MB read cap, then
 * store a JPEG whose longest edge is 288 px (72 dp at xxxhdpi).
 */
internal object BookCoverLimits {
    const val MAX_SOURCE_BYTES = 5 * 1024 * 1024
    const val MAX_EDGE_PX = 288
    const val JPEG_QUALITY = 80
    const val MAX_UNSCALED_FALLBACK_BYTES = 80 * 1024
}
