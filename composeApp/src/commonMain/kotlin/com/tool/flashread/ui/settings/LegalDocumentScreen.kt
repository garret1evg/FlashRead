package com.tool.flashread.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tool.flashread.resources.Res
import com.tool.flashread.resources.*
import com.tool.flashread.ui.theme.FlashReadDimens
import com.tool.flashread.ui.theme.FlashReadTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun LegalDocumentScreen(
    document: LegalDocument,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FlashReadDimens.screenHorizontalPadding)
            .padding(top = FlashReadDimens.space8, bottom = FlashReadDimens.space24),
    ) {
        Text(
            text = stringResource(Res.string.legal_last_updated, document.lastUpdated),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FlashReadDimens.space16))
        document.sections.forEachIndexed { index, section ->
            if (index > 0) {
                Spacer(Modifier.height(FlashReadDimens.space16))
            }
            Text(
                text = section.heading,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(FlashReadDimens.space8))
            Text(
                text = section.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun PrivacyPolicyPreview() {
    FlashReadTheme {
        LegalDocumentScreen(document = LegalDocuments.privacyPolicy)
    }
}
