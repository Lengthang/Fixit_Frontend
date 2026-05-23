package com.fixit.app.ui.signup.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.PrimaryButton
import com.fixit.app.ui.components.Progress
import com.fixit.app.ui.components.ScreenTitle
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.signup.SignupDraftViewModel
import com.fixit.app.ui.theme.C

@Composable
fun PaymentScreen(
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    draftVm: SignupDraftViewModel,
    vm: ProviderRegistrationViewModel = hiltViewModel(),
) {
    var selected by remember { mutableStateOf("bank") }
    val draft by draftVm.state.collectAsState()
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.effects.collect {
            when (it) { is RegistrationEffect.Registered -> onRegistered() }
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); vm.dismissError() }
    }

    FixItScreen {
        TopBar(step = 10, total = 11, onBack = onBack)
        Progress(step = 10, total = 11)
        ScreenTitle(
            "Select payment method",
            "How would you like to receive payouts from customer jobs?",
        )
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PayOption(Icons.Filled.AccountBalance, C.Blue,
                "Bank account", "Direct deposit · 1–2 business days",
                selected == "bank") { selected = "bank" }
            PayOption(Icons.Filled.Payment, C.Orange,
                "PayPal", "Instant transfer · 1% fee",
                selected == "paypal") { selected = "paypal" }
            PayOption(Icons.Filled.Circle, C.Ink,
                "Apple Pay", "Instant transfer",
                selected == "apple") { selected = "apple" }
        }
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(C.OrangeSoft).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Lock, null, tint = C.Orange, modifier = Modifier.size(18.dp))
                Text(
                    "All payment info is encrypted end-to-end. We never store full card numbers.",
                    fontSize = 12.sp, color = C.Ink, lineHeight = 18.sp,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        SnackbarHost(snackbar, modifier = Modifier.padding(horizontal = 16.dp))
        PrimaryButton(
            text = if (state.submitting) "Submitting…" else "Continue",
            enabled = !state.submitting,
            onClick = {
                // Capture preference locally (not posted to backend per design decision)
                draftVm.update { it.copy(payoutPreference = selected) }
                // Submit the full provider registration in one shot
                vm.submit(draft.copy(payoutPreference = selected))
            },
        )
    }
}

@Composable
private fun PayOption(
    icon: ImageVector, accent: Color,
    title: String, sub: String,
    selected: Boolean, onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) C.BlueSoft else Color.White)
            .border(1.5.dp, if (selected) C.Blue else C.Line, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(accent),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
            Spacer(Modifier.height(2.dp))
            Text(sub, fontSize = 12.sp, color = C.Slate)
        }
        Box(
            Modifier.size(20.dp).clip(CircleShape)
                .border(2.dp, if (selected) C.Blue else C.Line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(C.Blue))
        }
    }
}