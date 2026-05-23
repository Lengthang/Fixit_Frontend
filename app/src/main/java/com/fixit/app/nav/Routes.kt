package com.fixit.app.nav

import com.fixit.app.domain.model.UserRole

object Routes {
    const val WELCOME = "welcome"
    const val PHONE   = "phone"
    // nav/Routes.kt
    const val OTP = "otp?phone={phone}"
    fun otp(phone: String) = "otp?phone=${java.net.URLEncoder.encode(phone, "UTF-8")}"

    const val SIGNUP_GRAPH = "signup"
    const val ABOUT_YOU = "about_you"
    const val ROLE      = "role"
    const val LOCATION  = "location"
    const val PROMO     = "promo"
    const val PROVIDER_GRAPH    = "provider_signup"
    const val PROV_SERVICE_AREA = "prov_service_area"
    const val PROV_SERVICES     = "prov_services"
    const val PROV_SCHEDULE     = "prov_schedule"
    const val PROV_CERTIFICATE  = "prov_certificate"
    const val PROV_PAYMENT      = "prov_payment"
    const val PROV_RECEIVED     = "prov_received"
    const val PLACEHOLDER = "placeholder/{role}"
    fun placeholder(role: UserRole) = "placeholder/${role.api}"
}