package com.fixit.app.ui.signup

import androidx.lifecycle.ViewModel
import com.fixit.app.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

data class AvailabilitySlot(
    val dayOfWeek: String,   // "monday" .. "sunday"
    val openTime: String,    // "HH:MM"
    val closeTime: String,   // "HH:MM"
)

data class SignupDraft(
    val phone: String = "",
    val name: String = "",
    val email: String = "",
    val dob: String = "",
    val referral: String = "",
    val role: UserRole = UserRole.CUSTOMER,

    // Provider-only fields, ignored on the customer branch
    val latitude: Double? = null,
    val longitude: Double? = null,
    val serviceRadiusKm: Int = 15,
    val categoryIds: List<String> = emptyList(),
    val yearsExperience: Int = 0,
    val availability: List<AvailabilitySlot> = emptyList(),
    val certification: String? = null,
    val payoutPreference: String = "bank",   // "bank" | "paypal" | "apple"
)

@HiltViewModel
class SignupDraftViewModel @Inject constructor() : ViewModel() {
    val state = MutableStateFlow(SignupDraft())
    fun update(block: (SignupDraft) -> SignupDraft) {
        state.value = block(state.value)
    }
}