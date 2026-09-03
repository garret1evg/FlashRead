package com.evgeniich.flashread.analytics

import com.evgeniich.flashread.core.reading.ReaderTextSettings
import com.evgeniich.flashread.core.speedread.SpeedReadSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class SettingsChangeLogger(
    private val analytics: AnalyticsLogger,
    private val scope: CoroutineScope,
    private val debounceMs: Long = DEBOUNCE_MS,
) {
    private val jobs = mutableMapOf<AnalyticsEvent.SettingsChange.SettingName, Job>()
    private val pending = mutableMapOf<AnalyticsEvent.SettingsChange.SettingName, String>()

    fun logReaderDiff(previous: ReaderTextSettings, updated: ReaderTextSettings) {
        if (previous.theme != updated.theme) {
            logImmediate(AnalyticsEvent.SettingsChange.SettingName.Theme, updated.theme.name.lowercase())
        }
        if (previous.alignment != updated.alignment) {
            logImmediate(
                AnalyticsEvent.SettingsChange.SettingName.Alignment,
                updated.alignment.name.lowercase(),
            )
        }
        if (previous.fontSizeSp != updated.fontSizeSp) {
            logDebounced(
                AnalyticsEvent.SettingsChange.SettingName.FontSize,
                updated.fontSizeSp.toString(),
            )
        }
        if (previous.lineHeightMultiplier != updated.lineHeightMultiplier) {
            logDebounced(
                AnalyticsEvent.SettingsChange.SettingName.LineHeight,
                updated.lineHeightMultiplier.toString(),
            )
        }
    }

    fun logSpeedReadDiff(previous: SpeedReadSettings, updated: SpeedReadSettings) {
        if (previous.wpm != updated.wpm) {
            logDebounced(AnalyticsEvent.SettingsChange.SettingName.Wpm, updated.wpm.toString())
        }
        if (previous.chunkSize != updated.chunkSize) {
            logImmediate(
                AnalyticsEvent.SettingsChange.SettingName.ChunkSize,
                updated.chunkSize.toString(),
            )
        }
        if (previous.spritzEnabled != updated.spritzEnabled) {
            logImmediate(
                AnalyticsEvent.SettingsChange.SettingName.SpritzEnabled,
                updated.spritzEnabled.toString(),
            )
        }
        if (previous.loopEnabled != updated.loopEnabled) {
            logImmediate(
                AnalyticsEvent.SettingsChange.SettingName.LoopEnabled,
                updated.loopEnabled.toString(),
            )
        }
    }

    fun flush() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        val toLog = pending.toMap()
        pending.clear()
        toLog.forEach { (name, value) -> logImmediate(name, value) }
    }

    private fun logImmediate(name: AnalyticsEvent.SettingsChange.SettingName, value: String) {
        jobs.remove(name)?.cancel()
        pending.remove(name)
        analytics.log(AnalyticsEvent.SettingsChange(name, value))
    }

    private fun logDebounced(name: AnalyticsEvent.SettingsChange.SettingName, value: String) {
        pending[name] = value
        jobs[name]?.cancel()
        jobs[name] = scope.launch {
            delay(debounceMs)
            val latest = pending.remove(name) ?: return@launch
            jobs.remove(name)
            analytics.log(AnalyticsEvent.SettingsChange(name, latest))
        }
    }

    companion object {
        const val DEBOUNCE_MS = 400L
    }
}
