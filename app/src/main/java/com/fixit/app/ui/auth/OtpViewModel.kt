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

data class OtpState(
    val code: String = "",
    val verifying: Boolean = false,
    val error: String? = null,
)

sealed interface OtpEffect {
    data class Verified(val isNewUser: Boolean, val isProvider: Boolean) : OtpEffect
}

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val authRepo: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OtpState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<OtpEffect>()
    val effects = _effects.asSharedFlow()

    fun onCodeChange(v: String) {
        _state.value = _state.value.copy(code = v.filter { it.isDigit() }.take(6))
    }
    fun dismissError() { _state.value = _state.value.copy(error = null) }

    fun verify(phone: String, name: String?) {
        if (_state.value.verifying || _state.value.code.length < 6) return
        viewModelScope.launch {
            _state.value = _state.value.copy(verifying = true, error = null)
            try {
                val res = authRepo.verifyOtp(phone, _state.value.code, name)
                _effects.emit(OtpEffect.Verified(
                    isNewUser = res.isNewUser,
                    isProvider = res.providerProfile != null,
                ))
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Verification failed")
            } finally {
                _state.value = _state.value.copy(verifying = false)
            }
        }
    }
}