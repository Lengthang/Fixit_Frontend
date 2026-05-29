package com.fixit.app.ui.customer.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.auth.AuthRepository
import com.fixit.app.data.customer.CustomerRepository
import com.fixit.app.data.location.LocationRepository
import com.fixit.app.data.promo.PromoRepository
import com.fixit.app.data.wallet.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class CustomerProfileState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // user
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val profilePhotoUrl: String? = null,
    // wallet
    val walletBalance: BigDecimal = BigDecimal.ZERO,
    // counts driving the row subtitles/badges
    val savedAddressesCount: Int = 0,
)

@HiltViewModel
class CustomerProfileViewModel @Inject constructor(
    private val customerRepo: CustomerRepository,
    private val walletRepo: WalletRepository,
    private val locationRepo: LocationRepository,
    private val authRepo: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerProfileState())
    val state = _state.asStateFlow()

    /**
     * Observed by NavGraph (not the screen). When this flips to true we
     * navigate to Welcome. Kept separate from `state` so the screen doesn't
     * need to know about it — mirrors the provider profile pattern.
     */
    private val _signedOut = MutableStateFlow(false)
    val signedOut = _signedOut.asStateFlow()

    // NOTE: no init { refresh() } — the screen drives the first load via
    // OnLifecycleStart so the same code path also handles "navigated back".

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
        val meDeferred        = async { runCatching { customerRepo.me() } }
        val walletDeferred    = async { runCatching { walletRepo.me() } }
        val addressesDeferred = async { runCatching { locationRepo.list() } }

        val me        = meDeferred.await().getOrNull()
        val wallet    = walletDeferred.await().getOrNull()
        val addresses = addressesDeferred.await().getOrNull().orEmpty()

        _state.value = _state.value.copy(
            name                = me?.name,
            email               = me?.email,
            phone               = me?.phone,
            profilePhotoUrl     = me?.profilePhotoUrl,
            walletBalance       = wallet?.balance?.let { BigDecimal.valueOf(it) } ?: BigDecimal.ZERO,
            savedAddressesCount = addresses.size,
        )
    }
}