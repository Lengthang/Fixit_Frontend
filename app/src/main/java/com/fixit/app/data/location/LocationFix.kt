package com.fixit.app.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * One place that knows how to get a device location quickly and safely.
 *
 * Strategy (fast-first, never hangs):
 *  1. Return the cached last-known fix immediately if present — this is what
 *     makes the screen feel instant when a fix already exists.
 *  2. Otherwise request a fresh fix, but race it against [timeoutMs] so the UI
 *     is never blocked for the up-to-a-minute that getCurrentLocation can take
 *     on a cold GPS. If it times out we return null and the caller falls back
 *     to its default centre / lets the user pick manually.
 *
 * All callers (customer signup, provider service area, edit profile, saved
 * addresses, customer home) should use this instead of hand-rolling the
 * FusedLocationProviderClient dance, so behaviour and latency are consistent.
 */
@Singleton
class LocationFix @Inject constructor() {

    @SuppressLint("MissingPermission")
    suspend fun current(
        context: Context,
        timeoutMs: Long = 6_000L,
    ): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val client = LocationServices.getFusedLocationProviderClient(context)

        // 1) Instant cached fix.
        val cached = runCatching {
            suspendCancellableCoroutine<Location?> { cont ->
                client.lastLocation
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
        }.getOrNull()
        if (cached != null) return@withContext cached.latitude to cached.longitude

        // 2) Fresh fix, bounded by a timeout so we never hang the UI.
        val fresh = withTimeoutOrNull(timeoutMs) {
            val cts = CancellationTokenSource()
            suspendCancellableCoroutine<Location?> { cont ->
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
                cont.invokeOnCancellation { cts.cancel() }
            }
        }
        fresh?.let { it.latitude to it.longitude }
    }
}