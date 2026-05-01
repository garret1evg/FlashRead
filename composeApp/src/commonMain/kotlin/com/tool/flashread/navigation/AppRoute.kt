package com.tool.flashread.navigation

import androidx.navigation3.runtime.NavKey

sealed interface AppRoute : NavKey {
    data object Library : AppRoute
    data object Reader : AppRoute
    data object SpeedRead : AppRoute
    data object Settings : AppRoute
}
