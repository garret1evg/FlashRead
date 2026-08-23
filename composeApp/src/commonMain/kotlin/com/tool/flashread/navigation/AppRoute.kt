package com.tool.flashread.navigation

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

val AppRoute.showsScaffoldTopBar: Boolean
    get() = this is AppRoute.SpeedRead ||
        this is AppRoute.PrivacyPolicy ||
        this is AppRoute.Terms

val AppRoute.title: String
    get() = when (this) {
        AppRoute.Home -> "Home"
        AppRoute.Library -> "Library"
        AppRoute.Reader -> "Reader"
        AppRoute.SpeedRead -> "Скорочтение"
        AppRoute.SpeedReadPlayer -> "SpeedRead"
        AppRoute.Settings -> "Settings"
        AppRoute.PrivacyPolicy -> "Privacy Policy"
        AppRoute.Terms -> "Terms & Conditions"
        AppRoute.BookEditor -> "Редактор"
        AppRoute.QuickSpeedRead -> "Скорочтение"
    }
