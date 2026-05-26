package com.fixit.app.ui.provider.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.payment.PaymentRepository
import com.fixit.app.data.wallet.WalletRepository
import com.fixit.app.domain.model.SavedPaymentMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class WithdrawConfirmState(
    val isLoading: Boolean = false,
    val balance: BigDecimal = BigDecimal.ZERO,
    val defaultMethod: SavedPaymentMethod? = null,
    val withdrawAmountInput: String = "",
    val withdrawAmountError: String? = null,
    val errorMessage: String? = null,
    val isWithdrawing: Boolean = false,
    /**
     * Set once after a successful refresh so subsequent ON_STARTs don't reset
     * the user's typed-in amount. The form value belongs to the user, not the
     * server — we only want refresh-on-resume for the wallet/methods data,
     * which has already populated on first load anyway.
     */
    val hasLoaded: Boolean = false,
) {
    val withdrawAmount: BigDecimal?
        get() = withdrawAmountInput.toBigDecimalOrNull()
            ?.takeIf { it.signum() > 0 && it <= balance }
}

sealed class WithdrawConfirmEffect {
    object WithdrawalComplete : WithdrawConfirmEffect()
    data class ShowError(val message: String) : WithdrawConfirmEffect()
}

@HiltViewModel
class WithdrawConfirmViewModel @Inject constructor(
    private val walletRepo: WalletRepository,
    private val paymentRepo: PaymentRepository,
) : ViewModel() {

    private val _state   = MutableStateFlow(WithdrawConfirmState())
    val state = _state.asStateFlow()

    private val _effects = Channel<WithdrawConfirmEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // First load driven by the screen's OnLifecycleStart. The `hasLoaded` guard
    // prevents subsequent ON_STARTs from clobbering the user-typed amount.

    fun refresh() {
        if (_state.value.hasLoaded) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            runCatching {
                coroutineScope {
                    val walletD  = async { walletRepo.me() }
                    val methodsD = async { paymentRepo.listMethods() }
                    walletD.await() to methodsD.await()
                }
            }.onSuccess { (wallet, methods) ->
                val balance = BigDecimal.valueOf(wallet.balance)
                _state.value = _state.value.copy(
                    isLoading           = false,
                    balance             = balance,
                    withdrawAmountInput = balance.toPlainString(),
                    withdrawAmountError = null,
                    defaultMethod       = methods.firstOrNull { it.isDefault },
                    hasLoaded           = true,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading    = false,
                    errorMessage = e.message ?: "Couldn't load withdrawal data",
                )
            }
        }
    }

    fun updateWithdrawAmount(input: String) {
        _state.value = _state.value.copy(
            withdrawAmountInput = input,
            withdrawAmountError = validateAmount(input, _state.value.balance),
        )
    }

    fun setMaxWithdrawAmount() {
        _state.value = _state.value.copy(
            withdrawAmountInput = _state.value.balance.toPlainString(),
            withdrawAmountError = null,
        )
    }

    fun confirmWithdrawal() {
        val method = _state.value.defaultMethod ?: return
        val amount = _state.value.withdrawAmount ?: run {
            _state.value = _state.value.copy(
                withdrawAmountError = validateAmount(
                    _state.value.withdrawAmountInput, _state.value.balance
                )
            )
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isWithdrawing = true)
            runCatching { paymentRepo.withdraw(amount, method.id) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        isWithdrawing = false,
                        balance       = (_state.value.balance - amount).coerceAtLeast(BigDecimal.ZERO),
                    )
                    _effects.send(WithdrawConfirmEffect.WithdrawalComplete)
                }.onFailure { e ->
                    _state.value = _state.value.copy(isWithdrawing = false)
                    _effects.send(WithdrawConfirmEffect.ShowError(e.message ?: "Withdrawal failed"))
                }
        }
    }

    private fun validateAmount(input: String, balance: BigDecimal): String? {
        if (input.isBlank()) return "Please enter an amount"
        val amount = input.toBigDecimalOrNull() ?: return "Enter a valid number"
        if (amount.signum() <= 0) return "Amount must be greater than zero"
        if (amount > balance) return "Exceeds available balance of ${balance.toPlainString()}"
        return null
    }
}

private fun BigDecimal.coerceAtLeast(min: BigDecimal): BigDecimal =
    if (this < min) min else this