package com.fixit.app.ui.provider.disputes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.dispute.DisputeRepository
import com.fixit.app.domain.model.Dispute
import com.fixit.app.domain.model.DisputeStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DisputeListTab { ALL, OPEN, RESOLVED }

data class DisputeListState(
    val isLoading: Boolean = false,
    val disputes: List<Dispute> = emptyList(),
    val tab: DisputeListTab = DisputeListTab.ALL,
    val errorMessage: String? = null,
) {
    val pendingCount: Int  get() = disputes.count { it.disputeStatus == DisputeStatus.PENDING_RESPONSE }
    val reviewCount: Int   get() = disputes.count { it.disputeStatus == DisputeStatus.AWAITING_REVIEW }
    val resolvedCount: Int get() = disputes.count { it.disputeStatus == DisputeStatus.RESOLVED }

    val displayed: List<Dispute> get() = when (tab) {
        DisputeListTab.ALL      -> disputes
        DisputeListTab.OPEN     -> disputes.filter { it.disputeStatus != DisputeStatus.RESOLVED }
        DisputeListTab.RESOLVED -> disputes.filter { it.disputeStatus == DisputeStatus.RESOLVED }
    }

    val openCount: Int get() = disputes.count { it.disputeStatus != DisputeStatus.RESOLVED }
}

@HiltViewModel
class DisputeListViewModel @Inject constructor(
    private val disputeRepo: DisputeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DisputeListState())
    val state = _state.asStateFlow()

    // First load + every subsequent ON_START refresh driven by the screen.
    // Picking up status transitions made on DisputeDetailScreen happens here
    // when the user pops back.

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { disputeRepo.providerDisputes() }
                .onSuccess { disputes ->
                    _state.value = _state.value.copy(isLoading = false, disputes = disputes)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't load disputes",
                    )
                }
        }
    }

    fun selectTab(tab: DisputeListTab) {
        _state.value = _state.value.copy(tab = tab)
    }

    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}