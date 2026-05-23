// domain/model/UserRole.kt
package com.fixit.app.domain.model

enum class UserRole {
    CUSTOMER, PROVIDER, ADMIN;
    val api: String get() = name.lowercase()
    companion object {
        fun from(api: String?): UserRole = when (api?.lowercase()) {
            "provider" -> PROVIDER
            "admin" -> ADMIN
            else -> CUSTOMER
        }
    }
}