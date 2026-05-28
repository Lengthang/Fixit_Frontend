package com.fixit.app.ui.customer.bookings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.booking.BookingRepository
import com.fixit.app.data.review.ReviewRepository
import com.fixit.app.domain.model.Booking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaveReviewState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val booking: Booking? = null,
    val rating: Int = 0,            // 0 means "not picked yet"
    val comment: String = "",
) {
    /** Rating is required (1-5). Comment is optional but capped at 2000 by the backend. */
    val canSubmit: Boolean
        get() = !isSubmitting && rating in 1..5 && comment.length <= 2000
}

sealed interface LeaveReviewEffect {
    data class Message(val text: String) : LeaveReviewEffect
    /** Review submitted — pop back. */
    data object Submitted : LeaveReviewEffect
}

@HiltViewModel
class LeaveReviewViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val bookingRepo: BookingRepository,
    private val reviewRepo: ReviewRepository,
) : ViewModel() {

    private val book: String =
        checkNotNull(savedState["bookingId"]) { "bookingId nav arg missing" }
    private val _state = MutableStateFlow(LeaveReviewState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<LeaveReviewEffect>(extraBufferCapacity = 4)
    val effects = _effects.asSharedFlow()

    init {
        loadBooking()
    }

    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun onRatingChange(value: Int) {
        _state.value = _state.value.copy(rating = value.coerceIn(0, 5))
    }

    fun onCommentChange(value: String) {
        _state.value = _state.value.copy(comment = value)
    }

    private fun loadBooking() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { bookingRepo.byId(book) }
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
            runCatching {
                reviewRepo.createReview(
                    bookingId = book,
                    rating = s.rating,
                    comment = s.comment.takeIf { it.isNotBlank() },
                )
            }
                .onSuccess {
                    _state.value = _state.value.copy(isSubmitting = false)
                    _effects.tryEmit(LeaveReviewEffect.Message("Thanks for your review!"))
                    _effects.tryEmit(LeaveReviewEffect.Submitted)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Couldn't submit review",
                    )
                }
        }
    }
}