package com.fixit.app.domain.model

import kotlin.time.Instant

/**
 * A service location a customer has saved for reuse when booking.
 * Mirrors `schemas.location.SavedLocationResponse` on the backend.
 *
 * The booking flow copies these coordinates onto the booking row, so editing
 * or deleting a saved location never affects bookings already made from it.
 */
data class SavedLocation(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val isDefault: Boolean,
    val createdAt: Instant,
)