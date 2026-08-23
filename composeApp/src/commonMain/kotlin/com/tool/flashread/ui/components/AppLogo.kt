package com.tool.flashread.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tool.flashread.resources.Res
import com.tool.flashread.resources.*
import com.tool.flashread.ui.theme.FlashReadDimens
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    contentDescription: String? = stringResource(Res.string.app_name),
) {
    Image(
        painter = painterResource(Res.drawable.app_logo),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}

@Composable
fun ScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppLogo(size = 36.dp)
        Spacer(Modifier.width(FlashReadDimens.space12))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
