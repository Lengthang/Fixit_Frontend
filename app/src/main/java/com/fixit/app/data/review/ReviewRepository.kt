package com.fixit.app.data.review

import com.fixit.app.domain.model.RatingSummary
import com.fixit.app.domain.model.Review
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

@Singleton
class ReviewRepository @Inject constructor(private val api: ReviewApi) {

    suspend fun forProvider(providerId: String): List<Review> =
        api.forProvider(providerId).map { it.toDomain() }

    suspend fun summaryForProvider(providerId: String): RatingSummary =
        api.summaryForProvider(providerId).toDomain()

    private fun ReviewResponse.toDomain(): Review = Review(
        id        = id,
        rating    = rating,
        comment   = comment,
        createdAt = parseInstantSafe(createdAt),
        // ReviewOut doesn't include customer name / photo today.
        // Will start populating once the backend joins users → reviews.
        customerName     = null,
        customerPhotoUrl = null,
    )

    private fun RatingSummaryResponse.toDomain(): RatingSummary = RatingSummary(
        avgRating    = avgRating,
        totalReviews = totalReviews,
    )

    private fun parseInstantSafe(iso: String?): Instant =
        iso?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: Clock.System.now()
}