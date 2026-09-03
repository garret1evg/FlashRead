package com.evgeniich.flashread

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.evgeniich.flashread.ads.AdMobManager
import com.evgeniich.flashread.consent.ConsentManager
import com.evgeniich.flashread.platform.ExternalBookImporter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            dispatchOpenIntent(intent)
        }
        setContent {
            App()
        }
        ConsentManager.gatherConsent(this) {
            // После consent flow - инициализировать AdMob если разрешено
            AdMobManager.initializeIfAllowed(applicationContext)
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
