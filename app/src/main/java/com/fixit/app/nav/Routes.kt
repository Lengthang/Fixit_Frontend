package com.fixit.app.nav

import com.fixit.app.domain.model.UserRole

object Routes {
    const val WELCOME = "welcome"
    const val PHONE   = "phone"
    const val OTP     = "otp?phone={phone}"
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

    // ── Provider main tabs ──
    const val PROVIDER_HOME      = "provider/home"
    const val PROVIDER_JOBS      = "provider/jobs"
    const val PROVIDER_CALENDAR  = "provider/calendar"
    const val PROVIDER_MESSAGES  = "provider/messages"
    const val PROVIDER_PROFILE   = "provider/profile"

    // ── Provider sub-screens (off-tab) ──
    const val PROVIDER_EDIT_PROFILE    = "provider/edit_profile"
    const val PROVIDER_EARNINGS        = "provider/earnings"
    const val PROVIDER_REVIEWS         = "provider/reviews"
    // ── Provider disputes ──
    /** Full dispute history list. */
    const val PROVIDER_DISPUTE_HISTORY = "provider/disputes"
    /** Single dispute detail — disputeId is a required nav arg. */
    const val PROVIDER_DISPUTE_DETAIL  = "provider/disputes/{disputeId}"

    fun providerDisputeDetail(disputeId: String) = "provider/disputes/$disputeId"
    const val PROVIDER_PAYMENT_PAYOUTS = "provider/payment_payouts"
    const val PROVIDER_WITHDRAW_CONFIRM = "provider/withdraw_confirm"

    // ── Provider services sub-screens ──
    const val PROVIDER_SERVICES        = "provider/services"
    const val PROVIDER_SERVICE_NEW     = "provider/service_new"
    const val PROVIDER_SERVICE_DETAIL  = "provider/service/{serviceId}"
    const val PROVIDER_SERVICE_EDIT    = "provider/service/{serviceId}/edit"

    fun providerServiceDetail(id: String) = "provider/service/$id"
    fun providerServiceEdit(id: String)   = "provider/service/$id/edit"

    const val JOB_DETAIL = "provider/jobs/{bookingId}"
    fun jobDetail(bookingId: String) = "provider/jobs/$bookingId"

    // ── Customer main tabs ──
    const val CUSTOMER_HOME     = "customer/home"
    const val CUSTOMER_BOOKINGS = "customer/bookings"
    const val CUSTOMER_MESSAGES = "customer/messages"
    const val CUSTOMER_PROFILE  = "customer/profile"

    // ── Customer sub-screens (off-tab placeholders) ──
    const val CUSTOMER_SEARCH      = "customer/search"
    const val CUSTOMER_FILTERS     = "customer/filters"
    const val CUSTOMER_CATEGORIES  = "customer/categories"
    const val CUSTOMER_PROVIDERS   = "customer/providers"
    const val CUSTOMER_PROVIDERS_BY_CATEGORY = "customer/providers?categoryId={categoryId}&categoryName={categoryName}"
    fun customerProvidersByCategory(categoryId: String, categoryName: String): String {
        val encId   = java.net.URLEncoder.encode(categoryId, "UTF-8")
        val encName = java.net.URLEncoder.encode(categoryName, "UTF-8")
        return "customer/providers?categoryId=$encId&categoryName=$encName"
    }

    const val CUSTOMER_PROVIDER_DETAIL = "customer/provider/{providerId}"
    fun customerProviderDetail(id: String) = "customer/provider/$id"

    const val PLACEHOLDER = "placeholder/{role}"
    fun placeholder(role: UserRole) = "placeholder/${role.api}"

    fun home(role: UserRole): String = when (role) {
        UserRole.PROVIDER -> PROVIDER_HOME
        UserRole.CUSTOMER -> CUSTOMER_HOME
        else              -> placeholder(role)
    }

    fun providerTab(tabId: String): String = when (tabId) {
        "home"     -> PROVIDER_HOME
        "jobs"     -> PROVIDER_JOBS
        "calendar" -> PROVIDER_CALENDAR
        "messages" -> PROVIDER_MESSAGES
        "profile"  -> PROVIDER_PROFILE
        else       -> PROVIDER_HOME
    }

    fun customerTab(tabId: String): String = when (tabId) {
        "home"     -> CUSTOMER_HOME
        "bookings" -> CUSTOMER_BOOKINGS
        "messages" -> CUSTOMER_MESSAGES
        "profile"  -> CUSTOMER_PROFILE
        else       -> CUSTOMER_HOME
    }
}