package com.fixit.app.data.provider
import com.fixit.app.domain.model.NearbyProvider
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepository @Inject constructor(private val api: ProviderApi) {
    suspend fun register(body: ProviderRegisterRequest): ProviderResponse = api.register(body)
    suspend fun me(): ProviderResponse = api.me()

    suspend fun updateProfile(body: ProviderUpdateRequest): ProviderResponse =
        api.updateMe(body)

    suspend fun list(
        categoryId: String? = null,
        customerLat: Double? = null,
        customerLng: Double? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): List<ProviderListItemResponse> =
        api.list(categoryId, customerLat, customerLng, limit, offset)

    /** Mapped to domain — what the Customer Home Screen uses. */
    suspend fun listNearby(
        customerLat: Double?,
        customerLng: Double?,
        limit: Int = 20,
    ): List<NearbyProvider> =
        api.list(
            categoryId = null,
            customerLat = customerLat,
            customerLng = customerLng,
            limit = limit,
            offset = 0,
        ).map { it.toDomain() }
}
private fun ProviderListItemResponse.toDomain(): NearbyProvider = NearbyProvider(
    id = id,
    userId = userId,
    name = name.orEmpty(),
    profilePhotoUrl = profilePhotoUrl,
    location = location,
    avgRating = avgRating,
    reviewCount = reviewCount,
    distanceKm = distanceKm,
    yearsExperience = yearsExperience,
    minPrice = minPrice?.let { runCatching { BigDecimal(it) }.getOrNull() },
    primaryCategoryName = categories.firstOrNull()?.name,
)