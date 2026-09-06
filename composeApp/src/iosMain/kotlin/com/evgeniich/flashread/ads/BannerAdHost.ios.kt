package com.evgeniich.flashread.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * No-op implementation for iOS.
 * Banner ads are not supported on iOS in this implementation.
 */
@Composable
actual fun BannerAdHost(modifier: Modifier) {
    // No-op on iOS - no banner ads
}

/**
 * Always returns false on iOS as banner ads are not supported.
 */
actual fun canShowBannerAds(): Boolean = false
