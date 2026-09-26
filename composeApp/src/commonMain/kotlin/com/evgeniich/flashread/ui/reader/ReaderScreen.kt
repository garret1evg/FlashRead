package com.evgeniich.flashread.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evgeniich.flashread.ads.BannerAdHost
import com.evgeniich.flashread.ads.canShowBannerAds
import com.evgeniich.flashread.core.model.Book
import com.evgeniich.flashread.core.theme.AppTheme
import com.evgeniich.flashread.core.reading.ReaderAlignment
import com.evgeniich.flashread.core.reading.ReaderTextDefaults
import com.evgeniich.flashread.core.reading.ReaderTextSettings
import com.evgeniich.flashread.monetization.MonetizationManager
import com.evgeniich.flashread.navigation.AppRoute
import com.evgeniich.flashread.resources.Res
import com.evgeniich.flashread.resources.*
import com.evgeniich.flashread.ui.library.MaterialTitleFormatter
import com.evgeniich.flashread.ui.theme.FlashReadDimens
import com.evgeniich.flashread.ui.theme.FlashReadShapes
import com.evgeniich.flashread.ui.theme.FlashReadTheme
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

private val ReaderContentMaxWidth = 680.dp
private val ReadingProgressTrackHeight = 6.dp
private val SelectedWordTriangleWidth = 12.dp
private val SelectedWordTriangleHeight = 8.dp
private val SelectedWordTickWidth = 3.dp
private val SelectedWordTickHeight = 14.dp
private val SelectedWordMarkerGap = 2.dp
private val SelectedWordTickCornerRadius = 1.dp
private val SelectedWordTickOutlineWidth = 1.dp

private data class ReaderPalette(
    val background: Color,
    val onBackground: Color,
    val outline: Color,
    val progressTrack: Color,
    val wordHighlight: Color,
    val wordHighlightText: Color,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: Book,
    onBack: () -> Unit,
    onOpenSpeedRead: () -> Unit,
    modifier: Modifier = Modifier,
    isActiveRoute: Boolean = true,
    viewModel: ReaderViewModel = viewModel(key = book.id) { ReaderViewModel(book) },
) {
    val document by viewModel.document.collectAsStateWithLifecycle()
    val paragraphs = document?.paragraphs.orEmpty()
    val restoredParagraphIndex = document?.initialParagraphIndex
    val listState = rememberSaveable(restoredParagraphIndex, saver = LazyListState.Saver) {
        LazyListState(firstVisibleItemIndex = restoredParagraphIndex ?: 0)
    }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val startWord by viewModel.startWord.collectAsStateWithLifecycle()
    val scrollToParagraph by viewModel.scrollToParagraph.collectAsStateWithLifecycle()
    var showTextSettings by remember { mutableStateOf(false) }
    val palette = readerPalette()
    val displayTitle = remember(book.title) { MaterialTitleFormatter.displayTitle(book.title) }
    val backLabel = stringResource(Res.string.action_back)
    val textSettingsLabel = stringResource(Res.string.reader_text_settings)
    val openSpeedReadLabel = stringResource(Res.string.reader_open_speed_read)
    // Collect state to trigger recomposition when monetization state changes
    @Suppress("UNUSED_VARIABLE")
    val monetizationState by MonetizationManager.state.collectAsStateWithLifecycle()
    val showBanner = canShowBannerAds() && MonetizationManager.shouldShowBanner(AppRoute.Reader)

    LaunchedEffect(isActiveRoute, document) {
        if (isActiveRoute && document != null) {
            viewModel.refreshPosition()
        }
    }

    LaunchedEffect(scrollToParagraph, document) {
        val targetParagraph = scrollToParagraph
        if (document != null && targetParagraph != null) {
            listState.scrollToItem(targetParagraph)
            viewModel.onScrollHandled()
        }
    }

    LaunchedEffect(book.id, listState, document) {
        val currentDocument = document ?: return@LaunchedEffect
        val restoredIndex = currentDocument.initialParagraphIndex
        if (restoredIndex > 0 && listState.firstVisibleItemIndex == 0) {
            listState.scrollToItem(restoredIndex)
        }
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { it.coerceAtLeast(0) }
            .distinctUntilChanged()
            .collect { paragraphIndex ->
                viewModel.saveParagraphIndex(paragraphIndex)
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = displayTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = palette.onBackground,
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(FlashReadDimens.minTouchTarget)
                        .semantics { contentDescription = backLabel },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = backLabel,
                        tint = palette.onBackground,
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { showTextSettings = true },
                    modifier = Modifier
                        .size(FlashReadDimens.minTouchTarget)
                        .semantics { contentDescription = textSettingsLabel },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.TextFields,
                        contentDescription = textSettingsLabel,
                        tint = palette.onBackground,
                    )
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = palette.background,
                titleContentColor = palette.onBackground,
                navigationIconContentColor = palette.onBackground,
                actionIconContentColor = palette.onBackground,
            ),
        )

        val loadedDocument = document
        if (loadedDocument != null) {
            ReaderProgressBar(
                listState = listState,
                bookId = book.id,
                paragraphStartOffsets = loadedDocument.paragraphStartOffsets,
                contentLength = loadedDocument.contentLength,
                fontSizeSp = settings.fontSizeSp,
                lineHeightMultiplier = settings.lineHeightMultiplier,
                cursorContentOffset = startWord?.contentOffset,
                palette = palette,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FlashReadDimens.screenHorizontalPadding),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = ReaderContentMaxWidth)
                    .fillMaxWidth()
                    .fillMaxHeight(),
                state = listState,
                contentPadding = PaddingValues(
                    horizontal = FlashReadDimens.screenHorizontalPadding,
                    vertical = FlashReadDimens.space16,
                ),
            ) {
                itemsIndexed(paragraphs) { paragraphIndex, paragraph ->
                    HighlightedParagraph(
                        text = paragraph,
                        paragraphIndex = paragraphIndex,
                        startWord = startWord,
                        style = readerBodyStyle(settings, palette.onBackground),
                        textAlign = settings.alignment.toTextAlign(),
                        highlightColor = palette.wordHighlight,
                        highlightTextColor = palette.wordHighlightText,
                        onWordSelected = viewModel::selectWord,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = FlashReadDimens.space16),
                    )
                }
            }
        }

        HorizontalDivider(color = palette.outline)
        Button(
            onClick = onOpenSpeedRead,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FlashReadDimens.screenHorizontalPadding)
                .padding(top = FlashReadDimens.space12)
                .heightIn(min = FlashReadDimens.minTouchTarget)
                .semantics { contentDescription = openSpeedReadLabel },
            shape = FlashReadShapes.button,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            contentPadding = PaddingValues(horizontal = FlashReadDimens.space16),
        ) {
            Text(
                text = openSpeedReadLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showBanner) {
            Spacer(Modifier.height(FlashReadDimens.space12))
            BannerAdHost(modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(FlashReadDimens.space16))
    }

    if (showTextSettings) {
        ReaderTextSettingsSheet(
            settings = settings,
            onDismiss = { showTextSettings = false },
            onSettingsChange = viewModel::updateSettings,
        )
    }
}

@Composable
private fun ReaderProgressBar(
    listState: LazyListState,
    bookId: String,
    paragraphStartOffsets: List<Int>,
    contentLength: Int,
    fontSizeSp: Int,
    lineHeightMultiplier: Float,
    cursorContentOffset: Int?,
    palette: ReaderPalette,
    modifier: Modifier = Modifier,
) {
    val fitsOnScreen by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            info.totalItemsCount > 0 &&
                info.visibleItemsInfo.size == info.totalItemsCount &&
                !listState.canScrollForward &&
                !listState.canScrollBackward
        }
    }
    if (fitsOnScreen) return

    var viewportChars by remember(
        bookId,
        contentLength,
        fontSizeSp,
        lineHeightMultiplier,
    ) { mutableFloatStateOf(Float.NaN) }
    LaunchedEffect(
        bookId,
        contentLength,
        fontSizeSp,
        lineHeightMultiplier,
        paragraphStartOffsets,
    ) {
        viewportChars = Float.NaN
        viewportChars = snapshotFlow {
            viewportCharSpan(listState, paragraphStartOffsets, contentLength)
        }.first { !it.isNaN() }
    }
    val progressFraction by remember(paragraphStartOffsets, contentLength) {
        derivedStateOf {
            if (contentLength <= 0) return@derivedStateOf 0f
            val visible = listState.layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) return@derivedStateOf 0f
            val topChar = charOffsetAt(visible, paragraphStartOffsets, contentLength, y = 0)
            val reserved = if (viewportChars.isNaN()) 0f else viewportChars
            val maxTop = (contentLength - reserved).coerceAtLeast(1f)
            (topChar / maxTop).coerceIn(0f, 1f)
        }
    }
    val cursorFraction = if (cursorContentOffset != null && contentLength > 0) {
        (cursorContentOffset.toFloat() / contentLength).coerceIn(0f, 1f)
    } else {
        null
    }
    ReadingProgressRow(
        progressFraction = progressFraction,
        cursorFraction = cursorFraction,
        palette = palette,
        modifier = modifier,
    )
}

@Composable
private fun ReadingProgressRow(
    progressFraction: Float,
    cursorFraction: Float?,
    palette: ReaderPalette,
    modifier: Modifier = Modifier,
) {
    val percent = (progressFraction * 100).roundToInt()
    val progressCd = stringResource(Res.string.reader_progress_cd, percent)
    Row(
        modifier = modifier
            .heightIn(min = FlashReadDimens.minTouchTarget)
            .semantics { contentDescription = progressCd },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlashReadDimens.space12),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(FlashReadDimens.minTouchTarget),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(ReadingProgressTrackHeight)
                    .clip(RoundedCornerShape(FlashReadDimens.space4))
                    .background(palette.progressTrack),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            val cursor = cursorFraction
            if (cursor != null) {
                SelectedWordPositionMarker(
                    fraction = cursor,
                    color = palette.wordHighlight,
                    outlineColor = palette.background,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        val percentLabel = stringResource(Res.string.percent_value, percent)
        val widestLabel = stringResource(Res.string.percent_value, 100)
        val labelStyle = MaterialTheme.typography.labelLarge
        Box {
            Text(
                text = widestLabel,
                style = labelStyle,
                modifier = Modifier
                    .alpha(0f)
                    .clearAndSetSemantics {},
                maxLines = 1,
            )
            Text(
                text = percentLabel,
                style = labelStyle,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                modifier = Modifier.align(Alignment.CenterEnd),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SelectedWordPositionMarker(
    fraction: Float,
    color: Color,
    outlineColor: Color,
    modifier: Modifier = Modifier,
) {
    val overhangPx = with(LocalDensity.current) {
        ceil((SelectedWordTriangleWidth / 2).toPx()).toInt()
    }
    // Wider than the track so the triangle is not clipped at 0% and 100%.
    Canvas(
        modifier.layout { measurable, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val canvasWidth = width + overhangPx * 2
            val placeable = measurable.measure(
                constraints.copy(
                    minWidth = canvasWidth,
                    maxWidth = canvasWidth,
                ),
            )
            layout(width, height) {
                placeable.place(-overhangPx, 0)
            }
        },
    ) {
        drawSelectedWordMarker(
            fraction = fraction,
            color = color,
            outlineColor = outlineColor,
            trackLeft = overhangPx.toFloat(),
            trackWidth = (size.width - overhangPx * 2).coerceAtLeast(0f),
        )
    }
}

private fun DrawScope.drawSelectedWordMarker(
    fraction: Float,
    color: Color,
    outlineColor: Color,
    trackLeft: Float,
    trackWidth: Float,
) {
    val centerX = trackLeft + trackWidth * fraction.coerceIn(0f, 1f)
    val centerY = size.height / 2f
    val triangleWidth = SelectedWordTriangleWidth.toPx()
    val triangleHeight = SelectedWordTriangleHeight.toPx()
    val tickWidth = SelectedWordTickWidth.toPx()
    val tickHeight = SelectedWordTickHeight.toPx()
    val gap = SelectedWordMarkerGap.toPx()
    val corner = SelectedWordTickCornerRadius.toPx()
    val outline = SelectedWordTickOutlineWidth.toPx()

    val tickTop = centerY - tickHeight / 2f
    val tickLeft = centerX - tickWidth / 2f
    val tipY = tickTop - gap
    drawRoundRect(
        color = outlineColor,
        topLeft = Offset(tickLeft - outline, tickTop - outline),
        size = Size(tickWidth + outline * 2f, tickHeight + outline * 2f),
        cornerRadius = CornerRadius(corner + outline, corner + outline),
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(tickLeft, tickTop),
        size = Size(tickWidth, tickHeight),
        cornerRadius = CornerRadius(corner, corner),
    )
    val triangle = Path().apply {
        moveTo(centerX - triangleWidth / 2f, tipY - triangleHeight)
        lineTo(centerX + triangleWidth / 2f, tipY - triangleHeight)
        lineTo(centerX, tipY)
        close()
    }
    drawPath(path = triangle, color = color)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTextSettingsSheet(
    settings: ReaderTextSettings,
    onDismiss: () -> Unit,
    onSettingsChange: (ReaderTextSettings) -> Unit,
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
                text = stringResource(Res.string.reader_text_settings),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(FlashReadDimens.space20))

            SettingsSlider(
                title = stringResource(Res.string.reader_font_size),
                valueLabel = "${settings.fontSizeSp} sp",
                value = settings.fontSizeSp.toFloat(),
                valueRange = ReaderTextDefaults.MIN_FONT_SIZE_SP.toFloat()..
                    ReaderTextDefaults.MAX_FONT_SIZE_SP.toFloat(),
                steps = ReaderTextDefaults.FONT_SIZE_SLIDER_STEPS,
                contentDescription = stringResource(
                    Res.string.reader_font_size_cd,
                    settings.fontSizeSp,
                ),
                onValueChange = { onSettingsChange(settings.copy(fontSizeSp = it.toInt())) },
            )

            Spacer(Modifier.height(FlashReadDimens.space16))

            SettingsSlider(
                title = stringResource(Res.string.reader_line_height),
                valueLabel = formatLineHeight(settings.lineHeightMultiplier),
                value = settings.lineHeightMultiplier,
                valueRange = ReaderTextDefaults.MIN_LINE_HEIGHT..ReaderTextDefaults.MAX_LINE_HEIGHT,
                steps = ReaderTextDefaults.LINE_HEIGHT_SLIDER_STEPS,
                contentDescription = stringResource(
                    Res.string.reader_line_height_cd,
                    formatLineHeight(settings.lineHeightMultiplier),
                ),
                onValueChange = {
                    onSettingsChange(
                        settings.copy(
                            lineHeightMultiplier = ReaderTextDefaults.snapLineHeight(it),
                        ),
                    )
                },
            )

            Spacer(Modifier.height(FlashReadDimens.space20))
            Text(
                text = stringResource(Res.string.reader_alignment),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(FlashReadDimens.space8))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(FlashReadDimens.space8),
            ) {
                ReaderAlignment.entries.forEach { alignment ->
                    val selected = settings.alignment == alignment
                    IconButton(
                        onClick = { onSettingsChange(settings.copy(alignment = alignment)) },
                        modifier = Modifier.size(FlashReadDimens.minTouchTarget),
                    ) {
                        Icon(
                            imageVector = alignment.icon(),
                            contentDescription = alignment.label(),
                            tint = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightedParagraph(
    text: String,
    paragraphIndex: Int,
    startWord: ReaderStartWord?,
    style: TextStyle,
    textAlign: TextAlign,
    highlightColor: Color,
    highlightTextColor: Color,
    onWordSelected: (paragraphIndex: Int, localCharOffset: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val annotatedText = remember(text, startWord, paragraphIndex, highlightColor, highlightTextColor) {
        buildHighlightedText(text, paragraphIndex, startWord, highlightColor, highlightTextColor)
    }

    Text(
        text = annotatedText,
        style = style,
        textAlign = textAlign,
        onTextLayout = { textLayoutResult = it },
        modifier = modifier.pointerInput(paragraphIndex) {
            detectTapGestures { offset ->
                textLayoutResult?.let { layoutResult ->
                    val charOffset = layoutResult.getOffsetForPosition(offset)
                    onWordSelected(paragraphIndex, charOffset)
                }
            }
        },
    )
}

private fun buildHighlightedText(
    text: String,
    paragraphIndex: Int,
    startWord: ReaderStartWord?,
    highlightColor: Color,
    highlightTextColor: Color,
): AnnotatedString {
    if (startWord == null || startWord.paragraphIndex != paragraphIndex) {
        return AnnotatedString(text)
    }
    val start = startWord.localStart.coerceIn(0, text.length)
    val end = startWord.localEnd.coerceIn(start, text.length)
    return buildAnnotatedString {
        append(text.substring(0, start))
        withStyle(
            SpanStyle(
                background = highlightColor,
                color = highlightTextColor,
            ),
        ) {
            append(text.substring(start, end))
        }
        append(text.substring(end))
    }
}

@Composable
private fun SettingsSlider(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    contentDescription: String,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = valueLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FlashReadDimens.minTouchTarget)
            .semantics { this.contentDescription = contentDescription },
    )
}

private fun viewportCharSpan(
    listState: LazyListState,
    paragraphStartOffsets: List<Int>,
    contentLength: Int,
): Float {
    if (contentLength <= 0) return Float.NaN
    val info = listState.layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty()) return Float.NaN
    val viewportHeight = info.viewportSize.height.takeIf { it > 0 }
        ?: (info.viewportEndOffset - info.viewportStartOffset).coerceAtLeast(1)
    if (visible.sumOf { it.size } < viewportHeight / 2) return Float.NaN
    val topChar = charOffsetAt(visible, paragraphStartOffsets, contentLength, y = 0)
    val bottomChar = charOffsetAt(visible, paragraphStartOffsets, contentLength, y = viewportHeight)
    val measured = bottomChar - topChar
    return if (measured > 0f && measured < contentLength) measured else Float.NaN
}

private fun charOffsetAt(
    items: List<LazyListItemInfo>,
    paragraphStartOffsets: List<Int>,
    contentLength: Int,
    y: Int,
): Float {
    val item = items.lastOrNull { it.offset <= y } ?: items.first()
    val start = paragraphStartOffsets.getOrElse(item.index.coerceAtLeast(0)) { 0 }
    val end = paragraphStartOffsets.getOrElse(item.index + 1) { contentLength }
    val size = item.size.coerceAtLeast(1)
    val yInItem = (y - item.offset).coerceIn(0, size)
    return start + (end - start) * (yInItem.toFloat() / size)
}

private fun readerBodyStyle(settings: ReaderTextSettings, color: Color): TextStyle {
    val fontSize = settings.fontSizeSp.sp
    return TextStyle(
        fontSize = fontSize,
        lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
        color = color,
    )
}

@Composable
private fun readerPalette(): ReaderPalette {
    val colors = MaterialTheme.colorScheme
    return ReaderPalette(
        background = colors.background,
        onBackground = colors.onBackground,
        outline = colors.outline,
        progressTrack = colors.primaryContainer,
        wordHighlight = colors.primary,
        wordHighlightText = colors.onPrimary,
    )
}

private fun ReaderAlignment.toTextAlign(): TextAlign = when (this) {
    ReaderAlignment.Start -> TextAlign.Start
    ReaderAlignment.Center -> TextAlign.Center
    ReaderAlignment.Justify -> TextAlign.Justify
}

@Composable
private fun ReaderAlignment.label(): String = when (this) {
    ReaderAlignment.Start -> stringResource(Res.string.reader_alignment_start)
    ReaderAlignment.Center -> stringResource(Res.string.reader_alignment_center)
    ReaderAlignment.Justify -> stringResource(Res.string.reader_alignment_justify)
}

private fun ReaderAlignment.icon(): ImageVector = when (this) {
    ReaderAlignment.Start -> Icons.AutoMirrored.Filled.FormatAlignLeft
    ReaderAlignment.Center -> Icons.Filled.FormatAlignCenter
    ReaderAlignment.Justify -> Icons.Filled.FormatAlignJustify
}

private fun formatLineHeight(value: Float): String {
    val hundredths = (value * 100f).roundToInt()
    val fraction = (hundredths % 100).toString().padStart(2, '0')
    return "${hundredths / 100}.$fraction"
}

@Preview(name = "Reader 320", widthDp = 320, heightDp = 640, showBackground = true)
@Preview(name = "Reader 390", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ReaderScreenPreview() {
    val book = Book(
        id = "preview",
        title = "very_long_imported_book_title_that_should_ellipsis.txt",
        content = "Subvocalization is one of the things that can keep your reading speed down.\n\n" +
            "Speed reading trains you to take in words visually without sounding them out.",
    )
    FlashReadTheme {
        ReaderScreen(
            book = book,
            onBack = {},
            onOpenSpeedRead = {},
            viewModel = remember { ReaderViewModel(book) },
        )
    }
}

@Preview(name = "Progress marker light", widthDp = 360, heightDp = 240)
@Composable
private fun ReadingProgressMarkerLightPreview() {
    ReadingProgressMarkerPreview(AppTheme.Light)
}

@Preview(name = "Progress marker dark", widthDp = 360, heightDp = 240)
@Composable
private fun ReadingProgressMarkerDarkPreview() {
    ReadingProgressMarkerPreview(AppTheme.Dark)
}

@Composable
private fun ReadingProgressMarkerPreview(theme: AppTheme) {
    FlashReadTheme(theme = theme) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = FlashReadDimens.space8),
            verticalArrangement = Arrangement.spacedBy(FlashReadDimens.space4),
        ) {
            listOf(
                0f to 0.35f,
                0.58f to 0.22f,
                1f to 0.4f,
            ).forEach { (cursor, progress) ->
                ReadingProgressRow(
                    progressFraction = progress,
                    cursorFraction = cursor,
                    palette = readerPalette(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FlashReadDimens.screenHorizontalPadding),
                )
            }
        }
    }
}
