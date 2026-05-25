package com.fixit.app.data.review

import retrofit2.http.GET
import retrofit2.http.Path

interface ReviewApi {
    /** GET /reviews/providers/{provider_id} — every review left for this provider. */
    @GET("reviews/providers/{providerId}")
    suspend fun forProvider(@Path("providerId") providerId: String): List<ReviewResponse>

    /** GET /reviews/providers/{provider_id}/summary — avg + count. */
    @GET("reviews/providers/{providerId}/summary")
    suspend fun summaryForProvider(@Path("providerId") providerId: String): RatingSummaryResponse
}