package com.fixit.app.data.upload

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UploadImageResponse(
    val url: String,
    val path: String,
)