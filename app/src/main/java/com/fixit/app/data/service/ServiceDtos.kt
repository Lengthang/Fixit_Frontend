package com.fixit.app.data.service

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ServiceResponse(
    val id: String,
    @Json(name = "provider_id")      val providerId: String,
    @Json(name = "category_id")      val categoryId: String? = null,
    val title: String,
    val description: String? = null,
    @Json(name = "image_url")        val imageUrl: String? = null,
    val price: Double,
    @Json(name = "duration_minutes") val durationMinutes: Int? = null,
    @Json(name = "min_quantity")     val minQuantity: Int = 1,
    @Json(name = "is_active")        val isActive: Boolean,
    @Json(name = "created_at")       val createdAt: String,
    @Json(name = "updated_at")       val updatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class ServiceCreateRequest(
    @Json(name = "category_id")      val categoryId: String,
    val title: String,
    val description: String? = null,
    @Json(name = "image_url")        val imageUrl: String? = null,
    val price: Double,
    @Json(name = "duration_minutes") val durationMinutes: Int? = null,
    @Json(name = "min_quantity")     val minQuantity: Int = 1,
)

/**
 * All fields nullable — the backend PATCH handler only applies non-null values.
 * Sending null for a field means "leave it unchanged".
 */
@JsonClass(generateAdapter = true)
data class ServiceUpdateRequest(
    @Json(name = "category_id")      val categoryId: String? = null,
    val title: String? = null,
    val description: String? = null,
    @Json(name = "image_url")        val imageUrl: String? = null,
    val price: Double? = null,
    @Json(name = "duration_minutes") val durationMinutes: Int? = null,
    @Json(name = "min_quantity")     val minQuantity: Int? = null,
    @Json(name = "is_active")        val isActive: Boolean? = null,
)