package com.fixit.app.domain.model

import kotlin.time.Instant


/**
 * Customer-facing fields (`customerName`, `customerPhotoUrl`) aren't returned
 * by `GET /reviews/providers/{id}` yet, so they're nullable. The UI handles
 * null gracefully ("Customer" fallback). When the backend grows a denormalised
 * customer summary on ReviewOut, the mapper will start populating these.
 */
data class Review(
    val id: String,
    val rating: Int,
    val comment: String?,
    val createdAt: Instant,
    val customerName: String? = null,
    val customerPhotoUrl: String? = null,
)