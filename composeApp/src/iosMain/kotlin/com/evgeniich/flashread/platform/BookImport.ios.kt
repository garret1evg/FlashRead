package com.evgeniich.flashread.platform

import androidx.compose.runtime.Composable
import com.evgeniich.flashread.analytics.Analytics
import com.evgeniich.flashread.analytics.AnalyticsEvent
import com.evgeniich.flashread.navigation.AppRoute
import com.evgeniich.flashread.resources.Res
import com.evgeniich.flashread.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun rememberBookImportLauncher(
    onImported: (ImportedBook) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val unsupported = stringResource(Res.string.import_ios_unsupported)
    return {
        Analytics.log(
            AnalyticsEvent.BookImport(
                format = AnalyticsEvent.BookImport.Format.Unknown,
                result = AnalyticsEvent.BookImport.Result.Unsupported,
                source = AnalyticsEvent.BookImport.Source.Picker,
            ),
        )
        onError(unsupported)
    }
}

actual fun launchRouteForExternalBookOpen(): AppRoute? = null

@Composable
actual fun ObserveExternalBookOpens(
    onOpenStarted: () -> Unit,
    onImported: (ImportedBook) -> Unit,
    onError: (String) -> Unit,
) = Unit
