package com.fixit.app.data.category

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CategoryResponse(
    val id: String,
    val name: String,
    @Json(name = "icon_url") val iconUrl: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true,
)