package com.fixit.app.ui.signup.provider

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.location.LocationFix
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServiceAreaState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusKm: Int = 15,
    val locating: Boolean = false,
    /**
     * True once the user has explicitly chosen a point (tapped/dragged the pin)
     * OR a GPS fix has populated the coordinates. Stops a late/empty
     * resolveLocation() callback from wiping a valid selection.
     */
    val hasUserSelection: Boolean = false,
)

@HiltViewModel
class ServiceAreaViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationFix: LocationFix,
) : ViewModel() {

    private val _state = MutableStateFlow(ServiceAreaState())
    val state = _state.asStateFlow()

    /**
     * Seed from coordinates already captured on the shared LocationScreen so the
     * map opens centred on the user instead of the Phnom Penh default. Called
     * once from the screen with the draft's lat/lng. Treated as a real selection
     * so the Confirm button is immediately enabled and register() has data even
     * if the provider never touches the map.
     */
    fun seed(latitude: Double?, longitude: Double?, radiusKm: Int) {
        if (_state.value.hasUserSelection) return
        if (latitude != null && longitude != null) {
            _state.value = _state.value.copy(
                latitude = latitude,
                longitude = longitude,
                radiusKm = radiusKm,
                hasUserSelection = true,
            )
        } else {
            _state.value = _state.value.copy(radiusKm = radiusKm)
        }
    }

    fun onRadiusChange(km: Int) {
        _state.value = _state.value.copy(radiusKm = km.coerceIn(1, 50))
    }

    /** User tapped/dragged the pin. Authoritative. */
    fun onPick(latitude: Double, longitude: Double) {
        _state.value = _state.value.copy(
            latitude = latitude,
            longitude = longitude,
            hasUserSelection = true,
        )
    }

    /**
     * Fills lat/lng from the device location using the shared fast resolver.
     * Only applied when a non-null fix arrives AND the user hasn't already
     * picked, so a slow/empty callback can never null out a chosen point.
     */
    fun resolveLocation() {
        if (_state.value.locating) return
        if (_state.value.hasUserSelection) return

        _state.value = _state.value.copy(locating = true)
        viewModelScope.launch {
            val fix = locationFix.current(context)
            val current = _state.value
            if (current.hasUserSelection || fix == null) {
                _state.value = current.copy(locating = false)
            } else {
                _state.value = current.copy(
                    latitude = fix.first,
                    longitude = fix.second,
                    locating = false,
                    hasUserSelection = true,
                )
            }
        }
    }
}