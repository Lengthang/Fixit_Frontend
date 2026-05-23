package com.fixit.app.data.customer

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CustomerUpdateRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @Json(name = "google_id") val googleId: String? = null,
    @Json(name = "profile_photo_url") val profilePhotoUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val phone: String,
    val role: String,
    @Json(name = "is_active") val isActive: Boolean,
)