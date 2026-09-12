package com.evgeniich.flashread.core.speedread

data class SpeedReadSettings(
    val wpm: Int = SpeedReadDefaults.DEFAULT_WPM,
    val chunkSize: Int = SpeedReadDefaults.DEFAULT_CHUNK_SIZE,
    val spritzEnabled: Boolean = SpeedReadDefaults.DEFAULT_SPRITZ_ENABLED,
    val loopEnabled: Boolean = SpeedReadDefaults.DEFAULT_LOOP_ENABLED,
    val textSize: Int = SpeedReadDefaults.DEFAULT_TEXT_SIZE,
) {
    val isSpritzAvailable: Boolean
        get() = chunkSize <= SpeedReadDefaults.MIN_CHUNK_SIZE

    val effectiveSpritzEnabled: Boolean
        get() = spritzEnabled && isSpritzAvailable

    fun normalized(): SpeedReadSettings = copy(
        wpm = SpeedReadDefaults.snapWpm(wpm),
        chunkSize = chunkSize.coerceIn(SpeedReadDefaults.MIN_CHUNK_SIZE, SpeedReadDefaults.MAX_CHUNK_SIZE),
        textSize = SpeedReadDefaults.snapTextSize(textSize),
    )
}
