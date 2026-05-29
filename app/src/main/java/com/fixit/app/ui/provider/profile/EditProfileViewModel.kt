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

enum class UploadTarget { PHOTO, CERTIFICATE, NATIONAL_ID }

private val DAY_WIRE = listOf(
    "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
)

data class EditProfileState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,

    val originalName: String = "",
    val name: String = "",

    val bio: String = "",
    val yearsExperience: String = "0",
    val profilePhotoUrl: String? = null,

    val certificationName: String = "",
    val certificationUrl: String? = null,
    val nationalIdUrl: String? = null,

    val availableCategories: List<CategoryResponse> = emptyList(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val categoryPickerOpen: Boolean = false,

    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String = "",
    val serviceRadiusKm: Int = 10,
    val locating: Boolean = false,

    val activeDays: List<Boolean> = List(7) { false },
    val openTime: String = "08:00",
    val closeTime: String = "17:00",

    val uploadingPhoto: Boolean = false,
    val uploadingCert: Boolean = false,
    val uploadingNationalId: Boolean = false,

    /**
     * True after the first successful load. Used by the screen-side lifecycle
     * refresh to skip clobbering user-edited form fields with server data on
     * ON_START if we've already loaded once and the user has the form open.
     */
    val hasLoaded: Boolean = false,
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

    var pendingUploadTarget: UploadTarget = UploadTarget.PHOTO
        private set

    // NOTE: no init { load() } — screen-side OnLifecycleStart calls refresh().

    fun onNameChange(v: String)              { _state.value = _state.value.copy(name = v.take(120)) }
    fun onBioChange(v: String)               { _state.value = _state.value.copy(bio = v.take(280)) }
    fun onYearsChange(v: String) {
        val clean = v.filter { it.isDigit() }.take(2)
        _state.value = _state.value.copy(yearsExperience = clean.ifBlank { "0" })
    }
    fun onCertNameChange(v: String)          { _state.value = _state.value.copy(certificationName = v.take(120)) }
    fun onRadiusChange(km: Int)              { _state.value = _state.value.copy(serviceRadiusKm = km.coerceIn(1, 50)) }

    fun onPick(latitude: Double, longitude: Double) {
        _state.value = _state.value.copy(
            latitude = latitude,
            longitude = longitude,
            locationLabel = "%.5f, %.5f".format(latitude, longitude),
        )
        viewModelScope.launch {
            val address = reverseGeocode(latitude, longitude)
            if (!address.isNullOrBlank()) {
                _state.value = _state.value.copy(locationLabel = address)
            }
        }
    }
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

    fun setUploadTarget(target: UploadTarget) {
        pendingUploadTarget = target
    }

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

    @SuppressLint("MissingPermission")
    fun resolveLocation() {
        if (_state.value.locating) return
        viewModelScope.launch {
            _state.value = _state.value.copy(locating = true)
            val fix = runCatching { fetchLastKnownLocation() }.getOrNull()
            if (fix != null) {
                _state.value = _state.value.copy(
                    latitude       = fix.first,
                    longitude      = fix.second,
                    locationLabel  = "%.5f, %.5f".format(fix.first, fix.second),
                    locating       = false,
                )
                val address = reverseGeocode(fix.first, fix.second)
                if (!address.isNullOrBlank()) {
                    _state.value = _state.value.copy(locationLabel = address)
                }
            } else {
                _state.value = _state.value.copy(locating = false)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchLastKnownLocation(): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts    = CancellationTokenSource()
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

    /**
     * Loads the form from the server. Called once on first ON_START, then again
     * on every subsequent ON_START via the screen's lifecycle observer.
     *
     * After [hasLoaded] flips to true we skip subsequent calls: the user is
     * actively editing the form, and a refresh would clobber their unsaved
     * changes with server values. The single exception is right after save —
     * the SavedOk effect pops the screen so the next entry is a fresh VM.
     */
    fun refresh() {
        if (_state.value.hasLoaded) return
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
                    _state.value = _state.value.copy(isLoading = false, hasLoaded = true)
                }
        }
    }

    private suspend fun loadAll() = coroutineScope {
        val meDeferred       = async { runCatching { customerRepo.me() } }
        val providerDeferred = async { runCatching { providerRepo.me() } }
        val categoriesDef    = async { runCatching { categoryRepo.list() } }

        val me         = meDeferred.await().getOrNull()
        val provider   = providerDeferred.await().getOrNull()
        val categories = categoriesDef.await().getOrNull().orEmpty()

        val activeDays = MutableList(7) { false }
        var openTime  = _state.value.openTime
        var closeTime = _state.value.closeTime
        var sawWindow = false
        provider?.availability?.forEach { slot ->
            val idx = DAY_WIRE.indexOf(slot.dayOfWeek.lowercase())
            if (idx in 0..6) {
                activeDays[idx] = true
                if (!sawWindow) {
                    openTime  = slot.openTime.take(5)
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

    fun save() {
        val s = _state.value
        if (s.isSaving) return

        val slots = s.activeDays.mapIndexedNotNull { i, on ->
            if (!on) null else AvailabilityInput(
                dayOfWeek = DAY_WIRE[i],
                openTime  = s.openTime,
                closeTime = s.closeTime,
            )
        }

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