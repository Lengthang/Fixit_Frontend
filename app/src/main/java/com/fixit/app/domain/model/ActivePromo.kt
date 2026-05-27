package com.fixit.app.domain.model

import java.math.BigDecimal

data class ActivePromo(
    val id: String,
    val code: String,
    /** 0–100 — display as "{n}% off". */
    val discountPercentage: BigDecimal,
    val expiresAtIso: String?,
) {
    /** "25" for 25%. Drops trailing zeros so 25.00 → "25". */
    val percentLabel: String
        get() = discountPercentage.stripTrailingZeros().toPlainString()
}