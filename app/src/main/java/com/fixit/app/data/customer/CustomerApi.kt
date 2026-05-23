package com.fixit.app.data.customer

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface CustomerApi {
    @GET("customer/me")
    suspend fun me(): UserResponse

    @PATCH("customer/me")
    suspend fun updateMe(@Body body: CustomerUpdateRequest): UserResponse
}