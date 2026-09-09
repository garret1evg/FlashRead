package com.evgeniich.flashread.navigation

import androidx.navigation3.runtime.NavKey

sealed interface AppRoute : NavKey {
    data object Home : AppRoute
    data object Library : AppRoute
    data object Reader : AppRoute
    data object SpeedRead : AppRoute
    data object SpeedReadPlayer : AppRoute
    data object Settings : AppRoute
    data object PrivacyPolicy : AppRoute
    data object Terms : AppRoute
    data object BookEditor : AppRoute
    data object QuickSpeedRead : AppRoute
}

val AppRoute.isTopLevel: Boolean
    get() = when (this) {
        AppRoute.Home, AppRoute.Library, AppRoute.Settings -> true
        AppRoute.Reader,
        AppRoute.SpeedRead,
        AppRoute.SpeedReadPlayer,
        AppRoute.PrivacyPolicy,
        AppRoute.Terms,
        AppRoute.BookEditor,
        AppRoute.QuickSpeedRead,
        -> false
    }

/** Banner ads sit above the bottom bar on Home and Library only. */
val AppRoute.showsBannerAd: Boolean
    get() = this is AppRoute.Home || this is AppRoute.Library
