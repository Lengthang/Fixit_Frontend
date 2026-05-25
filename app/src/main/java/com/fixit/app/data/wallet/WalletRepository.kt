package com.fixit.app.data.wallet

import com.fixit.app.domain.model.TransactionType
import com.fixit.app.domain.model.WalletTransaction
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

@Singleton
class WalletRepository @Inject constructor(private val api: WalletApi) {

    suspend fun me(): WalletResponse = api.me()

    /** Latest first (backend already orders by created_at DESC). */
    suspend fun transactions(): List<WalletTransaction> =
        api.transactions().map { it.toDomain() }

    private fun WalletTransactionResponse.toDomain(): WalletTransaction =
        WalletTransaction(
            id          = id,
            type        = TransactionType.fromApi(type),
            amount      = BigDecimal.valueOf(amount),
            referenceId = referenceId,
            description = description,
            createdAt   = parseInstantSafe(createdAt),
        )

    private fun parseInstantSafe(iso: String?): Instant =
        iso?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: Clock.System.now()
}