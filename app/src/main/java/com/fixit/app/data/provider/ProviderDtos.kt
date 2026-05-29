package com.fixit.app.data.provider

import com.fixit.app.data.category.CategoryResponse
import com.fixit.app.data.service.ServiceResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AvailabilityInput(
    @Json(name = "day_of_week") val dayOfWeek: String,
    @Json(name = "open_time")   val openTime: String,
    @Json(name = "close_time")  val closeTime: String,
)

/**
 * Returned with the provider profile. Backend serialises Python `time` as
 * "HH:MM:SS"; the screen strips seconds before display.
 */
@JsonClass(generateAdapter = true)
data class AvailabilityResponse(
    val id: String,
    @Json(name = "day_of_week") val dayOfWeek: String,
    @Json(name = "open_time")   val openTime: String,
    @Json(name = "close_time")  val closeTime: String,
)

@JsonClass(generateAdapter = true)
data class ProviderRegisterRequest(
    val bio: String? = null,
    @Json(name = "years_experience")  val yearsExperience: Int = 0,
    val certification: String? = null,
    @Json(name = "certification_url") val certificationUrl: String? = null,
    @Json(name = "national_id_url")   val nationalIdUrl: String? = null,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    @Json(name = "service_radius_km") val serviceRadiusKm: Int = 10,
    @Json(name = "category_ids")      val categoryIds: List<String> = emptyList(),
    val availability: List<AvailabilityInput>,
)

/**
 * PATCH /providers/me — every field optional. Only non-null fields are
 * applied by the backend, so partial updates are safe.
 */
@JsonClass(generateAdapter = true)
data class ProviderUpdateRequest(
    val bio: String? = null,
    @Json(name = "profile_photo_url") val profilePhotoUrl: String? = null,
    @Json(name = "years_experience")  val yearsExperience: Int? = null,
    val certification: String? = null,
    @Json(name = "certification_url") val certificationUrl: String? = null,
    @Json(name = "national_id_url")   val nationalIdUrl: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "service_radius_km") val serviceRadiusKm: Int? = null,
    @Json(name = "is_available")      val isAvailable: Boolean? = null,
    @Json(name = "category_ids")      val categoryIds: List<String>? = null,
    val availability: List<AvailabilityInput>? = null,
)

@JsonClass(generateAdapter = true)
data class ProviderResponse(
    val id: String,
    @Json(name = "user_id")           val userId: String,
    val bio: String? = null,
    @Json(name = "profile_photo_url") val profilePhotoUrl: String? = null,
    @Json(name = "years_experience")  val yearsExperience: Int = 0,
    val certification: String? = null,
    @Json(name = "certification_url") val certificationUrl: String? = null,
    @Json(name = "national_id_url")   val nationalIdUrl: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "service_radius_km") val serviceRadiusKm: Int = 10,
    @Json(name = "avg_rating")        val avgRating: Double = 0.0,
    @Json(name = "is_available")      val isAvailable: Boolean = true,
    val status: String = "pending",
    val categories: List<CategoryResponse> = emptyList(),
    val availability: List<AvailabilityResponse> = emptyList(),
)
@JsonClass(generateAdapter = true)
data class ProviderListItemResponse(
    val id: String,
    @Json(name = "user_id")           val userId: String,
    val name: String? = null,
    val bio: String? = null,
    @Json(name = "profile_photo_url") val profilePhotoUrl: String? = null,
    val location: String? = null,
    @Json(name = "avg_rating")        val avgRating: Double = 0.0,
    @Json(name = "is_available")      val isAvailable: Boolean = true,
    @Json(name = "service_radius_km") val serviceRadiusKm: Int = 10,
    @Json(name = "distance_km")       val distanceKm: Double? = null,
    val categories: List<CategoryResponse> = emptyList(),
    @Json(name = "years_experience")  val yearsExperience: Int = 0,
    @Json(name = "min_price")         val minPrice: String? = null, // Decimal serialised as string
    @Json(name = "review_count")      val reviewCount: Int = 0,
    @Json(name = "is_verified")       val isVerified: Boolean = false,
)
@JsonClass(generateAdapter = true)
data class ProviderDetailResponse(
    val id: String,
    @Json(name = "user_id")             val userId: String,
    val name: String? = null,
    val bio: String? = null,
    @Json(name = "profile_photo_url")   val profilePhotoUrl: String? = null,
    @Json(name = "years_experience")    val yearsExperience: Int = 0,
    val certification: String? = null,
    @Json(name = "certification_url")   val certificationUrl: String? = null,
    @Json(name = "national_id_url")     val nationalIdUrl: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "service_radius_km")   val serviceRadiusKm: Int = 10,
    @Json(name = "avg_rating")          val avgRating: Double = 0.0,
    @Json(name = "is_available")        val isAvailable: Boolean = true,
    val status: String = "pending",
    @Json(name = "is_verified")         val isVerified: Boolean = false,
    @Json(name = "created_at")          val createdAt: String? = null,
    @Json(name = "distance_km")         val distanceKm: Double? = null,
    @Json(name = "total_jobs_completed") val totalJobsCompleted: Int = 0,
    val categories: List<CategoryResponse> = emptyList(),
    val availability: List<AvailabilityResponse> = emptyList(),
    val services: List<ServiceResponse> = emptyList(),
)