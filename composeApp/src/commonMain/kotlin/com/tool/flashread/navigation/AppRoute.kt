package com.tool.flashread.navigation

import androidx.navigation3.runtime.NavKey

sealed interface AppRoute : NavKey {
    data object Home : AppRoute
    data object Library : AppRoute
    data object Reader : AppRoute
    data object SpeedRead : AppRoute
    data object SpeedReadPlayer : AppRoute
    data object Settings : AppRoute
}

val AppRoute.isTopLevel: Boolean
    get() = when (this) {
        AppRoute.Home, AppRoute.Library, AppRoute.Settings -> true
        AppRoute.Reader, AppRoute.SpeedRead, AppRoute.SpeedReadPlayer -> false
    }

val AppRoute.title: String
    get() = when (this) {
        AppRoute.Home -> "Home"
        AppRoute.Library -> "Library"
        AppRoute.Reader -> "Reader"
        AppRoute.SpeedRead -> "SpeedRead"
        AppRoute.SpeedReadPlayer -> "SpeedRead"
        AppRoute.Settings -> "Settings"
    }
