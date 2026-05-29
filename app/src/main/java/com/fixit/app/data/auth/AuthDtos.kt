package com.fixit.app.data.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SendOtpRequest(val phone: String)

@JsonClass(generateAdapter = true)
data class VerifyOtpRequest(val phone: String, val code: String, val name: String? = null)

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String = "bearer",
    @Json(name = "is_new_user") val isNewUser: Boolean,
    @Json(name = "role") val role: String? = null,
    @Json(name = "provider_profile") val providerProfile: ProviderProfileMini? = null,
)

@JsonClass(generateAdapter = true)
data class ProviderProfileMini(
    val id: String,
    val status: String,
    @Json(name = "is_available") val isAvailable: Boolean,
)