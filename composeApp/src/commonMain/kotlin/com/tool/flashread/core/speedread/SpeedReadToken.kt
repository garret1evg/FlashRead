package com.tool.flashread.core.speedread

data class SpeedReadToken(
    val text: String,
    val paragraphIndex: Int,
    val pauseMultiplier: Double,
    val isSentenceEnd: Boolean,
)

data class SpeedReadChunk(
    val tokens: List<SpeedReadToken>,
    val startTokenIndex: Int,
) {
    val displayText: String
        get() = tokens.joinToString(" ") { it.text }

    val paragraphIndex: Int
        get() = tokens.first().paragraphIndex
}
