package com.fixit.app.data.wallet

import retrofit2.http.GET

interface WalletApi {
    @GET("payments/wallet")
    suspend fun me(): WalletResponse
}