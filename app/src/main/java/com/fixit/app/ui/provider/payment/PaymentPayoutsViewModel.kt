package com.fixit.app.ui.provider.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.payment.PaymentRepository
import com.fixit.app.data.wallet.WalletRepository
import com.fixit.app.domain.model.SavedPaymentMethod
import com.fixit.app.domain.model.WalletTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

// ── State ────────────────────────────────────────────────────────────────────

data class PaymentPayoutsState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val balance: BigDecimal = BigDecimal.ZERO,
    val weeklyBars: List<BigDecimal> = List(7) { BigDecimal.ZERO },
    val thisWeekTotal: BigDecimal = BigDecimal.ZERO,
    val weekChangePercent: Int? = null,
    val methods: List<SavedPaymentMethod> = emptyList(),
    val recentTransactions: List<WalletTransaction> = emptyList(),
    // Withdraw sheet
    val showWithdrawSheet: Boolean = false,
    val selectedMethodIdForWithdraw: String? = null,
    val saveAsDefault: Boolean = false,
    val withdrawAmountInput: String = "",
    val withdrawAmountError: String? = null,
    val isWithdrawing: Boolean = false,
    // Add-method sheet
    val showAddMethodSheet: Boolean = false,
    val addMethodType: String = "bank",
    val addMethodDisplayName: String = "",
    val addMethodLastFour: String = "",
    val addMethodIsDefault: Boolean = false,
    val isAddingMethod: Boolean = false,
) {
    val defaultMethod: SavedPaymentMethod? get() = methods.firstOrNull { it.isDefault }

    /** Parsed, validated withdrawal amount. Null when input is invalid. */
    val withdrawAmount: BigDecimal?
        get() = withdrawAmountInput.toBigDecimalOrNull()
            ?.takeIf { it.signum() > 0 && it <= balance }
}

// ── One-shot effects ─────────────────────────────────────────────────────────

sealed class PaymentPayoutsEffect {
    object NavigateToWithdrawConfirm : PaymentPayoutsEffect()
    object WithdrawalComplete : PaymentPayoutsEffect()
    data class ShowError(val message: String) : PaymentPayoutsEffect()
}

// ── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class PaymentPayoutsViewModel @Inject constructor(
    private val walletRepo: WalletRepository,
    private val paymentRepo: PaymentRepository,
) : ViewModel() {

    private val _state   = MutableStateFlow(PaymentPayoutsState())
    val state = _state.asStateFlow()

    private val _effects = Channel<PaymentPayoutsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init { refresh() }

    // ── Load ─────────────────────────────────────────────────────────────────

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                coroutineScope {
                    val walletD  = async { walletRepo.me() }
                    val txD      = async { walletRepo.transactions() }
                    val methodsD = async { paymentRepo.listMethods() }
                    Triple(walletD.await(), txD.await(), methodsD.await())
                }
            }.onSuccess { (wallet, transactions, methods) ->
                val tz            = TimeZone.currentSystemDefault()
                val now           = Clock.System.now()
                val weekStart     = startOfWeek(now, tz)
                val lastWeekStart = offsetDays(weekStart, -7, tz)

                val incomeThis = transactions.filter {
                    it.createdAt >= weekStart && it.amount.signum() > 0
                }
                val incomeLast = transactions.filter {
                    it.createdAt >= lastWeekStart && it.createdAt < weekStart && it.amount.signum() > 0
                }
                val byDow = incomeThis.groupBy {
                    it.createdAt.toLocalDateTime(tz).date.dayOfWeek.ordinal
                }
                val bars = (0..6).map { day ->
                    byDow[day]?.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
                        ?: BigDecimal.ZERO
                }
                val weekTotal     = bars.fold(BigDecimal.ZERO, BigDecimal::add)
                val lastWeekTotal = incomeLast.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
                val changePercent: Int? = if (lastWeekTotal.signum() > 0) {
                    weekTotal.subtract(lastWeekTotal)
                        .divide(lastWeekTotal, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal(100))
                        .toInt()
                } else null

                _state.value = _state.value.copy(
                    isLoading          = false,
                    balance            = BigDecimal.valueOf(wallet.balance),
                    weeklyBars         = bars,
                    thisWeekTotal      = weekTotal,
                    weekChangePercent  = changePercent,
                    methods            = methods,
                    recentTransactions = transactions.take(10),
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading    = false,
                    errorMessage = e.message ?: "Couldn't load payment data",
                )
            }
        }
    }

    // ── Withdraw flow ─────────────────────────────────────────────────────────

    fun onWithdrawClicked() {
        val default = _state.value.defaultMethod
        if (default != null) {
            viewModelScope.launch { _effects.send(PaymentPayoutsEffect.NavigateToWithdrawConfirm) }
        } else {
            val firstId = _state.value.methods.firstOrNull()?.id
            _state.value = _state.value.copy(
                showWithdrawSheet           = true,
                selectedMethodIdForWithdraw = firstId,
                saveAsDefault               = false,
                // Pre-fill with full available balance
                withdrawAmountInput         = _state.value.balance.toPlainString(),
                withdrawAmountError         = null,
            )
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

    fun selectMethodForWithdraw(methodId: String) {
        _state.value = _state.value.copy(selectedMethodIdForWithdraw = methodId)
    }

    fun setSaveAsDefault(value: Boolean) {
        _state.value = _state.value.copy(saveAsDefault = value)
    }

    fun dismissWithdrawSheet() {
        _state.value = _state.value.copy(showWithdrawSheet = false)
    }

    fun submitWithdrawal() {
        val methodId = _state.value.selectedMethodIdForWithdraw ?: return
        val amount   = _state.value.withdrawAmount ?: run {
            _state.value = _state.value.copy(
                withdrawAmountError = validateAmount(
                    _state.value.withdrawAmountInput, _state.value.balance
                )
            )
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isWithdrawing = true)
            runCatching {
                if (_state.value.saveAsDefault) paymentRepo.setDefault(methodId)
                paymentRepo.withdraw(amount, methodId)
            }.onSuccess {
                _state.value = _state.value.copy(
                    isWithdrawing     = false,
                    showWithdrawSheet = false,
                    balance           = (_state.value.balance - amount).coerceAtLeast(BigDecimal.ZERO),
                )
                _effects.send(PaymentPayoutsEffect.WithdrawalComplete)
            }.onFailure { e ->
                _state.value = _state.value.copy(isWithdrawing = false)
                _effects.send(PaymentPayoutsEffect.ShowError(e.message ?: "Withdrawal failed"))
            }
        }
    }

    // ── Payment method management ─────────────────────────────────────────────

    fun setDefault(methodId: String) {
        viewModelScope.launch {
            runCatching { paymentRepo.setDefault(methodId) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        methods = _state.value.methods.map { m ->
                            m.copy(isDefault = m.id == methodId)
                        }
                    )
                }.onFailure { e ->
                    _effects.send(PaymentPayoutsEffect.ShowError(e.message ?: "Failed to update default"))
                }
        }
    }

    fun deleteMethod(methodId: String) {
        viewModelScope.launch {
            runCatching { paymentRepo.deleteMethod(methodId) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        methods = _state.value.methods.filter { it.id != methodId }
                    )
                }.onFailure { e ->
                    _effects.send(PaymentPayoutsEffect.ShowError(e.message ?: "Failed to remove method"))
                }
        }
    }

    // ── Add-method sheet ──────────────────────────────────────────────────────

    fun showAddMethodSheet() {
        _state.value = _state.value.copy(
            showAddMethodSheet   = true,
            addMethodType        = "bank",
            addMethodDisplayName = "",
            addMethodLastFour    = "",
            addMethodIsDefault   = _state.value.methods.isEmpty(),
        )
    }

    fun dismissAddMethodSheet() {
        _state.value = _state.value.copy(showAddMethodSheet = false)
    }

    fun updateAddMethodType(type: String) {
        _state.value = _state.value.copy(addMethodType = type)
    }

    fun updateAddMethodDisplayName(name: String) {
        _state.value = _state.value.copy(addMethodDisplayName = name)
    }

    fun updateAddMethodLastFour(digits: String) {
        if (digits.length <= 4 && digits.all { it.isDigit() }) {
            _state.value = _state.value.copy(addMethodLastFour = digits)
        }
    }

    fun updateAddMethodIsDefault(value: Boolean) {
        _state.value = _state.value.copy(addMethodIsDefault = value)
    }

    fun submitAddMethod() {
        val s = _state.value
        if (s.addMethodDisplayName.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isAddingMethod = true)
            runCatching {
                paymentRepo.addMethod(
                    type        = s.addMethodType,
                    displayName = s.addMethodDisplayName.trim(),
                    lastFour    = s.addMethodLastFour.takeIf { it.length == 4 },
                    isDefault   = s.addMethodIsDefault,
                )
            }.onSuccess { newMethod ->
                val updatedMethods = if (s.addMethodIsDefault) {
                    _state.value.methods.map { it.copy(isDefault = false) } + newMethod
                } else {
                    _state.value.methods + newMethod
                }
                _state.value = _state.value.copy(
                    methods            = updatedMethods,
                    showAddMethodSheet = false,
                    isAddingMethod     = false,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(isAddingMethod = false)
                _effects.send(PaymentPayoutsEffect.ShowError(e.message ?: "Failed to add method"))
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun startOfWeek(now: Instant, tz: TimeZone): Instant {
        val today        = now.toLocalDateTime(tz).date
        val daysSinceMon = today.dayOfWeek.ordinal.toLong()
        return today.minus(daysSinceMon, DateTimeUnit.DAY).atStartOfDayIn(tz)
    }

    private fun offsetDays(instant: Instant, days: Int, tz: TimeZone): Instant {
        val date    = instant.toLocalDateTime(tz).date
        val shifted = LocalDate.fromEpochDays(date.toEpochDays() + days)
        return shifted.atStartOfDayIn(tz)
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