package com.fixit.app.ui.provider.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.provider.ProviderRepository
import com.fixit.app.data.provider.ProviderUpdateRequest
import com.fixit.app.data.upload.UploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Which document slot the single image picker is currently filling.
 *
 * The screen reuses one ActivityResultContracts.GetContent() launcher for both
 * slots and sets the target right before launching, mirroring the pattern
 * EditProfileScreen used before cert/ID upload was moved here.
 */
enum class VerificationTarget { CERTIFICATE, NATIONAL_ID }

/** Default certificate name when the provider uploads a file but leaves the name blank. */
private const val DEFAULT_CERT_NAME = "Certificate"

data class VerificationState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,

    /** Status string from the backend ("pending" | "approved" | "rejected"). */
    val status: String = "pending",

    // ── National ID ──────────────────────────────────────────────────────
    val nationalIdUrl: String? = null,
    val uploadingNationalId: Boolean = false,

    // ── Certificate (one named certificate + one file) ───────────────────
    val certificationName: String = "",
    val certificationUrl: String? = null,
    val uploadingCert: Boolean = false,

    /** True once the first server load has completed. */
    val hasLoaded: Boolean = false,
) {
    /**
     * Mirrors the backend's derived `is_verified` rule:
     * approved AND at least one uploaded document (National ID or Certificate).
     * The free-text certification name alone does NOT count — only a file.
     */
    val isVerified: Boolean
        get() = status == "approved" && (nationalIdUrl != null || certificationUrl != null)
}

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val providerRepo: ProviderRepository,
    private val uploadRepo: UploadRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(VerificationState())
    val state = _state.asStateFlow()

    /** Set by the screen right before launching the shared image picker. */
    var pendingTarget: VerificationTarget = VerificationTarget.CERTIFICATE
        private set

    // NOTE: no init { refresh() } — the screen drives the first load via
    // OnLifecycleStart, so the same path also handles "navigated back".

    fun dismissError() { _state.value = _state.value.copy(errorMessage = null) }

    fun setTarget(target: VerificationTarget) { pendingTarget = target }

    fun onImagePicked(uri: Uri) {
        when (pendingTarget) {
            VerificationTarget.CERTIFICATE  -> uploadCertificate(uri)
            VerificationTarget.NATIONAL_ID  -> uploadNationalId(uri)
        }
    }

    // ── Load ──────────────────────────────────────────────────────────────

    fun refresh() {
        if (_state.value.hasLoaded) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { providerRepo.me() }
                .onSuccess { provider ->
                    _state.value = _state.value.copy(
                        isLoading         = false,
                        hasLoaded         = true,
                        status            = provider.status,
                        nationalIdUrl     = provider.nationalIdUrl,
                        certificationName = provider.certification.orEmpty(),
                        certificationUrl  = provider.certificationUrl,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading    = false,
                        errorMessage = e.message ?: "Couldn't load verification documents",
                    )
                }
        }
    }

    // ── Certificate name ────────────────────────────────────────────────────

    /** Updates the in-memory name as the user types. Persisted on focus-loss via [commitCertName]. */
    fun onCertNameChange(v: String) {
        _state.value = _state.value.copy(certificationName = v.take(120))
    }

    /**
     * Persists the certificate name. Called by the screen when the name field
     * loses focus. No-ops when there's no uploaded certificate yet (a name
     * without a file is meaningless — the backend's verified rule ignores it),
     * and falls back to "Certificate" when the field was left blank.
     */
    fun commitCertName() {
        val s = _state.value
        if (s.certificationUrl == null) return
        val name = s.certificationName.trim().ifBlank { DEFAULT_CERT_NAME }
        _state.value = s.copy(certificationName = name)
        patch(ProviderUpdateRequest(certification = name)) {
            // Roll back to the prior trimmed value would be confusing here;
            // the typed value stays on screen and the user can retry on next blur.
            it.copy(errorMessage = it.errorMessage ?: "Couldn't save certificate name")
        }
    }

    // ── Uploads (immediate PATCH on success) ────────────────────────────────

    private fun uploadCertificate(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(uploadingCert = true, errorMessage = null)
            runCatching { uploadRepo.uploadImage(uri) }
                .onSuccess { url ->
                    // If the name is still blank, default it so the saved record
                    // always has a human-readable label.
                    val name = _state.value.certificationName.trim().ifBlank { DEFAULT_CERT_NAME }
                    _state.value = _state.value.copy(
                        certificationUrl  = url,
                        certificationName = name,
                        uploadingCert     = false,
                    )
                    patch(
                        ProviderUpdateRequest(certification = name, certificationUrl = url),
                    ) { it.copy(errorMessage = it.errorMessage ?: "Couldn't save certificate") }
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        uploadingCert = false,
                        errorMessage  = e.message ?: "Couldn't upload certificate",
                    )
                }
        }
    }

    private fun uploadNationalId(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(uploadingNationalId = true, errorMessage = null)
            runCatching { uploadRepo.uploadImage(uri) }
                .onSuccess { url ->
                    _state.value = _state.value.copy(
                        nationalIdUrl       = url,
                        uploadingNationalId = false,
                    )
                    patch(ProviderUpdateRequest(nationalIdUrl = url)) {
                        it.copy(errorMessage = it.errorMessage ?: "Couldn't save National ID")
                    }
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        uploadingNationalId = false,
                        errorMessage        = e.message ?: "Couldn't upload National ID",
                    )
                }
        }
    }

    // ── Remove ──────────────────────────────────────────────────────────────
    //
    // The backend PATCH only applies non-null fields, so we can't clear a column
    // by sending null. We send an empty string — the column is nullable Text and
    // accepts "", which the verified rule treats as "no document" (bool("") ==
    // False). The local state uses null so the UI flips back to the empty card.

    fun removeCertificate() {
        val prev = _state.value
        _state.value = prev.copy(certificationUrl = null, certificationName = "")
        patch(ProviderUpdateRequest(certification = "", certificationUrl = "")) {
            // Restore on failure so the UI doesn't lie about what's stored.
            it.copy(
                certificationUrl  = prev.certificationUrl,
                certificationName = prev.certificationName,
                errorMessage      = it.errorMessage ?: "Couldn't remove certificate",
            )
        }
    }

    fun removeNationalId() {
        val prev = _state.value
        _state.value = prev.copy(nationalIdUrl = null)
        patch(ProviderUpdateRequest(nationalIdUrl = "")) {
            it.copy(
                nationalIdUrl = prev.nationalIdUrl,
                errorMessage  = it.errorMessage ?: "Couldn't remove National ID",
            )
        }
    }

    // ── Shared PATCH helper ─────────────────────────────────────────────────

    /**
     * Sends a partial provider update. On failure, [onError] receives the
     * current state and returns the state to roll back to (e.g. restoring a
     * removed URL and surfacing an error message). Refreshes the verified
     * status from the server response on success.
     */
    private fun patch(
        body: ProviderUpdateRequest,
        onError: (VerificationState) -> VerificationState,
    ) {
        viewModelScope.launch {
            runCatching { providerRepo.updateProfile(body) }
                .onSuccess { provider ->
                    _state.value = _state.value.copy(status = provider.status)
                }
                .onFailure {
                    _state.value = onError(_state.value)
                }
        }
    }
}