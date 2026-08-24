package com.evgeniich.flashread.navigation

enum class AppScreen(val route: AppRoute) {
    Home(AppRoute.Home),
    Library(AppRoute.Library),
    Settings(AppRoute.Settings);

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
