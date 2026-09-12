package com.evgeniich.flashread.ui.ads

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evgeniich.flashread.ads.RewardedAdHost
import com.evgeniich.flashread.ads.RewardedAdResult
import com.evgeniich.flashread.ads.canShowRewardedAds
import com.evgeniich.flashread.monetization.AdLevel
import com.evgeniich.flashread.monetization.MonetizationConfig
import com.evgeniich.flashread.monetization.MonetizationManager
import com.evgeniich.flashread.monetization.MonetizationState
import com.evgeniich.flashread.monetization.TimeProvider
import com.evgeniich.flashread.resources.Res
import com.evgeniich.flashread.resources.*
import com.evgeniich.flashread.ui.theme.FlashReadDimens
import com.evgeniich.flashread.ui.theme.FlashReadShapes
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/**
 * Whether the rewarded-ad offer should be shown: after the initial ad-free period,
 * or while a reward is already active (paused-until status). Hidden when ads are
 * not allowed unless a reward is currently active.
 */
fun shouldShowRewardedAdOffer(
    state: MonetizationState,
    nowMs: Long,
    adsAllowed: Boolean,
): Boolean {
    val rewardActive = state.isRewardActive(nowMs)
    val pastInitialPeriod = state.appliedLayoutLevel != AdLevel.Initial ||
        state.calculatedLevel != AdLevel.Initial ||
        state.totalUsageMs >= MonetizationConfig.initialAdFreePeriodMs
    return rewardActive || (pastInitialPeriod && adsAllowed)
}

/**
 * Shared watch-ad offer: explicit tap, loading, English errors, retry, paused-until.
 *
 * Composes nothing when the offer is not eligible. [decorate] wraps the button and
 * optional status (Settings uses a Card; the player uses the default column).
 */
@Composable
fun RewardedAdOffer(
    modifier: Modifier = Modifier,
    statusMaxLines: Int = Int.MAX_VALUE,
    onRewardEarned: (() -> Unit)? = null,
    decorate: (@Composable (inner: @Composable () -> Unit) -> Unit)? = null,
) {
    val state by MonetizationManager.state.collectAsStateWithLifecycle()
    val host = remember { RewardedAdHost.getInstance() }
    var nowMs by remember { mutableLongStateOf(TimeProvider.currentTimeMs()) }
    var isHostLoading by remember { mutableStateOf(host.isLoading()) }
    var isRequesting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = TimeProvider.currentTimeMs()
            isHostLoading = host.isLoading()
            delay(1000)
        }
    }

    val rewardActive = state.isRewardActive(nowMs)
    val adsAllowed = canShowRewardedAds()
    if (!shouldShowRewardedAdOffer(state, nowMs, adsAllowed)) return

    val isBusy = isRequesting || isHostLoading
    val buttonEnabled = adsAllowed && !isBusy && !rewardActive
    val buttonLabel = if (isBusy) {
        stringResource(Res.string.settings_ad_loading)
    } else {
        stringResource(Res.string.settings_watch_ad)
    }
    val pausedUntilText = if (rewardActive) {
        val expiresAt = state.rewardExpiresAtMs
        if (expiresAt != null) {
            rewardedAdsPausedUntilText(expiresAtMs = expiresAt, nowMs = nowMs)
        } else {
            null
        }
    } else {
        null
    }
    val statusText = pausedUntilText ?: errorMessage
    val statusIsError = pausedUntilText == null && errorMessage != null

    val body: @Composable () -> Unit = {
        Button(
            onClick = {
                if (isRequesting || host.isLoading() || !canShowRewardedAds()) return@Button
                isRequesting = true
                errorMessage = null
                host.show { result ->
                    isRequesting = false
                    when (result) {
                        RewardedAdResult.RewardEarned -> {
                            MonetizationManager.clearReadingActivity()
                            errorMessage = null
                            onRewardEarned?.invoke()
                        }
                        RewardedAdResult.ClosedWithoutReward -> {
                            errorMessage = "Ad closed without a reward."
                        }
                        is RewardedAdResult.Failed -> {
                            errorMessage = result.reason
                        }
                        RewardedAdResult.Cancelled -> {
                            errorMessage = null
                        }
                        RewardedAdResult.NotReady -> {
                            errorMessage = "Ad is not ready. Please try again."
                        }
                    }
                }
            },
            enabled = buttonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FlashReadDimens.minTouchTarget),
            shape = FlashReadShapes.button,
        ) {
            Text(
                text = buttonLabel,
                textAlign = TextAlign.Center,
            )
        }
        if (statusText != null) {
            Spacer(Modifier.height(FlashReadDimens.space8))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (statusIsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = statusMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (decorate != null) {
        decorate(body)
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            body()
        }
    }
}

@Composable
internal fun rewardedAdsPausedUntilText(expiresAtMs: Long, nowMs: Long): String {
    val time = formatLocalTime(expiresAtMs)
    return if (isSameLocalDate(expiresAtMs, nowMs)) {
        stringResource(Res.string.settings_ads_paused_until, time)
    } else {
        stringResource(
            Res.string.settings_ads_paused_until_date,
            formatLocalDayMonth(expiresAtMs),
            time,
        )
    }
}

private val MONTH_ABBREVS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private fun formatLocalTime(epochMs: Long): String {
    val local = Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
}

private fun formatLocalDayMonth(epochMs: Long): String {
    val local = Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.dayOfMonth} ${MONTH_ABBREVS[local.month.ordinal]}"
}

private fun isSameLocalDate(epochMsA: Long, epochMsB: Long): Boolean {
    val timeZone = TimeZone.currentSystemDefault()
    val dateA = Instant.fromEpochMilliseconds(epochMsA).toLocalDateTime(timeZone).date
    val dateB = Instant.fromEpochMilliseconds(epochMsB).toLocalDateTime(timeZone).date
    return dateA == dateB
}
