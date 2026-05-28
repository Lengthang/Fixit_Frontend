package com.fixit.app.data.booking

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BookingCustomerSummary(
    val id: String,
    val name: String? = null,
    @Json(name = "profile_photo_url") val profilePhotoUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class BookingProviderUserSummary(
    val id: String,
    val name: String? = null,
    @Json(name = "profile_photo_url") val profilePhotoUrl: String? = null,
    @Json(name = "avg_rating") val avgRating: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class BookingItemResponse(
    @Json(name = "service_id") val serviceId: String,
    val title: String,
    @Json(name = "image_url") val imageUrl: String? = null,
    val quantity: Int = 1,
    @Json(name = "unit_price") val unitPrice: Double = 0.0,
    @Json(name = "line_total") val lineTotal: Double = 0.0,
    @Json(name = "duration_minutes") val durationMinutes: Int? = null,
)

/**
 * One before/after job photo. Server-side this lives on a separate
 * `booking_photos` row, but the BookingResponse embeds the list so the
 * screen can render the gallery without a second round-trip.
 *
 * `kind` is the server's "before" or "after" string — mapped to the
 * PhotoKind enum at the domain layer.
 */
@JsonClass(generateAdapter = true)
data class BookingPhotoResponse(
    val id: String,
    val url: String,
    val kind: String,
    @Json(name = "uploaded_at") val uploadedAt: String,
)

/** Body for POST /bookings/{id}/photos. */
@JsonClass(generateAdapter = true)
data class BookingPhotoCreate(
    val url: String,
    val kind: String,  // "before" | "after"
)

@JsonClass(generateAdapter = true)
data class BookingResponse(
    val id: String,
    @Json(name = "customer_id") val customerId: String,
    @Json(name = "provider_id") val providerId: String,
    val status: String,
    @Json(name = "scheduled_at") val scheduledAt: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String? = null,
    @Json(name = "created_at") val createdAt: String,
    val subtotal: Double = 0.0,
    @Json(name = "discount_amount") val discountAmount: Double = 0.0,
    @Json(name = "total_amount") val totalAmount: Double = 0.0,
    val currency: String = "SGD",
    @Json(name = "distance_km") val distanceKm: Double? = null,
    val customer: BookingCustomerSummary? = null,
    val provider: BookingProviderUserSummary? = null,
    val items: List<BookingItemResponse> = emptyList(),
    val photos: List<BookingPhotoResponse> = emptyList(),
    @Json(name = "has_review") val hasReview: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class BookingStatusUpdate(
    val status: String,
)

/**
 * Response from `GET /bookings/{id}/payout`.
 *
 * All monetary fields come over the wire as JSON numbers (FastAPI's default
 * Decimal encoder converts to float). We widen to BigDecimal at the mapper
 * layer to avoid Double precision creep when amounts are summed.
 */
@JsonClass(generateAdapter = true)
data class BookingPayoutResponse(
    @Json(name = "booking_id") val bookingId: String,
    val currency: String = "SGD",
    @Json(name = "gross_amount") val grossAmount: Double = 0.0,
    @Json(name = "provider_payout") val providerPayout: Double = 0.0,
    @Json(name = "platform_commission") val platformCommission: Double = 0.0,
    @Json(name = "commission_rate") val commissionRate: Double = 0.0,
    @Json(name = "escrow_status") val escrowStatus: String,
    @Json(name = "is_estimate") val isEstimate: Boolean,
)