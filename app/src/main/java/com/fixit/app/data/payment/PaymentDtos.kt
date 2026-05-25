package com.fixit.app.data.payment

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Response from POST /payments/confirm/{booking_id}. */
@JsonClass(generateAdapter = true)
data class ConfirmationResponse(
    @Json(name = "booking_id") val bookingId: String,
    @Json(name = "provider_confirmed") val providerConfirmed: Boolean,
    @Json(name = "customer_confirmed") val customerConfirmed: Boolean,
    @Json(name = "provider_confirmed_at") val providerConfirmedAt: String? = null,
    @Json(name = "customer_confirmed_at") val customerConfirmedAt: String? = null,
)