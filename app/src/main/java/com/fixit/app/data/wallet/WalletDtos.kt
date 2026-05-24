package com.fixit.app.data.wallet

import com.squareup.moshi.JsonClass

/**
 * Backend returns Decimal as JSON number via FastAPI's jsonable_encoder
 * (see `decimal_encoder` in fastapi.encoders). Keep it as Double here and
 * widen to BigDecimal at the VM layer for display.
 */
@JsonClass(generateAdapter = true)
data class WalletResponse(
    val id: String,
    val balance: Double = 0.0,
)