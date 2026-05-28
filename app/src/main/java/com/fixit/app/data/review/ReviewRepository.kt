package com.fixit.app.data.review

import com.fixit.app.domain.model.RatingSummary
import com.fixit.app.domain.model.Review
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

@Singleton
class ReviewRepository @Inject constructor(private val api: ReviewApi) {

    /**
     * Customer-side: create a review for a completed booking.
     * Server enforces booking.status == 'completed', payment paid, one review per booking.
     */
    suspend fun createReview(bookingId: String, rating: Int, comment: String?): Review =
        api.createReview(
            ReviewCreateRequest(
                bookingId = bookingId,
                rating    = rating,
                comment   = comment?.takeIf { it.isNotBlank() },
            )
        ).toDomain()

    suspend fun forProvider(providerId: String): List<Review> =
        api.forProvider(providerId).map { it.toDomain() }

    suspend fun summaryForProvider(providerId: String): RatingSummary =
        api.summaryForProvider(providerId).toDomain()

    /**
     * Returns the review for [bookingId] if one exists, or null when none.
     * The server returns 404 for missing reviews; we map that to null so
     * the caller doesn't have to catch.
     */
    suspend fun forBookingOrNull(bookingId: String): Review? =
        runCatching { api.forBooking(bookingId).toDomain() }.getOrNull()

    private fun ReviewResponse.toDomain(): Review = Review(
        id        = id,
        rating    = rating,
        comment   = comment,
        createdAt = parseInstantSafe(createdAt),
        customerName     = customer?.name,
        customerPhotoUrl = customer?.profilePhotoUrl,
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