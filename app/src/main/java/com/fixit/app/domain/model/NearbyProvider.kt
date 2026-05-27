package com.fixit.app.domain.model

import java.math.BigDecimal

/**
 * Customer-facing row for the "Provider near you" list on Customer Home.
 * Mapped from ProviderListItemResponse — keep this lean; full provider
 * details come from a separate fetch on the detail screen.
 */
data class NearbyProvider(
    val id: String,
    val userId: String,
    val name: String,
    val profilePhotoUrl: String?,
    val location: String?,
    val avgRating: Double,
    val reviewCount: Int,
    val distanceKm: Double?,
    val yearsExperience: Int,
    val minPrice: BigDecimal?,
    /** First category name — used as the "Plumber" / "Electrician" subtitle. */
    val primaryCategoryName: String?,
) {
    /**
     * TOP PRO derivation (client-side per implementation decision):
     * highly rated AND has enough reviews to be statistically meaningful.
     */
    val isTopPro: Boolean
        get() = avgRating >= 4.8 && reviewCount >= 10
}