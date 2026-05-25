package com.fixit.app.data.booking

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface BookingApi {
    /** GET /bookings/provider — every booking assigned to the signed-in provider. */
    @GET("bookings/provider")
    suspend fun providerBookings(): List<BookingResponse>

    /** GET /bookings/{id} — single booking detail. */
    @GET("bookings/{id}")
    suspend fun byId(@Path("id") id: String): BookingResponse

    /** PATCH /bookings/{id}/status — moves the booking through its state machine. */
    @PATCH("bookings/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: String,
        @Body body: BookingStatusUpdate,
    ): BookingResponse

    /**
     * GET /bookings/{id}/payout — server-computed earnings breakdown.
     * Provider-only; admins can also call this. Returns 403 otherwise.
     */
    @GET("bookings/{id}/payout")
    suspend fun payoutFor(@Path("id") id: String): BookingPayoutResponse
}