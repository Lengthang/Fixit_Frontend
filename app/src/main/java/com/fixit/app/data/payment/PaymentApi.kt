package com.fixit.app.data.payment

import retrofit2.http.POST
import retrofit2.http.Path

interface PaymentApi {
    /** POST /payments/confirm/{booking_id} — the provider half of dual confirmation. */
    @POST("payments/confirm/{bookingId}")
    suspend fun confirmCompletion(@Path("bookingId") bookingId: String): ConfirmationResponse
}