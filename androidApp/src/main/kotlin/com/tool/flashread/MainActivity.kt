package com.tool.flashread

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tool.flashread.platform.AndroidAppContext
import com.tool.flashread.platform.ExternalBookImporter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidAppContext.init(applicationContext)
        if (savedInstanceState == null) {
            dispatchOpenIntent(intent)
        }
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dispatchOpenIntent(intent)
    }

    private fun dispatchOpenIntent(intent: Intent?) {
        ExternalBookImporter.handleIntent(
            intent = intent,
            contentResolver = contentResolver,
            cacheDir = cacheDir,
        )
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
