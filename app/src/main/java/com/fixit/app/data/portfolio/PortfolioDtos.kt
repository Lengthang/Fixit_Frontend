package com.fixit.app.data.portfolio

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PortfolioItemResponse(
    val id: String,
    @Json(name = "provider_id")       val providerId: String,
    @Json(name = "before_photo_url")  val beforePhotoUrl: String,
    @Json(name = "after_photo_url")   val afterPhotoUrl: String,
    val caption: String? = null,
    @Json(name = "created_at")        val createdAt: String,
)