package com.fixit.app.data.wallet

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WalletResponse(
    val id: String,
    val balance: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class WalletTransactionResponse(
    val id: String,
    val type: String,
    val amount: Double = 0.0,
    @Json(name = "reference_id") val referenceId: String? = null,
    val description: String? = null,
    @Json(name = "created_at") val createdAt: String,
)
/** POST /payments/wallet/top-up body. Mirrors backend WalletTopUpRequest. */
@JsonClass(generateAdapter = true)
data class WalletTopUpRequest(
    val amount: Double,
    @Json(name = "payment_method_id") val paymentMethodId: String,
)