package com.fixit.app.data.location

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SavedLocationResponse(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    @Json(name = "is_default") val isDefault: Boolean = false,
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class SavedLocationCreateRequest(
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    @Json(name = "is_default") val isDefault: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class SavedLocationUpdateRequest(
    val label: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    @Json(name = "is_default") val isDefault: Boolean? = null,
)