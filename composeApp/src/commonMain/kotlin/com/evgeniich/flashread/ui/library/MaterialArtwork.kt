package com.evgeniich.flashread.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.evgeniich.flashread.platform.CoverStorage
import com.evgeniich.flashread.resources.Res
import com.evgeniich.flashread.resources.*
import com.evgeniich.flashread.ui.theme.FlashReadDimens
import org.jetbrains.compose.resources.stringResource

@Composable
fun MaterialArtwork(
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
                contentDescription = stringResource(Res.string.cd_cover),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = { MaterialArtworkPlaceholder() },
            )
        } else {
            MaterialArtworkPlaceholder()
        }
    }
}

@Composable
private fun MaterialArtworkPlaceholder() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.MenuBook,
        contentDescription = stringResource(Res.string.cd_book),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(22.dp),
    )
}
