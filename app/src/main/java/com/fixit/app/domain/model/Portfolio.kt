package com.fixit.app.domain.model

import kotlin.time.Instant

/**
 * One before/after pair the provider uploaded to showcase work. Rendered as
 * a tile in the "Recent work" gallery on the customer-facing detail screen;
 * the UI uses [afterPhotoUrl] as the primary thumbnail.
 */
data class PortfolioItem(
    val id: String,
    val providerId: String,
    val beforePhotoUrl: String,
    val afterPhotoUrl: String,
    val caption: String?,
    val createdAt: Instant,
)