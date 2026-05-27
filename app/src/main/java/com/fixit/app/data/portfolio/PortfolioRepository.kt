package com.fixit.app.data.portfolio

import com.fixit.app.domain.model.PortfolioItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

@Singleton
class PortfolioRepository @Inject constructor(private val api: PortfolioApi) {

    suspend fun forProvider(providerId: String): List<PortfolioItem> =
        api.forProvider(providerId).map { it.toDomain() }

    private fun PortfolioItemResponse.toDomain(): PortfolioItem = PortfolioItem(
        id              = id,
        providerId      = providerId,
        beforePhotoUrl  = beforePhotoUrl,
        afterPhotoUrl   = afterPhotoUrl,
        caption         = caption,
        createdAt       = parseInstantSafe(createdAt),
    )

    private fun parseInstantSafe(iso: String?): Instant =
        iso?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: Clock.System.now()
}