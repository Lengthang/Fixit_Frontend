package com.fixit.app.ui.provider.disputes

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.dispute.DisputeRepository
import com.fixit.app.data.local.DisputeDraftStorage
import com.fixit.app.data.upload.UploadRepository
import com.fixit.app.domain.model.Dispute
import com.fixit.app.domain.model.DisputeStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DisputeDetailState(
    val isLoading: Boolean = false,
    val dispute: Dispute? = null,
    val errorMessage: String? = null,
    // ── Response form (PENDING_RESPONSE only) ──────────────────────────
    val responseText: String = "",
    val uploadedImageUrls: List<String> = emptyList(),
    val isUploading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isDraftSaved: Boolean = false,
    // ── Image viewer ───────────────────────────────────────────────────
    val viewingImages: List<String>? = null,
) {
    val canSubmit: Boolean
        get() = responseText.length >= 10 && !isUploading && !isSubmitting

    val charCount: Int get() = responseText.length
}

sealed interface DisputeDetailEffect {
    data class ShowSnackbar(val message: String) : DisputeDetailEffect
    data object Submitted : DisputeDetailEffect
}

@HiltViewModel
class DisputeDetailViewModel @Inject constructor(
    private val disputeRepo: DisputeRepository,
    private val uploadRepo: UploadRepository,
    private val draftStorage: DisputeDraftStorage,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val disputeId: String =
        checkNotNull(savedState["disputeId"]) { "disputeId nav arg missing" }

    private val _state  = MutableStateFlow(DisputeDetailState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<DisputeDetailEffect>()
    val effects = _effects.asSharedFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                coroutineScope {
                    val disputeDeferred = async { disputeRepo.byId(disputeId) }
                    val draftDeferred   = async { draftStorage.loadDraft(disputeId) }
                    Pair(disputeDeferred.await(), draftDeferred.await())
                }
            }
                .onSuccess { (dispute, draft) ->
                    _state.update { s ->
                        s.copy(
                            isLoading    = false,
                            dispute      = dispute,
                            // Pre-populate from draft only if still pending
                            responseText = if (dispute.disputeStatus == DisputeStatus.PENDING_RESPONSE)
                                draft ?: s.responseText
                            else
                                s.responseText,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, errorMessage = e.message ?: "Couldn't load dispute") }
                }
        }
    }

    fun onResponseTextChange(text: String) {
        _state.update { it.copy(responseText = text, isDraftSaved = false) }
    }

    /** Called by the screen after the photo picker returns URIs. */
    fun onImagesSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true) }
            val results = uris.map { uri ->
                async { runCatching { uploadRepo.uploadImage(uri) } }
            }.map { it.await() }

            val successes = results.mapNotNull { it.getOrNull() }
            val failCount = results.count { it.isFailure }

            _state.update { s ->
                s.copy(
                    isUploading       = false,
                    uploadedImageUrls = s.uploadedImageUrls + successes,
                )
            }
            if (failCount > 0) {
                _effects.emit(DisputeDetailEffect.ShowSnackbar("$failCount image(s) failed to upload"))
            }
        }
    }

    fun removeUploadedImage(url: String) {
        _state.update { it.copy(uploadedImageUrls = it.uploadedImageUrls - url) }
    }

    fun saveDraft() {
        val text = _state.value.responseText
        viewModelScope.launch {
            draftStorage.saveDraft(disputeId, text)
            _state.update { it.copy(isDraftSaved = true) }
            _effects.emit(DisputeDetailEffect.ShowSnackbar("Draft saved"))
        }
    }

    fun submitResponse() {
        val s = _state.value
        if (!s.canSubmit) return
        val dispute = s.dispute ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            runCatching {
                disputeRepo.respond(dispute.id, s.responseText, s.uploadedImageUrls)
            }
                .onSuccess { updated ->
                    draftStorage.clearDraft(disputeId)
                    _state.update { it.copy(isSubmitting = false, dispute = updated) }
                    _effects.emit(DisputeDetailEffect.ShowSnackbar("Response submitted successfully"))
                    _effects.emit(DisputeDetailEffect.Submitted)
                }
                .onFailure { e ->
                    _state.update { it.copy(isSubmitting = false) }
                    _effects.emit(DisputeDetailEffect.ShowSnackbar(e.message ?: "Failed to submit response"))
                }
        }
    }

    fun showImages(urls: List<String>) {
        if (urls.isEmpty()) return
        _state.update { it.copy(viewingImages = urls) }
    }

    fun dismissImageViewer() {
        _state.update { it.copy(viewingImages = null) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}