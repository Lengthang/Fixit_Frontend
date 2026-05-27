package com.fixit.app.domain.model

/**
 * Lean category type for home-screen consumption. We don't expose every
 * CategoryResponse field to the UI — just what the circle button needs.
 */
data class HomeCategory(
    val id: String,
    val name: String,
    val iconUrl: String?,
)