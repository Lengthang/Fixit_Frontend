package com.fixit.app.ui.customer.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.payment.PaymentRepository
import com.fixit.app.data.wallet.WalletRepository
import com.fixit.app.domain.model.SavedPaymentMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class WalletTopUpState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val balance: BigDecimal = BigDecimal.ZERO,
    val methods: List<SavedPaymentMethod> = emptyList(),
    val selectedMethodId: String? = null,
    val amountInput: String = "",
) {
    val amount: BigDecimal?
        get() = amountInput.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }

    val hasMethods: Boolean get() = methods.isNotEmpty()

    val canSubmit: Boolean
        get() = !isSubmitting && !isLoading && hasMethods &&
                selectedMethodId != null && amount != null
}

sealed interface WalletTopUpEffect {
    data class Success(val newBalance: BigDecimal) : WalletTopUpEffect
    data class ShowError(val message: String) : WalletTopUpEffect
}

@HiltViewModel
class WalletTopUpViewModel @Inject constructor(
    private val walletRepo: WalletRepository,
    private val paymentRepo: PaymentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WalletTopUpState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<WalletTopUpEffect>()
    val effects = _effects.asSharedFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            runCatching {
                coroutineScope {
                    val walletDeferred = async { runCatching { walletRepo.me() }.getOrNull() }
                    val methodsDeferred = async { runCatching { paymentRepo.listMethods() }.getOrDefault(emptyList()) }
                    walletDeferred.await() to methodsDeferred.await()
                }
            }
                .onSuccess { (wallet, methods) ->
                    val default = methods.firstOrNull { it.isDefault } ?: methods.firstOrNull()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        balance = wallet?.balance?.let { BigDecimal.valueOf(it) } ?: BigDecimal.ZERO,
                        methods = methods,
                        selectedMethodId = _state.value.selectedMethodId ?: default?.id,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isLoading = false)
                    _effects.emit(WalletTopUpEffect.ShowError(e.message ?: "Couldn't load wallet"))
                }
        }
    }

    fun onAmountChange(value: String) {
        // Allow digits and a single decimal point.
        val cleaned = value.filter { it.isDigit() || it == '.' }
            .let { s -> if (s.count { it == '.' } > 1) s.dropLast(1) else s }
        _state.value = _state.value.copy(amountInput = cleaned)
    }

    fun onQuickAmount(value: Int) {
        _state.value = _state.value.copy(amountInput = value.toString())
    }

    fun selectMethod(methodId: String) {
        _state.value = _state.value.copy(selectedMethodId = methodId)
    }

    fun submit() {
        val s = _state.value
        val amount = s.amount ?: return
        val methodId = s.selectedMethodId ?: return
        if (!s.canSubmit) return

        viewModelScope.launch {
            _state.value = s.copy(isSubmitting = true)
            runCatching { walletRepo.topUp(amount, methodId) }
                .onSuccess { newBalance ->
                    _state.value = _state.value.copy(isSubmitting = false, balance = newBalance)
                    _effects.emit(WalletTopUpEffect.Success(newBalance))
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isSubmitting = false)
                    _effects.emit(WalletTopUpEffect.ShowError(e.message ?: "Top up failed"))
                }
        }
    }
}