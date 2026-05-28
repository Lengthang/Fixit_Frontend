package com.fixit.app.ui.customer.browse

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.portfolio.PortfolioRepository
import com.fixit.app.data.provider.ProviderRepository
import com.fixit.app.data.review.ReviewRepository
import com.fixit.app.domain.model.PortfolioItem
import com.fixit.app.domain.model.ProviderDetail
import com.fixit.app.domain.model.RatingSummary
import com.fixit.app.domain.model.Review
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * State container for the customer-facing provider detail screen. Each
 * sub-fetch is allowed to fail independently — the screen still renders the
 * pieces that did load (matches the pattern in ProviderProfileViewModel).
 */
data class CustomerProviderDetailState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val detail: ProviderDetail? = null,
    val summary: RatingSummary? = null,
    val reviews: List<Review> = emptyList(),
    val portfolio: List<PortfolioItem> = emptyList(),
    /**
     * Cart of service quantities, keyed by service id. Entries with qty == 0
     * are pruned (see [CustomerProviderDetailViewModel.decrement]) so map
     * iteration doubles as "what's actually selected".
     */
    val quantities: Map<String, Int> = emptyMap(),
) {
    /** Star rating → count of reviews with that rating. Always covers 1..5. */
    val distribution: Map<Int, Int>
        get() = (1..5).associateWith { s -> reviews.count { it.rating == s } }

    /**
     * Derived "Top Pro" flag — never sent by backend. Same threshold logic
     * defined on ProviderDetail; we just feed it the live review count from
     * the summary so the badge updates as reviews flow in.
     */
    val isTopPro: Boolean
        get() = detail?.isTopPro(summary?.totalReviews ?: 0) == true

    /**
     * Running subtotal — Σ(price × qty) across every selected service.
     * Calculated on the client purely for instant UI feedback; the server
     * recomputes from the same source data via POST /bookings/price-preview
     * when the customer actually proceeds to the booking flow.
     */
    val subtotal: BigDecimal
        get() {
            val servicesById = detail?.services?.associateBy { it.id } ?: return BigDecimal.ZERO
            return quantities.entries.fold(BigDecimal.ZERO) { acc, (id, qty) ->
                val service = servicesById[id] ?: return@fold acc
                acc + service.price.multiply(BigDecimal(qty))
            }
        }

    /** True when at least one service has a quantity above zero. */
    val hasSelection: Boolean
        get() = quantities.values.any { it > 0 }

    /** Sum of quantities across all selected services — used for "X items" label. */
    val totalItems: Int
        get() = quantities.values.sum()
}

@HiltViewModel
class CustomerProviderDetailViewModel @Inject constructor(
    private val providerRepo: ProviderRepository,
    private val reviewRepo: ReviewRepository,
    private val portfolioRepo: PortfolioRepository,
    @ApplicationContext private val appContext: Context,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val providerId: String =
        checkNotNull(savedState["providerId"]) { "providerId nav arg missing" }

    private val _state = MutableStateFlow(CustomerProviderDetailState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { loadAll() }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't load provider profile",
                    )
                }
                .onSuccess {
                    _state.value = _state.value.copy(isLoading = false)
                }
        }
    }

    /**
     * Bump a service's quantity by one. No upper bound is enforced here —
     * the backend's `min_quantity` constraint is the only hard limit (see
     * booking_service.py), and that gets re-checked server-side at booking
     * time, so we don't fight the user on the cart screen.
     */
    fun increment(serviceId: String) {
        val current = _state.value
        val newQty = (current.quantities[serviceId] ?: 0) + 1
        _state.value = current.copy(
            quantities = current.quantities + (serviceId to newQty),
        )
    }

    /**
     * Drop one off a service's quantity. When it hits zero we remove the key
     * entirely so the map only ever holds genuinely-selected items —
     * keeps `hasSelection` and `subtotal` cheap and obvious.
     */
    fun decrement(serviceId: String) {
        val current = _state.value
        val currentQty = current.quantities[serviceId] ?: 0
        val newQty = (currentQty - 1).coerceAtLeast(0)
        val newQuantities = if (newQty == 0) {
            current.quantities - serviceId
        } else {
            current.quantities + (serviceId to newQty)
        }
        _state.value = current.copy(quantities = newQuantities)
    }

    private suspend fun loadAll() = coroutineScope {
        // 1) Resolve the customer's location first so the detail request can
        //    include it and the backend can compute distance_km. Permission
        //    was already requested during signup (LocationScreen); if it was
        //    denied or unavailable we'll get null back and the distance
        //    label will show "—".
        val (lat, lng) = resolveCustomerLocation()

        // 2) Fire all four reads in parallel. Each is wrapped in runCatching
        //    so a single 404/500 doesn't tank the whole screen — matches the
        //    "render partial data" pattern from ProviderProfileViewModel.
        val detailDeferred    = async { runCatching { providerRepo.byId(providerId, lat, lng) } }
        val reviewsDeferred   = async { runCatching { reviewRepo.forProvider(providerId) } }
        val summaryDeferred   = async { runCatching { reviewRepo.summaryForProvider(providerId) } }
        val portfolioDeferred = async { runCatching { portfolioRepo.forProvider(providerId) } }

        val detail    = detailDeferred.await().getOrNull()
        val reviews   = reviewsDeferred.await().getOrNull().orEmpty()
        val summary   = summaryDeferred.await().getOrNull()
        val portfolio = portfolioDeferred.await().getOrNull().orEmpty()

        if (detail == null) {
            // No detail at all is the one fatal failure — everything else is
            // optional cosmetic data, but without the provider profile there
            // is nothing meaningful to render.
            throw IllegalStateException("Provider not found")
        }

        _state.value = _state.value.copy(
            detail    = detail,
            summary   = summary,
            reviews   = reviews,
            portfolio = portfolio,
        )
    }

    /**
     * Best-effort fix on the device's location, mirroring ServiceAreaViewModel
     * (see also ProviderNearbyCard usage). Tries cached `lastLocation` first
     * for speed; falls back to a single fresh `getCurrentLocation` if the
     * cache is empty. Permission was already obtained on the LocationScreen,
     * so we suppress the lint here; if it wasn't granted both calls just
     * return null and the screen carries on without distance.
     */
    @SuppressLint("MissingPermission")
    private suspend fun resolveCustomerLocation(): Pair<Double?, Double?> {
        val client = LocationServices.getFusedLocationProviderClient(appContext)

        val cached = suspendCancellableCoroutine<android.location.Location?> { cont ->
            client.lastLocation
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }

        val resolved = cached ?: suspendCancellableCoroutine<android.location.Location?> { cont ->
            val cts = CancellationTokenSource()
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
            cont.invokeOnCancellation { cts.cancel() }
        }

        return resolved?.latitude to resolved?.longitude
    }
}