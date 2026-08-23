package com.tool.flashread.ui.speedread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.tool.flashread.ui.theme.FlashReadDimens
import com.tool.flashread.ui.theme.FlashReadTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSpeedReadScreen(
    onBack: () -> Unit,
    onContinue: (content: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var content by remember { mutableStateOf("") }
    val canContinue = content.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Скорочтение",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(FlashReadDimens.minTouchTarget)
                        .semantics { contentDescription = "Назад" },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = { onContinue(content) },
                    enabled = canContinue,
                    modifier = Modifier
                        .heightIn(min = FlashReadDimens.minTouchTarget)
                        .semantics { contentDescription = "Далее" },
                ) {
                    Text(
                        text = "Далее",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.primary,
            ),
        )

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = FlashReadDimens.screenHorizontalPadding)
                .padding(top = FlashReadDimens.space8, bottom = FlashReadDimens.space16),
            label = { Text("Текст") },
            placeholder = { Text("Вставьте текст — он не сохранится в библиотеке") },
        )
    }
}

@Preview(name = "Quick speed read 320", widthDp = 320, heightDp = 640, showBackground = true)
@Preview(name = "Quick speed read 390", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun QuickSpeedReadScreenPreview() {
    FlashReadTheme {
        QuickSpeedReadScreen(
            onBack = {},
            onContinue = {},
        )
    }
}
