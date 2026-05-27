package com.fixit.app.ui.customer.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.ui.components.Avatar
import com.fixit.app.ui.components.CustomerTabBar
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.IconBox
import com.fixit.app.ui.components.SettingRow
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.OnLifecycleStart
import com.fixit.app.ui.util.formatMoney
import com.fixit.app.ui.util.initialsFor

@Composable
fun CustomerProfileScreen(
    onTabClick: (String) -> Unit,
    onPersonalInfo: () -> Unit,
    onSavedAddresses: () -> Unit,
    onPaymentMethods: () -> Unit,
    onPromos: () -> Unit,
    onTopUp: () -> Unit,
    onHelp: () -> Unit,
    viewModel: CustomerProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }
    OnLifecycleStart(viewModel::refresh)

    FixItScreen(bg = C.Subtle) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── Hero ───────────────────────────────────────────────
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val displayName = state.name?.takeIf { it.isNotBlank() } ?: "Customer"
                    Avatar(
                        initials = initialsFor(displayName),
                        color = C.Blue,
                        size = 80,
                        fontSize = 28,
                        photoUrl = state.profilePhotoUrl,
                    )
                    Text(
                        displayName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        color = C.Ink,
                    )
                    // Email matches the screenshot. Falls back to phone if email
                    // wasn't captured during signup.
                    val subtitle = state.email?.takeIf { it.isNotBlank() }
                        ?: state.phone
                    if (!subtitle.isNullOrBlank()) {
                        Text(subtitle, fontSize = 13.sp, color = C.Slate)
                    }
                }

                // ── Wallet card ────────────────────────────────────────
                Column(
                    Modifier.padding(horizontal = 20.dp).padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WalletCard(
                        balanceText = formatMoney(state.walletBalance),
                        onTopUp = onTopUp,
                    )

                    Spacer(Modifier.height(4.dp))

                    // ── Settings ──────────────────────────────────────
                    SettingRow(
                        icon = Icons.Filled.Person,
                        label = "Personal info",
                        onClick = onPersonalInfo,
                    )
                    SettingRow(
                        icon = Icons.Filled.LocationOn,
                        label = "Saved addresses",
                        subtitle = when (state.savedAddressesCount) {
                            0 -> null
                            1 -> "1 address"
                            else -> "${state.savedAddressesCount} addresses"
                        },
                        onClick = onSavedAddresses,
                    )
                    SettingRow(
                        icon = Icons.Filled.CreditCard,
                        label = "Payment methods",
                        onClick = onPaymentMethods,
                    )
                    SettingRow(
                        icon = Icons.Filled.LocalOffer,
                        label = "Promos & rewards",
                        badge = state.activePromosCount.takeIf { it > 0 }?.toString(),
                        badgeAccent = true,
                        onClick = onPromos,
                    )
                    SettingRow(
                        icon = Icons.Filled.HelpOutline,
                        label = "Help & support",
                        onClick = onHelp,
                    )
                    SettingRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        label = "Sign out",
                        danger = true,
                        onClick = { showSignOutDialog = true },
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            if (state.isLoading && state.name == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = C.Blue)
                }
            }
        }
        CustomerTabBar(active = "profile", onTabClick = onTabClick)
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
            },
        )
    }
}

/**
 * The wallet block at the top of the settings list. Inline (not promoted to
 * ui/components/) — it's a one-off layout specific to this screen. The icon
 * + bordered card pattern itself reuses [IconBox] from ui/components.
 */
@Composable
private fun WalletCard(
    balanceText: String,
    onTopUp: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, C.Line, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBox(bg = C.Blue) {
            Icon(
                Icons.Filled.CreditCard,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text("FixIt Wallet", fontSize = 11.5.sp, color = C.Slate)
            Text(
                balanceText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = C.Ink,
            )
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(C.Orange)
                .clickable { onTopUp() }
                .padding(horizontal = 18.dp, vertical = 9.dp),
        ) {
            Text(
                "Top up",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}