package com.fixit.app.data.provider

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AvailabilityInput(
    @Json(name = "day_of_week") val dayOfWeek: String,
    @Json(name = "open_time") val openTime: String,
    @Json(name = "close_time") val closeTime: String,
)

@JsonClass(generateAdapter = true)
data class ProviderRegisterRequest(
    val bio: String? = null,
    @Json(name = "years_experience") val yearsExperience: Int = 0,
    val certification: String? = null,
    @Json(name = "certification_url") val certificationUrl: String? = null,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    @Json(name = "service_radius_km") val serviceRadiusKm: Int = 10,
    @Json(name = "category_ids") val categoryIds: List<String> = emptyList(),
    val availability: List<AvailabilityInput>,
)

@JsonClass(generateAdapter = true)
data class ProviderResponse(
    val id: String,
    @Json(name = "user_id") val userId: String,
    val bio: String? = null,
    @Json(name = "profile_photo_url") val profilePhotoUrl: String? = null,
    @Json(name = "years_experience") val yearsExperience: Int = 0,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "service_radius_km") val serviceRadiusKm: Int = 10,
    @Json(name = "avg_rating") val avgRating: Double = 0.0,
    @Json(name = "is_available") val isAvailable: Boolean = true,
    val status: String = "pending",
)