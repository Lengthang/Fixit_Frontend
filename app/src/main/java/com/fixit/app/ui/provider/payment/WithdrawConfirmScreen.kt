package com.fixit.app.ui.provider.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.domain.model.PaymentMethodType
import com.fixit.app.domain.model.SavedPaymentMethod
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.formatMoney

@Composable
fun WithdrawConfirmScreen(
    onBack: () -> Unit,
    viewModel: WithdrawConfirmViewModel = hiltViewModel(),
) {
    val state         by viewModel.state.collectAsState()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WithdrawConfirmEffect.WithdrawalComplete -> {
                    snackbarState.showSnackbar("Withdrawal submitted — funds arriving in 1–2 days")
                    onBack()
                }
                is WithdrawConfirmEffect.ShowError ->
                    snackbarState.showSnackbar(effect.message)
            }
        }
    }

    FixItScreen(bg = C.Subtle) {
        // ── Top bar ───────────────────────────────────────────────────────────
        WithdrawTopBar(
            title    = "Withdraw funds",
            subtitle = state.defaultMethod?.let { "To ${it.shortLabel}" }
                ?: "Withdrawing from your wallet",
            onBack   = onBack,
        )

        if (state.isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = C.Blue)
            }
        } else {
            // ── Scrollable body ────────────────────────────────────────────────
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {

                // Available balance chip
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(C.BlueSoft)
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                    ) {
                        Text(
                            "Available: ${formatMoney(state.balance)}",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = C.Blue,
                        )
                    }
                }

                // Big centered amount input
                CenteredAmountInput(
                    input         = state.withdrawAmountInput,
                    onValueChange = { viewModel.updateWithdrawAmount(it) },
                    error         = state.withdrawAmountError,
                    maxLabel      = formatMoney(state.balance),
                )

                // Quick amount chips
                QuickAmountChips(
                    input        = state.withdrawAmountInput,
                    onPick       = { viewModel.updateWithdrawAmount(it) },
                    onMax        = { viewModel.setMaxWithdrawAmount() },
                )

                Spacer(Modifier.height(24.dp))

                // Sending to
                SectionLabel(
                    "Sending to",
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
                )

                SendingToCard(
                    defaultMethod = state.defaultMethod,
                    onChange      = onBack,
                )

                // No-fee notice
                NoFeeNotice()

                Spacer(Modifier.height(16.dp))
            }

            // ── Sticky confirm button ─────────────────────────────────────────
            Column(Modifier.fillMaxWidth().background(C.Bg)) {
                HorizontalDivider(color = C.Line)
                val canConfirm = !state.isWithdrawing &&
                        state.defaultMethod != null &&
                        state.withdrawAmount != null
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .padding(bottom = 4.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (canConfirm) C.Blue else C.Mute)
                        .clickable(enabled = canConfirm) { viewModel.confirmWithdrawal() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isWithdrawing) {
                        CircularProgressIndicator(
                            color       = Color.White,
                            modifier    = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        val displayAmount = state.withdrawAmount?.let { formatMoney(it) } ?: "—"
                        Text(
                            "Confirm $displayAmount",
                            color      = Color.White,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        SnackbarHost(snackbarState, modifier = Modifier.padding(horizontal = 16.dp))
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@Composable
private fun WithdrawTopBar(title: String, subtitle: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(C.Bg)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(C.Subtle)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint     = C.Ink,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize      = 16.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = C.Ink,
                    letterSpacing = (-0.2).sp,
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color    = C.Slate,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        HorizontalDivider(color = C.Line)
    }
}

// ── Big centered amount input ─────────────────────────────────────────────────

@Composable
private fun CenteredAmountInput(
    input: String,
    onValueChange: (String) -> Unit,
    error: String?,
    maxLabel: String,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$",
                fontSize   = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color      = C.Slate,
                modifier   = Modifier.padding(end = 4.dp, bottom = 10.dp),
            )

            BasicTextField(
                value           = input,
                onValueChange   = onValueChange,
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle       = TextStyle(
                    color         = C.Ink,
                    fontSize      = 64.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = (-2).sp,
                    textAlign     = TextAlign.Center,
                ),
                cursorBrush = SolidColor(C.Blue),
                decorationBox = { inner ->
                    if (input.isEmpty()) {
                        Text(
                            "0",
                            color      = C.Mute,
                            fontSize   = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-2).sp,
                        )
                    }
                    inner()
                },
                modifier = Modifier.widthIn(min = 60.dp),
            )

            // Faint ".00" suffix when the user hasn't typed cents
            if (!input.contains(".")) {
                Text(
                    ".00",
                    fontSize   = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = C.Mute,
                    modifier   = Modifier.padding(start = 2.dp, bottom = 10.dp),
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            error ?: "Max: $maxLabel",
            fontSize = 12.sp,
            color    = if (error != null) Color(0xFFDC2626) else C.Mute,
        )
    }
}

// ── Quick amount chips ─────────────────────────────────────────────────────────

@Composable
private fun QuickAmountChips(
    input: String,
    onPick: (String) -> Unit,
    onMax: () -> Unit,
) {
    val presets = listOf("100", "500", "1000")
    val parsedInput = input.toBigDecimalOrNull()

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        presets.forEach { value ->
            val active = parsedInput != null &&
                    parsedInput == value.toBigDecimalOrNull()
            QuickChip(
                label  = "$$value",
                active = active,
                onClick = { onPick(value) },
                modifier = Modifier.weight(1f),
            )
        }
        QuickChip(
            label   = "Max",
            active  = false,
            onClick = { onMax() },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuickChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .border(
                1.5.dp,
                if (active) C.Blue else C.Line,
                RoundedCornerShape(19.dp),
            )
            .background(if (active) C.BlueSoft else C.Bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (active) C.Blue else C.Slate,
        )
    }
}

// ── Sending to card ────────────────────────────────────────────────────────────

@Composable
private fun SendingToCard(
    defaultMethod: SavedPaymentMethod?,
    onChange: () -> Unit,
) {
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .background(C.Bg),
    ) {
        // Default method — selected
        if (defaultMethod != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(C.BlueSoft.copy(alpha = 0.55f))
                    .clickable { /* already selected */ }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                MethodIcon(defaultMethod)
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            defaultMethod.shortLabel,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = C.Ink,
                        )
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(C.GreenSoft)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "DEFAULT",
                                fontSize      = 9.sp,
                                fontWeight    = FontWeight.Bold,
                                color         = C.GreenText,
                                letterSpacing = 0.3.sp,
                            )
                        }
                    }
                    Text(
                        "Est. 1–2 business days",
                        fontSize = 12.sp,
                        color    = C.Slate,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                SelectedRadio()
            }

            HorizontalDivider(color = C.Line)

            // Instant option — informational (no logic to drive it)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF7C3AED)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = null,
                        tint     = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Instant to debit card",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = C.Ink,
                    )
                    Text(
                        "Arrives today · 1.5% fee",
                        fontSize = 12.sp,
                        color    = C.Slate,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                UnselectedRadio()
            }
        } else {
            // No default — let the user go back and pick a method
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onChange() }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(C.Subtle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.AccountBalance,
                        contentDescription = null,
                        tint     = C.Slate,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Choose a method",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = C.Ink,
                    )
                    Text(
                        "Tap to pick where to send funds",
                        fontSize = 12.sp,
                        color    = C.Slate,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    "Change",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = C.Blue,
                )
            }
        }
    }
}

@Composable
private fun SelectedRadio() {
    Box(
        Modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(2.dp, C.Blue, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(C.Blue),
        )
    }
}

@Composable
private fun UnselectedRadio() {
    Box(
        Modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(2.dp, C.Line, CircleShape),
    )
}

@Composable
private fun MethodIcon(method: SavedPaymentMethod) {
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (method.type == PaymentMethodType.BANK) C.Blue else C.Ink),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (method.type == PaymentMethodType.BANK)
                Icons.Filled.AccountBalance else Icons.Filled.CreditCard,
            contentDescription = null,
            tint     = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ── No-fee notice ──────────────────────────────────────────────────────────────

@Composable
private fun NoFeeNotice() {
    Row(
        Modifier
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(C.GreenSoft)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(C.GreenText),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint     = Color.White,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            "No fees for standard bank transfer",
            fontSize   = 13.sp,
            color      = C.GreenText,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ── Section label ──────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text          = text.uppercase(),
        fontSize      = 11.sp,
        fontWeight    = FontWeight.Bold,
        color         = C.Slate,
        letterSpacing = 0.5.sp,
        modifier      = modifier,
    )
}