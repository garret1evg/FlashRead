package com.evgeniich.flashread.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.evgeniich.flashread.monetization.AdLevel
import com.evgeniich.flashread.monetization.InterstitialBlockReason
import com.evgeniich.flashread.monetization.MonetizationConfig
import com.evgeniich.flashread.monetization.MonetizationManager
import com.evgeniich.flashread.monetization.MonetizationPolicy
import com.evgeniich.flashread.monetization.MonetizationState
import com.evgeniich.flashread.monetization.TimeProvider
import com.evgeniich.flashread.ui.theme.FlashReadDimens
import com.evgeniich.flashread.ui.theme.FlashReadShapes
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Developer diagnostics menu, shown only when developer mode is unlocked.
 * All text is English hardcoded, never localized.
 */
@Composable
fun DeveloperMenu(
    state: MonetizationState,
    onLockDeveloperMode: () -> Unit,
    onSetActiveUsageTime: (Long) -> Unit,
    onGrantReward: () -> Unit,
    onClearReward: () -> Unit,
    onSetUsageDays: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentTimeMs by remember { mutableLongStateOf(TimeProvider.currentTimeMs()) }
    var showTimeOverrideDialog by remember { mutableStateOf(false) }
    var showDaysOverrideDialog by remember { mutableStateOf(false) }

    // Pull live usage from the tracker and refresh clock every second.
    LaunchedEffect(Unit) {
        while (true) {
            MonetizationManager.refreshStateFromUsageTracker(touchLastActivity = false)
            currentTimeMs = TimeProvider.currentTimeMs()
            delay(1000)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section title
        Text(
            text = "Developer",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = FlashReadDimens.space8),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = FlashReadShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(modifier = Modifier.padding(FlashReadDimens.space16)) {
                // === Usage Section ===
                DiagnosticsSectionTitle("Usage")
                DiagnosticsRow(
                    "Total active usage",
                    formatDuration(state.totalUsageMs) + " (${state.totalUsageMs} ms)",
                )
                DiagnosticsRow(
                    "Distinct usage days",
                    "${state.uniqueUsageDays}",
                )
                if (state.usageDates.isNotEmpty()) {
                    DiagnosticsRow(
                        "Usage dates",
                        state.usageDates.joinToString(", "),
                    )
                } else {
                    DiagnosticsRow("Usage dates", "none recorded")
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = FlashReadDimens.space8),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                // === Ad Level Section ===
                DiagnosticsSectionTitle("Ad Level")
                DiagnosticsRow("Calculated level", state.calculatedLevel.name)
                DiagnosticsRow("Applied layout level", state.appliedLayoutLevel.name)
                DiagnosticsRow(
                    "Pending transition",
                    if (state.hasPendingLevelTransition) {
                        "${state.appliedLayoutLevel.name} → ${state.calculatedLevel.name}"
                    } else {
                        "none"
                    },
                )
                DiagnosticsRow(
                    "Level 2 ever activated",
                    if (state.level2EverActivated) "yes" else "no",
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = FlashReadDimens.space8),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                // === Thresholds Section ===
                DiagnosticsSectionTitle("Thresholds (from config)")
                DiagnosticsRow(
                    "Initial ad-free",
                    formatDuration(MonetizationConfig.initialAdFreePeriodMs),
                )
                DiagnosticsRow(
                    "Level 1",
                    formatDuration(MonetizationConfig.level1ThresholdMs),
                )
                DiagnosticsRow(
                    "Level 2",
                    "${formatDuration(MonetizationConfig.level2TimeThresholdMs)} + ${MonetizationConfig.level2DaysThreshold} days",
                )
                DiagnosticsRow(
                    "Reward duration",
                    formatDuration(MonetizationConfig.rewardDurationMs),
                )
                DiagnosticsRow(
                    "Session timeout",
                    formatDuration(MonetizationConfig.sessionTimeoutMs),
                )
                DiagnosticsRow(
                    "Interstitial interval",
                    formatDuration(MonetizationConfig.interstitialIntervalMs),
                )
                DiagnosticsRow(
                    "Interstitial caps",
                    "session: ${MonetizationConfig.interstitialSessionCap}, daily: ${MonetizationConfig.interstitialDailyCap}",
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = FlashReadDimens.space8),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                // === Session Section ===
                DiagnosticsSectionTitle("Session")
                DiagnosticsRow("Session ID", "#${state.sessionId}")
                DiagnosticsRow(
                    "Session start",
                    state.sessionStartMs?.let { formatTimestamp(it) } ?: "N/A",
                )
                DiagnosticsRow(
                    "Session interstitial consumed",
                    "${state.sessionInterstitialCount} / ${MonetizationConfig.interstitialSessionCap}",
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = FlashReadDimens.space8),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                // === Interstitial Section ===
                DiagnosticsSectionTitle("Interstitial Eligibility")
                DiagnosticsRow(
                    "Current date",
                    TimeProvider.currentDateString(),
                )
                DiagnosticsRow(
                    "Daily count",
                    "${state.dailyInterstitialCount} / ${MonetizationConfig.interstitialDailyCap}" +
                        (state.lastInterstitialResetDate?.let { " (reset: $it)" } ?: ""),
                )
                DiagnosticsRow(
                    "Eligibility accumulated",
                    formatDuration(state.interstitialEligibilityAccumulatedMs),
                )
                val remainingEligibility = (MonetizationConfig.interstitialIntervalMs - state.interstitialEligibilityAccumulatedMs)
                    .coerceAtLeast(0)
                DiagnosticsRow(
                    "Eligibility remaining",
                    formatDuration(remainingEligibility),
                )

                // Block reasons
                val blockReason = MonetizationPolicy.getInterstitialBlockReason(
                    state = state,
                    currentTimeMs = currentTimeMs,
                    context = MonetizationPolicy.InterstitialContext(
                        isHintBeingShown = false,
                        isAdReady = true,
                        isAppInForeground = true,
                    ),
                )
                DiagnosticsRow(
                    "Block reason",
                    blockReason?.toDisplayString() ?: "none (eligible)",
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = FlashReadDimens.space8),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                // === Reward Section ===
                DiagnosticsSectionTitle("Reward")
                val isRewardActive = state.isRewardActive(currentTimeMs)
                DiagnosticsRow("Reward active", if (isRewardActive) "yes" else "no")
                if (state.rewardExpiresAtMs != null) {
                    DiagnosticsRow("Reward expires", formatTimestamp(state.rewardExpiresAtMs))
                    if (isRewardActive) {
                        val remaining = state.rewardExpiresAtMs - currentTimeMs
                        DiagnosticsRow("Remaining", formatDuration(remaining.coerceAtLeast(0)))
                    }
                }
                DiagnosticsRow(
                    "Reward ever earned",
                    if (state.hasEverEarnedReward) "yes" else "no",
                )
                DiagnosticsRow(
                    "Hint shown",
                    if (state.rewardHintShown) "yes" else "no",
                )
                Spacer(Modifier.height(FlashReadDimens.space8))
                OutlinedButton(
                    onClick = onGrantReward,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRewardActive,
                ) {
                    Text(
                        if (isRewardActive) {
                            "Grant 2-hour reward (already active)"
                        } else {
                            "Grant 2-hour reward"
                        },
                    )
                }
                Spacer(Modifier.height(FlashReadDimens.space8))
                OutlinedButton(
                    onClick = onClearReward,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.rewardExpiresAtMs != null,
                ) {
                    Text("Clear reward")
                }
                Spacer(Modifier.height(FlashReadDimens.space8))
                Text(
                    text = "Diagnostic override — does not play an ad.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = FlashReadDimens.space8),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                // === Actions ===
                Spacer(Modifier.height(FlashReadDimens.space8))

                OutlinedButton(
                    onClick = { showTimeOverrideDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Set active usage time")
                }

                Spacer(Modifier.height(FlashReadDimens.space8))

                OutlinedButton(
                    onClick = { showDaysOverrideDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Set usage days")
                }

                Spacer(Modifier.height(FlashReadDimens.space8))

                Button(
                    onClick = onLockDeveloperMode,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("Lock developer menu")
                }
            }
        }
    }

    if (showTimeOverrideDialog) {
        TimeOverrideDialog(
            currentTotalMs = state.totalUsageMs,
            onDismiss = { showTimeOverrideDialog = false },
            onConfirm = { newTotalMs ->
                showTimeOverrideDialog = false
                onSetActiveUsageTime(newTotalMs)
            },
        )
    }

    if (showDaysOverrideDialog) {
        DaysOverrideDialog(
            currentDays = state.uniqueUsageDays,
            onDismiss = { showDaysOverrideDialog = false },
            onConfirm = { newDays ->
                showDaysOverrideDialog = false
                onSetUsageDays(newDays)
            },
        )
    }
}

@Composable
private fun DiagnosticsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = FlashReadDimens.space4),
    )
}

@Composable
private fun DiagnosticsRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(FlashReadDimens.space8))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TimeOverrideDialog(
    currentTotalMs: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    // Initialize from current total
    val currentHours = (currentTotalMs / 3600000).toInt()
    val currentMinutes = ((currentTotalMs % 3600000) / 60000).toInt()
    val currentSeconds = ((currentTotalMs % 60000) / 1000).toInt()

    var hoursText by remember { mutableStateOf(currentHours.toString()) }
    var minutesText by remember { mutableStateOf(currentMinutes.toString()) }
    var secondsText by remember { mutableStateOf(currentSeconds.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmation by remember { mutableStateOf(false) }
    var pendingTotalMs by remember { mutableLongStateOf(0L) }

    fun validateAndCalculate(): Long? {
        val hours = hoursText.toLongOrNull()
        val minutes = minutesText.toIntOrNull()
        val seconds = secondsText.toIntOrNull()

        when {
            hours == null || hours < 0 -> {
                errorMessage = "Hours must be a non-negative number"
                return null
            }
            minutes == null || minutes < 0 || minutes > 59 -> {
                errorMessage = "Minutes must be 0-59"
                return null
            }
            seconds == null || seconds < 0 || seconds > 59 -> {
                errorMessage = "Seconds must be 0-59"
                return null
            }
        }

        // Check for overflow
        val totalMs = try {
            val hoursMs = hours * 3600000L
            val minutesMs = minutes * 60000L
            val secondsMs = seconds * 1000L
            // Check for overflow by verifying hours doesn't exceed a reasonable limit
            if (hours > Long.MAX_VALUE / 3600000L) {
                errorMessage = "Total time is too large (overflow)"
                return null
            }
            hoursMs + minutesMs + secondsMs
        } catch (e: Exception) {
            errorMessage = "Total time is too large (overflow)"
            return null
        }

        errorMessage = null
        return totalMs
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Confirm time override") },
            text = {
                Column {
                    Text(
                        text = "Set active usage time to:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(FlashReadDimens.space8))
                    Text(
                        text = formatDuration(pendingTotalMs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "($pendingTotalMs ms)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(FlashReadDimens.space16))
                    Text(
                        text = "Note: Changing time does not add usage days. " +
                            "Level 2 still requires 5 hours and 3 distinct dates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onConfirm(pendingTotalMs) }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = FlashReadShapes.card,
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Set active usage time") },
            text = {
                Column {
                    Text(
                        text = "Enter the total active usage time:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(FlashReadDimens.space16))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FlashReadDimens.space8),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = hoursText,
                            onValueChange = {
                                hoursText = it.filter { c -> c.isDigit() }
                                errorMessage = null
                            },
                            label = { Text("Hours") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = minutesText,
                            onValueChange = {
                                minutesText = it.filter { c -> c.isDigit() }
                                errorMessage = null
                            },
                            label = { Text("Min") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = secondsText,
                            onValueChange = {
                                secondsText = it.filter { c -> c.isDigit() }
                                errorMessage = null
                            },
                            label = { Text("Sec") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(Modifier.height(FlashReadDimens.space8))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Spacer(Modifier.height(FlashReadDimens.space16))
                    Text(
                        text = "Note: Changing time does not add usage days. " +
                            "Level 2 still requires 5 hours and 3 distinct dates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val totalMs = validateAndCalculate()
                        if (totalMs != null) {
                            pendingTotalMs = totalMs
                            showConfirmation = true
                        }
                    },
                    modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = FlashReadShapes.card,
        )
    }
}

@Composable
private fun DaysOverrideDialog(
    currentDays: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var daysText by remember { mutableStateOf(currentDays.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmation by remember { mutableStateOf(false) }
    var pendingDays by remember { mutableIntStateOf(0) }

    fun validateAndParse(): Int? {
        val trimmed = daysText.trim()
        if (trimmed.isEmpty()) {
            errorMessage = "Days must be a non-negative integer"
            return null
        }
        val days = trimmed.toIntOrNull()
        when {
            days == null -> {
                errorMessage = if (trimmed.all { it.isDigit() }) {
                    "Days value is too large (overflow)"
                } else {
                    "Days must be a non-negative integer"
                }
                return null
            }
            days < 0 -> {
                errorMessage = "Days must be a non-negative integer"
                return null
            }
        }
        errorMessage = null
        return days
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Confirm usage days override") },
            text = {
                Column {
                    Text(
                        text = "Set usage days to:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(FlashReadDimens.space8))
                    Text(
                        text = pendingDays.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(FlashReadDimens.space16))
                    Text(
                        text = "Level 2 still requires 5 hours and 3 distinct dates. " +
                            "Changing days does not change active usage time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onConfirm(pendingDays) }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = FlashReadShapes.card,
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Set usage days") },
            text = {
                Column {
                    Text(
                        text = "Enter the number of distinct usage days:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(FlashReadDimens.space16))
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = {
                            daysText = it
                            errorMessage = null
                        },
                        label = { Text("Days") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (errorMessage != null) {
                        Spacer(Modifier.height(FlashReadDimens.space8))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(FlashReadDimens.space16))
                    Text(
                        text = "Level 2 still requires 5 hours and 3 distinct dates. " +
                            "Changing days does not change active usage time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val days = validateAndParse()
                        if (days != null) {
                            pendingDays = days
                            showConfirmation = true
                        }
                    },
                    modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = FlashReadShapes.card,
        )
    }
}

/**
 * Formats milliseconds as "Xh Ym Zs" duration string.
 */
private fun formatDuration(ms: Long): String {
    if (ms < 0) return "N/A"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "${hours}h ${minutes}m ${seconds}s"
}

/**
 * Formats epoch milliseconds as readable timestamp.
 */
private fun formatTimestamp(epochMs: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(epochMs)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${localDateTime.date} ${localDateTime.hour.toString().padStart(2, '0')}:" +
            "${localDateTime.minute.toString().padStart(2, '0')}:" +
            "${localDateTime.second.toString().padStart(2, '0')}"
    } catch (e: Exception) {
        epochMs.toString()
    }
}

/**
 * Converts InterstitialBlockReason to English display string.
 */
private fun InterstitialBlockReason.toDisplayString(): String = when (this) {
    is InterstitialBlockReason.RewardActive -> "active reward"
    is InterstitialBlockReason.LevelTooLow -> "insufficient level (${currentLevel.name})"
    is InterstitialBlockReason.CooldownActive -> "cooldown (${formatDuration(remainingMs)} remaining)"
    is InterstitialBlockReason.SessionCapReached -> "session limit"
    is InterstitialBlockReason.DailyCapReached -> "daily limit"
    is InterstitialBlockReason.NoReadingActivity -> "no reading activity"
    is InterstitialBlockReason.AdNotReady -> "ad not ready"
    is InterstitialBlockReason.AppInBackground -> "background"
    is InterstitialBlockReason.HintShowing -> "hint showing"
}
