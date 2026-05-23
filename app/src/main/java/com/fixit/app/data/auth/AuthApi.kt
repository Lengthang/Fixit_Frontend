package com.fixit.app.data.auth

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {
    @POST("auth/send-otp")
    suspend fun sendOtp(@Body body: SendOtpRequest)

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): TokenResponse

    suspend fun devToken(@Query("phone") phone: String): TokenResponse
}