package com.evgeniich.flashread.ui.speedread

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evgeniich.flashread.ads.BannerAdHost
import com.evgeniich.flashread.ads.RewardedAdHost
import com.evgeniich.flashread.ads.canShowBannerAds
import com.evgeniich.flashread.ads.rememberReservedBannerAdHeight
import com.evgeniich.flashread.core.model.Book
import com.evgeniich.flashread.core.speedread.ContextMode
import com.evgeniich.flashread.core.speedread.ContextWindow
import com.evgeniich.flashread.core.speedread.SpeedReadDefaults
import com.evgeniich.flashread.core.speedread.extractSpeedReadContext
import com.evgeniich.flashread.core.speedread.SpeedReadPlayerStatus
import com.evgeniich.flashread.core.speedread.SpeedReadPlayerViewState
import com.evgeniich.flashread.core.speedread.SpeedReadPosition
import com.evgeniich.flashread.core.speedread.SpeedReadSettings
import com.evgeniich.flashread.core.speedread.orpParts
import com.evgeniich.flashread.core.speedread.spritzWordOverflows
import com.evgeniich.flashread.core.speedread.wrapFlashText
import com.evgeniich.flashread.monetization.MonetizationManager
import com.evgeniich.flashread.navigation.AppRoute
import com.evgeniich.flashread.resources.Res
import com.evgeniich.flashread.resources.*
import com.evgeniich.flashread.ui.ads.RewardedAdOffer
import com.evgeniich.flashread.ui.theme.FlashReadDimens
import com.evgeniich.flashread.ui.theme.FlashReadShapes
import com.evgeniich.flashread.ui.theme.FlashReadTheme
import com.evgeniich.flashread.ui.theme.flashReadSwitchColors
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

private val OrpFrameHeight = 168.dp
private val ContextFontSize = 18.sp
private val ContextLineHeight = 24.sp

@Composable
fun SpeedReadPlayerScreen(
    book: Book,
    onClose: () -> Unit = {},
    keepScreenOn: Boolean = true,
    modifier: Modifier = Modifier,
    viewModel: SpeedReadPlayerViewModel = viewModel(key = book.id) {
        SpeedReadPlayerViewModel(book)
    },
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    // Collect state to trigger recomposition when monetization state changes
    @Suppress("UNUSED_VARIABLE")
    val monetizationState by MonetizationManager.state.collectAsStateWithLifecycle()

    // Freeze banner visibility at playback start to prevent layout changes mid-playback.
    // - L1 play: snapshot false → hidden
    // - L2 play: snapshot true → shown (unless reward active)
    // - Reward granted mid-play: currentShowBanner becomes false → immediately hidden
    // - Reward expires mid-play: frozen stays false if reward was active at start → stays hidden until pause
    val currentShowBanner = canShowBannerAds() &&
        MonetizationManager.shouldShowBanner(AppRoute.SpeedReadPlayer, viewState.isPlaying)

    var frozenPlayingBanner by remember { mutableStateOf(false) }
    val wasPlaying = remember { mutableStateOf(false) }
    if (viewState.isPlaying && !wasPlaying.value) {
        // Snapshot banner state when playback starts
        frozenPlayingBanner = currentShowBanner
    }
    wasPlaying.value = viewState.isPlaying

    // While playing: allow hide (reward granted) but not introduce (threshold/expiry mid-play)
    val showBanner = if (viewState.isPlaying) {
        frozenPlayingBanner && currentShowBanner
    } else {
        currentShowBanner
    }

    // Monetization snapshot for this screen visit: ad-free / reward at entry never
    // grows a banner slot later. Once ads are actually requestable, latch the slot
    // height until leaving so hide/fail/load does not move the active word.
    val enteredWithBannerMonetization = remember {
        MonetizationManager.shouldShowBanner(AppRoute.SpeedReadPlayer, isSpeedReadPlaying = false) ||
            MonetizationManager.shouldShowBanner(AppRoute.SpeedReadPlayer, isSpeedReadPlaying = true)
    }
    var reserveBannerSlot by remember { mutableStateOf(false) }
    if (enteredWithBannerMonetization && canShowBannerAds()) {
        reserveBannerSlot = true
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.onHostStop()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.onHostStart()
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.persistNow()
            RewardedAdHost.getInstance().cancelPendingShow()
        }
    }

    LaunchedEffect(viewState.isPlaying) {
        if (viewState.isPlaying) {
            RewardedAdHost.getInstance().cancelPendingShow()
        } else {
            MonetizationManager.applyLayoutLevel()
        }
    }

    SpeedReadPlayerPane(
        state = viewState,
        content = book.content,
        showBanner = showBanner,
        reserveBannerSlot = reserveBannerSlot,
        showRewardedOffer = !viewState.isPlaying,
        onClose = {
            viewModel.persistNow()
            RewardedAdHost.getInstance().cancelPendingShow()
            onClose()
        },
        onRestart = viewModel::restart,
        onTogglePlayPause = viewModel::togglePlayPause,
        onPrevious = viewModel::stepBack,
        onNext = viewModel::stepForward,
        onSettingsChange = viewModel::updateSettings,
        modifier = modifier.then(
            if (keepScreenOn) Modifier.keepScreenOn() else Modifier
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpeedReadPlayerPane(
    state: SpeedReadPlayerViewState,
    content: String,
    showBanner: Boolean,
    showRewardedOffer: Boolean = false,
    reserveBannerSlot: Boolean = false,
    onClose: () -> Unit,
    onRestart: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSettingsChange: (SpeedReadSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showWpmSheet by remember { mutableStateOf(false) }
    var showTextSizeSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showRestartConfirm by remember { mutableStateOf(false) }
    val enabled = !state.isEmpty
    val playPauseCd = playPauseLabel(state)

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
                onTextSize = { showTextSizeSheet = true },
                onSettings = { showSettingsSheet = true },
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
                    .semantics { contentDescription = playPauseCd },
            ) {
                SpeedReadReadingArea(
                    state = state,
                    content = content,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            val showBannerSlot = reserveBannerSlot || showBanner
            if (showBannerSlot) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val widthDp = maxWidth
                        .takeIf { it.isSpecified && it.value.isFinite() && it.value > 0f }
                        ?.value
                        ?.roundToInt()
                        ?: 0
                    val reservedHeight = rememberReservedBannerAdHeight(widthDp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(reservedHeight),
                    ) {
                        if (showBanner) {
                            BannerAdHost(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            PlayerBottomBar(
                state = state,
                enabled = enabled,
                playPauseLabel = playPauseCd,
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
    if (showTextSizeSheet) {
        PlayerTextSizeSheet(
            textSize = state.settings.textSize,
            onTextSizeChange = { onSettingsChange(state.settings.copy(textSize = it)) },
            onDismiss = { showTextSizeSheet = false },
        )
    }
    if (showSettingsSheet) {
        PlayerSettingsSheet(
            settings = state.settings,
            restartEnabled = enabled,
            onRestartClick = { showRestartConfirm = true },
            onSettingsChange = onSettingsChange,
            onDismiss = { showSettingsSheet = false },
        )
    }
    if (showRestartConfirm) {
        RestartConfirmDialog(
            onDismiss = { showRestartConfirm = false },
            onConfirm = {
                showRestartConfirm = false
                showSettingsSheet = false
                onRestart()
            },
        )
    }
}

@Composable
private fun PlayerTopBar(
    onClose: () -> Unit,
    onTextSize: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textSizeLabel = stringResource(Res.string.player_text_size)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerIconButton(
            onClick = onClose,
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(Res.string.action_close),
        )
        Spacer(Modifier.weight(1f))
        PlayerIconButton(
            onClick = onTextSize,
            imageVector = Icons.Outlined.TextFields,
            contentDescription = textSizeLabel,
        )
        PlayerIconButton(
            onClick = onSettings,
            imageVector = Icons.Filled.Settings,
            contentDescription = stringResource(Res.string.player_settings),
        )
    }
}

@Composable
private fun PlayerBottomBar(
    state: SpeedReadPlayerViewState,
    enabled: Boolean,
    playPauseLabel: String,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenWpm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val elapsedClock = formatPlayerClock(state.elapsedMs)
    val remainingClock = formatPlayerClock(state.remainingMs)
    val totalClock = formatPlayerClock(state.elapsedMs + state.remainingMs)
    val progressCd = stringResource(Res.string.player_progress_cd, elapsedClock, totalClock)
    val wpmCd = stringResource(Res.string.wpm_value_cd, state.settings.wpm)
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
                    contentDescription = progressCd
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = elapsedClock,
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
                text = remainingClock,
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
                contentDescription = stringResource(Res.string.action_previous),
                enabled = enabled,
                size = 56.dp,
                iconSize = 32.dp,
            )
            PlayerIconButton(
                onClick = onTogglePlayPause,
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = playPauseLabel,
                enabled = enabled,
                size = 64.dp,
                iconSize = 36.dp,
            )
            PlayerIconButton(
                onClick = onNext,
                imageVector = Icons.Filled.SkipNext,
                contentDescription = stringResource(Res.string.action_next_item),
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
                .semantics { contentDescription = wpmCd },
        ) {
            Text(
                text = stringResource(Res.string.wpm_value, state.settings.wpm),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SpeedReadReadingArea(
    state: SpeedReadPlayerViewState,
    content: String,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val contextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val contextTextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = ContextFontSize,
        lineHeight = ContextLineHeight,
        color = contextColor,
        textAlign = TextAlign.Center,
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val paddingPx = with(density) {
            FlashReadDimens.screenHorizontalPadding.roundToPx()
        }
        val maxWidthPx = (constraints.maxWidth - paddingPx * 2).coerceAtLeast(0)
        val twoLineSlot = FlashReadDimens.contextSlotHeight
        val oneLineSlot = FlashReadDimens.contextSlotHeight / 2
        val preferredGap = FlashReadDimens.contextGap
        val minGap = FlashReadDimens.contextGapMin
        val sideSpace = ((maxHeight - OrpFrameHeight) / 2).coerceAtLeast(0.dp)
        val contextMaxLines: Int
        val contextSlotHeight: Dp
        val contextGap: Dp
        when {
            sideSpace >= twoLineSlot + preferredGap -> {
                contextMaxLines = 2
                contextSlotHeight = twoLineSlot
                contextGap = preferredGap
            }
            sideSpace >= twoLineSlot + minGap -> {
                contextMaxLines = 2
                contextSlotHeight = twoLineSlot
                contextGap = (sideSpace - twoLineSlot).coerceIn(minGap, preferredGap)
            }
            else -> {
                contextMaxLines = 1
                contextSlotHeight = oneLineSlot
                contextGap = (sideSpace - oneLineSlot).coerceIn(0.dp, preferredGap)
            }
        }
        val fontScale = density.fontScale
        val contextWindow = remember(
            content,
            state.position.tokenIndex,
            state.position.offset,
            state.position.paragraphIndex,
            state.settings.chunkSize,
            maxWidthPx,
            contextMaxLines,
            state.shouldExtractContext,
            fontScale,
        ) {
            if (!state.shouldExtractContext) {
                ContextWindow("", "")
            } else {
                extractSpeedReadContext(
                    content = content,
                    position = state.position,
                    chunkSize = state.settings.chunkSize,
                    maxWidthPx = maxWidthPx,
                    measureWidthPx = { line ->
                        textMeasurer.measure(
                            text = line,
                            style = contextTextStyle,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible,
                        ).size.width
                    },
                    maxLines = contextMaxLines,
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(contextSlotHeight),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        ContextTextBlock(
                            text = contextWindow.beforeText,
                            visible = state.isContextVisible,
                            maxLines = contextMaxLines,
                            textStyle = contextTextStyle,
                        )
                    }
                    Spacer(Modifier.height(contextGap))
                }
            }
            OrpWordFrame(
                text = state.text,
                spritzEnabled = state.settings.effectiveSpritzEnabled,
                wrapToTwoLines = !state.settings.isSpritzAvailable,
                textSize = state.settings.textSize,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(contextGap))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(contextSlotHeight),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        ContextTextBlock(
                            text = contextWindow.afterText,
                            visible = state.isContextVisible,
                            maxLines = contextMaxLines,
                            textStyle = contextTextStyle,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextTextBlock(
    text: String,
    visible: Boolean,
    maxLines: Int,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Text(
        text = if (visible) text else "",
        style = textStyle,
        maxLines = maxLines,
        overflow = TextOverflow.Clip,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FlashReadDimens.screenHorizontalPadding),
    )
}

@Composable
private fun OrpWordFrame(
    text: String,
    spritzEnabled: Boolean,
    wrapToTwoLines: Boolean,
    textSize: Int,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val pivotColor = MaterialTheme.colorScheme.primary
    val markerColor = MaterialTheme.colorScheme.outline
    val fontSize = textSize.sp
    val lineHeight = (textSize + 8).sp
    val textStyle = MaterialTheme.typography.headlineLarge.copy(
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        color = onSurface,
        letterSpacing = 0.sp,
    )

    BoxWithConstraints(
        modifier = modifier.height(OrpFrameHeight),
        contentAlignment = Alignment.Center,
    ) {
        val paddingPx = with(LocalDensity.current) {
            FlashReadDimens.screenHorizontalPadding.roundToPx()
        }
        val maxTextWidth = (constraints.maxWidth - paddingPx * 2).coerceAtLeast(0)
        val centerX = constraints.maxWidth / 2f

        val spritzParts = remember(text, spritzEnabled) {
            orpParts(text, spritzEnabled)
        }
        val spritzAnnotated = remember(text, spritzParts.pivotIndex, onSurface, pivotColor) {
            buildAnnotatedString {
                if (text.isEmpty()) return@buildAnnotatedString
                append(text)
                addStyle(SpanStyle(color = onSurface), 0, text.length)
                val pivotIndex = spritzParts.pivotIndex
                if (pivotIndex != null && pivotIndex in text.indices) {
                    addStyle(SpanStyle(color = pivotColor), pivotIndex, pivotIndex + 1)
                }
            }
        }
        val singleLineLayout = remember(text, textStyle, spritzAnnotated) {
            if (text.isEmpty()) null else {
                textMeasurer.measure(
                    text = spritzAnnotated,
                    style = textStyle,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                )
            }
        }
        val textWidthPx = singleLineLayout?.size?.width ?: 0
        val spritzOverflows = if (spritzEnabled && singleLineLayout != null) {
            val pivotCenterInWord = if (
                spritzParts.pivotIndex != null && spritzParts.pivotIndex in text.indices
            ) {
                val box = singleLineLayout.getBoundingBox(spritzParts.pivotIndex)
                box.left + box.width / 2f
            } else {
                singleLineLayout.size.width / 2f
            }
            spritzWordOverflows(
                wordWidthPx = singleLineLayout.size.width.toFloat(),
                pivotCenterInWordPx = pivotCenterInWord,
                containerWidthPx = constraints.maxWidth,
                paddingPx = paddingPx,
            )
        } else {
            false
        }
        val needsWrapping = wrapToTwoLines || textWidthPx > maxTextWidth || spritzOverflows

        val parts = remember(text, spritzEnabled, needsWrapping) {
            orpParts(text, spritzEnabled && !needsWrapping)
        }
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

        var wrappedTextHeightPx by remember(text, needsWrapping) { mutableStateOf(0f) }
        FlashFocusMarkers(
            color = markerColor,
            textHeightPx = if (needsWrapping) wrappedTextHeightPx else 0f,
        )

        if (text.isEmpty()) return@BoxWithConstraints

        if (needsWrapping) {
            val displayText = remember(text, maxTextWidth, textStyle) {
                wrapFlashText(
                    text = text,
                    maxWidthPx = maxTextWidth,
                    measureWidthPx = { line ->
                        textMeasurer.measure(
                            text = line,
                            style = textStyle,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible,
                        ).size.width
                    },
                    maxLines = 3,
                )
            }
            Text(
                text = displayText,
                style = textStyle.copy(
                    textAlign = TextAlign.Center,
                    lineHeight = lineHeight,
                ),
                maxLines = 3,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FlashReadDimens.screenHorizontalPadding),
                onTextLayout = { wrappedTextHeightPx = it.size.height.toFloat() },
            )
        } else {
            val layout = singleLineLayout ?: textMeasurer.measure(
                text = annotated,
                style = textStyle,
                maxLines = 1,
                overflow = TextOverflow.Visible,
            )
            val translationX = when {
                parts.pivotIndex != null && parts.pivotIndex in text.indices -> {
                    val box = layout.getBoundingBox(parts.pivotIndex)
                    centerX - (box.left + box.width / 2f)
                }
                else -> centerX - layout.size.width / 2f
            }
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
private fun FlashFocusMarkers(
    color: Color,
    textHeightPx: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.fillMaxSize()) {
        val stroke = 2.dp.toPx()
        val marker = 10.dp.toPx()
        val minGap = 30.dp.toPx()
        val gap = if (textHeightPx > 0f) {
            maxOf(minGap, textHeightPx / 2f + 8.dp.toPx())
        } else {
            minGap
        }
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawLine(
            color = color,
            start = Offset(cx, cy - gap - marker),
            end = Offset(cx, cy - gap),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(cx, cy + gap),
            end = Offset(cx, cy + gap + marker),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
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
private fun PlayerModalSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxBodyHeight = with(LocalDensity.current) {
        (LocalWindowInfo.current.containerSize.height / 3f).toDp()
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = FlashReadShapes.sheet,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxBodyHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FlashReadDimens.screenHorizontalPadding)
                .padding(bottom = FlashReadDimens.space24),
            content = content,
        )
    }
}

@Composable
private fun PlayerWpmSheet(
    wpm: Int,
    onWpmChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    PlayerModalSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(Res.string.wpm_value, wpm),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(Res.string.wpm_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(FlashReadDimens.space12))
        val wpmSliderCd = stringResource(Res.string.wpm_slider_cd)
        Slider(
            value = wpm.toFloat(),
            onValueChange = { value -> onWpmChange(SpeedReadDefaults.snapWpm(value.roundToInt())) },
            valueRange = SpeedReadDefaults.MIN_WPM.toFloat()..SpeedReadDefaults.MAX_WPM.toFloat(),
            steps = SpeedReadDefaults.WPM_SLIDER_STEPS,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FlashReadDimens.minTouchTarget)
                .semantics { contentDescription = wpmSliderCd },
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FlashReadDimens.space8),
            verticalArrangement = Arrangement.spacedBy(FlashReadDimens.space8),
        ) {
            SpeedReadDefaults.WPM_PRESETS.forEach { preset ->
                val presetCd = stringResource(Res.string.wpm_preset_cd, preset)
                FilterChip(
                    selected = wpm == preset,
                    onClick = { onWpmChange(preset) },
                    label = { Text("$preset") },
                    modifier = Modifier.semantics {
                        contentDescription = presetCd
                    },
                )
            }
        }
    }
}

@Composable
private fun PlayerTextSizeSheet(
    textSize: Int,
    onTextSizeChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    PlayerModalSheet(onDismiss = onDismiss) {
        Text(
            text = "$textSize sp",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(Res.string.player_text_size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(FlashReadDimens.space12))
        val textSizeCd = stringResource(Res.string.reader_font_size_cd, textSize)
        Slider(
            value = textSize.toFloat(),
            onValueChange = { value -> onTextSizeChange(SpeedReadDefaults.snapTextSize(value.roundToInt())) },
            valueRange = SpeedReadDefaults.MIN_TEXT_SIZE.toFloat()..SpeedReadDefaults.MAX_TEXT_SIZE.toFloat(),
            steps = SpeedReadDefaults.TEXT_SIZE_SLIDER_STEPS,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FlashReadDimens.minTouchTarget)
                .semantics { contentDescription = textSizeCd },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSettingsSheet(
    settings: SpeedReadSettings,
    restartEnabled: Boolean,
    onRestartClick: () -> Unit,
    onSettingsChange: (SpeedReadSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    PlayerModalSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(Res.string.player_settings),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(FlashReadDimens.space16))
        Text(
            text = stringResource(Res.string.words_per_flash),
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
        Text(
            text = stringResource(Res.string.context_display),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(FlashReadDimens.space8))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ContextMode.entries.forEachIndexed { index, mode ->
                val label = when (mode) {
                    ContextMode.Off -> stringResource(Res.string.context_mode_off)
                    ContextMode.WhenPaused -> stringResource(Res.string.context_mode_when_paused)
                    ContextMode.Always -> stringResource(Res.string.context_mode_always)
                }
                SegmentedButton(
                    selected = settings.contextMode == mode,
                    onClick = { onSettingsChange(settings.copy(contextMode = mode)) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ContextMode.entries.size,
                    ),
                    modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(FlashReadDimens.space8))
        if (settings.isSpritzAvailable) {
            PlayerSwitchRow(
                title = stringResource(Res.string.spritz),
                subtitle = stringResource(Res.string.spritz_subtitle_player),
                checked = settings.spritzEnabled,
                onCheckedChange = { onSettingsChange(settings.copy(spritzEnabled = it)) },
            )
        }
        PlayerSwitchRow(
            title = stringResource(Res.string.loop),
            subtitle = stringResource(Res.string.loop_subtitle_player),
            checked = settings.loopEnabled,
            onCheckedChange = { onSettingsChange(settings.copy(loopEnabled = it)) },
        )
        Spacer(Modifier.height(FlashReadDimens.space8))
        val restartLabel = stringResource(Res.string.action_restart)
        TextButton(
            onClick = onRestartClick,
            enabled = restartEnabled,
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = Modifier
                .heightIn(min = FlashReadDimens.minTouchTarget)
                .semantics { contentDescription = restartLabel },
        ) {
            Text(
                text = restartLabel,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RestartConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.player_restart_confirm_title),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Text(text = stringResource(Res.string.player_restart_confirm_message))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
            ) {
                Text(stringResource(Res.string.action_restart))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = FlashReadDimens.minTouchTarget),
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = FlashReadShapes.card,
    )
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
            colors = flashReadSwitchColors(),
        )
    }
}

@Composable
private fun playPauseLabel(state: SpeedReadPlayerViewState): String {
    return when (state.status) {
        SpeedReadPlayerStatus.Playing -> stringResource(Res.string.action_pause)
        SpeedReadPlayerStatus.Finished -> stringResource(Res.string.action_restart)
        SpeedReadPlayerStatus.Paused -> stringResource(Res.string.action_play)
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

internal object SpeedReadPlayerDemo {
    private val settings = SpeedReadSettings(wpm = 300, chunkSize = 1, spritzEnabled = true)
    val sampleContent =
        "Previous words sit on the lines above the flash so the reader can glance up wait, then more words follow after the active flash to fill the lines below Done. extra context remains after the finish marker."

    val longWord = SpeedReadPlayerViewState(
        status = SpeedReadPlayerStatus.Playing,
        text = "supercalifragilistic",
        position = SpeedReadPosition(tokenIndex = 2, offset = 0, paragraphIndex = 0),
        progress = 0.18f,
        elapsedMs = 24_000,
        remainingMs = 110_000,
        settings = settings.copy(contextMode = ContextMode.Always),
        isEmpty = false,
    )

    val multiWord = SpeedReadPlayerViewState(
        status = SpeedReadPlayerStatus.Playing,
        text = "one two three four",
        position = SpeedReadPosition(tokenIndex = 0, offset = 0, paragraphIndex = 0),
        progress = 0.34f,
        elapsedMs = 8_000,
        remainingMs = 16_000,
        settings = settings.copy(chunkSize = 4, contextMode = ContextMode.Always),
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
            content = SpeedReadPlayerDemo.sampleContent,
            showBanner = false,
            onClose = {},
            onRestart = {},
            onTogglePlayPause = {},
            onPrevious = {},
            onNext = {},
            onSettingsChange = {},
        )
    }
}
