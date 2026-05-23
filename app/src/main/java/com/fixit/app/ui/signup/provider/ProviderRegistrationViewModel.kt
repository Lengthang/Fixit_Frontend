package com.fixit.app.ui.signup.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.local.TokenStorage
import com.fixit.app.data.provider.ProviderRepository
import com.fixit.app.data.provider.AvailabilityInput
import com.fixit.app.data.provider.ProviderRegisterRequest
import com.fixit.app.domain.model.UserRole
import com.fixit.app.ui.signup.SignupDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegistrationState(
    val submitting: Boolean = false,
    val error: String? = null,
)

sealed interface RegistrationEffect {
    object Registered : RegistrationEffect
}

@HiltViewModel
class ProviderRegistrationViewModel @Inject constructor(
    private val providerRepo: ProviderRepository,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    private val _state = MutableStateFlow(RegistrationState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<RegistrationEffect>()
    val effects = _effects.asSharedFlow()

    fun dismissError() { _state.value = _state.value.copy(error = null) }

    fun submit(draft: SignupDraft) {
        if (_state.value.submitting) return
        val lat = draft.latitude
        val lng = draft.longitude
        if (lat == null || lng == null) {
            _state.value = _state.value.copy(
                error = "Set your service area before continuing",
            )
            return
        }
        if (draft.categoryIds.isEmpty()) {
            _state.value = _state.value.copy(error = "Pick at least one service")
            return
        }
        if (draft.availability.isEmpty()) {
            _state.value = _state.value.copy(error = "Pick at least one working day")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(submitting = true, error = null)
            runCatching {
                providerRepo.register(
                    ProviderRegisterRequest(
                        bio = null,
                        yearsExperience = draft.yearsExperience,
                        certification = draft.certification,
                        certificationUrl = null,
                        location = (draft.name.ifBlank { "Provider" }) + " — Service area",
                        latitude = lat,
                        longitude = lng,
                        serviceRadiusKm = draft.serviceRadiusKm,
                        categoryIds = draft.categoryIds,
                        availability = draft.availability.map {
                            AvailabilityInput(it.dayOfWeek, it.openTime, it.closeTime)
                        },
                    )
                )
            }
                .onSuccess {
                    tokenStorage.updateRole(UserRole.PROVIDER)
                    _effects.emit(RegistrationEffect.Registered)
                }
                .onFailure {
                    _state.value = _state.value.copy(error = it.message ?: "Registration failed")
                }
            _state.value = _state.value.copy(submitting = false)
        }
    }
}