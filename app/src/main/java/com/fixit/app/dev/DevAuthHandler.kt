package com.fixit.app.dev

import com.fixit.app.data.auth.AuthApi
import com.fixit.app.data.auth.TokenResponse
import com.fixit.app.data.local.TokenStorage
import com.fixit.app.domain.model.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevAuthHandler @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage,
) {
    suspend fun signIn(phone: String, role: UserRole? = null): TokenResponse {
        check(DevConfig.DEV_MODE) { "DevAuthHandler invoked while DEV_MODE is false" }
        val res = authApi.devToken(phone)
        val resolvedRole = role
            ?: res.providerProfile?.let { UserRole.PROVIDER }
            ?: UserRole.CUSTOMER
        tokenStorage.save(res.accessToken, resolvedRole)
        return res
    }
}