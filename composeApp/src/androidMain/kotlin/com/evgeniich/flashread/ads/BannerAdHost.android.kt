package com.evgeniich.flashread.ads

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.evgeniich.flashread.consent.ConsentManager
import com.evgeniich.flashread.shared.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import timber.log.Timber
import kotlin.math.roundToInt

/** Google sample banner unit. Debug builds must not request production ads. */
private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

@Composable
actual fun BannerAdHost(modifier: Modifier) {
    if (!canShowBannerAds()) {
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val adWidthDp = resolveAdWidthDp(maxWidth)
        if (adWidthDp <= 0) {
            return@BoxWithConstraints
        }

        AdaptiveBannerAdView(adWidthDp = adWidthDp)
    }
}

@Composable
private fun AdaptiveBannerAdView(adWidthDp: Int) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val orientation = LocalConfiguration.current.orientation

    val adSize = remember(adWidthDp, orientation) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)
    }
    if (adSize == AdSize.INVALID) {
        Timber.w("Invalid adaptive banner size for width %sdp", adWidthDp)
        return
    }

    val adUnitId = remember(context) { resolveBannerAdUnitId(context) }
    val adView = remember(adSize, adUnitId) {
        AdView(context).apply {
            setAdSize(adSize)
            this.adUnitId = adUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Timber.d("Banner ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("Banner ad failed to load: %s - %s", error.code, error.message)
                }

                override fun onAdOpened() {
                    Timber.d("Banner ad opened")
                }

                override fun onAdClicked() {
                    Timber.d("Banner ad clicked")
                }

                override fun onAdClosed() {
                    Timber.d("Banner ad closed")
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(adView) {
        if (ConsentManager.canRequestAds()) {
            adView.loadAd(AdRequest.Builder().build())
            Timber.d("Loading banner ad %sx%s", adSize.width, adSize.height)
        } else {
            Timber.d("Skipping banner load: consent does not allow requesting ads")
        }

        onDispose {
            adView.destroy()
            Timber.d("Banner ad destroyed")
        }
    }

    key(adView) {
        AndroidView(
            factory = { adView },
            modifier = Modifier
                .fillMaxWidth()
                .height(adSize.height.dp),
        )
    }
}

@Composable
private fun resolveAdWidthDp(maxWidth: Dp): Int {
    val context = LocalContext.current
    val metrics = context.resources.displayMetrics
    val fallbackWidthDp = (metrics.widthPixels / metrics.density).roundToInt()
    val constrainedWidthDp = maxWidth
        .takeIf { it.isSpecified && it.value.isFinite() && it.value > 0f }
        ?.value
        ?.roundToInt()
    return (constrainedWidthDp ?: fallbackWidthDp).coerceAtLeast(1)
}

actual fun canShowBannerAds(): Boolean {
    return AdMobManager.isInitialized && ConsentManager.canRequestAds()
}

/**
 * Debug / debuggable builds always use the Google sample banner unit.
 * Release builds use [R.string.admob_banner_unit_id] from the app module
 * (currently a TODO placeholder until a production unit is created in AdMob Console).
 */
private fun resolveBannerAdUnitId(context: Context): String {
    if (isDebuggable(context)) {
        Timber.d("Using test banner ad unit ID")
        return TEST_BANNER_AD_UNIT_ID
    }
    val configured = context.getString(R.string.admob_banner_unit_id)
    if (configured.isBlank()) {
        Timber.e("Empty @string/admob_banner_unit_id; falling back to test banner unit")
        return TEST_BANNER_AD_UNIT_ID
    }
    return configured
}

private fun isDebuggable(context: Context): Boolean {
    return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}
