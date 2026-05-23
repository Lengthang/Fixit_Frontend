package com.fixit.app.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.customer.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AboutYouState(
    val name: String = "",
    val email: String = "",
    val dateOfBirth: String = "",
    val referralCode: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface AboutYouEffect { object Submitted : AboutYouEffect }

@HiltViewModel
class AboutYouViewModel @Inject constructor(
    private val customerRepo: CustomerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AboutYouState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<AboutYouEffect>()
    val effects = _effects.asSharedFlow()

    fun onNameChange(v: String) { _state.value = _state.value.copy(name = v) }
    fun onEmailChange(v: String) { _state.value = _state.value.copy(email = v) }
    fun onDobChange(v: String) { _state.value = _state.value.copy(dateOfBirth = v) }
    fun onReferralChange(v: String) { _state.value = _state.value.copy(referralCode = v) }
    fun dismissError() { _state.value = _state.value.copy(errorMessage = null) }

    fun submit() {
        val s = _state.value
        if (s.isSubmitting) return
        if (s.name.isBlank()) {
            _state.value = s.copy(errorMessage = "Please enter your name")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isSubmitting = true, errorMessage = null)
            try {
                // dob & referralCode aren't on the backend schema; we keep them
                // in-memory so the rest of onboarding has them if needed later.
                customerRepo.update(name = s.name, email = s.email.ifBlank { null })
                _effects.emit(AboutYouEffect.Submitted)
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = e.message ?: "Couldn't save")
            } finally {
                _state.value = _state.value.copy(isSubmitting = false)
            }
        }
    }
}