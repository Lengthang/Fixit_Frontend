package com.fixit.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fixit.app.domain.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "fixit_auth")

@Singleton
class TokenStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val tokenKey = stringPreferencesKey("access_token")
    private val roleKey  = stringPreferencesKey("role")

    val token: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val role:  Flow<String?> = context.dataStore.data.map { it[roleKey] }

    suspend fun tokenBlocking(): String? = token.first()
    suspend fun roleBlocking(): UserRole = UserRole.from(role.first())

    suspend fun save(token: String, role: UserRole? = null) {
        context.dataStore.edit {
            it[tokenKey] = token
            if (role != null) it[roleKey] = role.api
        }
    }

    suspend fun updateRole(role: UserRole) {
        context.dataStore.edit { it[roleKey] = role.api }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}