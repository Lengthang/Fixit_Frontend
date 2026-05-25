package com.fixit.app.data.service

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ServiceApi {

    /** All services belonging to the signed-in provider (active + paused). */
    @GET("services/mine")
    suspend fun mine(): List<ServiceResponse>

    /** Create a new service. category_id must be one the provider is registered for. */
    @POST("services/")
    suspend fun create(@Body body: ServiceCreateRequest): ServiceResponse

    /** Partial update — only non-null fields are applied by the backend. */
    @PATCH("services/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: ServiceUpdateRequest,
    ): ServiceResponse

    /**
     * Soft-delete: sets is_active = false on the backend.
     * Returns a plain acknowledgement body — we don't model it; a successful
     * 200 response is sufficient confirmation.
     */
    @DELETE("services/{id}")
    suspend fun delete(@Path("id") id: String)
}