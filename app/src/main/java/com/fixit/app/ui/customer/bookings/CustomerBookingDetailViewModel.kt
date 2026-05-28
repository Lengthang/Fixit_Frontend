package com.fixit.app.ui.customer.bookings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.booking.BookingRepository
import com.fixit.app.data.payment.PaymentRepository
import com.fixit.app.data.review.ReviewRepository
import com.fixit.app.domain.model.Booking
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

data class CustomerBookingDetailState(
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val errorMessage: String? = null,
    val booking: Booking? = null,
    /** True when a review already exists for this booking — hides Leave review. */
    val hasReview: Boolean = false,
)

sealed interface CustomerBookingDetailEffect {
    data class Message(val text: String) : CustomerBookingDetailEffect
    /** Booking is gone (cancelled successfully) — pop the screen. */
    data object Dismiss : CustomerBookingDetailEffect
}

@HiltViewModel
class CustomerBookingDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val bookingRepo: BookingRepository,
    private val paymentRepo: PaymentRepository,
    private val reviewRepo: ReviewRepository,
) : ViewModel() {

    private val bookingId: String =
        checkNotNull(savedState["bookingId"]) { "bookingId nav arg missing" }

    private val _state = MutableStateFlow(CustomerBookingDetailState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<CustomerBookingDetailEffect>(extraBufferCapacity = 4)
    val effects = _effects.asSharedFlow()

    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { loadAll() }
                .onSuccess { (b, hasReview) ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        booking   = b,
                        hasReview = hasReview,
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

    private suspend fun loadAll(): Pair<Booking, Boolean> = coroutineScope {
        val bookingDeferred = async { bookingRepo.byId(bookingId) }
        val reviewDeferred  = async { reviewRepo.forBookingOrNull(bookingId) }
        val booking = bookingDeferred.await()
        val review  = reviewDeferred.await()
        booking to (review != null)
    }

    fun cancelBooking() {
        val current = _state.value
        val b = current.booking ?: return
        if (current.isMutating || !b.isCustomerCancellable()) return
        viewModelScope.launch {
            _state.value = current.copy(isMutating = true, errorMessage = null)
            runCatching { bookingRepo.updateStatus(bookingId, "cancelled") }
                .onSuccess { updated ->
                    _state.value = _state.value.copy(isMutating = false, booking = updated)
                    _effects.tryEmit(CustomerBookingDetailEffect.Message("Booking cancelled"))
                    _effects.tryEmit(CustomerBookingDetailEffect.Dismiss)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isMutating = false,
                        errorMessage = e.message ?: "Couldn't cancel booking",
                    )
                }
        }
    }

    fun confirmCompletion() {
        val current = _state.value
        val b = current.booking ?: return
        if (current.isMutating || b.status != BookingStatus.AWAITING_CONFIRMATION) return
        viewModelScope.launch {
            _state.value = current.copy(isMutating = true, errorMessage = null)
            runCatching {
                paymentRepo.confirmCompletion(bookingId)
                bookingRepo.byId(bookingId)
            }
                .onSuccess { updated ->
                    _state.value = _state.value.copy(isMutating = false, booking = updated)
                    val msg = when (updated.status) {
                        BookingStatus.COMPLETED -> "Confirmed — booking completed"
                        else                    -> "Confirmed — waiting on the provider"
                    }
                    _effects.tryEmit(CustomerBookingDetailEffect.Message(msg))
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isMutating = false,
                        errorMessage = e.message ?: "Couldn't confirm booking",
                    )
                }
        }
    }
}