package com.evgeniich.flashread.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.evgeniich.flashread.core.locale.isRtlLocaleOverride

expect object LocalAppLocale {
    val current: String
        @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}

@Composable
fun AppEnvironment(
    localeOverride: String?,
    content: @Composable () -> Unit,
) {
    val layoutDirection = if (isRtlLocaleOverride(localeOverride)) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    CompositionLocalProvider(
        LocalAppLocale provides localeOverride,
        LocalLayoutDirection provides layoutDirection,
    ) {
        key(localeOverride) {
            content()
        }
    }
}
