package com.evgeniich.flashread.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evgeniich.flashread.core.model.Book
import com.evgeniich.flashread.core.reading.ReaderAlignment
import com.evgeniich.flashread.core.reading.ReaderTextDefaults
import com.evgeniich.flashread.core.reading.ReaderTextSettings
import com.evgeniich.flashread.core.reading.ReaderTheme
import com.evgeniich.flashread.core.reading.bookProgressPercent
import com.evgeniich.flashread.resources.Res
import com.evgeniich.flashread.resources.*
import com.evgeniich.flashread.ui.library.MaterialTitleFormatter
import com.evgeniich.flashread.ui.theme.FlashReadColors
import com.evgeniich.flashread.ui.theme.FlashReadDimens
import com.evgeniich.flashread.ui.theme.FlashReadShapes
import com.evgeniich.flashread.ui.theme.FlashReadTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

private val ReaderContentMaxWidth = 680.dp

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
    val visibleParagraphIndex by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex.coerceAtLeast(0) }
    }
    val progressPercent = remember(paragraphs.size, visibleParagraphIndex) {
        bookProgressPercent(visibleParagraphIndex, paragraphs.size)
    }
    val palette = remember(settings.theme) { settings.theme.palette() }
    val displayTitle = remember(book.title) { MaterialTitleFormatter.displayTitle(book.title) }
    val backLabel = stringResource(Res.string.action_back)
    val textSettingsLabel = stringResource(Res.string.reader_text_settings)
    val openSpeedReadLabel = stringResource(Res.string.reader_open_speed_read)

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

        ReadingProgressRow(
            progressPercent = progressPercent,
            palette = palette,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FlashReadDimens.screenHorizontalPadding),
        )

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
                .padding(top = FlashReadDimens.space12, bottom = FlashReadDimens.space16)
                .heightIn(min = FlashReadDimens.minTouchTarget)
                .semantics { contentDescription = openSpeedReadLabel },
            shape = FlashReadShapes.button,
            colors = ButtonDefaults.buttonColors(
                containerColor = FlashReadColors.primary,
                contentColor = FlashReadColors.onPrimary,
            ),
            contentPadding = PaddingValues(horizontal = FlashReadDimens.space16),
        ) {
            Text(
                text = openSpeedReadLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
private fun ReadingProgressRow(
    progressPercent: Int,
    palette: ReaderPalette,
    modifier: Modifier = Modifier,
) {
    val progressCd = stringResource(Res.string.reader_progress_cd, progressPercent)
    Row(
        modifier = modifier
            .heightIn(min = FlashReadDimens.minTouchTarget)
            .semantics { contentDescription = progressCd },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FlashReadDimens.space12),
    ) {
        LinearProgressIndicator(
            progress = { progressPercent / 100f },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(FlashReadDimens.space4)),
            color = FlashReadColors.primary,
            trackColor = palette.progressTrack,
        )
        Text(
            text = stringResource(Res.string.percent_value, progressPercent),
            style = MaterialTheme.typography.labelLarge,
            color = FlashReadColors.primary,
            maxLines = 1,
        )
    }
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
                text = stringResource(Res.string.reader_theme),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(FlashReadDimens.space8))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(FlashReadDimens.space8),
                verticalArrangement = Arrangement.spacedBy(FlashReadDimens.space8),
            ) {
                ReaderTheme.entries.forEach { theme ->
                    val themeLabel = theme.label()
                    val themeCd = stringResource(Res.string.reader_theme_cd, themeLabel)
                    FilterChip(
                        selected = settings.theme == theme,
                        onClick = { onSettingsChange(settings.copy(theme = theme)) },
                        label = {
                            Text(
                                text = themeLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = FlashReadDimens.minTouchTarget)
                            .semantics { contentDescription = themeCd },
                    )
                }
            }

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
                fontWeight = FontWeight.SemiBold,
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

private fun readerBodyStyle(settings: ReaderTextSettings, color: Color): TextStyle {
    val fontSize = settings.fontSizeSp.sp
    return TextStyle(
        fontSize = fontSize,
        lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
        color = color,
    )
}

private fun ReaderTheme.palette(): ReaderPalette = when (this) {
    ReaderTheme.Light -> ReaderPalette(
        background = FlashReadColors.background,
        onBackground = FlashReadColors.textPrimary,
        outline = FlashReadColors.outline,
        progressTrack = FlashReadColors.primaryContainer,
        wordHighlight = FlashReadColors.primary,
        wordHighlightText = FlashReadColors.onPrimary,
    )
    ReaderTheme.Sepia -> ReaderPalette(
        background = Color(0xFFF4ECD8),
        onBackground = Color(0xFF5C4B32),
        outline = Color(0xFFE6D9BF),
        progressTrack = Color(0xFFE8DCC4),
        wordHighlight = Color(0xFF8B5A12),
        wordHighlightText = Color(0xFFFFF8E7),
    )
    ReaderTheme.Dark -> ReaderPalette(
        background = Color(0xFF121212),
        onBackground = Color(0xFFE8E6E3),
        outline = Color(0xFF3A3A3A),
        progressTrack = Color(0xFF2C2C2C),
        wordHighlight = Color(0xFFC4B0F0),
        wordHighlightText = Color(0xFF2D1B54),
    )
}

@Composable
private fun ReaderTheme.label(): String = when (this) {
    ReaderTheme.Light -> stringResource(Res.string.reader_theme_light)
    ReaderTheme.Sepia -> stringResource(Res.string.reader_theme_sepia)
    ReaderTheme.Dark -> stringResource(Res.string.reader_theme_dark)
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
