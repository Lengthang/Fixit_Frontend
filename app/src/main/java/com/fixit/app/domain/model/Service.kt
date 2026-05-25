package com.fixit.app.domain.model

import java.math.BigDecimal

data class Service(
    val id: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val price: BigDecimal,
    val durationMinutes: Int?,
    val minQuantity: Int,
    val isActive: Boolean,
    val categoryId: String?,
    /** Denormalised from the provider's category list at mapping time. */
    val categoryName: String?,
    /** ISO-8601 string from the backend — rendered as-is in the UI. */
    val updatedAt: String?,
    /** Client-side count derived from GET /bookings/provider items. */
    val bookingCount: Int = 0,
) {
    /** Human-readable duration: "45 min" / "1 hr" / "1 hr 30 min" / null. */
    val durationLabel: String?
        get() = durationMinutes?.let { mins ->
            val h = mins / 60
            val m = mins % 60
            when {
                h == 0 -> "$m min"
                m == 0 -> "$h hr"
                else   -> "$h hr $m min"
            }
        }
}