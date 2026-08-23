package com.tool.flashread.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.tool.flashread.core.model.MaterialSourceType
import com.tool.flashread.platform.CoverStorage
import com.tool.flashread.ui.theme.FlashReadDimens

@Composable
fun MaterialArtwork(
    sourceType: MaterialSourceType,
    coverFileName: String?,
    modifier: Modifier = Modifier,
) {
    val imageModel = remember(coverFileName) {
        coverFileName?.let { CoverStorage.coverImageModel(it) }
    }
    Box(
        modifier = modifier
            .size(
                width = FlashReadDimens.coverThumbWidth,
                height = FlashReadDimens.coverThumbHeight,
            )
            .clip(RoundedCornerShape(FlashReadDimens.space8))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (imageModel != null) {
            SubcomposeAsyncImage(
                model = imageModel,
                contentDescription = "Обложка",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = { MaterialArtworkPlaceholder(sourceType) },
            )
        } else {
            MaterialArtworkPlaceholder(sourceType)
        }
    }
}

@Composable
private fun MaterialArtworkPlaceholder(sourceType: MaterialSourceType) {
    val icon: ImageVector = when (sourceType) {
        MaterialSourceType.Book -> Icons.AutoMirrored.Filled.MenuBook
        MaterialSourceType.YouTube -> Icons.Filled.PlayCircle
    }
    Icon(
        imageVector = icon,
        contentDescription = when (sourceType) {
            MaterialSourceType.Book -> "Книга"
            MaterialSourceType.YouTube -> "YouTube-видео"
        },
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(22.dp),
    )
}
