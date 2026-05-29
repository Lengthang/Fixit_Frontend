package com.fixit.app.ui.customer.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.customer.CustomerRepository
import com.fixit.app.data.upload.UploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerEditProfileState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val errorMessage: String? = null,

    val name: String = "",
    val email: String = "",
    // Phone is shown read-only — it's the login identity and isn't edited here.
    val phone: String = "",
    val profilePhotoUrl: String? = null,
) {
    /** Save is allowed only when a name is present and nothing is in flight. */
    val canSave: Boolean
        get() = name.isNotBlank() && !isSaving && !isUploadingPhoto && !isLoading
}

sealed interface CustomerEditProfileEffect {
    data object SavedOk : CustomerEditProfileEffect
    data class ShowError(val message: String) : CustomerEditProfileEffect
}

@HiltViewModel
class CustomerEditProfileViewModel @Inject constructor(
    private val customerRepo: CustomerRepository,
    private val uploadRepo: UploadRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerEditProfileState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<CustomerEditProfileEffect>()
    val effects = _effects.asSharedFlow()

    // Driven by the screen via OnLifecycleStart — same pattern as the profile screen.
    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { customerRepo.me() }
                .onSuccess { me ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        name = me.name.orEmpty(),
                        email = me.email.orEmpty(),
                        phone = me.phone,
                        profilePhotoUrl = me.profilePhotoUrl,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't load your info",
                    )
                }
        }
    }

    fun onNameChange(value: String) {
        _state.value = _state.value.copy(name = value)
    }

    fun onEmailChange(value: String) {
        _state.value = _state.value.copy(email = value)
    }

    /** Upload a freshly-picked photo, then keep the returned URL for the save. */
    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploadingPhoto = true)
            runCatching { uploadRepo.uploadImage(uri) }
                .onSuccess { url ->
                    _state.value = _state.value.copy(
                        isUploadingPhoto = false,
                        profilePhotoUrl = url,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isUploadingPhoto = false)
                    _effects.emit(
                        CustomerEditProfileEffect.ShowError(
                            e.message ?: "Couldn't upload photo"
                        )
                    )
                }
        }
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            _state.value = s.copy(isSaving = true)
            runCatching {
                customerRepo.update(
                    name = s.name.trim(),
                    email = s.email.trim().takeIf { it.isNotBlank() },
                    profilePhotoUrl = s.profilePhotoUrl,
                )
            }
                .onSuccess {
                    _state.value = _state.value.copy(isSaving = false)
                    _effects.emit(CustomerEditProfileEffect.SavedOk)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isSaving = false)
                    _effects.emit(
                        CustomerEditProfileEffect.ShowError(
                            e.message ?: "Couldn't save changes"
                        )
                    )
                }
        }
    }
}