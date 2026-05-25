package com.fixit.app.domain.model

/** Mirrors `core.enums.ProviderStatus` on the backend. */
enum class ProviderStatus {
    APPROVED, PENDING, REJECTED;

    companion object {
        fun fromApi(api: String?): ProviderStatus? = when (api?.lowercase()) {
            "approved" -> APPROVED
            "pending"  -> PENDING
            "rejected" -> REJECTED
            else       -> null
        }
    }
}

/** Lightweight "who am I" shape for the profile screen. */
data class ProfileUser(
    val id: String,
    val name: String?,
    val phone: String,
    val profilePhotoUrl: String?,
    val providerStatus: ProviderStatus?,
)

/** Aggregated rating, separate from individual reviews. */
data class RatingSummary(
    val avgRating: Double,
    val totalReviews: Int,
)