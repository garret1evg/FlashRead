package com.evgeniich.flashread.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Host composable for displaying a banner advertisement.
 * On Android, this displays an AdMob adaptive banner.
 * On iOS, this is a no-op (empty composable).
 *
 * @param modifier Modifier to be applied to the banner container
 */
@Composable
expect fun BannerAdHost(modifier: Modifier = Modifier)

/**
 * Returns true if banner ads can be shown on this platform.
 * On Android, this checks if consent has been given and AdMob is initialized.
 * On iOS, this always returns false.
 */
expect fun canShowBannerAds(): Boolean
