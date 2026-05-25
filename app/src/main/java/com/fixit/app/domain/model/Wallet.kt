package com.fixit.app.domain.model


import java.math.BigDecimal
import kotlin.time.Instant

data class WalletTransaction(
    val id: String,
    val type: TransactionType,
    val amount: BigDecimal,
    val referenceId: String? = null,
    val description: String? = null,
    val createdAt: Instant,
)

/**
 * Backend `type` strings: top_up, escrow_hold, escrow_release, withdrawal, refund.
 * Mapped onto a smaller UI vocabulary — escrow_hold is collapsed into PAYMENT
 * (money leaving the customer's wallet) and escrow_release into RELEASE
 * (money landing in the provider's wallet from a completed job).
 */
enum class TransactionType {
    TOP_UP, PAYMENT, PAYOUT, REFUND, RELEASE, UNKNOWN;

    companion object {
        fun fromApi(api: String?): TransactionType = when (api) {
            "top_up"          -> TOP_UP
            "escrow_hold"     -> PAYMENT
            "escrow_release"  -> RELEASE
            "withdrawal"      -> PAYOUT
            "refund"          -> REFUND
            else              -> UNKNOWN
        }
    }
}