package com.evgeniich.flashread.platform

import androidx.compose.ui.graphics.Color
import com.evgeniich.flashread.core.theme.AppTheme
import com.evgeniich.flashread.ui.theme.splashBackground
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

actual fun applyPlatformTheme(theme: AppTheme) {
    val osDark = UIScreen.mainScreen.traitCollection.userInterfaceStyle ==
        UIUserInterfaceStyle.UIUserInterfaceStyleDark
    val background = theme.splashBackground(osDark).toUIColor()
    val style = when (theme) {
        AppTheme.Dark -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
        AppTheme.Light, AppTheme.Sepia -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
        AppTheme.System -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
    }
    UIApplication.sharedApplication.connectedScenes
        .mapNotNull { it as? UIWindowScene }
        .flatMap { scene -> scene.windows.mapNotNull { window -> window as? UIWindow } }
        .forEach { window ->
            window.overrideUserInterfaceStyle = style
            window.backgroundColor = background
        }
}

private fun Color.toUIColor(): UIColor {
    return UIColor.colorWithRed(
        red = red.toDouble(),
        green = green.toDouble(),
        blue = blue.toDouble(),
        alpha = alpha.toDouble(),
    )
}
