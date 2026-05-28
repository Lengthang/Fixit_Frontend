package com.fixit.app.data.review

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReviewCustomerSummary(
    val id: String,
    val name: String? = null,
    @Json(name = "profile_photo_url") val profilePhotoUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class ReviewResponse(
    val id: String,
    @Json(name = "booking_id") val bookingId: String,
    @Json(name = "customer_id") val customerId: String,
    @Json(name = "provider_id") val providerId: String,
    val rating: Int,
    val comment: String? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "customer_name")       val customerName: String? = null,
    @Json(name = "customer_photo_url")  val customerPhotoUrl: String? = null,
    val customer: ReviewCustomerSummary? = null,
)

@JsonClass(generateAdapter = true)
data class RatingSummaryResponse(
    @Json(name = "provider_id") val providerId: String,
    @Json(name = "avg_rating") val avgRating: Double = 0.0,
    @Json(name = "total_reviews") val totalReviews: Int = 0,
)

/** Body for POST /reviews/ */
@JsonClass(generateAdapter = true)
data class ReviewCreateRequest(
    @Json(name = "booking_id") val bookingId: String,
    val rating: Int,
    val comment: String? = null,
)