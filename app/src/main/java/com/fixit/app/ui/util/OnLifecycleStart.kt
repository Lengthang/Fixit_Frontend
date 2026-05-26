package com.fixit.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Runs [onStart] every time the host enters the STARTED state.
 *
 * Used by screens to re-fetch their data when:
 *   • the screen first becomes visible (initial load)
 *   • the user navigates back from a child screen
 *   • the app is resumed from background
 *
 * This is intentionally lifecycle-driven rather than time-driven — no polling,
 * no fixed interval, and zero work while the screen is off-screen.
 *
 * Pair with VM `refresh()` and REMOVE `init { refresh() }` from the VM so the
 * first ON_START is what drives the initial load (otherwise you get a double
 * fetch on first composition).
 *
 * Uses Compose UI's [LocalLifecycleOwner] + a plain [LifecycleEventObserver]
 * so it works without adding the lifecycle-runtime-compose artifact.
 */
@Composable
fun OnLifecycleStart(onStart: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                onStart()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}