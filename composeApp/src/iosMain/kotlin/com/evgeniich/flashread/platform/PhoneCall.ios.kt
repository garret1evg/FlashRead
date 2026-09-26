package com.evgeniich.flashread.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.CallKit.CXCall
import platform.CallKit.CXCallObserver
import platform.CallKit.CXCallObserverDelegateProtocol
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberPhoneCallActive(): Boolean {
    var active by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val watcher = PhoneCallWatcher { active = it }
        watcher.start()
        onDispose { watcher.stop() }
    }
    return active
}

private class PhoneCallWatcher(
    private val onActiveChanged: (Boolean) -> Unit,
) : NSObject(), CXCallObserverDelegateProtocol {
    private val observer = CXCallObserver()

    fun start() {
        observer.setDelegate(this, queue = dispatch_get_main_queue())
        publish()
    }

    fun stop() {
        observer.setDelegate(null, queue = null)
    }

    override fun callObserver(callObserver: CXCallObserver, callChanged: CXCall) {
        publish()
    }

    private fun publish() {
        val inCall = observer.calls.any { call ->
            call is CXCall && !call.hasEnded
        }
        onActiveChanged(inCall)
    }
}
