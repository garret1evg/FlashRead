package com.evgeniich.flashread

import androidx.compose.ui.window.ComposeUIViewController
import com.evgeniich.flashread.core.theme.AppTheme
import com.evgeniich.flashread.platform.AppThemeStorage
import com.evgeniich.flashread.platform.applyPlatformTheme
import com.evgeniich.flashread.ui.theme.splashBackground
import platform.UIKit.UIColor
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val theme = AppTheme.fromStorage(AppThemeStorage.load())
    applyPlatformTheme(theme)
    val osDark = UIScreen.mainScreen.traitCollection.userInterfaceStyle ==
        UIUserInterfaceStyle.UIUserInterfaceStyleDark
    val splash = theme.splashBackground(osDark)
    return ComposeUIViewController { App() }.apply {
        view.backgroundColor = UIColor.colorWithRed(
            red = splash.red.toDouble(),
            green = splash.green.toDouble(),
            blue = splash.blue.toDouble(),
            alpha = splash.alpha.toDouble(),
        )
    }
}