package com.fixit.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhoneEntryState(
    val countryCode: String = "+855",
    val phone: String = "",
    val sending: Boolean = false,
    val error: String? = null,
)

sealed interface PhoneEntryEffect { object OtpSent : PhoneEntryEffect }

@HiltViewModel
class PhoneEntryViewModel @Inject constructor(
    private val authRepo: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PhoneEntryState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<PhoneEntryEffect>()
    val effects = _effects.asSharedFlow()

    fun onPhoneChange(v: String) { _state.value = _state.value.copy(phone = v.filter { it.isDigit() }) }
    fun fullPhone(): String = _state.value.countryCode + _state.value.phone

    fun send() {
        if (_state.value.sending || _state.value.phone.length < 6) return
        viewModelScope.launch {
            _state.value = _state.value.copy(sending = true, error = null)
            try {
                authRepo.sendOtp(fullPhone())
                _effects.emit(PhoneEntryEffect.OtpSent)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Couldn't send code")
            } finally {
                _state.value = _state.value.copy(sending = false)
            }
        }
    }

    fun dismissError() { _state.value = _state.value.copy(error = null) }
}