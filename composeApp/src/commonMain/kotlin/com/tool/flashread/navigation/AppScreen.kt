package com.tool.flashread.navigation

enum class AppScreen(val title: String, val route: AppRoute) {
    Library("Library", AppRoute.Library),
    Reader("Reader", AppRoute.Reader),
    SpeedRead("SpeedRead", AppRoute.SpeedRead),
    Settings("Settings", AppRoute.Settings);

    companion object {
        fun fromRoute(route: AppRoute): AppScreen = when (route) {
            AppRoute.Library -> Library
            AppRoute.Reader -> Reader
            AppRoute.SpeedRead -> SpeedRead
            AppRoute.Settings -> Settings
        }
    }
}
