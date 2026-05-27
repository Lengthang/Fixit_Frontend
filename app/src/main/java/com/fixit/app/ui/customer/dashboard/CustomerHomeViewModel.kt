package com.fixit.app.ui.customer.dashboard

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.category.CategoryRepository
import com.fixit.app.data.customer.CustomerRepository
import com.fixit.app.data.promo.PromoRepository
import com.fixit.app.data.provider.ProviderRepository
import com.fixit.app.domain.model.ActivePromo
import com.fixit.app.domain.model.HomeCategory
import com.fixit.app.domain.model.NearbyProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

data class CustomerHomeState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val displayName: String = "",
    val firstName: String = "",
    val initials: String = "?",
    val avatarUrl: String? = null,
    val addressLabel: String? = null,           // "1247 Market St" — null = unresolved
    val isLocating: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val categories: List<HomeCategory> = emptyList(),
    val promos: List<ActivePromo> = emptyList(),
    val nearby: List<NearbyProvider> = emptyList(),
    val notificationsDot: Boolean = false,      // wired off until notifications backend lands
)

/**
 * Customer home dashboard. Loads four sources in parallel and tolerates
 * individual failures so the screen renders partial data rather than going
 * blank — same pattern ProviderHomeViewModel uses.
 *
 * Note on Application context: we inject @ApplicationContext so we can call
 * FusedLocationProviderClient and Geocoder. Both are safe with app context.
 */
@HiltViewModel
class CustomerHomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val customerRepo: CustomerRepository,
    private val categoryRepo: CategoryRepository,
    private val providerRepo: ProviderRepository,
    private val promoRepo: PromoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerHomeState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { loadAll() }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't load home",
                    )
                }
                .onSuccess {
                    _state.value = _state.value.copy(isLoading = false)
                }
        }
    }

    private suspend fun loadAll() = coroutineScope {
        // 1) Pull profile + categories + promos in parallel. Provider list
        //    depends on location so it runs after we resolve coords.
        val meDeferred         = async { runCatching { customerRepo.me() }.getOrNull() }
        val categoriesDeferred = async { runCatching { categoryRepo.list() }.getOrDefault(emptyList()) }
        val promosDeferred     = async { runCatching { promoRepo.active() }.getOrDefault(emptyList()) }
        val locationDeferred   = async { resolveLocationAndAddress() }

        val me            = meDeferred.await()
        val categoriesRaw = categoriesDeferred.await()
        val promos        = promosDeferred.await()
        val location      = locationDeferred.await()

        val first = firstNameOf(me?.name)
        _state.value = _state.value.copy(
            displayName     = me?.name.orEmpty(),
            firstName       = first,
            initials        = initialsOf(me?.name),
            avatarUrl       = me?.profilePhotoUrl,
            categories      = categoriesRaw.map {
                HomeCategory(id = it.id, name = it.name, iconUrl = it.iconUrl)
            },
            promos          = promos,
            addressLabel    = location?.address,
            latitude        = location?.lat,
            longitude       = location?.lng,
        )

        // 2) Provider list with the location we now have (may be null).
        val nearby = runCatching {
            providerRepo.listNearby(
                customerLat = location?.lat,
                customerLng = location?.lng,
                limit = 20,
            )
        }.getOrDefault(emptyList())

        _state.value = _state.value.copy(nearby = nearby)
    }

    // ── Location + reverse geocode ────────────────────────────────────────

    private data class ResolvedLocation(
        val lat: Double,
        val lng: Double,
        val address: String?,
    )

    @SuppressLint("MissingPermission")
    private suspend fun resolveLocationAndAddress(): ResolvedLocation? {
        if (!hasLocationPermission()) return null

        _state.value = _state.value.copy(isLocating = true)
        try {
            val client = LocationServices.getFusedLocationProviderClient(appContext)

            // Try cached last location first.
            val last: Location? = suspendCancellableCoroutine { cont ->
                client.lastLocation
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
            val loc = last ?: requestFreshLocation(client) ?: return null

            val address = withContext(Dispatchers.IO) {
                reverseGeocode(loc.latitude, loc.longitude)
            }
            return ResolvedLocation(loc.latitude, loc.longitude, address)
        } finally {
            _state.value = _state.value.copy(isLocating = false)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(
        client: com.google.android.gms.location.FusedLocationProviderClient,
    ): Location? = suspendCancellableCoroutine { cont ->
        val cts = CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(null) }
        cont.invokeOnCancellation { cts.cancel() }
    }

    /**
     * Short address — street + number when available, else locality.
     * Wrapped in runCatching: Geocoder throws on network failures on real
     * devices and we don't want to take down the whole screen for it.
     */
    private fun reverseGeocode(lat: Double, lng: Double): String? = runCatching {
        val geocoder = Geocoder(appContext, Locale.getDefault())
        @Suppress("DEPRECATION")
        val results = geocoder.getFromLocation(lat, lng, 1).orEmpty()
        val r = results.firstOrNull() ?: return@runCatching null

        val streetLine = listOfNotNull(r.subThoroughfare, r.thoroughfare)
            .joinToString(" ")
            .ifBlank { null }
        streetLine ?: r.locality ?: r.subAdminArea ?: r.adminArea
    }.getOrNull()

    private fun hasLocationPermission(): Boolean = listOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
    ).any {
        ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // ── Tiny formatters (kept local — identical to ProviderHomeViewModel) ─

    private fun firstNameOf(name: String?): String =
        name?.trim()?.split(Regex("\\s+"))?.firstOrNull().orEmpty()

    private fun initialsOf(name: String?): String {
        if (name.isNullOrBlank()) return "?"
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else            -> "${parts.first().first()}${parts.last().first()}".uppercase()
        }
    }
}