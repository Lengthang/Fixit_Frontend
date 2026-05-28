package com.fixit.app.ui.customer.bookings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.booking.BookingRepository
import com.fixit.app.data.review.ReviewRepository
import com.fixit.app.domain.model.Booking
import com.fixit.app.domain.model.Review
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    /**
     * Non-null when the customer has already submitted a review for this
     * booking. The screen renders read-only in that case, with rating and
     * comment pre-filled from the server.
     */
    val existingReview: Review? = null,
) {
    /** True when displaying an already-submitted review — no edits, no resubmit. */
    val isReadOnly: Boolean get() = existingReview != null

    /** Rating is required (1-5). Comment is optional but capped at 2000 by the backend. */
    val canSubmit: Boolean
        get() = !isSubmitting && !isReadOnly && rating in 1..5 && comment.length <= 2000
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
        // Reject edits silently in read-only mode — the click handlers in the
        // screen are already gated, this is belt-and-suspenders.
        if (_state.value.isReadOnly) return
        _state.value = _state.value.copy(rating = value.coerceIn(0, 5))
    }

    fun onCommentChange(value: String) {
        if (_state.value.isReadOnly) return
        _state.value = _state.value.copy(comment = value)
    }

    /**
     * Loads the booking and (in parallel) tries to fetch any existing review
     * for it. If a review comes back, the screen flips to read-only — same
     * route, same VM, just renders the previously-submitted rating/comment
     * with the submit button hidden. This is how the "View review" button on
     * the bookings History tab works without needing a separate screen.
     */
    private fun loadBooking() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                coroutineScope {
                    val b = async { bookingRepo.byId(book) }
                    val r = async { reviewRepo.forBookingOrNull(book) }
                    b.await() to r.await()
                }
            }
                .onSuccess { (b, r) ->
                    _state.value = _state.value.copy(
                        isLoading      = false,
                        booking        = b,
                        existingReview = r,
                        rating         = r?.rating ?: _state.value.rating,
                        comment        = r?.comment ?: _state.value.comment,
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