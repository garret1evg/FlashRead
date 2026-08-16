package com.tool.flashread.ui.speedread

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.tool.flashread.core.model.Book
import com.tool.flashread.core.speedread.SpeedReadDefaults
import com.tool.flashread.core.speedread.SpeedReadPlayback
import com.tool.flashread.core.speedread.SpeedReadPlayerController
import com.tool.flashread.core.speedread.SpeedReadPlayerStatus
import com.tool.flashread.core.speedread.SpeedReadPlayerViewState
import com.tool.flashread.core.speedread.SpeedReadPosition
import com.tool.flashread.core.speedread.SpeedReadSessionTotals
import com.tool.flashread.core.speedread.SpeedReadSettings
import com.tool.flashread.core.speedread.orpParts
import com.tool.flashread.ui.theme.FlashReadDimens
import com.tool.flashread.ui.theme.FlashReadShapes
import com.tool.flashread.ui.theme.FlashReadTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val OrpFrameHeight = 168.dp
private val PlayerWordSize = 34.sp

@Composable
fun SpeedReadPlayerScreen(
    book: Book,
    settings: SpeedReadSettings,
    startParagraphIndex: Int = 0,
    onParagraphIndexChanged: (Int) -> Unit = {},
    onSettingsChange: (SpeedReadSettings) -> Unit = {},
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var localSettings by remember(book.id) { mutableStateOf(settings.normalized()) }
    var savedTokenIndex by rememberSaveable(book.id) { mutableIntStateOf(-1) }
    var savedOffset by rememberSaveable(book.id) { mutableIntStateOf(0) }
    var savedParagraph by rememberSaveable(book.id) { mutableIntStateOf(startParagraphIndex) }
    var resumeOnStart by remember { mutableStateOf(false) }
    var lastSavedParagraph by remember(book.id) { mutableIntStateOf(startParagraphIndex) }

    val session by produceState<SpeedReadPlayerSession?>(
        initialValue = null,
        book.id,
        book.content,
        localSettings.chunkSize,
    ) {
        value = null
        value = withContext(Dispatchers.Default) {
            val playback = SpeedReadPlayback(book.content, localSettings.chunkSize)
            val totals = playback.sessionTotals()
            val restored = SpeedReadPosition(savedTokenIndex, savedOffset, savedParagraph)
            val start = when {
                savedTokenIndex >= 0 &&
                    savedTokenIndex % localSettings.chunkSize == 0 &&
                    playback.chunkAt(restored) != null -> restored
                savedTokenIndex >= 0 -> playback.startPosition(savedParagraph)
                else -> playback.startPosition(startParagraphIndex)
            }
            SpeedReadPlayerSession(playback, totals, start)
        }
    }

    val controller = remember(session) {
        session?.let { prepared ->
            SpeedReadPlayerController(
                playback = prepared.playback,
                totals = prepared.totals,
                initialPosition = prepared.start,
                initialSettings = localSettings,
            )
        }
    }
    var viewState by remember(controller) { mutableStateOf(controller?.viewState) }

    fun persistPosition(force: Boolean = false) {
        val position = controller?.viewState?.position ?: return
        savedTokenIndex = position.tokenIndex
        savedOffset = position.offset
        savedParagraph = position.paragraphIndex
        if (force || position.paragraphIndex != lastSavedParagraph) {
            lastSavedParagraph = position.paragraphIndex
            onParagraphIndexChanged(position.paragraphIndex)
        }
    }

    fun publish() {
        val current = controller ?: return
        viewState = current.viewState
        persistPosition()
    }

    fun changeSettings(updated: SpeedReadSettings) {
        val normalized = updated.normalized()
        localSettings = normalized
        controller?.updateSettings(normalized)
        onSettingsChange(normalized)
        publish()
    }

    LaunchedEffect(controller, viewState?.status, viewState?.position, viewState?.settings?.wpm) {
        val current = controller ?: return@LaunchedEffect
        if (current.viewState.status != SpeedReadPlayerStatus.Playing) return@LaunchedEffect
        delay(current.currentDelayMs())
        current.onTick()
        publish()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        persistPosition(force = true)
        if (controller?.viewState?.isPlaying == true) {
            resumeOnStart = true
            controller.pause()
            publish()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (resumeOnStart) {
            resumeOnStart = false
            controller?.play()
            publish()
        }
    }

    DisposableEffect(controller) {
        onDispose { persistPosition(force = true) }
    }

    val currentState = viewState
    if (controller == null || currentState == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    SpeedReadPlayerPane(
        state = currentState,
        onClose = {
            persistPosition(force = true)
            onClose()
        },
        onRestart = {
            controller.restart()
            publish()
        },
        onTogglePlayPause = {
            controller.togglePlayPause()
            publish()
        },
        onPrevious = {
            controller.stepBack()
            publish()
        },
        onNext = {
            controller.stepForward()
            publish()
        },
        onSettingsChange = ::changeSettings,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpeedReadPlayerPane(
    state: SpeedReadPlayerViewState,
    onClose: () -> Unit,
    onRestart: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSettingsChange: (SpeedReadSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showWpmSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val enabled = !state.isEmpty

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            PlayerTopBar(
                onClose = onClose,
                onRestart = onRestart,
                onSettings = { showSettingsSheet = true },
                restartEnabled = enabled,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(
                        enabled = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTogglePlayPause,
                    )
                    .semantics { contentDescription = playPauseLabel(state) },
                contentAlignment = Alignment.Center,
            ) {
                OrpWordFrame(
                    text = state.text,
                    spritzEnabled = state.settings.spritzEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PlayerBottomBar(
                state = state,
                enabled = enabled,
                onPrevious = onPrevious,
                onTogglePlayPause = onTogglePlayPause,
                onNext = onNext,
                onOpenWpm = { showWpmSheet = true },
            )
        }
    }

    if (showWpmSheet) {
        PlayerWpmSheet(
            wpm = state.settings.wpm,
            onWpmChange = { onSettingsChange(state.settings.copy(wpm = it)) },
            onDismiss = { showWpmSheet = false },
        )
    }
    if (showSettingsSheet) {
        PlayerSettingsSheet(
            settings = state.settings,
            onSettingsChange = onSettingsChange,
            onDismiss = { showSettingsSheet = false },
        )
    }
}

@Composable
private fun PlayerTopBar(
    onClose: () -> Unit,
    onRestart: () -> Unit,
    onSettings: () -> Unit,
    restartEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerIconButton(
            onClick = onClose,
            imageVector = Icons.Filled.Close,
            contentDescription = "Закрыть",
        )
        Spacer(Modifier.weight(1f))
        PlayerIconButton(
            onClick = onRestart,
            imageVector = Icons.Filled.Replay,
            contentDescription = "Начать сначала",
            enabled = restartEnabled,
        )
        PlayerIconButton(
            onClick = onSettings,
            imageVector = Icons.Filled.Settings,
            contentDescription = "Настройки",
        )
    }
}

@Composable
private fun PlayerBottomBar(
    state: SpeedReadPlayerViewState,
    enabled: Boolean,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenWpm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Прогресс ${formatPlayerClock(state.elapsedMs)} из " +
                        formatPlayerClock(state.elapsedMs + state.remainingMs)
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatPlayerClock(state.elapsedMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.widthIn(min = 36.dp),
            )
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                text = formatPlayerClock(state.remainingMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.widthIn(min = 36.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerIconButton(
                onClick = onPrevious,
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Предыдущее",
                enabled = enabled,
                size = 56.dp,
                iconSize = 32.dp,
            )
            PlayerIconButton(
                onClick = onTogglePlayPause,
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = playPauseLabel(state),
                enabled = enabled,
                size = 64.dp,
                iconSize = 36.dp,
            )
            PlayerIconButton(
                onClick = onNext,
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Следующее",
                enabled = enabled,
                size = 56.dp,
                iconSize = 32.dp,
            )
        }
        TextButton(
            onClick = onOpenWpm,
            enabled = enabled,
            modifier = Modifier
                .heightIn(min = FlashReadDimens.minTouchTarget)
                .semantics { contentDescription = "${state.settings.wpm} слов в минуту" },
        ) {
            Text(
                text = "${state.settings.wpm} WPM",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OrpWordFrame(
    text: String,
    spritzEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val pivotColor = MaterialTheme.colorScheme.primary
    val markerColor = MaterialTheme.colorScheme.outline
    val parts = remember(text, spritzEnabled) { orpParts(text, spritzEnabled) }
    val textStyle = MaterialTheme.typography.headlineLarge.copy(
        fontSize = PlayerWordSize,
        fontWeight = FontWeight.Medium,
        color = onSurface,
        letterSpacing = 0.sp,
    )
    val annotated = remember(text, parts.pivotIndex, onSurface, pivotColor) {
        buildAnnotatedString {
            if (text.isEmpty()) return@buildAnnotatedString
            append(text)
            addStyle(SpanStyle(color = onSurface), 0, text.length)
            val pivotIndex = parts.pivotIndex
            if (pivotIndex != null && pivotIndex in text.indices) {
                addStyle(SpanStyle(color = pivotColor), pivotIndex, pivotIndex + 1)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.height(OrpFrameHeight),
        contentAlignment = Alignment.Center,
    ) {
        val centerX = constraints.maxWidth / 2f
        val layout = textMeasurer.measure(
            text = annotated,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Visible,
        )
        val translationX = when {
            text.isEmpty() -> 0f
            parts.pivotIndex != null && parts.pivotIndex in text.indices -> {
                val box = layout.getBoundingBox(parts.pivotIndex)
                centerX - (box.left + box.width / 2f)
            }
            else -> centerX - layout.size.width / 2f
        }

        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            val marker = 10.dp.toPx()
            val gap = 30.dp.toPx()
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawLine(
                color = markerColor,
                start = Offset(cx, cy - gap - marker),
                end = Offset(cx, cy - gap),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = markerColor,
                start = Offset(cx, cy + gap),
                end = Offset(cx, cy + gap + marker),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }

        if (text.isNotEmpty()) {
            Text(
                text = annotated,
                style = textStyle,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .wrapContentWidth(unbounded = true)
                    .graphicsLayer { this.translationX = translationX },
            )
        }
    }
}

@Composable
private fun PlayerIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(size),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerWpmSheet(
    wpm: Int,
    onWpmChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = FlashReadShapes.sheet,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FlashReadDimens.screenHorizontalPadding)
                .padding(bottom = FlashReadDimens.space24),
        ) {
            Text(
                text = "$wpm WPM",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Слов в минуту",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(FlashReadDimens.space12))
            Slider(
                value = wpm.toFloat(),
                onValueChange = { value -> onWpmChange(SpeedReadDefaults.snapWpm(value.roundToInt())) },
                valueRange = SpeedReadDefaults.MIN_WPM.toFloat()..SpeedReadDefaults.MAX_WPM.toFloat(),
                steps = SpeedReadDefaults.WPM_SLIDER_STEPS,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = FlashReadDimens.minTouchTarget)
                    .semantics { contentDescription = "Скорость чтения, слов в минуту" },
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FlashReadDimens.space8),
                verticalArrangement = Arrangement.spacedBy(FlashReadDimens.space8),
            ) {
                SpeedReadDefaults.WPM_PRESETS.forEach { preset ->
                    FilterChip(
                        selected = wpm == preset,
                        onClick = { onWpmChange(preset) },
                        label = { Text("$preset") },
                        modifier = Modifier.semantics {
                            contentDescription = "Пресет $preset слов в минуту"
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSettingsSheet(
    settings: SpeedReadSettings,
    onSettingsChange: (SpeedReadSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = FlashReadShapes.sheet,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FlashReadDimens.screenHorizontalPadding)
                .padding(bottom = FlashReadDimens.space24),
        ) {
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(FlashReadDimens.space16))
            Text(
                text = "Слов за показ",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(FlashReadDimens.space8))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SpeedReadDefaults.CHUNK_SIZES.forEachIndexed { index, size ->
                    SegmentedButton(
                        selected = settings.chunkSize == size,
                        onClick = { onSettingsChange(settings.copy(chunkSize = size)) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = SpeedReadDefaults.CHUNK_SIZES.size,
                        ),
                        modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
                    ) {
                        Text(text = size.toString())
                    }
                }
            }
            Spacer(Modifier.height(FlashReadDimens.space8))
            PlayerSwitchRow(
                title = "Spritz",
                subtitle = "Подсвечивать опорную букву",
                checked = settings.spritzEnabled,
                onCheckedChange = { onSettingsChange(settings.copy(spritzEnabled = it)) },
            )
            PlayerSwitchRow(
                title = "Повтор",
                subtitle = "Начинать текст заново после окончания",
                checked = settings.loopEnabled,
                onCheckedChange = { onSettingsChange(settings.copy(loopEnabled = it)) },
            )
        }
    }
}

@Composable
private fun PlayerSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = FlashReadDimens.minTouchTarget)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            )
            .padding(vertical = FlashReadDimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(FlashReadDimens.space4))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}

private fun playPauseLabel(state: SpeedReadPlayerViewState): String {
    return when (state.status) {
        SpeedReadPlayerStatus.Playing -> "Пауза"
        SpeedReadPlayerStatus.Finished -> "Начать сначала"
        SpeedReadPlayerStatus.Paused -> "Воспроизведение"
    }
}

internal fun formatPlayerClock(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

private data class SpeedReadPlayerSession(
    val playback: SpeedReadPlayback,
    val totals: SpeedReadSessionTotals,
    val start: SpeedReadPosition,
)

internal object SpeedReadPlayerDemo {
    private val settings = SpeedReadSettings(wpm = 300, chunkSize = 1, spritzEnabled = true)

    val longWord = SpeedReadPlayerViewState(
        status = SpeedReadPlayerStatus.Playing,
        text = "supercalifragilistic",
        position = SpeedReadPosition(tokenIndex = 2, offset = 0, paragraphIndex = 0),
        progress = 0.18f,
        elapsedMs = 24_000,
        remainingMs = 110_000,
        settings = settings,
        isEmpty = false,
    )

    val multiWord = SpeedReadPlayerViewState(
        status = SpeedReadPlayerStatus.Playing,
        text = "one two three",
        position = SpeedReadPosition(tokenIndex = 0, offset = 0, paragraphIndex = 0),
        progress = 0.34f,
        elapsedMs = 8_000,
        remainingMs = 16_000,
        settings = settings.copy(chunkSize = 3),
        isEmpty = false,
    )

    val paused = SpeedReadPlayerViewState(
        status = SpeedReadPlayerStatus.Paused,
        text = "wait,",
        position = SpeedReadPosition(tokenIndex = 4, offset = 0, paragraphIndex = 0),
        progress = 0.52f,
        elapsedMs = 41_000,
        remainingMs = 38_000,
        settings = settings,
        isEmpty = false,
    )

    val finished = SpeedReadPlayerViewState(
        status = SpeedReadPlayerStatus.Finished,
        text = "Done.",
        position = SpeedReadPosition(tokenIndex = 9, offset = 0, paragraphIndex = 0),
        progress = 1f,
        elapsedMs = 90_000,
        remainingMs = 0,
        settings = settings,
        isEmpty = false,
    )
}

@Preview(name = "Long word 320", widthDp = 320, heightDp = 640, showBackground = true)
@Composable
private fun SpeedReadPlayerLongWordPreview() {
    PlayerPreview(SpeedReadPlayerDemo.longWord, dark = false)
}

@Preview(name = "Multi word dark", widthDp = 320, heightDp = 640, showBackground = true)
@Composable
private fun SpeedReadPlayerMultiWordPreview() {
    PlayerPreview(SpeedReadPlayerDemo.multiWord, dark = true)
}

@Preview(name = "Paused 320", widthDp = 320, heightDp = 640, showBackground = true)
@Composable
private fun SpeedReadPlayerPausedPreview() {
    PlayerPreview(SpeedReadPlayerDemo.paused, dark = false)
}

@Preview(name = "Finished dark", widthDp = 320, heightDp = 640, showBackground = true)
@Composable
private fun SpeedReadPlayerFinishedPreview() {
    PlayerPreview(SpeedReadPlayerDemo.finished, dark = true)
}

@Composable
private fun PlayerPreview(state: SpeedReadPlayerViewState, dark: Boolean) {
    FlashReadTheme(darkTheme = dark) {
        SpeedReadPlayerPane(
            state = state,
            onClose = {},
            onRestart = {},
            onTogglePlayPause = {},
            onPrevious = {},
            onNext = {},
            onSettingsChange = {},
        )
    }
}
