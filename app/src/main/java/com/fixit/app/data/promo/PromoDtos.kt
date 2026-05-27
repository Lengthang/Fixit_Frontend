package com.fixit.app.data.promo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ActivePromoResponse(
    val id: String,
    val code: String,
    @Json(name = "discount_percentage") val discountPercentage: String,
    @Json(name = "expires_at")          val expiresAt: String? = null,
)