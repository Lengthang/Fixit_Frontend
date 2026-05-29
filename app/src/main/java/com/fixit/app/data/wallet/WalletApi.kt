package com.fixit.app.data.wallet

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface WalletApi {
    @GET("payments/wallet")
    suspend fun me(): WalletResponse

    @GET("payments/wallet/transactions")
    suspend fun transactions(): List<WalletTransactionResponse>

    /** POST /payments/wallet/top-up — credits the wallet from a saved method. */
    @POST("payments/wallet/top-up")
    suspend fun topUp(@Body body: WalletTopUpRequest): WalletResponse
}