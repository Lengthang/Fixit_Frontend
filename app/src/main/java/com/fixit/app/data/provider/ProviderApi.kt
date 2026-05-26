package com.fixit.app.data.provider

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface ProviderApi {
    @POST("providers/register")
    suspend fun register(@Body body: ProviderRegisterRequest): ProviderResponse

    @GET("providers/me")
    suspend fun me(): ProviderResponse

    /** Partial update — only non-null fields in [body] are applied server-side. */
    @PATCH("providers/me")
    suspend fun updateMe(@Body body: ProviderUpdateRequest): ProviderResponse
}