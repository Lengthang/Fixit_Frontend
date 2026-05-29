package com.fixit.app.ui.customer.addresses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.location.LocationRepository
import com.fixit.app.domain.model.SavedLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedAddressesState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val addresses: List<SavedLocation> = emptyList(),
    /** Id currently being deleted, so the row can show a spinner / disable. */
    val deletingId: String? = null,
)

@HiltViewModel
class SavedAddressesViewModel @Inject constructor(
    private val locationRepo: LocationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SavedAddressesState())
    val state = _state.asStateFlow()

    // No init{} — the screen drives the first load via OnLifecycleStart so
    // returning from the create/edit screen re-fetches the fresh list. This
    // mirrors CustomerProfileViewModel's pattern.
    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { locationRepo.list() }
                .onSuccess { list ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        addresses = list.sortedByDescending { it.isDefault },
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't load addresses",
                    )
                }
        }
    }

    fun delete(id: String) {
        if (_state.value.deletingId != null) return
        _state.value = _state.value.copy(deletingId = id)
        viewModelScope.launch {
            runCatching { locationRepo.delete(id) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        deletingId = null,
                        addresses = _state.value.addresses.filterNot { it.id == id },
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        deletingId = null,
                        errorMessage = e.message ?: "Couldn't delete address",
                    )
                }
        }
    }

    fun dismissError() { _state.value = _state.value.copy(errorMessage = null) }
}