package com.fixit.app.dev

import com.fixit.app.BuildConfig
import com.fixit.app.domain.model.UserRole

object DevConfig {
    /** Single switch. Compile-time off in release via Gradle buildConfigField. */
    val DEV_MODE: Boolean = BuildConfig.DEV_MODE

    val testUsers = listOf(
        DevTestUser("Customer (Alice)", "+85570209098", UserRole.CUSTOMER),
        DevTestUser("Provider (Bob)",   "+85570209097", UserRole.PROVIDER),
        DevTestUser("Admin (Carol)",    "+15550000003", UserRole.ADMIN),
    )

    fun freshPhone(): String =
        "+1555" + (10_000_000..99_999_999).random().toString()
}

data class DevTestUser(val label: String, val phone: String, val role: UserRole)