package com.fixit.app.data.wallet

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepository @Inject constructor(private val api: WalletApi) {
    suspend fun me(): WalletResponse = api.me()
}