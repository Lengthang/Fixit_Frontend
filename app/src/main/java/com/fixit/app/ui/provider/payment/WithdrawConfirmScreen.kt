package com.fixit.app.ui.provider.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.domain.model.PaymentMethodType
import com.fixit.app.domain.model.SavedPaymentMethod
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.formatMoney
import java.math.BigDecimal

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
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint     = C.Ink,
                modifier = Modifier.size(22.dp).clickable { onBack() },
            )
            Column(Modifier.weight(1f)) {
                Text("Withdraw", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.Ink)
                Text(
                    "To your default method",
                    fontSize = 11.5.sp,
                    color    = C.Slate,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }

        if (state.isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = C.Blue)
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth()) {

                // ── Amount input card ──────────────────────────────────────────
                Box(Modifier.padding(horizontal = 20.dp).padding(top = 20.dp)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, C.Line, RoundedCornerShape(16.dp))
                            .padding(18.dp),
                    ) {
                        // Snapshot delegated property so Kotlin can smart-cast String? → String
                        val amountError = state.withdrawAmountError

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "AMOUNT TO WITHDRAW",
                                fontSize      = 11.sp,
                                color         = if (amountError != null) Color(0xFFDC2626) else C.Slate,
                                fontWeight    = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp,
                            )
                            Text(
                                "Available: ${formatMoney(state.balance)}",
                                fontSize = 11.sp,
                                color    = C.Mute,
                            )
                        }
                        Spacer(Modifier.height(10.dp))

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(C.Subtle)
                                .border(
                                    1.5.dp,
                                    if (amountError != null) Color(0xFFDC2626) else C.Line,
                                    RoundedCornerShape(10.dp),
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("$", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = C.Ink)
                            BasicTextField(
                                value         = state.withdrawAmountInput,
                                onValueChange = { viewModel.updateWithdrawAmount(it) },
                                singleLine    = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = TextStyle(
                                    color         = C.Ink,
                                    fontSize      = 28.sp,
                                    fontWeight    = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp,
                                ),
                                cursorBrush = SolidColor(C.Blue),
                                modifier    = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (state.withdrawAmountInput.isEmpty()) {
                                        Text(
                                            "0.00",
                                            color      = C.Mute,
                                            fontSize   = 28.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                        )
                                    }
                                    inner()
                                },
                            )
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(C.BlueSoft)
                                    .clickable { viewModel.setMaxWithdrawAmount() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Text("Max", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = C.Blue)
                            }
                        }

                        if (amountError != null) {
                            Text(
                                amountError,
                                fontSize = 11.5.sp,
                                color    = Color(0xFFDC2626),
                                modifier = Modifier.padding(top = 4.dp, start = 2.dp),
                            )
                        }
                    }
                }

                // ── Sending to ─────────────────────────────────────────────────
                Text(
                    "SENDING TO",
                    fontSize   = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color      = C.Slate,
                    letterSpacing = 0.4.sp,
                    modifier   = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 14.dp, bottom = 8.dp),
                )

                state.defaultMethod?.let { method ->
                    SendingToTile(method = method, onChangeMethod = onBack)
                }

                // ── Summary ────────────────────────────────────────────────────
                Box(Modifier.padding(horizontal = 20.dp).padding(top = 14.dp)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, C.Line, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                    ) {
                        val displayAmount = state.withdrawAmount
                        SummaryLine(
                            label = "Amount",
                            value = if (displayAmount != null) formatMoney(displayAmount) else "—",
                            bold  = false,
                        )
                        SummaryLine("Transfer fee", "Free", bold = false)

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .height(1.dp)
                                .background(C.Line)
                        )

                        SummaryLine(
                            label = "You'll receive",
                            value = if (displayAmount != null) formatMoney(displayAmount) else "—",
                            bold  = true,
                        )

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(C.BlueSoft)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment     = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                tint     = C.Blue,
                                modifier = Modifier.size(13.dp).padding(top = 1.dp),
                            )
                            Text(
                                "Expected in 1–2 business days · We'll notify you when funds arrive.",
                                fontSize   = 11.sp,
                                color      = C.BlueDark,
                                lineHeight = 16.sp,
                            )
                        }
                    }
                }
            }
        }

        // ── Bottom action bar ─────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(1.dp, C.Line, RoundedCornerShape(0.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color.White)
                    .border(1.5.dp, C.Line, RoundedCornerShape(25.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.Slate)
            }

            val canConfirm = !state.isWithdrawing &&
                    state.defaultMethod != null &&
                    state.withdrawAmount != null
            Box(
                Modifier
                    .weight(1.6f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
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
                    Text(
                        "Confirm withdrawal",
                        color      = Color.White,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        SnackbarHost(snackbarState, modifier = Modifier.padding(horizontal = 16.dp))
    }
}

// ── Sending-to tile ───────────────────────────────────────────────────────────

@Composable
private fun SendingToTile(method: SavedPaymentMethod, onChangeMethod: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.5.dp, C.Blue, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (method.type == PaymentMethodType.BANK) C.Blue else C.Ink),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (method.type == PaymentMethodType.BANK)
                    Icons.Filled.AccountBalance else Icons.Filled.CreditCard,
                contentDescription = null,
                tint     = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(method.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = C.Ink)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(C.Blue)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("Default", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            if (method.lastFour != null) {
                Text(
                    "••${method.lastFour} · Arrives in 1–2 days",
                    fontSize = 12.sp,
                    color    = C.Slate,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Text(
            "Change",
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color      = C.Blue,
            modifier   = Modifier.clickable { onChangeMethod() },
        )
    }
}

// ── Summary line ──────────────────────────────────────────────────────────────

@Composable
private fun SummaryLine(label: String, value: String, bold: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            fontSize   = if (bold) 14.sp else 12.5.sp,
            color      = if (bold) C.Ink else C.Slate,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            value,
            fontSize   = if (bold) 16.sp else 13.sp,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color      = C.Ink,
        )
    }
}