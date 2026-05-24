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

    /**
     * Tries to fill in lat/lng from the device's last known location.
     * Permission was requested on the previous (LocationScreen) step; if the
     * user declined, we keep lat/lng null and the screen Continue button
     * stays disabled.
     */
    @SuppressLint("MissingPermission")
    fun resolveLocation() {
        if (_state.value.locating) return
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

            _state.value = _state.value.copy(
                latitude = resolved?.latitude,
                longitude = resolved?.longitude,
                locating = false,
            )
        }
    }
}