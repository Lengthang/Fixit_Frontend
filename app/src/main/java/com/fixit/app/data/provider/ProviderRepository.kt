package com.fixit.app.data.provider

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepository @Inject constructor(private val api: ProviderApi) {
    suspend fun register(body: ProviderRegisterRequest): ProviderResponse = api.register(body)
    suspend fun me(): ProviderResponse = api.me()
}