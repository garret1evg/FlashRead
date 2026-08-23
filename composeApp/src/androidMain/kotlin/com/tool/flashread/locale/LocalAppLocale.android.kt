package com.tool.flashread.locale

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

actual object LocalAppLocale {
    private var default: Locale? = null

    actual val current: String
        @Composable get() = Locale.getDefault().toString()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val configuration = Configuration(LocalConfiguration.current)
        if (default == null) {
            default = Locale.getDefault()
        }
        val new = when (value) {
            null -> default!!
            else -> Locale.forLanguageTag(value)
        }
        Locale.setDefault(new)
        configuration.setLocales(LocaleList(new))
        val resources = LocalContext.current.resources
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return LocalConfiguration.provides(configuration)
    }
}
