package com.fixit.app.data.review

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ReviewApi {
    /** POST /reviews/ — customer creates a review for a completed, paid booking. */
    @POST("reviews/")
    suspend fun createReview(@Body body: ReviewCreateRequest): ReviewResponse

    /** GET /reviews/providers/{provider_id} — every review left for this provider. */
    @GET("reviews/providers/{providerId}")
    suspend fun forProvider(@Path("providerId") providerId: String): List<ReviewResponse>

    /** GET /reviews/providers/{provider_id}/summary — avg + count. */
    @GET("reviews/providers/{providerId}/summary")
    suspend fun summaryForProvider(@Path("providerId") providerId: String): RatingSummaryResponse

    /** GET /reviews/booking/{booking_id} — single review for a booking (404 if none). */
    @GET("reviews/booking/{bookingId}")
    suspend fun forBooking(@Path("bookingId") bookingId: String): ReviewResponse
}