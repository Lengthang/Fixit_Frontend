package com.fixit.app.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.fixit.app.ui.placeholder.UserPlaceholderScreen
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

@Composable
fun FixItNavGraph(startDestination: String) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val devAuth: DevAuthHandler = hiltViewModel<DevAuthBridge>().handler

    NavHost(navController = nav, startDestination = startDestination) {

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onCreate = { nav.navigate(Routes.PHONE) },
                onSignIn = { nav.navigate(Routes.PHONE) },
                onDevNewUser = {
                    scope.launch {
                        devAuth.signIn(DevConfig.freshPhone(), role = UserRole.CUSTOMER)
                        nav.navigate(Routes.SIGNUP_GRAPH) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    }
                },
                onDevExistingUser = { tu ->
                    scope.launch {
                        devAuth.signIn(tu.phone, role = tu.role)
                        nav.navigate(Routes.placeholder(tu.role)) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable(Routes.PHONE) { entry ->
            val owner = remember(entry) { nav.getBackStackEntry(Routes.WELCOME) }
            val draftVm: SignupDraftViewModel = hiltViewModel(owner)
            PhoneEntryScreen(
                onBack = { nav.popBackStack() },
                onCodeSent = { phone -> nav.navigate(Routes.otp(phone)) },
                draftVm = draftVm,
            )
        }

        composable(
            Routes.OTP,
            arguments = listOf(navArgument("phone") {
                type = NavType.StringType
                nullable = false
            }),
        ) { entry ->
            val phone = entry.arguments?.getString("phone").orEmpty()
            val owner = remember(entry) { nav.getBackStackEntry(Routes.WELCOME) }
            val draftVm: SignupDraftViewModel = hiltViewModel(owner)
            OtpScreen(
                phone = phone,
                onBack = { nav.popBackStack() },
                onVerifiedNew = {
                    nav.navigate(Routes.SIGNUP_GRAPH) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                onVerifiedExisting = { isProvider ->
                    val role = if (isProvider) UserRole.PROVIDER else UserRole.CUSTOMER
                    nav.navigate(Routes.placeholder(role)) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                draftVm = draftVm,
            )
        }

        // ── Signup sub-graph (customer + provider branches share the draft VM) ──
        navigation(startDestination = Routes.ABOUT_YOU, route = Routes.SIGNUP_GRAPH) {

            composable(Routes.ABOUT_YOU) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.SIGNUP_GRAPH) }
                val draftVm: SignupDraftViewModel = hiltViewModel(parent)
                AboutYouScreen(
                    onBack = { nav.popBackStack() },
                    onContinue = { nav.navigate(Routes.ROLE) },
                    draftVm = draftVm,
                )
            }

            composable(Routes.ROLE) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.SIGNUP_GRAPH) }
                val draftVm: SignupDraftViewModel = hiltViewModel(parent)
                RoleScreen(
                    onBack = { nav.popBackStack() },
                    onContinueCustomer = { nav.navigate(Routes.LOCATION) },
                    onContinueProvider = { nav.navigate(Routes.LOCATION) },
                    draftVm = draftVm,
                )
            }

            composable(Routes.LOCATION) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.SIGNUP_GRAPH) }
                val draftVm: SignupDraftViewModel = hiltViewModel(parent)
                LocationScreen(
                    onBack = { nav.popBackStack() },
                    onContinue = {
                        if (draftVm.state.value.role == UserRole.PROVIDER) {
                            nav.navigate(Routes.PROV_SERVICE_AREA)
                        } else {
                            nav.navigate(Routes.PROMO)
                        }
                    },
                )
            }

            // ── Customer terminal step ──
            composable(Routes.PROMO) {
                PromoScreen(
                    onBack = { nav.popBackStack() },
                    onFinish = {
                        nav.navigate(Routes.placeholder(UserRole.CUSTOMER)) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    },
                )
            }

            // ── Provider branch ──
            composable(Routes.PROV_SERVICE_AREA) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.SIGNUP_GRAPH) }
                val draftVm: SignupDraftViewModel = hiltViewModel(parent)
                ServiceAreaScreen(
                    onBack = { nav.popBackStack() },
                    onContinue = { nav.navigate(Routes.PROV_SERVICES) },
                    draftVm = draftVm,
                )
            }
            composable(Routes.PROV_SERVICES) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.SIGNUP_GRAPH) }
                val draftVm: SignupDraftViewModel = hiltViewModel(parent)
                ServicesScreen(
                    onBack = { nav.popBackStack() },
                    onContinue = { nav.navigate(Routes.PROV_SCHEDULE) },
                    draftVm = draftVm,
                )
            }
            composable(Routes.PROV_SCHEDULE) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.SIGNUP_GRAPH) }
                val draftVm: SignupDraftViewModel = hiltViewModel(parent)
                ScheduleScreen(
                    onBack = { nav.popBackStack() },
                    onContinue = { nav.navigate(Routes.PROV_CERTIFICATE) },
                    draftVm = draftVm,
                )
            }
            composable(Routes.PROV_CERTIFICATE) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.SIGNUP_GRAPH) }
                val draftVm: SignupDraftViewModel = hiltViewModel(parent)
                CertificateScreen(
                    onBack = { nav.popBackStack() },
                    onContinue = { nav.navigate(Routes.PROV_PAYMENT) },
                    onSkip = { nav.navigate(Routes.PROV_PAYMENT) },
                    draftVm = draftVm,
                )
            }
            composable(Routes.PROV_PAYMENT) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.SIGNUP_GRAPH) }
                val draftVm: SignupDraftViewModel = hiltViewModel(parent)
                PaymentScreen(
                    onBack = { nav.popBackStack() },
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
                        nav.navigate(Routes.placeholder(UserRole.PROVIDER)) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    },
                    draftVm = draftVm,
                )
            }
        }

        composable(
            Routes.PLACEHOLDER,
            arguments = listOf(navArgument("role") { type = NavType.StringType }),
        ) { entry ->
            val role = UserRole.from(entry.arguments?.getString("role"))
            UserPlaceholderScreen(
                role = role,
                onSignedOut = {
                    nav.navigate(Routes.WELCOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}

// Bridge so we can grab DevAuthHandler at the NavGraph level via Hilt.
@dagger.hilt.android.lifecycle.HiltViewModel
class DevAuthBridge @javax.inject.Inject constructor(val handler: DevAuthHandler)
    : androidx.lifecycle.ViewModel()