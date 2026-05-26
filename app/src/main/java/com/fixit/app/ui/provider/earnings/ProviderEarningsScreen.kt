package com.fixit.app.ui.provider.earnings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.domain.model.TransactionType
import com.fixit.app.domain.model.WalletTransaction
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.IconBox
import com.fixit.app.ui.components.ProviderTabBar
import com.fixit.app.ui.provider.payment.AddPaymentMethodSheet
import com.fixit.app.ui.provider.payment.WithdrawSelectSheet
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.OnLifecycleStart
import com.fixit.app.ui.util.formatMoney
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.time.toJavaInstant

@Composable
fun ProviderEarningsScreen(
    onBack: () -> Unit,
    onTabClick: (String) -> Unit,
    onNavigateToWithdrawConfirm: () -> Unit,
    viewModel: ProviderEarningsViewModel = hiltViewModel(),
) {
    val state         by viewModel.state.collectAsState()
    val snackbarState = remember { SnackbarHostState() }

    // Add method sheet transient state lives here so we can pass it down
    var addType        by remember { mutableStateOf("bank") }
    var addName        by remember { mutableStateOf("") }
    var addLastFour    by remember { mutableStateOf("") }
    var addIsDefault   by remember { mutableStateOf(false) }
    var isAddSubmitting by remember { mutableStateOf(false) }
    OnLifecycleStart(viewModel::refresh)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is EarningsEffect.NavigateToWithdrawConfirm -> onNavigateToWithdrawConfirm()
                is EarningsEffect.WithdrawalComplete ->
                    snackbarState.showSnackbar("Withdrawal submitted — funds arriving in 1–2 days")
                is EarningsEffect.ShowError ->
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
                tint = C.Ink,
                modifier = Modifier.size(22.dp).clickable { onBack() },
            )
            Text(
                "Earnings",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = C.Ink,
                modifier = Modifier.weight(1f),
            )
        }

        Box(Modifier.weight(1f)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── Available balance hero ────────────────────────────────────
                Box(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(C.Blue)
                            .padding(18.dp),
                    ) {
                        Column {
                            Text(
                                "AVAILABLE TO WITHDRAW",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.3.sp,
                            )
                            Text(
                                formatMoney(state.balance),
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-1).sp,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(C.Orange)
                                    .clickable(enabled = state.balance.signum() > 0) {
                                        viewModel.onWithdrawClicked()
                                    }
                                    .padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "Withdraw to bank · 1–2 days",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // ── This week chart ────────────────────────────────────────────
                Box(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                    ) {
                        Text("This week", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = C.Ink)

                        val maxBar  = state.weeklyBars.maxOrNull() ?: BigDecimal.ZERO
                        val todayIdx = java.time.LocalDate.now().dayOfWeek.value - 1
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            val labels = listOf("M", "T", "W", "T", "F", "S", "S")
                            state.weeklyBars.forEachIndexed { i, value ->
                                val frac = if (maxBar.signum() == 0) 0f
                                else value.divide(maxBar, 4, RoundingMode.HALF_UP).toFloat()
                                Column(
                                    Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(frac.coerceAtLeast(0.04f))
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (i == todayIdx) C.Orange
                                                else C.Blue.copy(alpha = 0.85f)
                                            )
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(labels[i], fontSize = 10.sp, color = C.Mute)
                                }
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                formatMoney(state.thisWeekTotal),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = C.Ink,
                            )
                            state.weekChangePercent?.let { pct ->
                                val positive = pct >= 0
                                Text(
                                    "${if (positive) "↑" else "↓"} ${abs(pct)}% vs last week",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (positive) C.GreenText else C.Slate,
                                )
                            }
                        }
                    }
                }

                // ── Recent activity ───────────────────────────────────────────
                Text(
                    "RECENT ACTIVITY",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.Slate,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(
                        start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp,
                    ),
                )

                if (state.transactions.isEmpty() && !state.isLoading) {
                    Text(
                        "No transactions yet.",
                        fontSize = 12.sp,
                        color = C.Mute,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                } else {
                    Column(
                        Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        state.transactions.take(20).forEach { tx -> PayoutRow(tx) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (state.isLoading && state.transactions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = C.Blue)
                }
            }
        }

        SnackbarHost(snackbarState, modifier = Modifier.padding(horizontal = 16.dp))
        ProviderTabBar(active = "home", onTabClick = onTabClick)
    }

    // ── Withdraw select sheet ─────────────────────────────────────────────────
    if (state.showWithdrawSheet) {
        WithdrawSelectSheet(
            balance               = state.balance,
            methods               = state.methods,
            selectedMethodId      = state.selectedMethodIdForWithdraw,
            saveAsDefault         = state.saveAsDefault,
            isWithdrawing         = state.isWithdrawing,
            withdrawAmountInput   = state.withdrawAmountInput,
            withdrawAmountError   = state.withdrawAmountError,
            onAmountChange        = { viewModel.updateWithdrawAmount(it) },
            onSetMax              = { viewModel.setMaxWithdrawAmount() },
            onSelectMethod        = { viewModel.selectMethodForWithdraw(it) },
            onSaveAsDefaultChange = { viewModel.setSaveAsDefault(it) },
            onAddMethod          = {
                addType      = "bank"
                addName      = ""
                addLastFour  = ""
                addIsDefault = state.methods.isEmpty()
                viewModel.showAddMethodSheet()
            },
            onWithdraw            = { viewModel.submitWithdrawal() },
            onDismiss             = { viewModel.dismissWithdrawSheet() },
        )
    }

    // ── Add method sheet ──────────────────────────────────────────────────────
    if (state.showAddMethodSheet) {
        AddPaymentMethodSheet(
            methodType          = addType,
            displayName         = addName,
            lastFour            = addLastFour,
            isDefault           = addIsDefault,
            isSubmitting        = isAddSubmitting,
            onTypeChange        = { addType = it },
            onDisplayNameChange = { addName = it },
            onLastFourChange    = {
                if (it.length <= 4 && it.all { c -> c.isDigit() }) addLastFour = it
            },
            onIsDefaultChange   = { addIsDefault = it },
            onSubmit            = {
                isAddSubmitting = true
                viewModel.submitAddMethod(
                    type        = addType,
                    displayName = addName.trim(),
                    lastFour    = addLastFour.takeIf { it.length == 4 },
                    isDefault   = addIsDefault,
                )
                isAddSubmitting = false
            },
            onDismiss           = { viewModel.dismissAddMethodSheet() },
        )
    }
}

// ── Updated PayoutRow — green for income, blue for withdrawal ─────────────────

@Composable
private fun PayoutRow(tx: WalletTransaction) {
    val isIncome  = tx.amount.signum() > 0
    val sign      = if (isIncome) "+" else "−"
    val label     = tx.description?.takeIf { it.isNotBlank() }
        ?: when (tx.type) {
            TransactionType.TOP_UP  -> "Top-up"
            TransactionType.PAYMENT -> "Payment"
            TransactionType.PAYOUT  -> "Withdrawal"
            TransactionType.REFUND  -> "Refund"
            TransactionType.RELEASE -> "Job payment"
            TransactionType.UNKNOWN -> "Transaction"
        }
    val date = tx.createdAt.toJavaInstant()
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d"))

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, C.Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconBox(bg = if (isIncome) C.GreenSoft else C.BlueSoft) {
            Icon(
                if (isIncome) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = null,
                tint = if (isIncome) C.GreenText else C.Blue,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
            Text(date, fontSize = 11.sp, color = C.Mute, modifier = Modifier.padding(top = 1.dp))
        }
        Text(
            "$sign${formatMoney(tx.amount.abs())}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isIncome) C.GreenText else C.Ink,
        )
    }
}