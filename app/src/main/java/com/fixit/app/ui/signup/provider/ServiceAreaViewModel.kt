package com.fixit.app.ui.signup.provider

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

data class ServiceAreaState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusKm: Int = 15,
    val locating: Boolean = false,
    /**
     * True once the user has explicitly chosen a point (dragged/tapped the pin)
     * OR a GPS fix has successfully populated the coordinates. Used to stop a
     * late/empty resolveLocation() callback from wiping a valid selection.
     */
    val hasUserSelection: Boolean = false,
)

@HiltViewModel
class ServiceAreaViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(ServiceAreaState())
    val state = _state.asStateFlow()

    fun onRadiusChange(km: Int) {
        _state.value = _state.value.copy(radiusKm = km.coerceIn(1, 50))
    }

    /** User dropped / dragged the pin or tapped the map. This is authoritative. */
    fun onPick(latitude: Double, longitude: Double) {
        _state.value = _state.value.copy(
            latitude = latitude,
            longitude = longitude,
            hasUserSelection = true,
        )
    }

    /**
     * Tries to fill in lat/lng from the device's last known location.
     * Permission was requested on the previous (LocationScreen) step; if the
     * user declined, we keep lat/lng null and the screen Continue button
     * stays disabled.
     *
     * Important: a resolved fix is ONLY applied when it is non-null AND the user
     * has not already chosen a point. This prevents a slow or failed GPS
     * callback from overwriting (and nulling out) a pin the provider already
     * dragged into place — the bug that was silently discarding the chosen
     * service-area coordinates before they could be saved.
     */
    @SuppressLint("MissingPermission")
    fun resolveLocation() {
        if (_state.value.locating) return
        // Don't re-resolve over an explicit user selection.
        if (_state.value.hasUserSelection) return

        _state.value = _state.value.copy(locating = true)
        viewModelScope.launch {
            val client = LocationServices.getFusedLocationProviderClient(context)

            // 1) Try cached last-known fix first (fast, no battery cost).
            val cached = suspendCancellableCoroutine<android.location.Location?> { cont ->
                client.lastLocation
                    .addOnSuccessListener {
                        Log.d("ServiceArea", "lastLocation returned: $it")
                        cont.resume(it)
                    }
                    .addOnFailureListener {
                        Log.w("ServiceArea", "lastLocation failed", it)
                        cont.resume(null)
                    }
            }

            val resolved = cached ?: run {
                // 2) No cached fix — actively request one. This takes a few seconds.
                Log.d("ServiceArea", "Requesting fresh location…")
                suspendCancellableCoroutine<android.location.Location?> { cont ->
                    val cts = CancellationTokenSource()
                    client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                        .addOnSuccessListener {
                            Log.d("ServiceArea", "getCurrentLocation returned: $it")
                            cont.resume(it)
                        }
                        .addOnFailureListener {
                            Log.w("ServiceArea", "getCurrentLocation failed", it)
                            cont.resume(null)
                        }
                    cont.invokeOnCancellation { cts.cancel() }
                }
            }

            // Guard: if the user dragged the pin while we were resolving, OR the
            // fix came back null, leave the existing selection untouched.
            val current = _state.value
            if (current.hasUserSelection || resolved == null) {
                _state.value = current.copy(locating = false)
            } else {
                _state.value = current.copy(
                    latitude = resolved.latitude,
                    longitude = resolved.longitude,
                    locating = false,
                    hasUserSelection = true,
                )
            }
        }
    }
}