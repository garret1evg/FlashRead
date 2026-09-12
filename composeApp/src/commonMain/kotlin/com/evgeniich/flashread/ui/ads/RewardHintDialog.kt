package com.evgeniich.flashread.ui.ads

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.evgeniich.flashread.ads.RewardedAdHost
import com.evgeniich.flashread.monetization.MonetizationManager
import com.evgeniich.flashread.resources.Res
import com.evgeniich.flashread.resources.*
import com.evgeniich.flashread.ui.theme.FlashReadDimens
import com.evgeniich.flashread.ui.theme.FlashReadShapes
import org.jetbrains.compose.resources.stringResource

/**
 * One-time hint after Level 2 activation: watching a rewarded ad pauses ads
 * for two hours, and the same action is available later in Settings.
 *
 * Marks the hint as shown on first presentation. Does not auto-start an ad.
 */
@Composable
fun RewardHintDialog(
    onDismiss: () -> Unit,
    onRewardEarned: () -> Unit,
) {
    val host = remember { RewardedAdHost.getInstance() }

    LaunchedEffect(Unit) {
        MonetizationManager.markRewardHintShown()
    }

    DisposableEffect(Unit) {
        onDispose { host.cancelPendingShow() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.reward_hint_title),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.reward_hint_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(FlashReadDimens.space16))
                RewardedAdOffer(onRewardEarned = onRewardEarned)
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
            ) {
                Text(stringResource(Res.string.reward_hint_dismiss))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = FlashReadShapes.card,
    )
}
