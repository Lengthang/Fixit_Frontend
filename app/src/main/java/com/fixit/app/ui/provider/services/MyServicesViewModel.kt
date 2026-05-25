package com.fixit.app.ui.provider.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.booking.BookingRepository
import com.fixit.app.data.provider.ProviderRepository
import com.fixit.app.data.review.ReviewRepository
import com.fixit.app.data.service.ServiceRepository
import com.fixit.app.data.service.ServiceResponse
import com.fixit.app.data.wallet.WalletRepository
import com.fixit.app.domain.model.Service
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

enum class ServiceFilter { ALL, ACTIVE, PAUSED }

data class MyServicesState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val services: List<Service> = emptyList(),
    val totalBookings: Int = 0,
    val walletBalance: BigDecimal = BigDecimal.ZERO,
    val avgRating: Double = 0.0,
    val activeFilter: ServiceFilter = ServiceFilter.ALL,
) {
    val filteredServices: List<Service> get() = when (activeFilter) {
        ServiceFilter.ALL    -> services
        ServiceFilter.ACTIVE -> services.filter { it.isActive }
        ServiceFilter.PAUSED -> services.filter { !it.isActive }
    }

    val activeCount: Int  get() = services.count { it.isActive }
    val pausedCount: Int  get() = services.count { !it.isActive }
}

@HiltViewModel
class MyServicesViewModel @Inject constructor(
    private val serviceRepo: ServiceRepository,
    private val providerRepo: ProviderRepository,
    private val bookingRepo: BookingRepository,
    private val walletRepo: WalletRepository,
    private val reviewRepo: ReviewRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MyServicesState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { loadAll() }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't load services",
                    )
                }
                .onSuccess {
                    _state.value = _state.value.copy(isLoading = false)
                }
        }
    }

    fun setFilter(filter: ServiceFilter) {
        _state.value = _state.value.copy(activeFilter = filter)
    }

    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private suspend fun loadAll() = coroutineScope {
        val servicesDeferred = async { serviceRepo.mine() }
        val providerDeferred = async { providerRepo.me() }
        val bookingsDeferred = async { runCatching { bookingRepo.providerBookings() } }
        val walletDeferred   = async { runCatching { walletRepo.me() } }

        val serviceResponses = servicesDeferred.await()
        val provider         = providerDeferred.await()
        val bookings         = bookingsDeferred.await().getOrNull().orEmpty()
        val wallet           = walletDeferred.await().getOrNull()

        // Avg rating requires the provider profile id; skip gracefully if unavailable.
        val ratingSummary = runCatching {
            reviewRepo.summaryForProvider(provider.id)
        }.getOrNull()

        // Build serviceId → bookingCount map from all booking line items.
        val bookingCountByServiceId: Map<String, Int> = bookings
            .flatMap { booking -> booking.items.map { item -> item.serviceId } }
            .groupingBy { it }
            .eachCount()

        val services = serviceResponses.map { dto ->
            dto.toDomain(
                categoryName        = provider.categories.firstOrNull { it.id == dto.categoryId }?.name,
                bookingCount        = bookingCountByServiceId[dto.id] ?: 0,
            )
        }

        _state.value = _state.value.copy(
            services       = services,
            totalBookings  = bookings.size,
            walletBalance  = wallet?.balance?.let { BigDecimal.valueOf(it) } ?: BigDecimal.ZERO,
            avgRating      = ratingSummary?.avgRating ?: 0.0,
        )
    }

    // ── private mapping ──────────────────────────────────────────────────

    private fun ServiceResponse.toDomain(
        categoryName: String?,
        bookingCount: Int,
    ): Service = Service(
        id              = id,
        title           = title,
        description     = description,
        imageUrl        = imageUrl,
        price           = BigDecimal.valueOf(price),
        durationMinutes = durationMinutes,
        minQuantity     = minQuantity,
        isActive        = isActive,
        categoryId      = categoryId,
        categoryName    = categoryName,
        updatedAt       = updatedAt,
        bookingCount    = bookingCount,
    )
}