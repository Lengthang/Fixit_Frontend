package com.fixit.app.ui.provider.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.domain.model.ProviderStatus
import com.fixit.app.ui.components.Avatar
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.IconBox
import com.fixit.app.ui.components.ProviderTabBar
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.initialsFor
import kotlin.let
import kotlin.takeIf
import kotlin.text.format
import kotlin.text.isNotBlank
import kotlin.text.uppercase

@Composable
fun ProviderProfileScreen(
    onTabClick: (String) -> Unit,
    onEditProfile: () -> Unit,
    onServicesAndRates: () -> Unit,
    onPaymentAndPayouts: () -> Unit,
    onReviews: () -> Unit,
    onHelp: () -> Unit,
    viewModel: ProviderProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }

    FixItScreen(bg = C.Subtle) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box {
                        val name = state.user?.name?.takeIf { it.isNotBlank() } ?: "Provider"
                        Avatar(
                            initials = initialsFor(name),
                            color = C.Orange,
                            size = 80,
                            fontSize = 28,
                            photoUrl = state.user?.profilePhotoUrl
                        )
                        if (state.user?.providerStatus == ProviderStatus.APPROVED) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 2.dp, y = 2.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(C.Blue)
                                    .border(2.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                    Text(
                        state.user?.name ?: "—",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        color = C.Ink
                    )
                    state.user?.phone?.let {
                        Text(it, fontSize = 13.sp, color = C.Slate)
                    }
                    Row(
                        Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Stat(
                            value = state.ratingSummary?.let { "%.1f ★".format(it.avgRating) } ?: "—",
                            label = "Rating"
                        )
                        Box(Modifier.width(1.dp).height(28.dp).background(C.Line))
                        Stat(value = state.totalJobs.toString(), label = "Jobs")
                        Box(Modifier.width(1.dp).height(28.dp).background(C.Line))
                        Stat(value = "${state.completionPercent}%", label = "Complete")
                    }
                }

                // Settings
                Column(
                    Modifier.padding(horizontal = 20.dp).padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingRow(Icons.Filled.Person, "Edit profile", onClick = onEditProfile)
                    SettingRow(Icons.Filled.Work, "Services & rates", onClick = onServicesAndRates)
                    SettingRow(
                        Icons.Filled.CreditCard,
                        "Payment & payouts",
                        onClick = onPaymentAndPayouts
                    )
                    SettingRow(Icons.Filled.Star, "Reviews", onClick = onReviews)
                    val statusBadge = when (state.user?.providerStatus) {
                        ProviderStatus.APPROVED -> "Verified"
                        ProviderStatus.PENDING  -> "Pending"
                        ProviderStatus.REJECTED -> "Rejected"
                        null -> null
                    }
                    SettingRow(Icons.Filled.Security, "Verification", badge = statusBadge)
                    SettingRow(Icons.Filled.HelpOutline, "Help & support", onClick = onHelp)
                    SettingRow(
                        Icons.AutoMirrored.Filled.Logout,
                        "Sign out",
                        danger = true,
                        onClick = { showSignOutDialog = true }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            if (state.isLoading && state.user == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = C.Blue)
                }
            }
        }
        ProviderTabBar(active = "profile", onTabClick = onTabClick)
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?") },
            text = { Text("You'll need your phone number to sign back in.") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    viewModel.signOut()
                }) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = C.Ink)
        Text(label, fontSize = 11.sp, color = C.Slate, modifier = Modifier.padding(top = 1.dp))
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    badge: String? = null,
    danger: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, C.Line, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconBox(bg = if (danger) Color(0xFFFEE2E2) else C.BlueSoft) {
            Icon(
                icon, null,
                tint = if (danger) Color(0xFFDC2626) else C.Blue,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (danger) Color(0xFFDC2626) else C.Ink,
            modifier = Modifier.weight(1f)
        )
        if (badge != null) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(C.GreenSoft)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    badge.uppercase(),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.GreenText,
                    letterSpacing = 0.3.sp
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = C.Mute,
            modifier = Modifier.size(14.dp)
        )
    }
}