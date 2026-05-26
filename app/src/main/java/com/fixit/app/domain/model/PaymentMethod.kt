package com.fixit.app.domain.model

data class SavedPaymentMethod(
    val id: String,
    val type: PaymentMethodType,
    val displayName: String,
    val lastFour: String?,
    val isDefault: Boolean,
) {
    /** Short label shown in tiles, e.g. "Chase Bank ••4829" or "Visa ••7124". */
    val shortLabel: String
        get() = if (lastFour != null) "$displayName ••$lastFour" else displayName
}

enum class PaymentMethodType {
    BANK, CARD;

    companion object {
        fun from(api: String?): PaymentMethodType = when (api) {
            "bank" -> BANK
            else   -> CARD
        }
    }
}