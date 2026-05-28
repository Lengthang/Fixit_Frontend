package com.fixit.app.ui.customer.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.booking.BookingRepository
import com.fixit.app.data.payment.PaymentRepository
import com.fixit.app.domain.model.Booking
import com.fixit.app.domain.model.BookingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BookingsTab { UPCOMING, HISTORY }

data class CustomerBookingsState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val tab: BookingsTab = BookingsTab.UPCOMING,
    val bookings: List<Booking> = emptyList(),
    /** Booking id currently being mutated (cancel/confirm) — drives row spinners. */
    val mutatingId: String? = null,
) {
    /** Bookings filtered for the active tab and sorted in a sensible order. */
    val visible: List<Booking>
        get() = when (tab) {
            BookingsTab.UPCOMING -> bookings
                .filter {
                    it.status == BookingStatus.PENDING ||
                            it.status == BookingStatus.IN_PROGRESS ||
                            it.status == BookingStatus.AWAITING_CONFIRMATION ||
                            it.status == BookingStatus.DISPUTED
                }
                // Soonest first — matches the screenshot where "Today" rows
                // appear above a future date.
                .sortedBy { it.scheduledAt }
            BookingsTab.HISTORY -> bookings
                .filter { it.status == BookingStatus.COMPLETED }
                .sortedByDescending { it.scheduledAt }
        }
}

/** One-shot effects the screen consumes (snackbar text, navigation prompts). */
sealed interface CustomerBookingsEffect {
    data class Message(val text: String) : CustomerBookingsEffect
}

@HiltViewModel
class CustomerBookingsViewModel @Inject constructor(
    private val bookingRepo: BookingRepository,
    private val paymentRepo: PaymentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerBookingsState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<CustomerBookingsEffect>(extraBufferCapacity = 4)
    val effects = _effects.asSharedFlow()

    fun selectTab(tab: BookingsTab) {
        if (_state.value.tab != tab) {
            _state.value = _state.value.copy(tab = tab)
        }
    }

    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { bookingRepo.myBookingsAsDomain() }
                .onSuccess { list ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        bookings  = list,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't load bookings",
                    )
                }
        }
    }

    /**
     * Customer cancellation. The server enforces the 24h buffer on
     * in_progress bookings — we still pre-check on the client so the button
     * is hidden when there's no chance of success, but if the user manages
     * to invoke this after the window has just closed the server's 403
     * will surface through the error message.
     */
    fun cancelBooking(bookingId: String) {
        val current = _state.value
        if (current.mutatingId != null) return
        viewModelScope.launch {
            _state.value = current.copy(mutatingId = bookingId, errorMessage = null)
            runCatching { bookingRepo.updateStatus(bookingId, "cancelled") }
                .onSuccess { updated ->
                    _state.value = _state.value.copy(
                        mutatingId = null,
                        bookings   = _state.value.bookings.map {
                            if (it.id == updated.id) updated else it
                        },
                    )
                    _effects.tryEmit(CustomerBookingsEffect.Message("Booking cancelled"))
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        mutatingId = null,
                        errorMessage = e.message ?: "Couldn't cancel booking",
                    )
                }
        }
    }

    /**
     * Customer half of dual confirmation. If both sides have already
     * confirmed (or the provider confirms within seconds) the backend
     * flips the booking to "completed" and releases escrow — we just
     * refresh our list to reflect whatever the new status is.
     */
    fun confirmCompletion(bookingId: String) {
        val current = _state.value
        if (current.mutatingId != null) return
        viewModelScope.launch {
            _state.value = current.copy(mutatingId = bookingId, errorMessage = null)
            runCatching {
                paymentRepo.confirmCompletion(bookingId)
                // Re-fetch the single booking so any status flip
                // (awaiting_confirmation → completed) is reflected immediately.
                bookingRepo.byId(bookingId)
            }
                .onSuccess { updated ->
                    _state.value = _state.value.copy(
                        mutatingId = null,
                        bookings   = _state.value.bookings.map {
                            if (it.id == updated.id) updated else it
                        },
                    )
                    val message = when (updated.status) {
                        BookingStatus.COMPLETED -> "Confirmed — booking completed"
                        else                    -> "Confirmed — waiting on the provider"
                    }
                    _effects.tryEmit(CustomerBookingsEffect.Message(message))
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        mutatingId = null,
                        errorMessage = e.message ?: "Couldn't confirm booking",
                    )
                }
        }
    }
}