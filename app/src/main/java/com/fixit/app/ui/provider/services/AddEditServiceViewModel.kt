package com.fixit.app.ui.provider.services

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.category.CategoryResponse
import com.fixit.app.data.provider.ProviderRepository
import com.fixit.app.data.service.ServiceCreateRequest
import com.fixit.app.data.service.ServiceRepository
import com.fixit.app.data.service.ServiceUpdateRequest
import com.fixit.app.data.upload.UploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditServiceState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val errorMessage: String? = null,

    // Form fields
    val title: String = "",
    val description: String = "",
    val selectedCategoryId: String = "",
    val price: String = "",
    val durationMinutes: String = "",
    val isVisible: Boolean = true,
    val imageUrl: String? = null,

    // Support data loaded on init
    val availableCategories: List<CategoryResponse> = emptyList(),

    // Edit mode
    val isEditing: Boolean = false,
)

sealed interface AddEditServiceEffect {
    /** Saved successfully — pop back to the list or detail screen. */
    data object SavedOk : AddEditServiceEffect
}

@HiltViewModel
class AddEditServiceViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val serviceRepo: ServiceRepository,
    private val providerRepo: ProviderRepository,
    private val uploadRepo: UploadRepository,
) : ViewModel() {

    /**
     * Present when editing an existing service; null when creating a new one.
     * Declared nullable because SavedStateHandle returns null for absent keys.
     */
    private val serviceId: String? = savedState["serviceId"]

    private val _state = MutableStateFlow(AddEditServiceState(isEditing = serviceId != null))
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<AddEditServiceEffect>()
    val effects = _effects.asSharedFlow()

    init { loadInitialData() }

    // ── form field updaters ───────────────────────────────────────────────

    fun onTitleChange(value: String)            { _state.value = _state.value.copy(title = value.take(150)) }
    fun onDescriptionChange(value: String)      { _state.value = _state.value.copy(description = value.take(500)) }
    fun onCategorySelected(categoryId: String)  { _state.value = _state.value.copy(selectedCategoryId = categoryId) }
    fun onPriceChange(value: String)            { _state.value = _state.value.copy(price = value) }
    fun onDurationChange(value: String)         { _state.value = _state.value.copy(durationMinutes = value) }
    fun onVisibilityChange(visible: Boolean)    { _state.value = _state.value.copy(isVisible = visible) }
    fun onRemovePhoto()                         { _state.value = _state.value.copy(imageUrl = null) }
    fun dismissError()                          { _state.value = _state.value.copy(errorMessage = null) }

    // ── photo upload ──────────────────────────────────────────────────────

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploadingPhoto = true)
            runCatching { uploadRepo.uploadImage(uri) }
                .onSuccess { url ->
                    _state.value = _state.value.copy(imageUrl = url, isUploadingPhoto = false)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isUploadingPhoto = false,
                        errorMessage     = e.message ?: "Failed to upload photo",
                    )
                }
        }
    }

    // ── save ──────────────────────────────────────────────────────────────

    fun save() {
        val s = _state.value

        // Validation
        if (s.title.isBlank()) {
            _state.value = s.copy(errorMessage = "Service name is required")
            return
        }
        if (s.selectedCategoryId.isBlank()) {
            _state.value = s.copy(errorMessage = "Please select a category")
            return
        }
        val priceDouble = s.price.toDoubleOrNull()
        if (priceDouble == null || priceDouble <= 0) {
            _state.value = s.copy(errorMessage = "Enter a valid price greater than 0")
            return
        }
        val durationInt = s.durationMinutes.takeIf { it.isNotBlank() }?.toIntOrNull()
        if (s.durationMinutes.isNotBlank() && (durationInt == null || durationInt <= 0)) {
            _state.value = s.copy(errorMessage = "Duration must be a positive number of minutes")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            runCatching {
                val id = serviceId
                if (id == null) {
                    serviceRepo.create(
                        ServiceCreateRequest(
                            categoryId      = s.selectedCategoryId,
                            title           = s.title.trim(),
                            description     = s.description.trim().ifBlank { null },
                            imageUrl        = s.imageUrl,
                            price           = priceDouble,
                            durationMinutes = durationInt,
                        )
                    )
                } else {
                    serviceRepo.update(
                        id = id,
                        body = ServiceUpdateRequest(
                            categoryId      = s.selectedCategoryId,
                            title           = s.title.trim(),
                            description     = s.description.trim().ifBlank { null },
                            imageUrl        = s.imageUrl,
                            price           = priceDouble,
                            durationMinutes = durationInt,
                            isActive        = s.isVisible,
                        ),
                    )
                }
            }.onSuccess {
                _state.value = _state.value.copy(isSaving = false)
                _effects.emit(AddEditServiceEffect.SavedOk)
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isSaving     = false,
                    errorMessage = e.message ?: "Failed to save service",
                )
            }
        }
    }

    // ── init loading ──────────────────────────────────────────────────────

    private fun loadInitialData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            runCatching { loadAll() }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading    = false,
                        errorMessage = e.message ?: "Couldn't load data",
                    )
                }
                .onSuccess {
                    _state.value = _state.value.copy(isLoading = false)
                }
        }
    }

    private suspend fun loadAll() = coroutineScope {
        val providerDeferred = async { providerRepo.me() }

        // Only fetch existing service when editing.
        val existingDeferred = serviceId?.let {
            async { runCatching { serviceRepo.mine() } }
        }

        val provider = providerDeferred.await()
        val existingServices = existingDeferred?.await()?.getOrNull()
        val existing = serviceId?.let { id ->
            existingServices?.firstOrNull { it.id == id }
        }

        _state.value = _state.value.copy(
            availableCategories = provider.categories,
            // Pre-populate fields when editing
            title               = existing?.title ?: _state.value.title,
            description         = existing?.description ?: _state.value.description,
            selectedCategoryId  = existing?.categoryId ?: _state.value.selectedCategoryId,
            price               = existing?.price?.let { "%.2f".format(it) } ?: _state.value.price,
            durationMinutes     = existing?.durationMinutes?.toString() ?: _state.value.durationMinutes,
            isVisible           = existing?.isActive ?: _state.value.isVisible,
            imageUrl            = existing?.imageUrl ?: _state.value.imageUrl,
        )
    }
}