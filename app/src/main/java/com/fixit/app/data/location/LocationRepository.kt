package com.fixit.app.data.location

import com.fixit.app.domain.model.SavedLocation
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

@Singleton
class LocationRepository @Inject constructor(private val api: LocationApi) {

    suspend fun list(): List<SavedLocation> = api.list().map { it.toDomain() }

    suspend fun get(id: String): SavedLocation = api.get(id).toDomain()

    suspend fun create(
        label: String,
        latitude: Double,
        longitude: Double,
        address: String? = null,
        isDefault: Boolean = false,
    ): SavedLocation = api.create(
        SavedLocationCreateRequest(
            label = label,
            latitude = latitude,
            longitude = longitude,
            address = address,
            isDefault = isDefault,
        )
    ).toDomain()

    suspend fun update(
        id: String,
        label: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        address: String? = null,
        isDefault: Boolean? = null,
    ): SavedLocation = api.update(
        id,
        SavedLocationUpdateRequest(
            label = label,
            latitude = latitude,
            longitude = longitude,
            address = address,
            isDefault = isDefault,
        )
    ).toDomain()

    suspend fun delete(id: String) = api.delete(id)

    private fun SavedLocationResponse.toDomain(): SavedLocation = SavedLocation(
        id = id,
        label = label,
        latitude = latitude,
        longitude = longitude,
        address = address,
        isDefault = isDefault,
        createdAt = parseInstantSafe(createdAt),
    )

    private fun parseInstantSafe(iso: String?): Instant =
        iso?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: Clock.System.now()
}