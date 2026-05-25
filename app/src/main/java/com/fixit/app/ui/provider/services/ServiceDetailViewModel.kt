package com.fixit.app.ui.provider.services

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.booking.BookingRepository
import com.fixit.app.data.booking.BookingResponse
import com.fixit.app.data.provider.ProviderRepository
import com.fixit.app.data.service.ServiceRepository
import com.fixit.app.data.service.ServiceResponse
import com.fixit.app.data.service.ServiceUpdateRequest
import com.fixit.app.domain.model.Service
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

data class RecentBooking(
    val customerName: String,
    val customerId: String,
    val amount: BigDecimal,
    val createdAt: Instant,
)

data class ServiceDetailState(
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val errorMessage: String? = null,
    val service: Service? = null,
    /** Mirrors service.isActive but updates instantly on toggle without a full reload. */
    val isVisible: Boolean = true,
    val recentBooking: RecentBooking? = null,
)

sealed interface ServiceDetailEffect {
    /** Pop back to the services list (after a successful delete). */
    data object NavigateBack : ServiceDetailEffect
    data class ShowMessage(val text: String) : ServiceDetailEffect
}

@HiltViewModel
class ServiceDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val serviceRepo: ServiceRepository,
    private val providerRepo: ProviderRepository,
    private val bookingRepo: BookingRepository,
) : ViewModel() {

    private val serviceId: String =
        checkNotNull(savedState["serviceId"]) { "serviceId nav arg missing" }

    private val _state = MutableStateFlow(ServiceDetailState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ServiceDetailEffect>()
    val effects = _effects.asSharedFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { loadAll() }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't load service",
                    )
                }
                .onSuccess {
                    _state.value = _state.value.copy(isLoading = false)
                }
        }
    }

    fun setVisibility(visible: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isMutating = true)
            runCatching {
                serviceRepo.update(serviceId, ServiceUpdateRequest(isActive = visible))
            }.onSuccess { updated ->
                _state.value = _state.value.copy(
                    isMutating = false,
                    isVisible  = updated.isActive,
                    service    = _state.value.service?.copy(isActive = updated.isActive),
                )
                val msg = if (updated.isActive) "Service is now visible to customers"
                else "Service paused"
                _effects.emit(ServiceDetailEffect.ShowMessage(msg))
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isMutating    = false,
                    errorMessage  = e.message ?: "Failed to update",
                )
            }
        }
    }

    fun deleteService() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isMutating = true)
            runCatching { serviceRepo.delete(serviceId) }
                .onSuccess {
                    _effects.emit(ServiceDetailEffect.NavigateBack)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isMutating   = false,
                        errorMessage = e.message ?: "Failed to delete",
                    )
                }
        }
    }

    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    // ── loading ───────────────────────────────────────────────────────────

    private suspend fun loadAll() = coroutineScope {
        val servicesDeferred = async { serviceRepo.mine() }
        val providerDeferred = async { providerRepo.me() }
        val bookingsDeferred = async { runCatching { bookingRepo.providerBookings() } }

        val services = servicesDeferred.await()
        val provider = providerDeferred.await()
        val bookings = bookingsDeferred.await().getOrNull().orEmpty()

        val dto = services.firstOrNull { it.id == serviceId }
            ?: throw Exception("Service not found")

        val bookingCountByServiceId: Map<String, Int> = bookings
            .flatMap { b -> b.items.map { it.serviceId } }
            .groupingBy { it }
            .eachCount()

        val service = dto.toDomain(
            categoryName = provider.categories.firstOrNull { it.id == dto.categoryId }?.name,
            bookingCount = bookingCountByServiceId[dto.id] ?: 0,
        )

        val recentBookingRaw: BookingResponse? = bookings
            .filter { b -> b.items.any { it.serviceId == serviceId } }
            .maxByOrNull { parseInstantSafe(it.createdAt) }

        val recentBooking = recentBookingRaw?.let { b ->
            RecentBooking(
                customerName = b.customer?.name?.takeIf { it.isNotBlank() } ?: "Customer",
                customerId   = b.customerId,
                amount       = BigDecimal.valueOf(b.totalAmount),
                createdAt    = parseInstantSafe(b.createdAt),
            )
        }

        _state.value = _state.value.copy(
            service       = service,
            isVisible     = service.isActive,
            recentBooking = recentBooking,
        )
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private fun ServiceResponse.toDomain(categoryName: String?, bookingCount: Int): Service =
        Service(
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

    private fun parseInstantSafe(iso: String?): Instant =
        iso?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: Clock.System.now()
}