package com.fixit.app.ui.provider.payment

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
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.fixit.app.domain.model.SavedPaymentMethod
import com.fixit.app.domain.model.TransactionType
import com.fixit.app.domain.model.WalletTransaction
import com.fixit.app.ui.components.EmptyState
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.IconBox
import com.fixit.app.ui.components.ProviderTabBar
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.formatMoney
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.toJavaInstant
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import com.fixit.app.domain.model.PaymentMethodType
import kotlin.math.abs

@Composable
fun PaymentPayoutsScreen(
    onBack: () -> Unit,
    onTabClick: (String) -> Unit,
    onNavigateToWithdrawConfirm: () -> Unit,
    onSeeAllHistory: () -> Unit,
    viewModel: PaymentPayoutsViewModel = hiltViewModel(),
) {
    val state           by viewModel.state.collectAsState()
    val snackbarState   = remember { SnackbarHostState() }

    // Collect one-shot effects
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PaymentPayoutsEffect.NavigateToWithdrawConfirm -> onNavigateToWithdrawConfirm()
                is PaymentPayoutsEffect.WithdrawalComplete ->
                    snackbarState.showSnackbar("Withdrawal submitted — funds arriving in 1–2 days")
                is PaymentPayoutsEffect.ShowError ->
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
            Column(Modifier.weight(1f)) {
                Text(
                    "Payment & payouts",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.Ink,
                )
                Text(
                    "Earnings, methods & withdrawals",
                    fontSize = 11.5.sp,
                    color = C.Slate,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }

        // ── Scrollable body ───────────────────────────────────────────────────
        Box(Modifier.weight(1f)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {

                // ── Balance hero ──────────────────────────────────────────────
                Box(Modifier.padding(horizontal = 20.dp).padding(top = 14.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(C.Blue)
                            .padding(18.dp)
                    ) {
                        // Decorative circles
                        Box(
                            Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(70.dp))
                                .background(C.Orange.copy(alpha = 0.2f))
                                .align(Alignment.TopEnd)
                                .offset(x = 30.dp, y = (-40).dp)
                        )
                        Box(
                            Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(45.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .align(Alignment.BottomEnd)
                                .offset(x = (-30).dp, y = 40.dp)
                        )

                        Column {
                            Text(
                                "AVAILABLE TO WITHDRAW",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.4.sp,
                            )
                            Text(
                                formatMoney(state.balance),
                                color = Color.White,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-1).sp,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            // Action buttons
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                // Withdraw
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(C.Orange)
                                        .clickable(enabled = state.balance.signum() > 0) {
                                            viewModel.onWithdrawClicked()
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.ArrowDownward,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp),
                                        )
                                        Text(
                                            "Withdraw",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                                // History
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(Color.White.copy(alpha = 0.16f))
                                        .border(
                                            1.5.dp,
                                            Color.White.copy(alpha = 0.3f),
                                            RoundedCornerShape(22.dp),
                                        )
                                        .clickable { onSeeAllHistory() },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "History",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }

                // ── This week chart ───────────────────────────────────────────
                Box(Modifier.padding(horizontal = 20.dp).padding(top = 14.dp)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("This week", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = C.Ink)
                        }

                        val maxBar = state.weeklyBars.maxOrNull() ?: BigDecimal.ZERO
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            val labels = listOf("M", "T", "W", "T", "F", "S", "S")
                            val todayIdx = java.time.LocalDate.now().dayOfWeek.value - 1
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
                                    Text(labels[i], fontSize = 9.5.sp, color = C.Mute)
                                }
                            }
                        }

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                formatMoney(state.thisWeekTotal),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = C.Ink,
                                letterSpacing = (-0.4).sp,
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

                // ── Payment methods ────────────────────────────────────────────
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "PAYMENT METHODS",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = C.Slate,
                        letterSpacing = 0.4.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "+ Add new",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = C.Blue,
                        modifier = Modifier.clickable { viewModel.showAddMethodSheet() },
                    )
                }
                Spacer(Modifier.height(10.dp))

                if (state.methods.isEmpty() && !state.isLoading) {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        EmptyState(
                            title = "No payment methods yet",
                            subtitle = "Add a bank account or card to receive payouts",
                        )
                    }
                } else {
                    Column(
                        Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        state.methods.forEach { method ->
                            PaymentMethodTile(
                                method     = method,
                                onSetDefault = { viewModel.setDefault(method.id) },
                                onDelete   = { viewModel.deleteMethod(method.id) },
                            )
                        }
                    }
                }

                // ── Add new method CTA (dashed) ─────────────────────────────
                Box(
                    Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 10.dp),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.5.dp, C.Line, RoundedCornerShape(14.dp))
                            .clickable { viewModel.showAddMethodSheet() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(C.BlueSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("+", fontSize = 20.sp, color = C.Blue, fontWeight = FontWeight.Light)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Add payment method",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = C.Blue,
                            )
                            Text(
                                "Bank or card",
                                fontSize = 11.sp,
                                color = C.Slate,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        Icon(
                            Icons.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = C.Blue,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // ── Recent payouts ─────────────────────────────────────────────
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "RECENT PAYOUTS",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = C.Slate,
                        letterSpacing = 0.4.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "See all",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = C.Blue,
                        modifier = Modifier.clickable { onSeeAllHistory() },
                    )
                }
                Spacer(Modifier.height(10.dp))

                if (state.recentTransactions.isEmpty() && !state.isLoading) {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        EmptyState(
                            title = "No transactions yet",
                            subtitle = "Completed jobs and withdrawals will appear here",
                        )
                    }
                } else {
                    Column(
                        Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        state.recentTransactions.forEach { tx ->
                            PayoutRow(tx)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // Loading overlay
            if (state.isLoading && state.methods.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = C.Blue)
                }
            }
        }

        SnackbarHost(snackbarState, modifier = Modifier.padding(horizontal = 16.dp))
        ProviderTabBar(active = "profile", onTabClick = onTabClick)
    }

    // ── Withdraw select sheet (no default path) ───────────────────────────────
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
                viewModel.dismissWithdrawSheet()
                viewModel.showAddMethodSheet()
            },
            onWithdraw            = { viewModel.submitWithdrawal() },
            onDismiss             = { viewModel.dismissWithdrawSheet() },
        )
    }

    // ── Add method sheet ──────────────────────────────────────────────────────
    if (state.showAddMethodSheet) {
        AddPaymentMethodSheet(
            methodType       = state.addMethodType,
            displayName      = state.addMethodDisplayName,
            lastFour         = state.addMethodLastFour,
            isDefault        = state.addMethodIsDefault,
            isSubmitting     = state.isAddingMethod,
            onTypeChange     = { viewModel.updateAddMethodType(it) },
            onDisplayNameChange = { viewModel.updateAddMethodDisplayName(it) },
            onLastFourChange = { viewModel.updateAddMethodLastFour(it) },
            onIsDefaultChange = { viewModel.updateAddMethodIsDefault(it) },
            onSubmit         = { viewModel.submitAddMethod() },
            onDismiss        = { viewModel.dismissAddMethodSheet() },
        )
    }
}

// ── Payment method tile ───────────────────────────────────────────────────────

@Composable
private fun PaymentMethodTile(
    method: SavedPaymentMethod,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(
                1.5.dp,
                if (method.isDefault) C.Blue else C.Line,
                RoundedCornerShape(14.dp),
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Icon
        MethodIcon(method)

        // Details
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(method.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = C.Ink)
                if (method.isDefault) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(C.Blue)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "✓  Default",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.3.sp,
                        )
                    }
                }
            }
            if (method.lastFour != null) {
                Text(
                    "••${method.lastFour}",
                    fontSize = 12.sp,
                    color = C.Slate,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (!method.isDefault) {
                Text(
                    "Set as default",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = C.Blue,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { onSetDefault() },
                )
            }
        }

        // Three-dot overflow
        Box {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(C.Subtle)
                    .clickable { menuExpanded = true },
                contentAlignment = Alignment.Center,
            ) {
                Text("•••", fontSize = 12.sp, color = C.Slate, letterSpacing = 1.sp)
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                if (!method.isDefault) {
                    DropdownMenuItem(
                        text = { Text("Set as default", fontSize = 13.sp) },
                        onClick = { menuExpanded = false; onSetDefault() },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Remove", fontSize = 13.sp, color = Color(0xFFDC2626)) },
                    onClick = { menuExpanded = false; onDelete() },
                )
            }
        }
    }
}

// ── Method icon ───────────────────────────────────────────────────────────────

@Composable
internal fun MethodIcon(method: SavedPaymentMethod, size: Int = 44) {
    val bg = when (method.type) {
        PaymentMethodType.BANK -> C.Blue
        PaymentMethodType.CARD -> C.Ink
    }
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = when (method.type) {
                PaymentMethodType.BANK -> Icons.Filled.AccountBalance
                PaymentMethodType.CARD -> Icons.Filled.CreditCard
            },
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size((size * 0.5f).dp),
        )
    }
}
// ── Recent payout row ─────────────────────────────────────────────────────────

@Composable
private fun PayoutRow(tx: WalletTransaction) {
    val isIncome = tx.amount.signum() > 0
    val sign     = if (isIncome) "+" else "−"
    val label    = tx.description?.takeIf { it.isNotBlank() }
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
        .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

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
            Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
            Text(date, fontSize = 10.5.sp, color = C.Mute, modifier = Modifier.padding(top = 1.dp))
        }
        Text(
            "$sign${formatMoney(tx.amount.abs())}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isIncome) C.GreenText else C.Ink,
        )
    }
}