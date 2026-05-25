package com.fixit.app.ui.provider.earnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.wallet.WalletRepository
import com.fixit.app.domain.model.WalletTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

data class ProviderEarningsState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val balance: BigDecimal = BigDecimal.ZERO,
    /** Mon..Sun amounts for the current week. Always 7 entries. */
    val weeklyBars: List<BigDecimal> = List(7) { BigDecimal.ZERO },
    val thisWeekTotal: BigDecimal = BigDecimal.ZERO,
    val transactions: List<WalletTransaction> = emptyList(),
)

@HiltViewModel
class ProviderEarningsViewModel @Inject constructor(
    private val walletRepo: WalletRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProviderEarningsState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                coroutineScope {
                    val walletDeferred = async { walletRepo.me() }
                    val txDeferred     = async { walletRepo.transactions() }
                    walletDeferred.await() to txDeferred.await()
                }
            }
                .onSuccess { (wallet, transactions) ->
                    val tz       = TimeZone.currentSystemDefault()
                    val now      = Clock.System.now()
                    val weekStart = startOfWeek(now, tz)

                    // Only positive amounts contribute to "earnings" bars —
                    // negative entries are withdrawals/holds which shouldn't
                    // appear on a provider's income chart.
                    val incomeThisWeek = transactions.filter {
                        it.createdAt >= weekStart && it.amount.signum() > 0
                    }
                    val byDow = incomeThisWeek.groupBy {
                        it.createdAt.toLocalDateTime(tz).date.dayOfWeek.ordinal
                    }
                    val bars = (0..6).map { day ->
                        byDow[day]
                            ?.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
                            ?: BigDecimal.ZERO
                    }
                    val weekTotal = bars.fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }

                    _state.value = _state.value.copy(
                        isLoading = false,
                        balance = BigDecimal.valueOf(wallet.balance),
                        weeklyBars = bars,
                        thisWeekTotal = weekTotal,
                        transactions = transactions,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't load earnings",
                    )
                }
        }
    }

    private fun startOfWeek(now: Instant, tz: TimeZone): Instant {
        val today: LocalDate = now.toLocalDateTime(tz).date
        val daysSinceMonday  = today.dayOfWeek.ordinal.toLong() // MON=0 … SUN=6
        return today.minus(daysSinceMonday, DateTimeUnit.DAY).atStartOfDayIn(tz)
    }
}