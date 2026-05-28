package com.fixit.app.domain.model

import kotlinx.datetime.Instant
import java.math.BigDecimal
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/**
 * UI-friendly booking shape. The backend's BookingResponse has more fields
 * (line items, status history, etc.); this collapses the bits the screens
 * actually render.
 *
 * `service` is derived from the first booking item — multi-item bookings are
 * shown as their primary line in summary rows. Price reflects the whole
 * booking's total_amount so the customer-pays figure always matches the
 * backend's escrow total.
 *
 * `customer` is populated on every response — used by provider screens.
 * `provider` is also populated on every response — used by customer screens.
 */
data class Booking(
    val id: String,
    val status: BookingStatus,
    val scheduledAt: Instant,
    val createdAt: Instant,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String? = null,
    val subtotal: BigDecimal = BigDecimal.ZERO,
    val discountAmount: BigDecimal = BigDecimal.ZERO,
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "SGD",
    val distanceKm: Double? = null,
    val customer: BookingCustomer? = null,
    val provider: BookingProvider? = null,
    val service: BookingService? = null,
    val photos: List<BookingPhoto> = emptyList(),
) {
    /**
     * Whether the signed-in customer can still cancel this booking, given the
     * backend's CANCELLATION_BUFFER_HOURS rule.
     *
     * Backend rules (services/booking_service.py update_booking_status):
     *  - pending: always cancellable by customer
     *  - in_progress: only if (scheduled_at - now) > 24h
     *  - any other status: not cancellable by customer
     *
     * The buffer constant is duplicated here intentionally — the server
     * does not expose it via API, and replicating it client-side avoids a
     * 403 round-trip when the user taps a button that has no chance of
     * succeeding.
     */
    fun isCustomerCancellable(now: Instant = Clock.System.now()): Boolean = when (status) {
        BookingStatus.PENDING     -> true
        BookingStatus.IN_PROGRESS -> scheduledAt > now + CANCELLATION_BUFFER_HOURS.hours
        else                      -> false
    }

    companion object {
        /** Mirrors backend CANCELLATION_BUFFER_HOURS in services/booking_service.py. */
        const val CANCELLATION_BUFFER_HOURS: Int = 24
    }
}

data class BookingCustomer(
    val id: String,
    val name: String? = null,
    val profilePhotoUrl: String? = null,
)

/** Provider info embedded on a booking as seen from the customer's side. */
data class BookingProvider(
    val id: String,
    val name: String? = null,
    val profilePhotoUrl: String? = null,
    val avgRating: Double = 0.0,
)

data class BookingService(
    val title: String,
    val price: BigDecimal,
    val durationMinutes: Int? = null,
)

/**
 * A before/after job photo the provider attaches while the booking is
 * in_progress or awaiting_confirmation. The image lives at `url`; `kind`
 * disambiguates which slot it fills in the UI.
 */
data class BookingPhoto(
    val id: String,
    val url: String,
    val kind: PhotoKind,
    val uploadedAt: Instant,
)

/** "Before" vs. "after" — maps to the backend's lowercase strings. */
enum class PhotoKind(val apiValue: String) {
    BEFORE("before"),
    AFTER("after");

    companion object {
        fun fromApi(api: String?): PhotoKind? = when (api) {
            "before" -> BEFORE
            "after"  -> AFTER
            else     -> null
        }
    }
}

/**
 * Status the UI cares about. Some values (CONFIRMED, AWAITING_PAYMENT) have
 * no backend equivalent yet — they appear in the JobDetail action bar so the
 * UI can render staged-payment workflows in future versions. The mapper
 * never produces them from current backend responses, so the corresponding
 * action-bar branches stay unreachable until the backend adds them.
 */
enum class BookingStatus {
    PENDING,
    CONFIRMED,
    AWAITING_PAYMENT,
    IN_PROGRESS,
    AWAITING_CONFIRMATION,
    COMPLETED,
    CANCELLED,
    DISPUTED;

    companion object {
        fun fromApi(api: String?): BookingStatus = when (api) {
            "pending"               -> PENDING
            "in_progress"           -> IN_PROGRESS
            "awaiting_confirmation" -> AWAITING_CONFIRMATION
            "completed"             -> COMPLETED
            "cancelled"             -> CANCELLED
            "rejected"              -> CANCELLED
            "disputed"              -> DISPUTED
            else                    -> PENDING
        }
    }
}

// ── Payout ───────────────────────────────────────────────────────────────

/**
 * Server-computed earnings breakdown for one booking. Commission is *never*
 * recalculated on the client — these values come straight from
 * `GET /bookings/{id}/payout`.
 *
 * While escrow is still holding, `isEstimate = true` and the figures are
 * projected at the current platform rate. Once escrow settles (released or
 * refunded), `isEstimate = false` and the numbers reflect the actual money
 * that moved.
 */
data class BookingPayout(
    val bookingId: String,
    val currency: String,
    val grossAmount: BigDecimal,
    val providerPayout: BigDecimal,
    val platformCommission: BigDecimal,
    val commissionRate: BigDecimal,
    val escrowStatus: EscrowStatus,
    val isEstimate: Boolean,
)

/** Lifecycle of the escrow account backing a booking. */
enum class EscrowStatus {
    /** Customer has paid; funds locked, awaiting provider to complete the job. */
    HOLDING,
    /** Both parties confirmed; funds disbursed to provider's wallet. */
    RELEASED,
    /** Booking cancelled/rejected; funds returned to customer. Provider earns 0. */
    REFUNDED,
    /** No escrow row exists yet (rare — basically only for unconfirmed test data). */
    NONE;

    companion object {
        fun fromApi(api: String?): EscrowStatus = when (api) {
            "holding"  -> HOLDING
            "released" -> RELEASED
            "refunded" -> REFUNDED
            else       -> NONE
        }
    }
}