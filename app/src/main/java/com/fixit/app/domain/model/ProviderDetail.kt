package com.fixit.app.domain.model

import java.math.BigDecimal

/**
 * Lightweight reference to a category — just what the UI needs to render
 * tag chips and identify which categories a provider works in. Created here
 * instead of in its own file because no other screen needs more than this
 * shape; promote to /domain/model/Category.kt if a category list screen
 * appears later.
 */
data class CategoryRef(
    val id: String,
    val name: String,
)

/** One row from the backend's `availability[]` array. */
data class AvailabilityWindow(
    val id: String,
    val dayOfWeek: String,
    /** Backend sends "HH:MM:SS"; rendered as-is by current UI. */
    val openTime: String,
    val closeTime: String,
)

/**
 * Full public profile for one provider, the shape the customer-facing detail
 * screen actually needs. Assembled by the mapper in ProviderRepository from
 * the backend's `GET /providers/{id}` response. Distance is null when no
 * customer lat/lng was supplied.
 */
data class ProviderDetail(
    val id: String,
    val userId: String,
    val name: String?,
    val bio: String?,
    val profilePhotoUrl: String?,
    val yearsExperience: Int,
    val certification: String?,
    val certificationUrl: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val serviceRadiusKm: Int,
    val avgRating: Double,
    val isAvailable: Boolean,
    val status: ProviderStatus?,
    val distanceKm: Double?,
    val totalJobsCompleted: Int,
    val categories: List<CategoryRef>,
    val services: List<Service>,
    val availability: List<AvailabilityWindow>,
) {
    /** Active services sorted by ascending price — used to pick the "from" price. */
    val activeServicesByPriceAsc: List<Service>
        get() = services.filter { it.isActive }.sortedBy { it.price }

    /** Lowest active-service price, for the "FROM $XX" footer label. */
    val lowestPrice: BigDecimal?
        get() = activeServicesByPriceAsc.firstOrNull()?.price

    /**
     * "Top Pro" is a derived badge: highly rated AND has enough reviews to
     * have earned it. Thresholds picked to match the screen's marketing
     * promise — adjust together with the badge label if tuning is needed.
     */
    fun isTopPro(totalReviews: Int): Boolean =
        avgRating >= 4.8 && totalReviews >= 50

    /** True iff customer can see them online right now. */
    val isOnline: Boolean
        get() = isAvailable && status == ProviderStatus.APPROVED
}