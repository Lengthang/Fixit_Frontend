package com.fixit.app.data.auth

import com.fixit.app.data.local.TokenStorage
import com.fixit.app.domain.model.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenStorage: TokenStorage,
) {
    suspend fun sendOtp(phone: String) {
        api.sendOtp(SendOtpRequest(phone))
    }

    suspend fun verifyOtp(phone: String, code: String, name: String? = null): TokenResponse {
        val res = api.verifyOtp(VerifyOtpRequest(phone, code, name))
        // Trust the server's role field, not the presence of a provider_profile.
        val role = UserRole.from(res.role)
        tokenStorage.save(res.accessToken, role)
        return res
    }

    suspend fun logout() = tokenStorage.clear()
}