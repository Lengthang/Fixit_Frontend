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
 * Subset of the backend's BookingResponse — the home screen only needs
 * these fields. Moshi ignores unknown JSON keys, so omitting
 * `status_history` etc. is safe.
 */
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
)