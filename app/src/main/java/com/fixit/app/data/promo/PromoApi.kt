package com.fixit.app.data.promo

import retrofit2.http.GET

interface PromoApi {
    @GET("promo-codes/active")
    suspend fun active(): List<ActivePromoResponse>
}