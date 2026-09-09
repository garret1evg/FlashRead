package com.evgeniich.flashread.platform

import android.app.Activity
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.toArgb
import com.evgeniich.flashread.core.theme.AppTheme
import com.evgeniich.flashread.ui.theme.splashBackground

fun applyLaunchTheme(activity: Activity) {
    val theme = AppTheme.fromStorage(AppThemeStorage.load())
    applyNightMode(theme)
    if (theme.resolve(isNightMode(activity)) == AppTheme.Sepia) {
        val sepiaTheme = activity.resources.getIdentifier(
            "Theme.FlashRead.Sepia",
            "style",
            activity.packageName,
        )
        if (sepiaTheme != 0) {
            activity.setTheme(sepiaTheme)
        }
    }
}

actual fun applyPlatformTheme(theme: AppTheme) {
    applyNightMode(theme)
    val activity = AndroidAppContext.currentActivity ?: return
    applyWindowAndSplash(activity, theme)
}

private fun applyNightMode(theme: AppTheme) {
    val nightMode = when (theme) {
        AppTheme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        AppTheme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
        AppTheme.Light, AppTheme.Sepia -> AppCompatDelegate.MODE_NIGHT_NO
    }
    if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}

private fun applyWindowAndSplash(activity: Activity, theme: AppTheme) {
    val splashColor = theme.splashBackground(isNightMode(activity)).toArgb()
    activity.window.setBackgroundDrawable(ColorDrawable(splashColor))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val styleName = if (theme.resolve(isNightMode(activity)) == AppTheme.Sepia) {
            "Theme.FlashRead.Sepia"
        } else {
            "Theme.FlashRead"
        }
        val styleId = activity.resources.getIdentifier(styleName, "style", activity.packageName)
        if (styleId != 0) {
            activity.splashScreen.setSplashScreenTheme(styleId)
        }
    }
}

private fun isNightMode(activity: Activity): Boolean {
    val night = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return night == Configuration.UI_MODE_NIGHT_YES
}
