package com.fixit.app.data.booking

import com.fixit.app.domain.model.Booking
import com.fixit.app.domain.model.BookingCustomer
import com.fixit.app.domain.model.BookingPayout
import com.fixit.app.domain.model.BookingPhoto
import com.fixit.app.domain.model.BookingProvider
import com.fixit.app.domain.model.BookingService
import com.fixit.app.domain.model.BookingStatus
import com.fixit.app.domain.model.EscrowStatus
import com.fixit.app.domain.model.PhotoKind
import java.math.BigDecimal
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * BookingResponse → domain Booking. Safe to call from any layer above the
 * data package — used by every provider screen's VM.
 *
 * Both timestamps fall back to "now" if the backend ever sends garbage;
 * the screens treat them as data anyway, so a sentinel beats a crash.
 */
internal fun BookingResponse.toDomain(): Booking = Booking(
    id           = id,
    status       = BookingStatus.fromApi(status),
    scheduledAt  = parseInstantSafe(scheduledAt),
    createdAt    = parseInstantSafe(createdAt),
    address      = address,
    latitude     = latitude,
    longitude    = longitude,
    notes        = notes,
    subtotal     = BigDecimal.valueOf(subtotal),
    discountAmount = BigDecimal.valueOf(discountAmount),
    totalAmount  = BigDecimal.valueOf(totalAmount),
    currency     = currency,
    distanceKm   = distanceKm,
    customer     = customer?.let {
        BookingCustomer(
            id              = it.id,
            name            = it.name,
            profilePhotoUrl = it.profilePhotoUrl,
        )
    },
    provider     = provider?.let {
        BookingProvider(
            id = it.id,
            name = it.name,
            profilePhotoUrl = it.profilePhotoUrl,
            avgRating = it.avgRating,
        )
    },
    service      = items.firstOrNull()?.let {
        BookingService(
            title           = it.title,
            // Customer-pays uses the booking total_amount so promo discounts
            // and multi-line totals are reflected correctly in summary rows.
            price           = BigDecimal.valueOf(totalAmount),
            durationMinutes = it.durationMinutes,
        )
    },
    photos       = photos.mapNotNull { it.toDomainOrNull() },
)
/**
 * BookingPhotoResponse → domain BookingPhoto. Returns null when the server
 * sends a kind we don't recognise so the UI can't accidentally render an
 * unknown bucket.
 */
internal fun BookingPhotoResponse.toDomainOrNull(): BookingPhoto? {
    val photoKind = PhotoKind.fromApi(kind) ?: return null
    return BookingPhoto(
        id         = id,
        url        = url,
        kind       = photoKind,
        uploadedAt = parseInstantSafe(uploadedAt),
    )
}

/**
 * BookingPayoutResponse → domain BookingPayout.
 * Server-side authoritative: the client never recomputes commission.
 */
internal fun BookingPayoutResponse.toDomain(): BookingPayout = BookingPayout(
    bookingId          = bookingId,
    currency           = currency,
    grossAmount        = BigDecimal.valueOf(grossAmount),
    providerPayout     = BigDecimal.valueOf(providerPayout),
    platformCommission = BigDecimal.valueOf(platformCommission),
    commissionRate     = BigDecimal.valueOf(commissionRate),
    escrowStatus       = EscrowStatus.fromApi(escrowStatus),
    isEstimate         = isEstimate,
)

private fun parseInstantSafe(iso: String?): Instant =
    iso?.takeIf { it.isNotBlank() }
        ?.let { runCatching { Instant.parse(it) }.getOrNull() }
        ?: Clock.System.now()