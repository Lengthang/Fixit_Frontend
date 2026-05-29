package com.fixit.app.data.provider
import com.fixit.app.data.service.ServiceResponse
import com.fixit.app.domain.model.AvailabilityWindow
import com.fixit.app.domain.model.CategoryRef
import com.fixit.app.domain.model.NearbyProvider
import com.fixit.app.domain.model.ProviderDetail
import com.fixit.app.domain.model.ProviderStatus
import com.fixit.app.domain.model.Service
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
        categoryId: String? = null,
        customerLat: Double?,
        customerLng: Double?,
        limit: Int = 20,
    ): List<NearbyProvider> =
        api.list(
            categoryId = categoryId,
            customerLat = customerLat,
            customerLng = customerLng,
            limit = limit,
            offset = 0,
        ).map { it.toDomain() }
    suspend fun byId(
        providerId: String,
        customerLat: Double? = null,
        customerLng: Double? = null,
    ): ProviderDetail =
        api.byId(providerId, customerLat, customerLng).toDomain()

    private fun ProviderDetailResponse.toDomain(): ProviderDetail {
        // Build the category id → name lookup once so we can denormalise it
        // onto each Service in the same pass MyServicesViewModel does.
        val categoryRefs = categories.map { CategoryRef(it.id, it.name) }
        val categoryNameById = categoryRefs.associate { it.id to it.name }

        return ProviderDetail(
            id = id,
            userId = userId,
            name = name,
            bio = bio,
            profilePhotoUrl = profilePhotoUrl,
            yearsExperience = yearsExperience,
            certification = certification,
            certificationUrl = certificationUrl,
            location = location,
            latitude = latitude,
            longitude = longitude,
            serviceRadiusKm = serviceRadiusKm,
            avgRating = avgRating,
            isAvailable = isAvailable,
            status = ProviderStatus.fromApi(status),
            isVerified = isVerified,
            distanceKm = distanceKm,
            totalJobsCompleted = totalJobsCompleted,
            categories = categoryRefs,
            services = services.map { it.toDomainService(categoryNameById) },
            availability = availability.map {
                AvailabilityWindow(
                    id = it.id,
                    dayOfWeek = it.dayOfWeek,
                    openTime = it.openTime,
                    closeTime = it.closeTime,
                )
            },
        )
    }

    /**
     * ServiceResponse → domain Service. Same shape MyServicesViewModel and
     * ServiceDetailViewModel use, minus the booking-count field which only
     * the signed-in provider has the data to compute.
     */
    private fun ServiceResponse.toDomainService(
        categoryNameById: Map<String, String>,
    ): Service = Service(
        id              = id,
        title           = title,
        description     = description,
        imageUrl        = imageUrl,
        price           = BigDecimal.valueOf(price),
        durationMinutes = durationMinutes,
        minQuantity     = minQuantity,
        isActive        = isActive,
        categoryId      = categoryId,
        categoryName    = categoryId?.let { categoryNameById[it] },
        updatedAt       = updatedAt,
        bookingCount    = 0,
    )
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
    isVerified = isVerified,
)
