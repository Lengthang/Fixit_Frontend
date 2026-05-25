package com.fixit.app.ui.provider.jobs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.booking.BookingRepository
import com.fixit.app.data.payment.PaymentRepository
import com.fixit.app.domain.model.Booking
import com.fixit.app.domain.model.BookingPayout
import com.fixit.app.domain.model.BookingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobDetailState(
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val errorMessage: String? = null,
    val booking: Booking? = null,
    /**
     * Server-computed earnings breakdown. Null while loading or if the
     * payout endpoint failed (e.g. 403 because the user isn't the assigned
     * provider). The screen falls back to gracefully rendering "—" in that
     * case rather than computing commission on the client.
     */
    val payout: BookingPayout? = null,
)

sealed interface JobDetailEffect {
    /** Show a snackbar; user stays on the detail screen. */
    data class StatusUpdated(val message: String) : JobDetailEffect
    /** Pop back to the previous screen (e.g. after decline/cancel/completion). */
    data object Dismiss : JobDetailEffect
}

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val bookingRepo: BookingRepository,
    private val paymentRepo: PaymentRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val bookingId: String =
        checkNotNull(savedState["bookingId"]) { "bookingId nav arg missing" }

    private val _state = MutableStateFlow(JobDetailState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<JobDetailEffect>()
    val effects = _effects.asSharedFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val outcome = runCatching { loadBookingAndPayout() }
            outcome
                .onSuccess { (booking, payout) ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        booking = booking,
                        payout = payout ?: _state.value.payout,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't load booking",
                    )
                }
        }
    }

    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    // ── actions ──────────────────────────────────────────────────────────

    fun accept() = changeStatus(
        newStatus      = "in_progress",
        successMessage = "Job accepted",
        dismissAfter   = false,
    )

    fun decline() = changeStatus(
        newStatus      = "rejected",
        successMessage = "Job declined",
        dismissAfter   = true,
    )

    /**
     * No backend equivalent today — the action only appears for the CONFIRMED
     * status which the backend never emits, so this is unreachable. Left as a
     * no-op until staged-payment support lands in the booking state machine.
     */
    fun markReadyForPayment() = Unit

    fun markJobDone() = changeStatus(
        newStatus      = "awaiting_confirmation",
        successMessage = "Marked as done — waiting for customer to confirm",
        dismissAfter   = false,
    )

    fun confirmCompletion() {
        if (_state.value.isMutating) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isMutating = true, errorMessage = null)
            val outcome = runCatching {
                paymentRepo.confirmCompletion(bookingId)
                // Refresh both — confirmation may flip escrow holding→released,
                // which means the payout numbers change from estimate to final.
                loadBookingAndPayout()
            }
            outcome
                .onSuccess { (refreshed, payout) ->
                    _state.value = _state.value.copy(
                        isMutating = false,
                        booking = refreshed,
                        payout = payout ?: _state.value.payout,
                    )
                    val both = refreshed.status == BookingStatus.COMPLETED
                    _effects.emit(
                        JobDetailEffect.StatusUpdated(
                            if (both) "Job completed — funds released"
                            else "Confirmed — waiting on the customer"
                        )
                    )
                    if (both) _effects.emit(JobDetailEffect.Dismiss)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isMutating = false,
                        errorMessage = e.message ?: "Couldn't confirm completion",
                    )
                }
        }
    }

    fun cancel() = changeStatus(
        newStatus      = "cancelled",
        successMessage = "Booking cancelled",
        dismissAfter   = true,
    )

    // ── shared status-mutation pipeline ──────────────────────────────────

    private fun changeStatus(
        newStatus: String,
        successMessage: String,
        dismissAfter: Boolean,
    ) {
        if (_state.value.isMutating) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isMutating = true, errorMessage = null)
            val outcome = runCatching {
                val updated = bookingRepo.updateStatus(bookingId, newStatus)
                // Status transitions can flip escrow state too:
                //  - cancel / decline → "refunded"
                //  - confirm-completion path (handled in confirmCompletion) → "released"
                // Refresh payout so the "Estimated/Paid out/Refunded" caption stays
                // accurate. Tolerate failure: a stale payout is better than an error.
                val payout = runCatching { bookingRepo.payoutFor(bookingId) }.getOrNull()
                updated to payout
            }
            outcome
                .onSuccess { (updated, payout) ->
                    _state.value = _state.value.copy(
                        isMutating = false,
                        booking = updated,
                        payout = payout ?: _state.value.payout,
                    )
                    _effects.emit(JobDetailEffect.StatusUpdated(successMessage))
                    if (dismissAfter) _effects.emit(JobDetailEffect.Dismiss)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isMutating = false,
                        errorMessage = e.message ?: "Couldn't update booking",
                    )
                }
        }
    }

    /**
     * Fetches booking + payout in parallel. Payout failure is swallowed so a
     * 403 (rare) or 500 doesn't blank out the rest of the screen.
     */
    private suspend fun loadBookingAndPayout(): Pair<Booking, BookingPayout?> =
        coroutineScope {
            val bookingDeferred = async { bookingRepo.byId(bookingId) }
            val payoutDeferred  = async {
                runCatching { bookingRepo.payoutFor(bookingId) }.getOrNull()
            }
            bookingDeferred.await() to payoutDeferred.await()
        }
}