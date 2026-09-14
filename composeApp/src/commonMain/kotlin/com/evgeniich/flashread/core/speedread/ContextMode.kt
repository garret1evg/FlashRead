package com.evgeniich.flashread.core.speedread

enum class ContextMode {
    Off,
    WhenPaused,
    Always;

    companion object {
        val DEFAULT = WhenPaused
    }
}

fun ContextMode.shouldShowContext(isPlaying: Boolean): Boolean = when (this) {
    ContextMode.Off -> false
    ContextMode.WhenPaused -> !isPlaying
    ContextMode.Always -> true
}

fun ContextMode.shouldExtractContext(isPlaying: Boolean): Boolean =
    shouldShowContext(isPlaying)

