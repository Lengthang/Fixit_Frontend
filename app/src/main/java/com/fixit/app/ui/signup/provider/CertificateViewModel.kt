package com.fixit.app.ui.signup.provider

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.upload.UploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CertificateUploadState(
    val certificateUrl: String? = null,
    val nationalIdUrl: String? = null,
    val isUploadingCertificate: Boolean = false,
    val isUploadingNationalId: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class CertificateViewModel @Inject constructor(
    private val uploadRepo: UploadRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CertificateUploadState())
    val state = _state.asStateFlow()

    fun dismissError() { _state.value = _state.value.copy(errorMessage = null) }

    fun onCertificatePicked(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploadingCertificate = true)
            runCatching { uploadRepo.uploadImage(uri) }
                .onSuccess { url ->
                    _state.value = _state.value.copy(
                        certificateUrl = url,
                        isUploadingCertificate = false,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isUploadingCertificate = false,
                        errorMessage = e.message ?: "Failed to upload certificate",
                    )
                }
        }
    }

    fun onNationalIdPicked(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploadingNationalId = true)
            runCatching { uploadRepo.uploadImage(uri) }
                .onSuccess { url ->
                    _state.value = _state.value.copy(
                        nationalIdUrl = url,
                        isUploadingNationalId = false,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isUploadingNationalId = false,
                        errorMessage = e.message ?: "Failed to upload National ID",
                    )
                }
        }
    }

    fun onRemoveCertificate() { _state.value = _state.value.copy(certificateUrl = null) }
    fun onRemoveNationalId()  { _state.value = _state.value.copy(nationalIdUrl = null) }
}