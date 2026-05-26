package com.fixit.app.ui.provider.profile

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.category.CategoryRepository
import com.fixit.app.data.category.CategoryResponse
import com.fixit.app.data.customer.CustomerRepository
import com.fixit.app.data.provider.AvailabilityInput
import com.fixit.app.data.provider.ProviderRepository
import com.fixit.app.data.provider.ProviderUpdateRequest
import com.fixit.app.data.upload.UploadRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

/** Which slot the image picker is targeting on its next callback. */
enum class UploadTarget { PHOTO, CERTIFICATE, NATIONAL_ID }

/** Day order matches ScheduleScreen — Monday first, indices 0..6. */
private val DAY_WIRE = listOf(
    "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
)

data class EditProfileState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,

    // Identity (User row)
    val originalName: String = "",
    val name: String = "",

    // Provider profile
    val bio: String = "",
    val yearsExperience: String = "0",
    val profilePhotoUrl: String? = null,

    // Certificates (Q3: single cert + single national ID, both images)
    val certificationName: String = "",
    val certificationUrl: String? = null,
    val nationalIdUrl: String? = null,

    // Categories
    val availableCategories: List<CategoryResponse> = emptyList(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val categoryPickerOpen: Boolean = false,

    // Work location (lat/lng + radius). `locationLabel` is the human-readable
    // address text — populated by Geocoder when available, else "lat, lng".
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String = "",
    val serviceRadiusKm: Int = 10,
    val locating: Boolean = false,

    // Availability — single shared window across all selected days, same as
    // ScheduleScreen. activeDays[0] == Monday, [6] == Sunday.
    val activeDays: List<Boolean> = List(7) { false },
    val openTime: String = "08:00",   // "HH:MM" 24h
    val closeTime: String = "17:00",  // "HH:MM" 24h

    // Uploading flags surface inline spinners on each row.
    val uploadingPhoto: Boolean = false,
    val uploadingCert: Boolean = false,
    val uploadingNationalId: Boolean = false,
)

sealed interface EditProfileEffect {
    data object SavedOk : EditProfileEffect
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val providerRepo: ProviderRepository,
    private val customerRepo: CustomerRepository,
    private val categoryRepo: CategoryRepository,
    private val uploadRepo: UploadRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<EditProfileEffect>()
    val effects = _effects.asSharedFlow()

    /**
     * Tracked outside StateFlow so it doesn't trigger recomposition — the
     * picker reads this synchronously when an image is returned.
     */
    var pendingUploadTarget: UploadTarget = UploadTarget.PHOTO
        private set

    init { load() }

    // ── form field updaters ───────────────────────────────────────────────

    fun onNameChange(v: String)              { _state.value = _state.value.copy(name = v.take(120)) }
    fun onBioChange(v: String)               { _state.value = _state.value.copy(bio = v.take(280)) }
    fun onYearsChange(v: String) {
        // Numeric only, clamp to 0..60.
        val clean = v.filter { it.isDigit() }.take(2)
        _state.value = _state.value.copy(yearsExperience = clean.ifBlank { "0" })
    }
    fun onCertNameChange(v: String)          { _state.value = _state.value.copy(certificationName = v.take(120)) }
    fun onRadiusChange(km: Int)              { _state.value = _state.value.copy(serviceRadiusKm = km.coerceIn(1, 50)) }
    fun toggleDay(index: Int) {
        if (index !in 0..6) return
        val updated = _state.value.activeDays.toMutableList().also { it[index] = !it[index] }
        _state.value = _state.value.copy(activeDays = updated)
    }
    fun onOpenTimeChange(hhmm: String)       { _state.value = _state.value.copy(openTime = hhmm) }
    fun onCloseTimeChange(hhmm: String)      { _state.value = _state.value.copy(closeTime = hhmm) }
    fun dismissError()                       { _state.value = _state.value.copy(errorMessage = null) }

    fun removePhoto()                        { _state.value = _state.value.copy(profilePhotoUrl = null) }
    fun removeCertificate() {
        _state.value = _state.value.copy(certificationUrl = null, certificationName = "")
    }
    fun removeNationalId()                   { _state.value = _state.value.copy(nationalIdUrl = null) }

    fun removeCategory(id: String) {
        _state.value = _state.value.copy(
            selectedCategoryIds = _state.value.selectedCategoryIds - id,
        )
    }

    fun openCategoryPicker()                 { _state.value = _state.value.copy(categoryPickerOpen = true) }
    fun closeCategoryPicker()                { _state.value = _state.value.copy(categoryPickerOpen = false) }
    fun toggleCategory(id: String) {
        val current = _state.value.selectedCategoryIds
        _state.value = _state.value.copy(
            selectedCategoryIds = if (id in current) current - id else current + id,
        )
    }

    // ── image picker plumbing ─────────────────────────────────────────────

    fun setUploadTarget(target: UploadTarget) {
        pendingUploadTarget = target
    }

    /**
     * Called by the screen when the system image picker returns a URI.
     * Dispatches the upload to the right slot based on [pendingUploadTarget].
     */
    fun onImagePicked(uri: Uri) {
        when (pendingUploadTarget) {
            UploadTarget.PHOTO        -> uploadPhoto(uri)
            UploadTarget.CERTIFICATE  -> uploadCert(uri)
            UploadTarget.NATIONAL_ID  -> uploadNationalId(uri)
        }
    }

    private fun uploadPhoto(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(uploadingPhoto = true)
            runCatching { uploadRepo.uploadImage(uri) }
                .onSuccess { url ->
                    _state.value = _state.value.copy(profilePhotoUrl = url, uploadingPhoto = false)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        uploadingPhoto = false,
                        errorMessage   = e.message ?: "Couldn't upload photo",
                    )
                }
        }
    }

    private fun uploadCert(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(uploadingCert = true)
            runCatching { uploadRepo.uploadImage(uri) }
                .onSuccess { url ->
                    _state.value = _state.value.copy(certificationUrl = url, uploadingCert = false)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        uploadingCert = false,
                        errorMessage  = e.message ?: "Couldn't upload certificate",
                    )
                }
        }
    }

    private fun uploadNationalId(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(uploadingNationalId = true)
            runCatching { uploadRepo.uploadImage(uri) }
                .onSuccess { url ->
                    _state.value = _state.value.copy(nationalIdUrl = url, uploadingNationalId = false)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        uploadingNationalId = false,
                        errorMessage        = e.message ?: "Couldn't upload national ID",
                    )
                }
        }
    }

    // ── location resolution (mirrors ServiceAreaViewModel) ────────────────

    /**
     * Pulls a fresh device fix and writes lat/lng into state, then resolves a
     * human-readable address via [Geocoder] and overwrites [locationLabel].
     *
     * Behaviour matches ServiceAreaViewModel exactly for the location part —
     * cached last-known first (fast), then a fresh fix if cache is empty.
     * The address resolution is the only addition; it runs after coordinates
     * have already been published so the screen updates "instantly" with
     * coords, then upgrades to a friendly name when geocoding lands.
     */
    @SuppressLint("MissingPermission")
    fun resolveLocation() {
        if (_state.value.locating) return
        _state.value = _state.value.copy(locating = true)
        viewModelScope.launch {
            val client = LocationServices.getFusedLocationProviderClient(context)

            val cached = suspendCancellableCoroutine<android.location.Location?> { cont ->
                client.lastLocation
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
            val resolved = cached ?: suspendCancellableCoroutine<android.location.Location?> { cont ->
                val cts = CancellationTokenSource()
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
                cont.invokeOnCancellation { cts.cancel() }
            }

            if (resolved == null) {
                _state.value = _state.value.copy(locating = false)
                return@launch
            }

            // Phase 1 — coords land instantly so the map/circle/badge reflects
            // the new fix without waiting for geocoding. Label is a coord
            // string in the meantime; it's overwritten below if geocoding
            // succeeds.
            val lat = resolved.latitude
            val lng = resolved.longitude
            _state.value = _state.value.copy(
                latitude      = lat,
                longitude     = lng,
                locationLabel = "%.4f, %.4f".format(lat, lng),
                locating      = false,
            )

            // Phase 2 — async reverse-geocode. Best-effort; failures leave the
            // coord label in place.
            val address = reverseGeocode(lat, lng)
            if (!address.isNullOrBlank()) {
                _state.value = _state.value.copy(locationLabel = address)
            }
        }
    }

    /**
     * Uses Android's built-in [Geocoder] to turn coordinates into a short
     * "City, Region" string. Runs on the IO dispatcher because the sync
     * `getFromLocation(...)` call performs a blocking network request on
     * older API levels. Deprecation is suppressed because the async variant
     * only exists from API 33 onward; the project's minSdk is 24.
     */
    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(lat: Double, lng: Double): String? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            runCatching {
                val results = Geocoder(context, Locale.getDefault())
                    .getFromLocation(lat, lng, 1)
                results?.firstOrNull()?.let { addr ->
                    val primary   = addr.locality ?: addr.subAdminArea ?: addr.subLocality
                    val secondary = addr.adminArea ?: addr.countryName
                    listOfNotNull(primary, secondary)
                        .joinToString(", ")
                        .ifBlank { null }
                }
            }.getOrNull()
        }

    // ── initial load ──────────────────────────────────────────────────────

    private fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { loadAll() }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading    = false,
                        errorMessage = e.message ?: "Couldn't load profile",
                    )
                }
                .onSuccess {
                    _state.value = _state.value.copy(isLoading = false)
                }
        }
    }

    private suspend fun loadAll() = coroutineScope {
        // Fetch in parallel — survive individual failures.
        val meDeferred       = async { runCatching { customerRepo.me() } }
        val providerDeferred = async { runCatching { providerRepo.me() } }
        val categoriesDef    = async { runCatching { categoryRepo.list() } }

        val me         = meDeferred.await().getOrNull()
        val provider   = providerDeferred.await().getOrNull()
        val categories = categoriesDef.await().getOrNull().orEmpty()

        // Map availability to a Monday-first 7-bit array plus a single window.
        // Multiple slots per day are flattened to "any slot active = day on";
        // the open/close shown is taken from the first slot found.
        val activeDays = MutableList(7) { false }
        var openTime  = _state.value.openTime
        var closeTime = _state.value.closeTime
        var sawWindow = false
        provider?.availability?.forEach { slot ->
            val idx = DAY_WIRE.indexOf(slot.dayOfWeek.lowercase())
            if (idx in 0..6) {
                activeDays[idx] = true
                if (!sawWindow) {
                    openTime  = slot.openTime.take(5)   // strip seconds: "08:00:00" → "08:00"
                    closeTime = slot.closeTime.take(5)
                    sawWindow = true
                }
            }
        }

        _state.value = _state.value.copy(
            originalName       = me?.name.orEmpty(),
            name               = me?.name.orEmpty(),
            bio                = provider?.bio.orEmpty(),
            yearsExperience    = (provider?.yearsExperience ?: 0).toString(),
            profilePhotoUrl    = me?.profilePhotoUrl ?: provider?.profilePhotoUrl,
            certificationName  = provider?.certification.orEmpty(),
            certificationUrl   = provider?.certificationUrl,
            nationalIdUrl      = provider?.nationalIdUrl,
            availableCategories = categories,
            selectedCategoryIds = provider?.categories?.map { it.id }?.toSet().orEmpty(),
            latitude           = provider?.latitude,
            longitude          = provider?.longitude,
            locationLabel      = provider?.location.orEmpty(),
            serviceRadiusKm    = provider?.serviceRadiusKm ?: 10,
            activeDays         = activeDays,
            openTime           = openTime,
            closeTime          = closeTime,
        )

        // If the saved provider record has coordinates but no human-readable
        // address (or a fallback like "Provider — Service area"), upgrade the
        // label in the background. Same Geocoder path the Change button uses.
        val savedLat = provider?.latitude
        val savedLng = provider?.longitude
        val savedLabel = provider?.location.orEmpty()
        if (savedLat != null && savedLng != null &&
            (savedLabel.isBlank() || savedLabel.contains("Service area"))
        ) {
            val address = reverseGeocode(savedLat, savedLng)
            if (!address.isNullOrBlank()) {
                _state.value = _state.value.copy(locationLabel = address)
            }
        }
    }

    // ── save ──────────────────────────────────────────────────────────────

    /**
     * Fan out the update: name goes through PATCH /customer/me (only if it
     * changed), everything else through PATCH /providers/me. If either fails
     * we surface a single combined error and don't pop the screen.
     */
    fun save() {
        val s = _state.value
        if (s.isSaving) return

        // Build the day→time slots payload. The backend rewrites all
        // availability rows on every PATCH that includes this field, so the
        // current state IS the desired full state.
        val slots = s.activeDays.mapIndexedNotNull { i, on ->
            if (!on) null else AvailabilityInput(
                dayOfWeek = DAY_WIRE[i],
                openTime  = s.openTime,
                closeTime = s.closeTime,
            )
        }

        // Validation that matches what the backend will enforce anyway —
        // catching it client-side gives a nicer message.
        if (s.latitude == null || s.longitude == null) {
            _state.value = s.copy(errorMessage = "Set your work location before saving")
            return
        }
        if (s.selectedCategoryIds.isEmpty()) {
            _state.value = s.copy(errorMessage = "Pick at least one service category")
            return
        }
        if (slots.isEmpty()) {
            _state.value = s.copy(errorMessage = "Pick at least one working day")
            return
        }

        viewModelScope.launch {
            _state.value = s.copy(isSaving = true, errorMessage = null)
            val outcome = runCatching {
                coroutineScope {
                    val providerJob = async {
                        providerRepo.updateProfile(
                            ProviderUpdateRequest(
                                bio              = s.bio.takeIf { it.isNotBlank() },
                                profilePhotoUrl  = s.profilePhotoUrl,
                                yearsExperience  = s.yearsExperience.toIntOrNull() ?: 0,
                                certification    = s.certificationName.takeIf { it.isNotBlank() },
                                certificationUrl = s.certificationUrl,
                                nationalIdUrl    = s.nationalIdUrl,
                                location         = s.locationLabel.takeIf { it.isNotBlank() },
                                latitude         = s.latitude,
                                longitude        = s.longitude,
                                serviceRadiusKm  = s.serviceRadiusKm,
                                categoryIds      = s.selectedCategoryIds.toList(),
                                availability     = slots,
                            )
                        )
                    }

                    // Name belongs to the User row, not ProviderProfile.
                    // Only call if it actually changed.
                    val nameJob = if (s.name.trim() != s.originalName.trim() &&
                        s.name.isNotBlank()
                    ) {
                        async {
                            customerRepo.update(
                                name            = s.name.trim(),
                                profilePhotoUrl = s.profilePhotoUrl,
                            )
                        }
                    } else null

                    providerJob.await()
                    nameJob?.await()
                }
            }

            outcome
                .onSuccess {
                    _state.value = _state.value.copy(isSaving = false)
                    _effects.emit(EditProfileEffect.SavedOk)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isSaving     = false,
                        errorMessage = e.message ?: "Couldn't save profile",
                    )
                }
        }
    }
}