package com.fixit.app.data.location

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface LocationApi {
    @GET("customer/locations")
    suspend fun list(): List<SavedLocationResponse>

    @GET("customer/locations/{id}")
    suspend fun get(@Path("id") id: String): SavedLocationResponse

    @POST("customer/locations")
    suspend fun create(@Body body: SavedLocationCreateRequest): SavedLocationResponse

    @PATCH("customer/locations/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: SavedLocationUpdateRequest,
    ): SavedLocationResponse

    @DELETE("customer/locations/{id}")
    suspend fun delete(@Path("id") id: String)
}