package com.fixit.app.ui.customer.bookings

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.booking.BookingRepository
import com.fixit.app.data.dispute.DisputeRepository
import com.fixit.app.data.upload.UploadRepository
import com.fixit.app.domain.model.Booking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The fixed reason categories shown as radio options. `OTHER` has no fixed
 * label for storage — when picked, the customer types their own text which
 * becomes the category portion of the persisted reason.
 *
 * NOTE: every preset label is already ≥ 10 characters, so the backend's
 * `reason` min-length(10) constraint is always satisfied for preset picks
 * even when the optional description is left blank.
 */
enum class DisputeReasonCategory(val label: String) {
    NO_SHOW("Pro didn't show up"),
    WORK_NOT_COMPLETED("Work wasn't completed"),
    DAMAGE("Damage to property"),
    CHARGED_INCORRECTLY("Charged incorrectly"),
    UNPROFESSIONAL("Unprofessional behavior"),
    OTHER("Other"),
}

data class OpenDisputeState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isUploading: Boolean = false,
    val errorMessage: String? = null,
    val booking: Booking? = null,
    val category: DisputeReasonCategory? = null,
    /** Custom reason text, only used when [category] == OTHER. */
    val otherText: String = "",
    /** Free-text "Describe what happened" — optional. */
    val description: String = "",
    val uploadedImageUrls: List<String> = emptyList(),
) {
    /** The category portion of the reason — the custom text when OTHER. */
    private val categoryLabel: String
        get() = when (category) {
            null -> ""
            DisputeReasonCategory.OTHER -> otherText.trim()
            else -> category.label
        }

    /**
     * What actually gets written to the backend's single `reason` column,
     * formatted as: "<Reason category> - <Customer description>".
     * Falls back to just the category when no description is given.
     */
    val composedReason: String
        get() {
            val cat = categoryLabel
            val desc = description.trim()
            return when {
                cat.isEmpty()  -> ""
                desc.isEmpty() -> cat
                else           -> "$cat - $desc"
            }
        }

    /**
     * Backend requires reason length in 10..2000. A category is required;
     * the description is optional.
     */
    val canSubmit: Boolean
        get() = !isSubmitting &&
                !isUploading &&
                category != null &&
                (category != DisputeReasonCategory.OTHER || otherText.isNotBlank()) &&
                composedReason.length in 10..2000
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
    private val uploadRepo: UploadRepository,
) : ViewModel() {

    private val bookingId: String =
        checkNotNull(savedState["bookingId"]) { "bookingId nav arg missing" }

    private val _state = MutableStateFlow(OpenDisputeState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<OpenDisputeEffect>(extraBufferCapacity = 4)
    val effects = _effects.asSharedFlow()

    /** Backend caps reason_image_urls at 10. */
    private val maxImages = 10

    init { loadBooking() }

    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun onCategorySelect(category: DisputeReasonCategory) {
        _state.value = _state.value.copy(category = category)
    }

    fun onOtherTextChange(value: String) {
        _state.value = _state.value.copy(otherText = value)
    }

    fun onDescriptionChange(value: String) {
        _state.value = _state.value.copy(description = value)
    }

    fun onImagesSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploading = true)
            val results = coroutineScope {
                uris.map { uri -> async { runCatching { uploadRepo.uploadImage(uri) } } }
                    .map { it.await() }
            }

            val successes = results.mapNotNull { it.getOrNull() }
            val failCount = results.count { it.isFailure }

            _state.value = _state.value.copy(
                isUploading       = false,
                uploadedImageUrls = (_state.value.uploadedImageUrls + successes).take(maxImages),
            )
            if (failCount > 0) {
                _effects.tryEmit(OpenDisputeEffect.Message("$failCount image(s) failed to upload"))
            }
        }
    }

    fun removeUploadedImage(url: String) {
        _state.value = _state.value.copy(
            uploadedImageUrls = _state.value.uploadedImageUrls - url,
        )
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
            runCatching {
                disputeRepo.raiseDispute(bookingId, s.composedReason, s.uploadedImageUrls)
            }
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