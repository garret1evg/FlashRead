package com.evgeniich.flashread.platform

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.evgeniich.flashread.navigation.AppRoute
import com.evgeniich.flashread.resources.Res
import com.evgeniich.flashread.resources.*
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

data class ExternalBookImportState(
    val sessionId: Long = 0,
    val phase: Phase = Phase.Idle,
    val book: ImportedBook? = null,
    val error: String? = null,
) {
    enum class Phase { Idle, Importing, Success, Error }
}

object ExternalBookImporter {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(ExternalBookImportState())
    private var sessionSeq = 0L
    private var importJob: Job? = null

    val state: StateFlow<ExternalBookImportState> = _state.asStateFlow()

    fun handleIntent(
        intent: Intent?,
        contentResolver: ContentResolver,
        cacheDir: File,
    ) {
        val uri = intent?.bookOpenUri() ?: return
        start(uri = uri, contentResolver = contentResolver, cacheDir = cacheDir)
    }

    fun start(
        uri: Uri,
        contentResolver: ContentResolver,
        cacheDir: File,
    ) {
        importJob?.cancel()
        val sessionId = ++sessionSeq
        _state.value = ExternalBookImportState(sessionId = sessionId, phase = ExternalBookImportState.Phase.Importing)
        importJob = scope.launch {
            try {
                val fallbackTitle = getString(Res.string.import_fallback_title)
                val imported = withContext(Dispatchers.IO) {
                    importBookFromUri(contentResolver, uri, cacheDir, fallbackTitle)
                }
                if (imported.content.isBlank()) {
                    _state.value = ExternalBookImportState(
                        sessionId = sessionId,
                        phase = ExternalBookImportState.Phase.Error,
                        error = getString(Res.string.import_file_empty),
                    )
                } else {
                    _state.value = ExternalBookImportState(
                        sessionId = sessionId,
                        phase = ExternalBookImportState.Phase.Success,
                        book = imported,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.value = ExternalBookImportState(
                    sessionId = sessionId,
                    phase = ExternalBookImportState.Phase.Error,
                    error = localizedImportError(error.message),
                )
            }
        }
    }

    fun consume(sessionId: Long) {
        val current = _state.value
        if (current.sessionId == sessionId && current.phase != ExternalBookImportState.Phase.Importing) {
            _state.value = ExternalBookImportState()
        }
    }
}

actual fun launchRouteForExternalBookOpen(): AppRoute? {
    return if (ExternalBookImporter.state.value.phase == ExternalBookImportState.Phase.Idle) {
        null
    } else {
        AppRoute.Library
    }
}

internal fun Intent.bookOpenUri(): Uri? {
    return when (action) {
        Intent.ACTION_VIEW -> data ?: firstClipUri()
        Intent.ACTION_SEND -> extraStreamUri() ?: data ?: firstClipUri()
        else -> null
    }
}

private fun Intent.firstClipUri(): Uri? {
    val clip = clipData ?: return null
    if (clip.itemCount <= 0) return null
    return clip.getItemAt(0).uri
}

private fun Intent.extraStreamUri(): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
    }
}
