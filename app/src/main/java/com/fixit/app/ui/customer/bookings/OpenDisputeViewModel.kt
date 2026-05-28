package com.fixit.app.ui.customer.bookings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.booking.BookingRepository
import com.fixit.app.data.dispute.DisputeRepository
import com.fixit.app.domain.model.Booking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OpenDisputeState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val booking: Booking? = null,
    val reason: String = "",
) {
    /** Backend requires min_length=10, max_length=2000 on the reason. */
    val canSubmit: Boolean
        get() = !isSubmitting && reason.trim().length in 10..2000
}

sealed interface OpenDisputeEffect {
    data class Message(val text: String) : OpenDisputeEffect
    /** Dispute filed — pop back. */
    data object Submitted : OpenDisputeEffect
}

@HiltViewModel
class OpenDisputeViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val bookingRepo: BookingRepository,
    private val disputeRepo: DisputeRepository,
) : ViewModel() {

    private val bookingId: String =
        checkNotNull(savedState["bookingId"]) { "bookingId nav arg missing" }

    private val _state = MutableStateFlow(OpenDisputeState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<OpenDisputeEffect>(extraBufferCapacity = 4)
    val effects = _effects.asSharedFlow()

    init { loadBooking() }

    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun onReasonChange(value: String) {
        _state.value = _state.value.copy(reason = value)
    }

    private fun loadBooking() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { bookingRepo.byId(bookingId) }
                .onSuccess { b ->
                    _state.value = _state.value.copy(isLoading = false, booking = b)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't load booking",
                    )
                }
        }
    }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        viewModelScope.launch {
            _state.value = s.copy(isSubmitting = true, errorMessage = null)
            runCatching { disputeRepo.raiseDispute(bookingId, s.reason.trim()) }
                .onSuccess {
                    _state.value = _state.value.copy(isSubmitting = false)
                    _effects.tryEmit(OpenDisputeEffect.Message("Dispute filed"))
                    _effects.tryEmit(OpenDisputeEffect.Submitted)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Couldn't file dispute",
                    )
                }
        }
    }
}