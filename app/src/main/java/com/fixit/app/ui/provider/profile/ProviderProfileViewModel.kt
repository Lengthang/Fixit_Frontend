package com.fixit.app.ui.provider.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.auth.AuthRepository
import com.fixit.app.data.booking.BookingRepository
import com.fixit.app.data.customer.CustomerRepository
import com.fixit.app.data.provider.ProviderRepository
import com.fixit.app.data.review.ReviewRepository
import com.fixit.app.domain.model.BookingStatus
import com.fixit.app.domain.model.ProfileUser
import com.fixit.app.domain.model.ProviderStatus
import com.fixit.app.domain.model.RatingSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderProfileState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val user: ProfileUser? = null,
    val ratingSummary: RatingSummary? = null,
    val totalJobs: Int = 0,
    val completionPercent: Int = 100,
)

@HiltViewModel
class ProviderProfileViewModel @Inject constructor(
    private val customerRepo: CustomerRepository,
    private val providerRepo: ProviderRepository,
    private val bookingRepo: BookingRepository,
    private val reviewRepo: ReviewRepository,
    private val authRepo: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProviderProfileState())
    val state = _state.asStateFlow()

    /**
     * Observed by NavGraph (not the screen). When this flips to true we
     * navigate to Welcome. Kept separate from `state` so the screen doesn't
     * need to know about it — preserves the rule of no UI changes.
     */
    private val _signedOut = MutableStateFlow(false)
    val signedOut = _signedOut.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { loadAll() }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't load profile",
                    )
                }
                .onSuccess {
                    _state.value = _state.value.copy(isLoading = false)
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepo.logout()
            _signedOut.value = true
        }
    }

    private suspend fun loadAll() = coroutineScope {
        // Parallel fetches — survive individual failures so the screen still
        // renders partial data instead of going completely blank.
        val meDeferred       = async { runCatching { customerRepo.me() } }
        val providerDeferred = async { runCatching { providerRepo.me() } }
        val bookingsDeferred = async { runCatching { bookingRepo.providerBookingsAsDomain() } }

        val me       = meDeferred.await().getOrNull()
        val provider = providerDeferred.await().getOrNull()
        val bookings = bookingsDeferred.await().getOrNull().orEmpty()

        // Reviews summary needs the provider profile id.
        val ratingSummary = provider?.id?.let { pid ->
            runCatching { reviewRepo.summaryForProvider(pid) }.getOrNull()
        }

        val completed = bookings.count { it.status == BookingStatus.COMPLETED }
        val cancelled = bookings.count { it.status == BookingStatus.CANCELLED }
        val finished  = completed + cancelled
        val completionPercent =
            if (finished == 0) 100 else (completed * 100) / finished

        _state.value = _state.value.copy(
            user = me?.let {
                ProfileUser(
                    id              = it.id,
                    name            = it.name,
                    phone           = it.phone,
                    profilePhotoUrl = it.profilePhotoUrl ?: provider?.profilePhotoUrl,
                    providerStatus  = ProviderStatus.fromApi(provider?.status),
                )
            },
            ratingSummary    = ratingSummary,
            totalJobs        = completed,
            completionPercent = completionPercent,
        )
    }
}