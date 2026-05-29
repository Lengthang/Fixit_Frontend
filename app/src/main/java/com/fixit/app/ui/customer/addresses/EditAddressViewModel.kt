package com.fixit.app.ui.customer.addresses

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.SavedStateHandle
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

data class EditAddressState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,

    /** Null when creating a new address; set when editing an existing one. */
    val editingId: String? = null,

    val label: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String = "",
    val isDefault: Boolean = false,
    val locating: Boolean = false,
) {
    val canSave: Boolean
        get() = !isSaving && label.isNotBlank() && latitude != null && longitude != null
}

sealed interface EditAddressEffect {
    data object SavedOk : EditAddressEffect
}

@HiltViewModel
class EditAddressViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepo: LocationRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    /** Present only on the edit route; null means "create". */
    private val locationId: String? = savedState["locationId"]

    private val _state = MutableStateFlow(EditAddressState(editingId = locationId))
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<EditAddressEffect>()
    val effects = _effects.asSharedFlow()

    init {
        if (locationId != null) loadExisting(locationId)
    }

    private fun loadExisting(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { locationRepo.get(id) }
                .onSuccess { loc ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        label = loc.label,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        address = loc.address.orEmpty(),
                        isDefault = loc.isDefault,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't load address",
                    )
                }
        }
    }

    fun onLabelChange(v: String) { _state.value = _state.value.copy(label = v.take(60)) }
    fun onDefaultChange(v: Boolean) { _state.value = _state.value.copy(isDefault = v) }
    fun dismissError() { _state.value = _state.value.copy(errorMessage = null) }

    /** Pin moved / map tapped — set coords now, reverse-geocode the label async. */
    fun onPick(latitude: Double, longitude: Double) {
        _state.value = _state.value.copy(
            latitude = latitude,
            longitude = longitude,
            address = "%.5f, %.5f".format(latitude, longitude),
        )
        viewModelScope.launch {
            val resolved = reverseGeocode(latitude, longitude)
            if (!resolved.isNullOrBlank()) {
                _state.value = _state.value.copy(address = resolved)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun resolveLocation() {
        if (_state.value.locating) return
        viewModelScope.launch {
            _state.value = _state.value.copy(locating = true)
            val fix = runCatching { fetchCurrentLocation() }.getOrNull()
            if (fix != null) onPick(fix.first, fix.second)
            _state.value = _state.value.copy(locating = false)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchCurrentLocation(): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            suspendCancellableCoroutine { cont ->
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { loc -> cont.resume(loc?.let { it.latitude to it.longitude }) }
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

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        val lat = s.latitude ?: return
        val lng = s.longitude ?: return
        _state.value = s.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                if (s.editingId == null) {
                    locationRepo.create(
                        label = s.label.trim(),
                        latitude = lat,
                        longitude = lng,
                        address = s.address.takeIf { it.isNotBlank() },
                        isDefault = s.isDefault,
                    )
                } else {
                    locationRepo.update(
                        id = s.editingId,
                        label = s.label.trim(),
                        latitude = lat,
                        longitude = lng,
                        address = s.address.takeIf { it.isNotBlank() },
                        isDefault = s.isDefault,
                    )
                }
            }.onSuccess {
                _state.value = _state.value.copy(isSaving = false)
                _effects.emit(EditAddressEffect.SavedOk)
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Couldn't save address",
                )
            }
        }
    }
}