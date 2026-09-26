package com.evgeniich.flashread.platform

import androidx.compose.runtime.Composable

/**
 * True while a cellular or VoIP call is ringing or connected.
 * Becomes false again when the call ends.
 */
@Composable
expect fun rememberPhoneCallActive(): Boolean
