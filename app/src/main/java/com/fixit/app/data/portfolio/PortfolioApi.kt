package com.fixit.app.data.portfolio

import retrofit2.http.GET
import retrofit2.http.Path

interface PortfolioApi {
    /** GET /portfolio/providers/{provider_id} — public; returns any provider's portfolio. */
    @GET("portfolio/providers/{providerId}")
    suspend fun forProvider(@Path("providerId") providerId: String): List<PortfolioItemResponse>
}