package com.fixit.app.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fixit.app.dev.DevAuthHandler
import com.fixit.app.dev.DevConfig
import com.fixit.app.domain.model.UserRole
import com.fixit.app.ui.auth.OtpScreen
import com.fixit.app.ui.auth.PhoneEntryScreen
import com.fixit.app.ui.customer.bookings.CustomerBookingDetailScreen
import com.fixit.app.ui.placeholder.UserPlaceholderScreen
import com.fixit.app.ui.provider.calendar.ProviderCalendarScreen
import com.fixit.app.ui.provider.dashboard.ProviderHomeScreen
import com.fixit.app.ui.provider.earnings.ProviderEarningsScreen
import com.fixit.app.ui.provider.jobs.JobDetailScreen
import com.fixit.app.ui.provider.jobs.ProviderJobsScreen
import com.fixit.app.ui.provider.messages.ProviderMessagesScreen
import com.fixit.app.ui.provider.payment.PaymentPayoutsScreen
import com.fixit.app.ui.provider.payment.WithdrawConfirmScreen
import com.fixit.app.ui.provider.profile.ProviderProfileScreen
import com.fixit.app.ui.provider.profile.ProviderProfileViewModel
import com.fixit.app.ui.provider.reviews.ProviderReviewsScreen
import com.fixit.app.ui.provider.services.AddEditServiceScreen
import com.fixit.app.ui.provider.services.MyServicesScreen
import com.fixit.app.ui.provider.services.ServiceDetailScreen
import com.fixit.app.ui.signup.AboutYouScreen
import com.fixit.app.ui.signup.LocationScreen
import com.fixit.app.ui.signup.PromoScreen
import com.fixit.app.ui.signup.RoleScreen
import com.fixit.app.ui.signup.SignupDraftViewModel
import com.fixit.app.ui.signup.provider.CertificateScreen
import com.fixit.app.ui.signup.provider.PaymentScreen
import com.fixit.app.ui.signup.provider.ReceivedScreen
import com.fixit.app.ui.signup.provider.ScheduleScreen
import com.fixit.app.ui.signup.provider.ServiceAreaScreen
import com.fixit.app.ui.signup.provider.ServicesScreen
import com.fixit.app.ui.welcome.WelcomeScreen
import kotlinx.coroutines.launch
import retrofit2.HttpException
import com.fixit.app.ui.provider.disputes.DisputeDetailScreen
import com.fixit.app.ui.provider.disputes.DisputeListScreen
import com.fixit.app.ui.provider.profile.EditProfileScreen
import com.fixit.app.ui.customer.bookings.CustomerBookingsScreen
import com.fixit.app.ui.customer.bookings.LeaveReviewScreen
import com.fixit.app.ui.customer.bookings.OpenDisputeScreen
import com.fixit.app.ui.customer.browse.CustomerBrowseScreen
import com.fixit.app.ui.customer.browse.CustomerProviderDetailScreen
import com.fixit.app.ui.customer.dashboard.CustomerHomeScreen
import com.fixit.app.ui.customer.messages.CustomerMessagesScreen
import com.fixit.app.ui.customer.profile.CustomerProfileScreen
import com.fixit.app.ui.customer.profile.CustomerProfileViewModel
import com.fixit.app.ui.customer.booking.CustomerBookingScreen
private const val DEV_TAG = "DevAuth"
private const val PROFILE_REFRESH_KEY = "profile_refresh"

private fun describe(e: Throwable): String = when (e) {
    is HttpException -> {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        "HTTP ${e.code()} — ${body ?: e.message()}"
    }
    else -> e.message ?: e.javaClass.simpleName
}

private fun NavHostController.switchProviderTab(tabId: String) {
    navigate(Routes.providerTab(tabId)) {
        launchSingleTop = true
        popUpTo(Routes.PROVIDER_HOME) { inclusive = false; saveState = true }
        restoreState = true
    }
}
/** Customer twin of switchProviderTab. */
private fun NavHostController.switchCustomerTab(tabId: String) {
    navigate(Routes.customerTab(tabId)) {
        launchSingleTop = true
        popUpTo(Routes.CUSTOMER_HOME) { inclusive = false; saveState = true }
        restoreState = true
    }
}

@Composable
fun FixItNavGraph(startDestination: String) {
    val nav   = rememberNavController()
    val scope = rememberCoroutineScope()
    val devAuth: DevAuthHandler = hiltViewModel<DevAuthBridge>().handler

    NavHost(navController = nav, startDestination = startDestination) {

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onCreate = { nav.navigate(Routes.PHONE) },
                onSignIn = { nav.navigate(Routes.PHONE) },
                onDevNewUser = {
                    scope.launch {
                        runCatching {
                            devAuth.signIn(DevConfig.freshPhone(), role = UserRole.CUSTOMER)
                        }.onSuccess {
                            nav.navigate(Routes.SIGNUP_GRAPH) {
                                popUpTo(Routes.WELCOME) { inclusive = true }
                            }
                        }.onFailure { e ->
                            android.util.Log.e(DEV_TAG, "dev new-user sign-in failed: ${describe(e)}", e)
                        }
                    }
                },
                onDevExistingUser = { tu ->
                    scope.launch {
                        runCatching {
                            devAuth.signIn(tu.phone, role = tu.role)
                        }.onSuccess {
                            nav.navigate(Routes.home(tu.role)) {
                                popUpTo(Routes.WELCOME) { inclusive = true }
                            }
                        }.onFailure { e ->
                            android.util.Log.e(DEV_TAG, "dev sign-in (${tu.label}) failed: ${describe(e)}", e)
                        }
                    }
                },
            )
        }

        composable(Routes.PHONE) { entry ->
            val owner  = remember(entry) { nav.getBackStackEntry(Routes.WELCOME) }
            val draftVm: SignupDraftViewModel = hiltViewModel(owner)
            PhoneEntryScreen(
                onBack     = { nav.popBackStack() },
                onCodeSent = { phone -> nav.navigate(Routes.otp(phone)) },
                draftVm    = draftVm,
            )
        }

        composable(
            Routes.OTP,
            arguments = listOf(navArgument("phone") {
                type     = NavType.StringType
                nullable = false
            }),
        ) { entry ->
            val phone  = entry.arguments?.getString("phone").orEmpty()
            val owner  = remember(entry) { nav.getBackStackEntry(Routes.WELCOME) }
            val draftVm: SignupDraftViewModel = hiltViewModel(owner)
            OtpScreen(
                phone            = phone,
                onBack           = { nav.popBackStack() },
                onVerifiedNew    = {
                    nav.navigate(Routes.SIGNUP_GRAPH) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                onVerifiedExisting = { isProvider ->
                    val role = if (isProvider) UserRole.PROVIDER else UserRole.CUSTOMER
                    nav.navigate(Routes.home(role)) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                draftVm = draftVm,
            )
        }

        // ── Signup sub-graph ──────────────────────────────────────────────────
        navigation(startDestination = Routes.ABOUT_YOU, route = Routes.SIGNUP_GRAPH) {

            composable(Routes.ABOUT_YOU) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.SIGNUP_GRAPH) }
                val draftVm: SignupDraftViewModel = hiltViewModel(parent)
                AboutYouScreen(onBack = { nav.popBackStack() }, onContinue = { nav.navigate(Routes.ROLE) }, draftVm = draftVm)
            }

            composable(Routes.ROLE) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.SIGNUP_GRAPH) }
                val draftVm: SignupDraftViewModel = hiltViewModel(parent)
                RoleScreen(
                    onBack               = { nav.popBackStack() },
                    onContinueCustomer   = { nav.navigate(Routes.LOCATION) },
                    onContinueProvider   = { nav.navigate(Routes.LOCATION) },
                    draftVm              = draftVm,
                )
            }

            composable(Routes.LOCATION) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.SIGNUP_GRAPH) }
                val draftVm: SignupDraftViewModel = hiltViewModel(parent)
                LocationScreen(
                    onBack     = { nav.popBackStack() },
                    onContinue = {
                        if (draftVm.state.value.role == UserRole.PROVIDER) {
                            nav.navigate(Routes.PROV_SERVICE_AREA)
                        } else {
                            nav.navigate(Routes.PROMO)
                        }
                    },
                )
            }

            composable(Routes.PROMO) {
                PromoScreen(
                    onBack   = { nav.popBackStack() },
                    onFinish = {
                        nav.navigate(Routes.home(UserRole.CUSTOMER)) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.PROV_SERVICE_AREA) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.SIGNUP_GRAPH) }
                val draftVm: SignupDraftViewModel = hiltViewModel(parent)
                ServiceAreaScreen(onBack = { nav.popBackStack() }, onContinue = { nav.navigate(Routes.PROV_SERVICES) }, draftVm = draftVm)
            }

            composable(Routes.PROV_SERVICES) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.SIGNUP_GRAPH) }
                val draftVm: SignupDraftViewModel = hiltViewModel(parent)
                ServicesScreen(onBack = { nav.popBackStack() }, onContinue = { nav.navigate(Routes.PROV_SCHEDULE) }, draftVm = draftVm)
            }

            composable(Routes.PROV_SCHEDULE) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.SIGNUP_GRAPH) }
                val draftVm: SignupDraftViewModel = hiltViewModel(parent)
                ScheduleScreen(onBack = { nav.popBackStack() }, onContinue = { nav.navigate(Routes.PROV_CERTIFICATE) }, draftVm = draftVm)
            }

            composable(Routes.PROV_CERTIFICATE) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.SIGNUP_GRAPH) }
                val draftVm: SignupDraftViewModel = hiltViewModel(parent)
                CertificateScreen(
                    onBack     = { nav.popBackStack() },
                    onContinue = { nav.navigate(Routes.PROV_PAYMENT) },
                    onSkip     = { nav.navigate(Routes.PROV_PAYMENT) },
                    draftVm    = draftVm,
                )
            }

            composable(Routes.PROV_PAYMENT) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.SIGNUP_GRAPH) }
                val draftVm: SignupDraftViewModel = hiltViewModel(parent)
                PaymentScreen(
                    onBack       = { nav.popBackStack() },
                    onRegistered = {
                        nav.navigate(Routes.PROV_RECEIVED) {
                            popUpTo(Routes.PROV_SERVICE_AREA) { inclusive = true }
                        }
                    },
                    draftVm = draftVm,
                )
            }

            composable(Routes.PROV_RECEIVED) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.SIGNUP_GRAPH) }
                val draftVm: SignupDraftViewModel = hiltViewModel(parent)
                ReceivedScreen(
                    onDashboard = {
                        nav.navigate(Routes.home(UserRole.PROVIDER)) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    },
                    draftVm = draftVm,
                )
            }
        }

        // ── Provider tabs ─────────────────────────────────────────────────────

        composable(Routes.PROVIDER_HOME) {
            ProviderHomeScreen(
                onTabClick          = { nav.switchProviderTab(it) },
                onNewRequestClick   = { req -> nav.navigate(Routes.jobDetail(req.id)) },
                onUpcomingClick     = { up -> nav.navigate(Routes.jobDetail(up.id)) },
                onWithdrawClick     = { nav.navigate(Routes.PROVIDER_EARNINGS) },
                onNotificationsClick = { /* TODO: notifications screen */ },
                onSeeAllRequests    = { nav.switchProviderTab("jobs") },
                onSeeAllUpcoming    = { nav.switchProviderTab("calendar") },
            )
        }

        composable(Routes.PROVIDER_JOBS) {
            ProviderJobsScreen(
                onTabClick  = { nav.switchProviderTab(it) },
                onJobClick  = { bookingId -> nav.navigate(Routes.jobDetail(bookingId)) },
            )
        }

        composable(Routes.PROVIDER_CALENDAR) {
            ProviderCalendarScreen(
                onTabClick  = { nav.switchProviderTab(it) },
                onJobClick  = { bookingId -> nav.navigate(Routes.jobDetail(bookingId)) },
            )
        }

        composable(Routes.PROVIDER_MESSAGES) {
            ProviderMessagesScreen(
                onTabClick    = { nav.switchProviderTab(it) },
                onMessageClick = { },
            )
        }

        composable(Routes.PROVIDER_PROFILE) {
            val vm: ProviderProfileViewModel = hiltViewModel()
            val signedOut by vm.signedOut.collectAsState()
            LaunchedEffect(signedOut) {
                if (signedOut) {
                    nav.navigate(Routes.WELCOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            ProviderProfileScreen(
                onTabClick         = { nav.switchProviderTab(it) },
                onEditProfile      = {  nav.navigate(Routes.PROVIDER_EDIT_PROFILE) },
                onServicesAndRates = { nav.navigate(Routes.PROVIDER_SERVICES) },
                onPaymentAndPayouts = { nav.navigate(Routes.PROVIDER_PAYMENT_PAYOUTS) },
                onReviews          = { nav.navigate(Routes.PROVIDER_REVIEWS) },
                onDisputeHistory   = { nav.navigate(Routes.PROVIDER_DISPUTE_HISTORY) },  // ← NEW
                onHelp             = { /* TODO: help & support */ },
                viewModel          = vm,
            )
        }

        // ── Provider sub-screens ──────────────────────────────────────────────
        composable(Routes.PROVIDER_EDIT_PROFILE) {
            EditProfileScreen(
                onBack  = { nav.popBackStack() },
                onSaved = { nav.popBackStack() },
            )
        }
        // Earnings (entry from Home → Withdraw button — stays as before)
        composable(Routes.PROVIDER_EARNINGS) {
            ProviderEarningsScreen(
                onBack                     = { nav.popBackStack() },
                onTabClick                 = { nav.switchProviderTab(it) },
                onNavigateToWithdrawConfirm = { nav.navigate(Routes.PROVIDER_WITHDRAW_CONFIRM) },
            )
        }

        // Payment & Payouts hub (entry from Profile → Payment & Payouts)
        composable(Routes.PROVIDER_PAYMENT_PAYOUTS) {
            PaymentPayoutsScreen(
                onBack                     = { nav.popBackStack() },
                onTabClick                 = { nav.switchProviderTab(it) },
                onNavigateToWithdrawConfirm = { nav.navigate(Routes.PROVIDER_WITHDRAW_CONFIRM) },
                onSeeAllHistory            = { nav.navigate(Routes.PROVIDER_EARNINGS) },
            )
        }

        // Withdraw confirm (reached from either screen when a default method exists)
        composable(Routes.PROVIDER_WITHDRAW_CONFIRM) {
            WithdrawConfirmScreen(
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.PROVIDER_REVIEWS) {
            ProviderReviewsScreen(
                onBack     = { nav.popBackStack() },
                onTabClick = { nav.switchProviderTab(it) },
            )
        }
        // ── Provider dispute history ──────────────────────────────────────

        composable(Routes.PROVIDER_DISPUTE_HISTORY) {
            DisputeListScreen(
                onBack          = { nav.popBackStack() },
                onDisputeClick  = { id -> nav.navigate(Routes.providerDisputeDetail(id)) },
                onTabClick      = { nav.switchProviderTab(it) },
            )
        }

        composable(
            Routes.PROVIDER_DISPUTE_DETAIL,
            arguments = listOf(navArgument("disputeId") {
                type     = NavType.StringType
                nullable = false
            }),
        ) {
            DisputeDetailScreen(
                onBack = { nav.popBackStack() },
            )
        }

        // ── Provider services sub-screens ─────────────────────────────────────

        composable(Routes.PROVIDER_SERVICES) {
            MyServicesScreen(
                onBack        = { nav.popBackStack() },
                onServiceClick = { id -> nav.navigate(Routes.providerServiceDetail(id)) },
                onNewService  = { nav.navigate(Routes.PROVIDER_SERVICE_NEW) },
                onTabClick    = { nav.switchProviderTab(it) },
            )
        }

        composable(Routes.PROVIDER_SERVICE_NEW) {
            AddEditServiceScreen(
                onBack  = { nav.popBackStack() },
                onSaved = { nav.popBackStack(Routes.PROVIDER_SERVICES, inclusive = false) },
            )
        }

        composable(
            Routes.PROVIDER_SERVICE_DETAIL,
            arguments = listOf(navArgument("serviceId") { type = NavType.StringType; nullable = false }),
        ) {
            ServiceDetailScreen(
                onBack = { nav.popBackStack() },
                onEdit = { id -> nav.navigate(Routes.providerServiceEdit(id)) },
            )
        }

        composable(
            Routes.PROVIDER_SERVICE_EDIT,
            arguments = listOf(navArgument("serviceId") { type = NavType.StringType; nullable = false }),
        ) {
            AddEditServiceScreen(
                onBack  = { nav.popBackStack() },
                onSaved = { nav.popBackStack(Routes.PROVIDER_SERVICES, inclusive = false) },
            )
        }

        composable(
            Routes.JOB_DETAIL,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType; nullable = false }),
        ) {
            JobDetailScreen(
                onBack           = { nav.popBackStack() },
                onActionCompleted = { nav.popBackStack() },
            )
        }
        // ── Customer tabs ──

        composable(Routes.CUSTOMER_HOME) {
            CustomerHomeScreen(
                onTabClick = { nav.switchCustomerTab(it) },
                onLocationClick = { /* TODO: open address picker */ },
                onNotificationsClick = { /* TODO: notifications screen */ },
                onSearchClick = { nav.navigate(Routes.CUSTOMER_SEARCH) },
                onFilterClick = { nav.navigate(Routes.CUSTOMER_FILTERS) },
                onSeeAllCategories = { nav.navigate(Routes.CUSTOMER_CATEGORIES) },
                onCategoryClick = { cat ->
                    nav.navigate(Routes.customerProvidersByCategory(cat.id, cat.name))
                },
                onSeeAllProviders = { nav.navigate(Routes.CUSTOMER_PROVIDERS) },
                onProviderClick = { p -> nav.navigate(Routes.customerProviderDetail(p.id)) },
                onProviderBookClick = { p -> nav.navigate(Routes.customerProviderDetail(p.id)) },
            )
        }

        composable(Routes.CUSTOMER_BOOKINGS) {
            CustomerBookingsScreen(
                onTabClick         = { nav.switchCustomerTab(it) },
                onBookingClick     = { bookingId ->
                    nav.navigate(Routes.customerBookingDetail(bookingId))
                },
                onLeaveReviewClick = { bookingId ->
                    nav.navigate(Routes.customerLeaveReview(bookingId))
                },
                onOpenDisputeClick = { bookingId ->
                    nav.navigate(Routes.customerOpenDispute(bookingId))
                },
            )
        }
        composable(
            Routes.CUSTOMER_BOOKING_DETAIL,
            arguments = listOf(navArgument("bookingId") {
                type = NavType.StringType
                nullable = false
            }),
        ) {
            CustomerBookingDetailScreen(
                onBack         = { nav.popBackStack() },
                onOpenDispute  = { bookingId ->
                    nav.navigate(Routes.customerOpenDispute(bookingId))
                },
                onLeaveReview  = { bookingId ->
                    nav.navigate(Routes.customerLeaveReview(bookingId))
                },
            )
        }
        composable(
            Routes.CUSTOMER_LEAVE_REVIEW,
            arguments = listOf(navArgument("bookingId") {
                type = NavType.StringType
                nullable = false
            }),
        ) {
            LeaveReviewScreen(onBack = { nav.popBackStack() })
        }

        composable(
            Routes.CUSTOMER_OPEN_DISPUTE,
            arguments = listOf(navArgument("bookingId") {
                type = NavType.StringType
                nullable = false
            }),
        ) {
            OpenDisputeScreen(onBack = { nav.popBackStack() })
        }

        composable(Routes.CUSTOMER_MESSAGES) {
            CustomerMessagesScreen(
                onTabClick    = { nav.switchCustomerTab(it) },
                onMessageClick = { },
            )
        }
        composable(Routes.CUSTOMER_PROFILE) {
            val vm: CustomerProfileViewModel = hiltViewModel()
            val signedOut by vm.signedOut.collectAsState()
            LaunchedEffect(signedOut) {
                if (signedOut) {
                    nav.navigate(Routes.WELCOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            CustomerProfileScreen(
                onTabClick       = { nav.switchCustomerTab(it) },
                onPersonalInfo   = { /* TODO: customer edit-profile screen */ },
                onSavedAddresses = { /* TODO: saved addresses screen */ },
                onPaymentMethods = { /* TODO: payment methods screen */ },
                onPromos         = { /* TODO: promos & rewards screen */ },
                onTopUp          = { /* TODO: wallet top-up flow */ },
                onHelp           = { /* TODO: help & support */ },
                viewModel        = vm,
            )
        }

        // ── Customer sub-screens (placeholder destinations) ──

        composable(Routes.CUSTOMER_SEARCH) {
            CustomerBrowseScreen(title = "Search", onBack = { nav.popBackStack() })
        }
        composable(Routes.CUSTOMER_FILTERS) {
            CustomerBrowseScreen(title = "Filters", onBack = { nav.popBackStack() })
        }
        composable(Routes.CUSTOMER_CATEGORIES) {
            CustomerBrowseScreen(title = "All categories", onBack = { nav.popBackStack() })
        }
        composable(Routes.CUSTOMER_PROVIDERS) {
            CustomerBrowseScreen(title = "Providers near you", onBack = { nav.popBackStack() })
        }
        composable(
            Routes.CUSTOMER_PROVIDERS_BY_CATEGORY,
            arguments = listOf(
                navArgument("categoryId")   { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType },
            ),
        ) { entry ->
            val name = entry.arguments?.getString("categoryName").orEmpty()
            CustomerBrowseScreen(title = name.ifBlank { "Providers" }, onBack = { nav.popBackStack() })
        }
        composable(
            Routes.CUSTOMER_PROVIDER_DETAIL,
            arguments = listOf(navArgument("providerId") {
                type = NavType.StringType
                nullable = false
            }),
        ) {
            CustomerProviderDetailScreen(
                onBack    = { nav.popBackStack() },
                onTabClick = { /* TODO: customer tab bar — no destination yet */ },
                onChat    = { /* TODO: messaging flow not built yet */ },
                onBookNow = { providerId, cart ->
                    nav.navigate(Routes.customerBookingNew(providerId, cart))
                },
            )
        }
        composable(
            Routes.CUSTOMER_BOOKING_NEW,
            arguments = listOf(
                navArgument("providerId") {
                    type = NavType.StringType
                    nullable = false
                },
                navArgument("items") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
            ),
        ) {
            CustomerBookingScreen(
                onBack = { nav.popBackStack() },
                onBooked = { bookingId ->
                    // Land on the booking detail, dropping the booking form and
                    // the provider detail from the back stack so "back" returns
                    // the customer to where they were browsing.
                    nav.navigate(Routes.customerBookingDetail(bookingId)) {
                        popUpTo(Routes.CUSTOMER_PROVIDER_DETAIL) { inclusive = true }
                    }
                },
            )
        }
//        composable(Routes.CUSTOMER_BOOKING_NEW) {
//            CustomerBrowseScreen(title = "Booking", onBack = { nav.popBackStack() })
//        }

        composable(
            Routes.PLACEHOLDER,
            arguments = listOf(navArgument("role") { type = NavType.StringType }),
        ) { entry ->
            val role = UserRole.from(entry.arguments?.getString("role"))
            UserPlaceholderScreen(
                role      = role,
                onSignedOut = {
                    nav.navigate(Routes.WELCOME) { popUpTo(0) { inclusive = true } }
                },
            )
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class DevAuthBridge @javax.inject.Inject constructor(val handler: DevAuthHandler)
    : androidx.lifecycle.ViewModel()