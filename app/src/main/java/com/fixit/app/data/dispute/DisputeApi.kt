package com.fixit.app.data.dispute

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface DisputeApi {

    /** GET /disputes/provider — disputes filed against this provider's bookings. */
    @GET("disputes/provider")
    suspend fun providerDisputes(): List<DisputeResponse>

    /** GET /disputes/{id} — single dispute; provider gets the full DisputeOut view. */
    @GET("disputes/{id}")
    suspend fun byId(@Path("id") id: String): DisputeResponse

    /** PATCH /disputes/{id}/respond — submit or update the provider's rebuttal. */
    @PATCH("disputes/{id}/respond")
    suspend fun respond(
        @Path("id") id: String,
        @Body body: DisputeRespondRequest,
    ): DisputeResponse
}