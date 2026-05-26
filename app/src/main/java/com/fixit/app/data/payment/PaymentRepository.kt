package com.fixit.app.data.payment

import com.fixit.app.domain.model.PaymentMethodType
import com.fixit.app.domain.model.SavedPaymentMethod
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(private val api: PaymentApi) {

    // ── Booking confirmation ─────────────────────────────────────────────────
    suspend fun confirmCompletion(bookingId: String): ConfirmationResponse =
        api.confirmCompletion(bookingId)

    // ── Saved payment methods ────────────────────────────────────────────────
    suspend fun listMethods(): List<SavedPaymentMethod> =
        api.listMethods().map { it.toDomain() }

    suspend fun addMethod(
        type: String,
        displayName: String,
        lastFour: String?,
        isDefault: Boolean,
    ): SavedPaymentMethod =
        api.addMethod(
            SavedPaymentMethodCreateDto(
                type = type,
                displayName = displayName,
                lastFour = lastFour?.takeIf { it.isNotBlank() },
                isDefault = isDefault,
            )
        ).toDomain()

    /**
     * Promotes [methodId] to the user's default payout method.
     * The backend atomically demotes all other methods, so the caller only needs
     * to apply a local optimistic update for the returned method.
     */
    suspend fun setDefault(methodId: String): SavedPaymentMethod =
        api.setDefault(methodId, SetDefaultDto(isDefault = true)).toDomain()

    suspend fun deleteMethod(methodId: String) =
        api.deleteMethod(methodId)

    // ── Withdrawals ──────────────────────────────────────────────────────────
    suspend fun withdraw(
        amount: BigDecimal,
        methodId: String,
    ): WithdrawalResponseDto =
        api.withdraw(
            WithdrawalCreateDto(
                amount = amount.toDouble(),
                paymentMethodId = methodId,
            )
        )

    // ── Mappers ──────────────────────────────────────────────────────────────
    private fun SavedPaymentMethodDto.toDomain() = SavedPaymentMethod(
        id          = id,
        type        = PaymentMethodType.from(type),
        displayName = displayName,
        lastFour    = lastFour,
        isDefault   = isDefault,
    )
}