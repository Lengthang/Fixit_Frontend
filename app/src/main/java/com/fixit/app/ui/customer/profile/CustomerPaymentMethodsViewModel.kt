package com.fixit.app.ui.customer.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.payment.PaymentRepository
import com.fixit.app.domain.model.PaymentMethodType
import com.fixit.app.domain.model.SavedPaymentMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerPaymentMethodsState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val methods: List<SavedPaymentMethod> = emptyList(),

    // Add-method sheet
    val sheetOpen: Boolean = false,
    val newType: PaymentMethodType = PaymentMethodType.CARD,
    val newDisplayName: String = "",
    val newLastFour: String = "",
    val newIsDefault: Boolean = false,
) {
    val canSubmit: Boolean
        get() = newDisplayName.isNotBlank() && !isSaving &&
                (newLastFour.isBlank() || newLastFour.length == 4)
}

sealed interface CustomerPaymentMethodsEffect {
    data class ShowError(val message: String) : CustomerPaymentMethodsEffect
    data class ShowMessage(val message: String) : CustomerPaymentMethodsEffect
}

@HiltViewModel
class CustomerPaymentMethodsViewModel @Inject constructor(
    private val paymentRepo: PaymentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerPaymentMethodsState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<CustomerPaymentMethodsEffect>()
    val effects = _effects.asSharedFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            runCatching { paymentRepo.listMethods() }
                .onSuccess { _state.value = _state.value.copy(isLoading = false, methods = it) }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false)
                    _effects.emit(
                        CustomerPaymentMethodsEffect.ShowError(
                            e.message ?: "Couldn't load payment methods"
                        )
                    )
                }
        }
    }

    // ── Add-method sheet controls ───────────────────────────────────────────
    fun openSheet() {
        _state.value = _state.value.copy(
            sheetOpen = true,
            newType = PaymentMethodType.CARD,
            newDisplayName = "",
            newLastFour = "",
            newIsDefault = _state.value.methods.isEmpty(), // first method defaults on
        )
    }

    fun closeSheet() {
        _state.value = _state.value.copy(sheetOpen = false)
    }

    fun setNewType(type: PaymentMethodType) {
        _state.value = _state.value.copy(newType = type)
    }

    fun setNewDisplayName(value: String) {
        _state.value = _state.value.copy(newDisplayName = value)
    }

    fun setNewLastFour(value: String) {
        // Keep digits only, max 4 — matches the backend's optional last_four.
        val digits = value.filter { it.isDigit() }.take(4)
        _state.value = _state.value.copy(newLastFour = digits)
    }

    fun toggleNewIsDefault() {
        _state.value = _state.value.copy(newIsDefault = !_state.value.newIsDefault)
    }

    fun addMethod() {
        val s = _state.value
        if (!s.canSubmit) return
        viewModelScope.launch {
            _state.value = s.copy(isSaving = true)
            runCatching {
                paymentRepo.addMethod(
                    type = if (s.newType == PaymentMethodType.BANK) "bank" else "card",
                    displayName = s.newDisplayName.trim(),
                    lastFour = s.newLastFour.takeIf { it.isNotBlank() },
                    isDefault = s.newIsDefault,
                )
            }
                .onSuccess {
                    _state.value = _state.value.copy(isSaving = false, sheetOpen = false)
                    refresh()
                    _effects.emit(CustomerPaymentMethodsEffect.ShowMessage("Payment method added"))
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isSaving = false)
                    _effects.emit(
                        CustomerPaymentMethodsEffect.ShowError(
                            e.message ?: "Couldn't add payment method"
                        )
                    )
                }
        }
    }

    fun setDefault(methodId: String) {
        viewModelScope.launch {
            // Optimistic: backend demotes others atomically.
            _state.value = _state.value.copy(
                methods = _state.value.methods.map { it.copy(isDefault = it.id == methodId) }
            )
            runCatching { paymentRepo.setDefault(methodId) }
                .onFailure { e ->
                    _effects.emit(
                        CustomerPaymentMethodsEffect.ShowError(
                            e.message ?: "Couldn't update default"
                        )
                    )
                    refresh() // reconcile on failure
                }
        }
    }

    fun deleteMethod(methodId: String) {
        viewModelScope.launch {
            val previous = _state.value.methods
            _state.value = _state.value.copy(methods = previous.filterNot { it.id == methodId })
            runCatching { paymentRepo.deleteMethod(methodId) }
                .onSuccess {
                    _effects.emit(CustomerPaymentMethodsEffect.ShowMessage("Payment method removed"))
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(methods = previous) // rollback
                    _effects.emit(
                        CustomerPaymentMethodsEffect.ShowError(
                            e.message ?: "Couldn't remove method"
                        )
                    )
                }
        }
    }
}