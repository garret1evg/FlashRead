package com.tool.flashread.navigation

enum class AppScreen(val title: String, val route: AppRoute) {
    Home("Home", AppRoute.Home),
    Library("Library", AppRoute.Library),
    Settings("Settings", AppRoute.Settings);

    companion object {
        fun fromRoute(route: AppRoute): AppScreen? = when (route) {
            AppRoute.Home -> Home
            AppRoute.Library -> Library
            AppRoute.Settings -> Settings
            AppRoute.Reader,
            AppRoute.SpeedRead,
            AppRoute.SpeedReadPlayer,
            AppRoute.PrivacyPolicy,
            AppRoute.Terms,
            AppRoute.BookEditor,
            AppRoute.QuickSpeedRead,
            -> null
        }
    }
}
