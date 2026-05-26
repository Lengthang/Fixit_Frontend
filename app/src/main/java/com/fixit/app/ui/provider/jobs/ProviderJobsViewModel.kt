package com.fixit.app.ui.provider.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.booking.BookingRepository
import com.fixit.app.domain.model.Booking
import com.fixit.app.domain.model.BookingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class JobsTab { NEW, SCHEDULED, COMPLETED }

data class ProviderJobsState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val tab: JobsTab = JobsTab.NEW,
    val bookings: List<Booking> = emptyList(),
) {
    val newCount: Int
        get() = bookings.count { it.status == BookingStatus.PENDING }

    val visible: List<Booking>
        get() = when (tab) {
            JobsTab.NEW       -> bookings
                .filter { it.status == BookingStatus.PENDING }
                .sortedByDescending { it.createdAt }
            JobsTab.SCHEDULED -> bookings
                .filter {
                    it.status == BookingStatus.IN_PROGRESS ||
                            it.status == BookingStatus.AWAITING_CONFIRMATION
                }
                .sortedBy { it.scheduledAt }
            JobsTab.COMPLETED -> bookings
                .filter { it.status == BookingStatus.COMPLETED }
                .sortedByDescending { it.scheduledAt }
        }
}

@HiltViewModel
class ProviderJobsViewModel @Inject constructor(
    private val bookingRepo: BookingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProviderJobsState())
    val state = _state.asStateFlow()

    // First load + every subsequent ON_START refresh driven by the screen.

    fun selectTab(tab: JobsTab) {
        _state.value = _state.value.copy(tab = tab)
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { bookingRepo.providerBookingsAsDomain() }
                .onSuccess { list ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        bookings = list,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't load jobs",
                    )
                }
        }
    }
}