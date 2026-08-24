package com.evgeniich.flashread.core.speedread

import kotlin.math.ceil
import kotlin.math.roundToLong

enum class SpeedReadPlayerStatus {
    Paused,
    Playing,
    Finished,
}

data class SpeedReadSessionTotals(
    val tokenCount: Int,
    val delayUnits: Double,
) {
    companion object {
        val Empty = SpeedReadSessionTotals(tokenCount = 0, delayUnits = 0.0)
    }
}

data class SpeedReadPlayerViewState(
    val status: SpeedReadPlayerStatus,
    val text: String,
    val position: SpeedReadPosition,
    val progress: Float,
    val elapsedMs: Long,
    val remainingMs: Long,
    val settings: SpeedReadSettings,
    val isEmpty: Boolean,
) {
    val isPlaying: Boolean get() = status == SpeedReadPlayerStatus.Playing
    val isFinished: Boolean get() = status == SpeedReadPlayerStatus.Finished

    companion object {
        fun placeholder(settings: SpeedReadSettings) = SpeedReadPlayerViewState(
            status = SpeedReadPlayerStatus.Paused,
            text = "",
            position = SpeedReadPosition.Empty,
            progress = 0f,
            elapsedMs = 0,
            remainingMs = 0,
            settings = settings,
            isEmpty = true,
        )
    }
}

/**
 * Mutable player state. Timing still comes from [SpeedReadPlayback.delayMs];
 * this class only decides whether to advance, pause, restart, or finish.
 */
class SpeedReadPlayerController(
    private val playback: SpeedReadPlayback,
    private var totals: SpeedReadSessionTotals,
    initialPosition: SpeedReadPosition,
    initialSettings: SpeedReadSettings,
) {
    private var status: SpeedReadPlayerStatus = initialStatus()
    private var position: SpeedReadPosition = initialPosition
    private var settings: SpeedReadSettings = initialSettings.normalized()
    private var elapsedUnits: Double = 0.0
    private var elapsedIsAbsolute: Boolean = initialPosition.tokenIndex == 0

    var viewState: SpeedReadPlayerViewState = computeViewState()
        private set

    fun currentDelayMs(): Long = playback.delayMs(position, settings.wpm)

    fun togglePlayPause() {
        if (status == SpeedReadPlayerStatus.Playing) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        if (playback.isEmpty) {
            status = SpeedReadPlayerStatus.Finished
            publish()
            return
        }
        if (status == SpeedReadPlayerStatus.Finished) {
            restartInternal()
        }
        status = SpeedReadPlayerStatus.Playing
        publish()
    }

    fun pause() {
        if (status == SpeedReadPlayerStatus.Playing) {
            status = SpeedReadPlayerStatus.Paused
            publish()
        }
    }

    fun stepForward() {
        moveToNext(fromTicker = false)
    }

    fun stepBack() {
        if (playback.isEmpty) return
        val previous = playback.previous(position)
        position = previous
        elapsedUnits = playback.elapsedDelayUnits(position)
        elapsedIsAbsolute = true
        if (status == SpeedReadPlayerStatus.Finished) {
            status = SpeedReadPlayerStatus.Paused
        }
        publish()
    }

    fun applySessionMetrics(totals: SpeedReadSessionTotals, elapsedAtInitialPosition: Double) {
        this.totals = totals
        if (!elapsedIsAbsolute) {
            elapsedUnits += elapsedAtInitialPosition
            elapsedIsAbsolute = true
        }
        if (status == SpeedReadPlayerStatus.Finished && totals.delayUnits > 0.0) {
            elapsedUnits = totals.delayUnits
        }
        publish()
    }

    fun restart() {
        restartInternal()
        status = if (playback.isEmpty) {
            SpeedReadPlayerStatus.Finished
        } else {
            SpeedReadPlayerStatus.Paused
        }
        publish()
    }

    fun updateSettings(updated: SpeedReadSettings) {
        settings = updated.normalized()
        publish()
    }

    /** Called by the UI after [currentDelayMs] has elapsed while playing. */
    fun onTick() {
        if (status != SpeedReadPlayerStatus.Playing) return
        moveToNext(fromTicker = true)
    }

    private fun moveToNext(fromTicker: Boolean) {
        if (playback.isEmpty) {
            status = SpeedReadPlayerStatus.Finished
            publish()
            return
        }
        val wasLast = playback.isLastChunk(position)
        val next = playback.next(position, settings.loopEnabled)
        if (next != null) {
            elapsedUnits = if (wasLast && next.tokenIndex == 0) {
                elapsedIsAbsolute = true
                0.0
            } else {
                elapsedUnits + playback.delayUnitsAt(position)
            }
            position = next
            if (status == SpeedReadPlayerStatus.Finished) {
                status = SpeedReadPlayerStatus.Paused
            }
        } else if (fromTicker) {
            status = SpeedReadPlayerStatus.Finished
            if (totals.delayUnits > 0.0) {
                elapsedUnits = totals.delayUnits
                elapsedIsAbsolute = true
            }
        }
        publish()
    }

    private fun restartInternal() {
        position = playback.startPosition(0)
        elapsedUnits = 0.0
        elapsedIsAbsolute = true
    }

    private fun initialStatus(): SpeedReadPlayerStatus {
        return if (playback.isEmpty) {
            SpeedReadPlayerStatus.Finished
        } else {
            SpeedReadPlayerStatus.Paused
        }
    }

    private fun computeViewState(): SpeedReadPlayerViewState {
        val remainingUnits = (totals.delayUnits - elapsedUnits).coerceAtLeast(0.0)
        val progress = when {
            playback.isEmpty || totals.tokenCount <= 0 -> 0f
            status == SpeedReadPlayerStatus.Finished -> 1f
            else -> (position.tokenIndex.toFloat() / totals.tokenCount).coerceIn(0f, 1f)
        }
        return SpeedReadPlayerViewState(
            status = status,
            text = playback.chunkAt(position)?.displayText.orEmpty(),
            position = position,
            progress = progress,
            elapsedMs = delayUnitsToMs(elapsedUnits, settings.wpm),
            remainingMs = delayUnitsToMs(remainingUnits, settings.wpm),
            settings = settings,
            isEmpty = playback.isEmpty,
        )
    }

    private fun publish() {
        viewState = computeViewState()
    }
}

internal fun delayUnitsToMs(units: Double, wpm: Int): Long {
    if (units <= 0.0) return 0L
    val clampedWpm = wpm.coerceIn(SpeedReadDefaults.MIN_WPM, SpeedReadDefaults.MAX_WPM)
    return (60_000.0 / clampedWpm * units).roundToLong()
}

internal fun remainingMsToMinutes(remainingMs: Long): Int {
    if (remainingMs <= 0L) return 0
    return ceil(remainingMs / 60_000.0).toInt()
}
