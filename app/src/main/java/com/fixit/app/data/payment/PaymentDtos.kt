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

/** GET/POST/PATCH /payments/methods */
@JsonClass(generateAdapter = true)
data class SavedPaymentMethodDto(
    val id: String,
    val type: String,                                     // "bank" | "card"
    @Json(name = "display_name") val displayName: String,
    @Json(name = "last_four") val lastFour: String? = null,
    @Json(name = "is_default") val isDefault: Boolean = false,
)

/** POST /payments/methods body */
@JsonClass(generateAdapter = true)
data class SavedPaymentMethodCreateDto(
    val type: String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "last_four") val lastFour: String? = null,
    @Json(name = "is_default") val isDefault: Boolean = false,
)

/** PATCH /payments/methods/{id} body */
@JsonClass(generateAdapter = true)
data class SetDefaultDto(
    @Json(name = "is_default") val isDefault: Boolean = true,
)

/** POST /payments/withdraw body */
@JsonClass(generateAdapter = true)
data class WithdrawalCreateDto(
    val amount: Double,
    @Json(name = "payment_method_id") val paymentMethodId: String,
)

/** POST /payments/withdraw response */
@JsonClass(generateAdapter = true)
data class WithdrawalResponseDto(
    val id: String,
    val amount: Double,
    val status: String,
    @Json(name = "created_at") val createdAt: String,
)