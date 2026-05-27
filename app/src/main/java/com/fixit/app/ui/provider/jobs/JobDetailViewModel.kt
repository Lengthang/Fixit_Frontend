package com.fixit.app.ui.provider.jobs

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.booking.BookingRepository
import com.fixit.app.data.payment.PaymentRepository
import com.fixit.app.data.upload.UploadRepository
import com.fixit.app.domain.model.Booking
import com.fixit.app.domain.model.BookingPayout
import com.fixit.app.domain.model.BookingStatus
import com.fixit.app.domain.model.PhotoKind
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
    val payout: BookingPayout? = null,
    /**
     * Non-null while a before/after upload is in flight. The UI uses this to
     * gate the correct slot (BEFORE → spinner over the before tile) and to
     * disable the "Mark job done" button so the photo doesn't get orphaned
     * if the state transitions before the upload completes.
     */
    val uploadingKind: PhotoKind? = null,
    /**
     * Non-null while a photo deletion is in flight. Holds the photo id so
     * the corresponding tile can show a dimmed/loading appearance.
     */
    val deletingPhotoId: String? = null,
)

sealed interface JobDetailEffect {
    data class StatusUpdated(val message: String) : JobDetailEffect
    data object Dismiss : JobDetailEffect
}

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val bookingRepo: BookingRepository,
    private val paymentRepo: PaymentRepository,
    private val uploadRepo: UploadRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val bookingId: String =
        checkNotNull(savedState["bookingId"]) { "bookingId nav arg missing" }

    private val _state = MutableStateFlow(JobDetailState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<JobDetailEffect>()
    val effects = _effects.asSharedFlow()

    // First load + every subsequent ON_START refresh driven by the screen.

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

    // ── Before/after photo handling ──────────────────────────────────────

    /**
     * Two-step: upload bytes via /uploads/image → attach the returned URL
     * to the booking via POST /bookings/{id}/photos. Refreshes the booking
     * on success so `state.booking.photos` becomes the source of truth.
     *
     * Guards against concurrent uploads (uploadingKind != null) and against
     * uploads while the screen is busy mutating status, since the server
     * rejects attachments outside in_progress/awaiting_confirmation.
     */
    fun onPhotoPicked(uri: Uri, kind: PhotoKind) {
        if (_state.value.uploadingKind != null) return
        if (_state.value.isMutating) return
        viewModelScope.launch {
            _state.value = _state.value.copy(uploadingKind = kind, errorMessage = null)
            val outcome = runCatching {
                val url = uploadRepo.uploadImage(uri)
                bookingRepo.addPhoto(bookingId, url, kind)
                // Refresh so the embedded photo list (and any server-side
                // ordering by uploaded_at) is what the UI renders.
                bookingRepo.byId(bookingId)
            }
            outcome
                .onSuccess { refreshed ->
                    _state.value = _state.value.copy(
                        booking = refreshed,
                        uploadingKind = null,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        uploadingKind = null,
                        errorMessage = e.message ?: "Couldn't add photo",
                    )
                }
        }
    }

    /** Provider-only delete; server returns 204 on success. */
    fun removePhoto(photoId: String) {
        if (_state.value.deletingPhotoId != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(deletingPhotoId = photoId, errorMessage = null)
            val outcome = runCatching {
                bookingRepo.deletePhoto(bookingId, photoId)
                bookingRepo.byId(bookingId)
            }
            outcome
                .onSuccess { refreshed ->
                    _state.value = _state.value.copy(
                        booking = refreshed,
                        deletingPhotoId = null,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        deletingPhotoId = null,
                        errorMessage = e.message ?: "Couldn't remove photo",
                    )
                }
        }
    }

    // ── shared status-mutation pipeline ──────────────────────────────────

    private fun changeStatus(
        newStatus: String,
        successMessage: String,
        dismissAfter: Boolean,
    ) {
        if (_state.value.isMutating) return
        // Don't let "Mark job done" race a pending photo upload — the photo
        // would never get attached (server rejects in non-editable status).
        if (_state.value.uploadingKind != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isMutating = true, errorMessage = null)
            val outcome = runCatching {
                val updated = bookingRepo.updateStatus(bookingId, newStatus)
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

    private suspend fun loadBookingAndPayout(): Pair<Booking, BookingPayout?> =
        coroutineScope {
            val bookingDeferred = async { bookingRepo.byId(bookingId) }
            val payoutDeferred  = async {
                runCatching { bookingRepo.payoutFor(bookingId) }.getOrNull()
            }
            bookingDeferred.await() to payoutDeferred.await()
        }
}