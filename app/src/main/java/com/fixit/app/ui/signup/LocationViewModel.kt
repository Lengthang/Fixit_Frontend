package com.fixit.app.ui.signup

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.location.LocationRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

data class SignupLocationState(
    val locating: Boolean = false,
    val saving: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val error: String? = null,
) {
    val hasFix: Boolean get() = latitude != null && longitude != null
}

sealed interface SignupLocationEffect {
    /** Location saved (or intentionally skipped) — safe to advance. */
    data object Done : SignupLocationEffect
}

/**
 * Backs the customer signup [LocationScreen]. Resolves a GPS fix via
 * FusedLocationProviderClient (same flow EditAddressViewModel uses) and
 * persists it to the backend through [LocationRepository.create] so the
 * customer's location is actually saved during onboarding.
 *
 * Kept deliberately thin: all network/Geocoder work lives here, none in the
 * composable — matching the established MVVM + Clean Architecture split.
 */
@HiltViewModel
class LocationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepo: LocationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SignupLocationState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<SignupLocationEffect>()
    val effects = _effects.asSharedFlow()

    fun dismissError() { _state.value = _state.value.copy(error = null) }

    /** Manual pin selection (if the screen ever exposes the map picker). */
    fun onPick(latitude: Double, longitude: Double) {
        _state.value = _state.value.copy(
            latitude = latitude,
            longitude = longitude,
            address = "%.5f, %.5f".format(latitude, longitude),
        )
        viewModelScope.launch {
            reverseGeocode(latitude, longitude)?.let { resolved ->
                _state.value = _state.value.copy(address = resolved)
            }
        }
    }

    /**
     * Called after the OS permission dialog returns "granted". Resolves the
     * current location, saves it as the customer's default location, then
     * emits [SignupLocationEffect.Done]. On any failure we surface an error but
     * still allow the user to proceed (signup must never hard-block here).
     */
    @SuppressLint("MissingPermission")
    fun resolveAndSave() {
        if (_state.value.locating || _state.value.saving) return
        _state.value = _state.value.copy(locating = true, error = null)
        viewModelScope.launch {
            val fix = runCatching { fetchCurrentLocation() }.getOrNull()
            if (fix == null) {
                // No fix (denied/unavailable). Don't block onboarding.
                _state.value = _state.value.copy(locating = false)
                _effects.emit(SignupLocationEffect.Done)
                return@launch
            }

            val (lat, lng) = fix
            val address = reverseGeocode(lat, lng) ?: "%.5f, %.5f".format(lat, lng)
            _state.value = _state.value.copy(
                locating = false,
                saving = true,
                latitude = lat,
                longitude = lng,
                address = address,
            )

            runCatching {
                locationRepo.create(
                    label = "Home",
                    latitude = lat,
                    longitude = lng,
                    address = address,
                    isDefault = true,
                )
            }.onSuccess {
                _state.value = _state.value.copy(saving = false)
                _effects.emit(SignupLocationEffect.Done)
            }.onFailure { e ->
                // Surface the error but still advance — the customer can add an
                // address later from Saved Addresses; we don't trap them here.
                _state.value = _state.value.copy(
                    saving = false,
                    error = e.message ?: "Couldn't save your location",
                )
                _effects.emit(SignupLocationEffect.Done)
            }
        }
    }

    /** "Not now" / permission denied — advance without saving anything. */
    fun skip() {
        viewModelScope.launch { _effects.emit(SignupLocationEffect.Done) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchCurrentLocation(): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            suspendCancellableCoroutine { cont ->
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        cont.resume(loc?.let { it.latitude to it.longitude })
                    }
                    .addOnFailureListener { cont.resume(null) }
                cont.invokeOnCancellation { cts.cancel() }
            }
        }

    private suspend fun reverseGeocode(lat: Double, lng: Double): String? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            runCatching {
                val results = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lng, 1)
                results?.firstOrNull()?.let { addr ->
                    val parts = (0..addr.maxAddressLineIndex).map { addr.getAddressLine(it) }
                    parts.joinToString(", ").ifBlank { null }
                }
            }.getOrNull()
        }
}