package com.tool.flashread.ui.speedread

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tool.flashread.core.model.Book
import com.tool.flashread.core.speedread.SpeedReadPlayback
import com.tool.flashread.core.speedread.SpeedReadSettings
import com.tool.flashread.core.speedread.orpIndex
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

private val OrpHighlightColor = Color(0xFFE53935)

@Composable
fun SpeedReadPlayerScreen(
    book: Book,
    settings: SpeedReadSettings,
    startParagraphIndex: Int = 0,
    onParagraphIndexChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val playback = remember(book.content, settings.chunkSize) {
        SpeedReadPlayback(book.content, settings.chunkSize)
    }
    var chunkIndex by remember(playback, startParagraphIndex) {
        mutableIntStateOf(playback.startChunkIndex(startParagraphIndex))
    }
    var isPlaying by remember { mutableStateOf(false) }
    val currentText = playback.chunks.getOrNull(chunkIndex)?.displayText.orEmpty()
    val hasChunks = playback.chunks.isNotEmpty()

    LaunchedEffect(book.id, playback) {
        snapshotFlow { playback.chunks.getOrNull(chunkIndex)?.paragraphIndex }
            .distinctUntilChanged()
            .collect { paragraphIndex ->
                if (paragraphIndex != null) {
                    onParagraphIndexChanged(paragraphIndex)
                }
            }
    }

    LaunchedEffect(isPlaying, chunkIndex, playback, settings.wpm, settings.loopEnabled) {
        if (!isPlaying || !hasChunks) return@LaunchedEffect
        delay(playback.delayMs(chunkIndex, settings.wpm))
        val next = playback.nextChunkIndex(chunkIndex, settings.loopEnabled)
        if (next != null) {
            chunkIndex = next
        } else {
            isPlaying = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        OrpWordFrame(
            text = currentText,
            spritzEnabled = settings.spritzEnabled,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.weight(1f))
        PlayerControls(
            isPlaying = isPlaying,
            enabled = hasChunks,
            onPreviousSentence = {
                chunkIndex = playback.previousSentenceChunkIndex(chunkIndex)
            },
            onStepBack = {
                chunkIndex = playback.previousChunkIndex(chunkIndex)
            },
            onPlayPause = {
                if (isPlaying) {
                    isPlaying = false
                } else if (hasChunks) {
                    if (!settings.loopEnabled && chunkIndex >= playback.chunks.lastIndex) {
                        chunkIndex = 0
                    }
                    isPlaying = true
                }
            },
            onStepForward = {
                val next = playback.nextChunkIndex(chunkIndex, settings.loopEnabled)
                if (next != null) chunkIndex = next
            },
            onNextSentence = {
                chunkIndex = playback.nextSentenceChunkIndex(chunkIndex)
            },
        )
        Spacer(Modifier.height(24.dp))
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
    val lineColor = MaterialTheme.colorScheme.onSurface
    val textStyle = MaterialTheme.typography.headlineLarge.copy(
        fontSize = 36.sp,
        color = onSurface,
    )
    val orp = orpIndex(text, spritzEnabled)
    val annotated = remember(text, orp, onSurface) {
        buildAnnotatedString {
            if (text.isEmpty()) return@buildAnnotatedString
            append(text)
            addStyle(SpanStyle(color = onSurface), 0, text.length)
            if (orp != null && orp in text.indices) {
                addStyle(SpanStyle(color = OrpHighlightColor), orp, orp + 1)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        val centerX = constraints.maxWidth / 2f
        val layout = textMeasurer.measure(
            text = annotated,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Visible,
        )
        val offsetX = when {
            text.isEmpty() -> 0f
            orp != null && orp in text.indices -> {
                val box = layout.getBoundingBox(orp)
                centerX - (box.left + box.width / 2f)
            }
            else -> centerX - layout.size.width / 2f
        }

        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            val hPad = 24.dp.toPx()
            val tick = 18.dp.toPx()
            val topY = 24.dp.toPx()
            val bottomY = size.height - 24.dp.toPx()
            val cx = size.width / 2f

            drawLine(
                color = lineColor,
                start = Offset(hPad, topY),
                end = Offset(size.width - hPad, topY),
                strokeWidth = stroke,
                cap = StrokeCap.Square,
            )
            drawLine(
                color = lineColor,
                start = Offset(cx, topY),
                end = Offset(cx, topY + tick),
                strokeWidth = stroke,
                cap = StrokeCap.Square,
            )
            drawLine(
                color = lineColor,
                start = Offset(hPad, bottomY),
                end = Offset(size.width - hPad, bottomY),
                strokeWidth = stroke,
                cap = StrokeCap.Square,
            )
            drawLine(
                color = lineColor,
                start = Offset(cx, bottomY),
                end = Offset(cx, bottomY - tick),
                strokeWidth = stroke,
                cap = StrokeCap.Square,
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
                    .offset { IntOffset(offsetX.roundToInt(), 0) },
            )
        }
    }
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    enabled: Boolean,
    onPreviousSentence: () -> Unit,
    onStepBack: () -> Unit,
    onPlayPause: () -> Unit,
    onStepForward: () -> Unit,
    onNextSentence: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerControlButton(
            onClick = onPreviousSentence,
            imageVector = Icons.Filled.KeyboardDoubleArrowLeft,
            contentDescription = "Previous sentence",
            enabled = enabled,
        )
        PlayerControlButton(
            onClick = onStepBack,
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Previous word",
            enabled = enabled,
        )
        PlayerControlButton(
            onClick = onPlayPause,
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            enabled = enabled,
            size = 64.dp,
            iconSize = 32.dp,
        )
        PlayerControlButton(
            onClick = onStepForward,
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Next word",
            enabled = enabled,
        )
        PlayerControlButton(
            onClick = onNextSentence,
            imageVector = Icons.Filled.KeyboardDoubleArrowRight,
            contentDescription = "Next sentence",
            enabled = enabled,
        )
    }
}

@Composable
private fun PlayerControlButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 28.dp,
) {
    FilledTonalIconButton(
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

@Preview
@Composable
private fun SpeedReadPlayerScreenPreview() {
    MaterialTheme {
        SpeedReadPlayerScreen(
            book = Book(
                id = "preview",
                title = "Sample book",
                content = "subvocalization is one of the things that can keep your reading speed down.",
            ),
            settings = SpeedReadSettings(),
        )
    }
}
