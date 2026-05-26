package com.fixit.app.ui.provider.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.booking.BookingRepository
import com.fixit.app.data.booking.BookingResponse
import com.fixit.app.data.customer.CustomerRepository
import com.fixit.app.data.provider.ProviderRepository
import com.fixit.app.data.wallet.WalletRepository
import com.fixit.app.domain.model.DashboardStats
import com.fixit.app.domain.model.JobRequest
import com.fixit.app.domain.model.ProviderStatus
import com.fixit.app.domain.model.UpcomingJob
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

data class ProviderHomeState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val greeting: String = "",
    val displayName: String = "",
    val initials: String = "?",
    val avatarUrl: String? = null,
    val balance: BigDecimal = BigDecimal.ZERO,
    val stats: DashboardStats = DashboardStats(),
    val newRequests: List<JobRequest> = emptyList(),
    val upcoming: List<UpcomingJob> = emptyList(),
    val providerStatus: ProviderStatus? = null,
)

@HiltViewModel
class ProviderHomeViewModel @Inject constructor(
    private val customerRepo: CustomerRepository,
    private val providerRepo: ProviderRepository,
    private val walletRepo: WalletRepository,
    private val bookingRepo: BookingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProviderHomeState())
    val state = _state.asStateFlow()

    // NOTE: no init { refresh() } — screen-side OnLifecycleStart calls refresh()
    // on first composition AND on every subsequent ON_START (e.g. after a child
    // screen pops back, or after the app returns from background).

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { loadAll() }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't load dashboard",
                    )
                }
                .onSuccess {
                    _state.value = _state.value.copy(isLoading = false)
                }
        }
    }

    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private suspend fun loadAll() = coroutineScope {
        val meDeferred       = async { runCatching { customerRepo.me() } }
        val providerDeferred = async { runCatching { providerRepo.me() } }
        val walletDeferred   = async { runCatching { walletRepo.me() } }
        val bookingsDeferred = async { runCatching { bookingRepo.providerBookings() } }

        val me       = meDeferred.await().getOrNull()
        val provider = providerDeferred.await().getOrNull()
        val wallet   = walletDeferred.await().getOrNull()
        val bookings = bookingsDeferred.await().getOrNull().orEmpty()

        val now       = Clock.System.now()
        val tz        = TimeZone.currentSystemDefault()
        val nowLocal  = now.toLocalDateTime(tz)
        val avatarUrl = provider?.profilePhotoUrl ?: me?.profilePhotoUrl

        _state.value = _state.value.copy(
            greeting       = timeOfDayGreeting(nowLocal.hour),
            displayName    = firstName(me?.name).ifBlank { me?.name.orEmpty() },
            initials       = initialsOf(me?.name),
            avatarUrl      = avatarUrl,
            balance        = wallet?.balance?.let { BigDecimal.valueOf(it) } ?: BigDecimal.ZERO,
            providerStatus = ProviderStatus.fromApi(provider?.status),
            stats          = computeStats(
                bookings   = bookings,
                avgRating  = provider?.avgRating ?: 0.0,
                now        = now,
                tz         = tz,
            ),
            newRequests    = bookings
                .filter { it.status == STATUS_PENDING }
                .sortedByDescending { safeInstant(it.createdAt) }
                .take(MAX_PREVIEW_ROWS)
                .map { it.toJobRequest(now) },
            upcoming       = bookings
                .filter {
                    it.status == STATUS_IN_PROGRESS &&
                            isOnSameDay(safeInstant(it.scheduledAt), now, tz)
                }
                .sortedBy { safeInstant(it.scheduledAt) }
                .take(MAX_PREVIEW_ROWS)
                .map { it.toUpcomingJob(tz) },
        )
    }

    private fun computeStats(
        bookings: List<BookingResponse>,
        avgRating: Double,
        now: Instant,
        tz: TimeZone,
    ): DashboardStats {
        val completed = bookings.filter { it.status == STATUS_COMPLETED }
        val weekStart = startOfWeek(now, tz)

        val thisWeek = completed.filter { b ->
            val whenDone = safeInstant(b.scheduledAt) ?: safeInstant(b.createdAt)
            whenDone != null && whenDone >= weekStart
        }
        val weekEarnings = thisWeek.fold(BigDecimal.ZERO) { acc, b ->
            acc.add(BigDecimal.valueOf(b.totalAmount))
        }

        return DashboardStats(
            weekEarnings      = weekEarnings,
            weekDeltaPercent  = 0,
            jobsDoneTotal     = completed.size,
            jobsDoneThisWeek  = thisWeek.size,
            ratingAverage     = avgRating,
            ratingReviewCount = 0,
        )
    }

    private fun BookingResponse.toJobRequest(now: Instant): JobRequest {
        val name  = customer?.name?.takeIf { it.isNotBlank() } ?: "Customer"
        val issue = (notes?.takeIf { it.isNotBlank() } ?: items.firstOrNull()?.title)
            ?: "Service request"
        val created = safeInstant(createdAt)
        val metaParts = buildList {
            distanceKm?.let { add("%.1f km".format(it)) }
            created?.let { add(relativeTime(it, now)) }
        }
        return JobRequest(
            id               = id,
            customerName     = name,
            customerInitials = initialsOf(name),
            avatarColorHex   = pickAvatarColor(customerId),
            price            = BigDecimal.valueOf(totalAmount),
            issue            = issue,
            meta             = metaParts.joinToString(" · ").ifBlank { "Just now" },
        )
    }

    private fun BookingResponse.toUpcomingJob(tz: TimeZone): UpcomingJob {
        val scheduled = safeInstant(scheduledAt)
        val title     = items.firstOrNull()?.title ?: "Booking"
        val name      = customer?.name?.takeIf { it.isNotBlank() } ?: "Customer"
        val subParts  = listOfNotNull(name, address.takeIf { it.isNotBlank() })
        return UpcomingJob(
            id    = id,
            time  = scheduled?.let { formatTime(it, tz) } ?: "—",
            title = title,
            sub   = subParts.joinToString(" · "),
        )
    }

    private fun timeOfDayGreeting(hourOfDay: Int): String = when {
        hourOfDay < 12 -> "Good morning"
        hourOfDay < 17 -> "Good afternoon"
        else           -> "Good evening"
    }

    private fun firstName(name: String?): String =
        name?.trim()?.split(Regex("\\s+"))?.firstOrNull().orEmpty()

    private fun initialsOf(name: String?): String {
        if (name.isNullOrBlank()) return "?"
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.isEmpty()  -> "?"
            parts.size == 1  -> parts[0].take(2).uppercase()
            else             -> "${parts.first().first()}${parts.last().first()}".uppercase()
        }
    }

    private fun safeInstant(iso: String?): Instant? =
        iso?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }

    private fun startOfWeek(now: Instant, tz: TimeZone): Instant {
        val today: LocalDate = now.toLocalDateTime(tz).date
        val daysSinceMonday  = today.dayOfWeek.ordinal.toLong()
        val monday = today.minus(daysSinceMonday, DateTimeUnit.DAY)
        return monday.atStartOfDayIn(tz)
    }

    private fun isOnSameDay(instant: Instant?, now: Instant, tz: TimeZone): Boolean {
        if (instant == null) return false
        return instant.toLocalDateTime(tz).date == now.toLocalDateTime(tz).date
    }

    private fun formatTime(instant: Instant, tz: TimeZone): String {
        val ldt = instant.toLocalDateTime(tz)
        val h12 = when {
            ldt.hour == 0 -> 12
            ldt.hour > 12 -> ldt.hour - 12
            else          -> ldt.hour
        }
        val ampm = if (ldt.hour < 12) "AM" else "PM"
        return "%d:%02d %s".format(h12, ldt.minute, ampm)
    }

    private fun relativeTime(then: Instant, now: Instant): String {
        val seconds = (now - then).inWholeSeconds
        return when {
            seconds < 60                   -> "Just now"
            seconds < 60 * 60              -> "${seconds / 60} min ago"
            seconds < 60 * 60 * 24         -> "${seconds / 3600} h ago"
            seconds < 60 * 60 * 24 * 7     -> "${seconds / (3600 * 24)} d ago"
            else                            -> "${seconds / (3600 * 24 * 7)} w ago"
        }
    }

    private fun pickAvatarColor(seed: String): Long {
        val idx = ((seed.hashCode().toLong() and 0x7FFFFFFFL) % AVATAR_PALETTE.size).toInt()
        return AVATAR_PALETTE[idx]
    }

    companion object {
        private const val STATUS_PENDING     = "pending"
        private const val STATUS_IN_PROGRESS = "in_progress"
        private const val STATUS_COMPLETED   = "completed"
        private const val MAX_PREVIEW_ROWS   = 5

        private val AVATAR_PALETTE = longArrayOf(
            0xFF2563EB,
            0xFFF97316,
            0xFF10B981,
            0xFF8B5CF6,
            0xFFEC4899,
            0xFF06B6D4,
            0xFFEAB308,
        )
    }
}