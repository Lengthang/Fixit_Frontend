package com.fixit.app.data.promo

import com.fixit.app.domain.model.ActivePromo
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromoRepository @Inject constructor(private val api: PromoApi) {
    suspend fun active(): List<ActivePromo> = api.active().map { it.toDomain() }
}

private fun ActivePromoResponse.toDomain(): ActivePromo = ActivePromo(
    id = id,
    code = code,
    discountPercentage = runCatching { BigDecimal(discountPercentage) }.getOrDefault(BigDecimal.ZERO),
    expiresAtIso = expiresAt,
)