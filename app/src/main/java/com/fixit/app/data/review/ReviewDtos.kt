package com.fixit.app.data.review

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReviewResponse(
    val id: String,
    @Json(name = "booking_id") val bookingId: String,
    @Json(name = "customer_id") val customerId: String,
    @Json(name = "provider_id") val providerId: String,
    val rating: Int,
    val comment: String? = null,
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class RatingSummaryResponse(
    @Json(name = "provider_id") val providerId: String,
    @Json(name = "avg_rating") val avgRating: Double = 0.0,
    @Json(name = "total_reviews") val totalReviews: Int = 0,
)