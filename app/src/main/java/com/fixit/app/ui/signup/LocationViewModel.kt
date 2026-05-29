package com.fixit.app.ui.signup

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.location.LocationFix
import com.fixit.app.data.location.LocationRepository
import com.fixit.app.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

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
    /** Location resolved/stored (or intentionally skipped) — safe to advance. */
    data object Done : SignupLocationEffect
}

/**
 * Backs the shared signup [LocationScreen] used by BOTH customer and provider.
 *
 * Responsibilities:
 *  1. Resolve a device fix quickly (via [LocationFix], which is fast-first and
 *     timeout-bounded so the screen never hangs ~a minute).
 *  2. Write the fix into the shared [SignupDraftViewModel] draft so the PROVIDER
 *     branch (ServiceAreaScreen / register) carries real coordinates forward.
 *     This is the fix for "provider location never recorded": previously the
 *     permission step saved a customer row but fed nothing to the provider draft.
 *  3. For CUSTOMERS only, also persist a default SavedLocation to the backend.
 *
 * The draft is passed in per-call (it is owned by the signup nav graph, not by
 * this ViewModel) so we don't duplicate or fight its lifecycle.
 */
@HiltViewModel
class LocationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepo: LocationRepository,
    private val locationFix: LocationFix,
) : ViewModel() {

    private val _state = MutableStateFlow(SignupLocationState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<SignupLocationEffect>()
    val effects = _effects.asSharedFlow()

    fun dismissError() { _state.value = _state.value.copy(error = null) }

    /**
     * Called after the OS permission dialog returns "granted".
     *
     * @param role  current signup role (decides whether we also POST a
     *              customer SavedLocation).
     * @param onResolved called with (lat,lng,address) the moment we have a fix,
     *              BEFORE any network save, so the caller can write it into the
     *              shared draft synchronously. Always invoked on a fix; not
     *              invoked when no fix could be obtained.
     */
    fun resolveAndSave(
        role: UserRole,
        onResolved: (Double, Double, String?) -> Unit,
    ) {
        if (_state.value.locating || _state.value.saving) return
        _state.value = _state.value.copy(locating = true, error = null)
        viewModelScope.launch {
            val fix = locationFix.current(context)
            if (fix == null) {
                // No fix within the timeout. Don't block onboarding; the
                // provider can still set the pin on ServiceAreaScreen, and the
                // customer can add an address later.
                _state.value = _state.value.copy(locating = false)
                _effects.emit(SignupLocationEffect.Done)
                return@launch
            }

            val (lat, lng) = fix
            val address = reverseGeocode(lat, lng) ?: "%.5f, %.5f".format(lat, lng)

            // Feed the draft immediately so BOTH branches have coordinates even
            // if the customer save below fails or is skipped.
            onResolved(lat, lng, address)

            _state.value = _state.value.copy(
                locating = false,
                latitude = lat,
                longitude = lng,
                address = address,
            )

            if (role != UserRole.CUSTOMER) {
                // Provider: coordinates now live in the draft; the dedicated
                // ServiceAreaScreen owns the final service-area persistence at
                // register time. Nothing to POST here.
                _effects.emit(SignupLocationEffect.Done)
                return@launch
            }

            // Customer: persist a default saved location.
            _state.value = _state.value.copy(saving = true)
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