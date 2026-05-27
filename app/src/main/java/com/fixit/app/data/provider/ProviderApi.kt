package com.fixit.app.data.provider

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
interface ProviderApi {
    @POST("providers/register")
    suspend fun register(@Body body: ProviderRegisterRequest): ProviderResponse

    @GET("providers/me")
    suspend fun me(): ProviderResponse

    /** Partial update — only non-null fields in [body] are applied server-side. */
    @PATCH("providers/me")
    suspend fun updateMe(@Body body: ProviderUpdateRequest): ProviderResponse
    @GET("providers")
    suspend fun list(
        @Query("category_id")  categoryId: String? = null,
        @Query("customer_lat") customerLat: Double? = null,
        @Query("customer_lng") customerLng: Double? = null,
        @Query("limit")        limit: Int = 20,
        @Query("offset")       offset: Int = 0,
    ): List<ProviderListItemResponse>
}