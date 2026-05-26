package com.fixit.app.ui.provider.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.domain.model.PaymentMethodType
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.time.toJavaInstant

@Composable
fun PaymentPayoutsScreen(
    onBack: () -> Unit,
    onTabClick: (String) -> Unit,
    onNavigateToWithdrawConfirm: () -> Unit,
    onSeeAllHistory: () -> Unit,
    viewModel: PaymentPayoutsViewModel = hiltViewModel(),
) {
    val state         by viewModel.state.collectAsState()
    val snackbarState = remember { SnackbarHostState() }

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
        // ── Top bar ───────────────────────────────────────────────────────────
        PaymentTopBar(title = "Payment & payouts", onBack = onBack)

        // ── Scrollable body ───────────────────────────────────────────────────
        Box(Modifier.weight(1f)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── Balance hero card ─────────────────────────────────────────
                BalanceHeroCard(
                    balance       = state.balance,
                    onWithdraw    = { viewModel.onWithdrawClicked() },
                    onHistory     = { onSeeAllHistory() },
                )

                // ── Snapshot stats row ────────────────────────────────────────
                SnapshotStatsRow(
                    state = state,
                )

                // ── Payout methods section ────────────────────────────────────
                SectionLabel(
                    "Payout methods",
                    modifier = Modifier.padding(
                        start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp
                    ),
                )

                if (state.methods.isEmpty() && !state.isLoading) {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        EmptyState(
                            title    = "No payout methods yet",
                            subtitle = "Add a bank account or card to receive payouts",
                        )
                    }
                } else {
                    Column(
                        Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
                            .background(C.Bg),
                    ) {
                        state.methods.forEachIndexed { idx, method ->
                            PayoutMethodRow(
                                method       = method,
                                showDivider  = idx != state.methods.lastIndex,
                                onSetDefault = { viewModel.setDefault(method.id) },
                                onDelete     = { viewModel.deleteMethod(method.id) },
                            )
                        }
                    }
                }

                // ── Add method row (separate card) ────────────────────────────
                AddMethodRow(onClick = { viewModel.showAddMethodSheet() })

                // ── Auto-payout notice (only when a default exists) ───────────
                state.defaultMethod?.let { default ->
                    if (state.balance.signum() > 0) {
                        AutoPayoutNotice(
                            method  = default,
                            balance = state.balance,
                        )
                    }
                }

                // ── Recent payouts ────────────────────────────────────────────
                Row(
                    Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel("Recent payouts", modifier = Modifier.weight(1f))
                    Text(
                        "See all",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = C.Blue,
                        modifier   = Modifier.clickable { onSeeAllHistory() },
                    )
                }

                if (state.recentTransactions.isEmpty() && !state.isLoading) {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        EmptyState(
                            title    = "No transactions yet",
                            subtitle = "Completed jobs and withdrawals will appear here",
                        )
                    }
                } else {
                    Column(
                        Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
                            .background(C.Bg),
                    ) {
                        val recent = state.recentTransactions.take(4)
                        recent.forEachIndexed { idx, tx ->
                            PayoutHistoryRow(
                                tx          = tx,
                                showDivider = idx != recent.lastIndex,
                            )
                        }
                    }
                }

                // ── View all history button ───────────────────────────────────
                Box(
                    Modifier
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .border(1.5.dp, C.Line, RoundedCornerShape(23.dp))
                        .background(C.Bg)
                        .clickable { onSeeAllHistory() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "View all history",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = C.Slate,
                    )
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
            onAddMethod           = {
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
            methodType          = state.addMethodType,
            displayName         = state.addMethodDisplayName,
            lastFour            = state.addMethodLastFour,
            isDefault           = state.addMethodIsDefault,
            isSubmitting        = state.isAddingMethod,
            onTypeChange        = { viewModel.updateAddMethodType(it) },
            onDisplayNameChange = { viewModel.updateAddMethodDisplayName(it) },
            onLastFourChange    = { viewModel.updateAddMethodLastFour(it) },
            onIsDefaultChange   = { viewModel.updateAddMethodIsDefault(it) },
            onSubmit            = { viewModel.submitAddMethod() },
            onDismiss           = { viewModel.dismissAddMethodSheet() },
        )
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@Composable
private fun PaymentTopBar(title: String, onBack: () -> Unit) {
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
            Text(
                title,
                fontSize      = 16.sp,
                fontWeight    = FontWeight.Bold,
                color         = C.Ink,
                letterSpacing = (-0.2).sp,
                modifier      = Modifier.weight(1f),
            )
        }
        HorizontalDivider(color = C.Line)
    }
}

// ── Balance hero card ──────────────────────────────────────────────────────────

@Composable
private fun BalanceHeroCard(
    balance: BigDecimal,
    onWithdraw: () -> Unit,
    onHistory: () -> Unit,
) {
    Box(
        Modifier
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(C.Blue),
    ) {
        // Decorative circle (top-right)
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-40).dp)
                .size(140.dp)
                .clip(CircleShape)
                .background(C.Orange.copy(alpha = 0.18f)),
        )
        // Decorative circle (bottom-left)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-30).dp, y = 40.dp)
                .size(90.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f)),
        )

        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Text(
                "AVAILABLE TO WITHDRAW",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Medium,
                color         = Color.White.copy(alpha = 0.85f),
                letterSpacing = 0.4.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                formatMoney(balance),
                fontSize      = 36.sp,
                fontWeight    = FontWeight.ExtraBold,
                color         = Color.White,
                letterSpacing = (-1).sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Withdraw
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(C.Orange)
                        .clickable(enabled = balance.signum() > 0) { onWithdraw() },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Filled.ArrowDownward,
                            contentDescription = null,
                            tint     = Color.White,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            "Withdraw",
                            color      = Color.White,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                // History
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { onHistory() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "History",
                        color      = Color.White,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ── Snapshot stats row ─────────────────────────────────────────────────────────

@Composable
private fun SnapshotStatsRow(state: PaymentPayoutsState) {
    val weekDelta = state.weekChangePercent
    val weekTrend = weekDelta?.let {
        val arrow = if (it >= 0) "↑" else "↓"
        "$arrow ${abs(it)}%"
    } ?: "—"
    val weekTrendColor = when {
        weekDelta == null  -> C.Mute
        weekDelta >= 0     -> C.GreenText
        else               -> C.Slate
    }

    val nextPayoutLabel = nextFridayLabel()

    val jobsDoneCount = state.recentTransactions.count { it.type == TransactionType.RELEASE }
    val jobsThisWeek  = state.recentTransactions.count {
        it.type == TransactionType.RELEASE && isThisWeek(it)
    }

    Row(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .background(C.Bg)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SnapStat(
            value      = formatMoney(state.thisWeekTotal),
            label      = "This week",
            trend      = weekTrend,
            trendColor = weekTrendColor,
        )
        VertDivider()
        SnapStat(
            value      = formatMoney(state.balance),
            label      = "Pending payout",
            trend      = nextPayoutLabel,
            trendColor = C.Mute,
        )
        VertDivider()
        SnapStat(
            value      = jobsDoneCount.toString(),
            label      = "Jobs done",
            trend      = "$jobsThisWeek this week",
            trendColor = C.Mute,
        )
    }
}

@Composable
private fun RowScope.SnapStat(
    value: String,
    label: String,
    trend: String,
    trendColor: Color,
) {
    Column(
        Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            fontSize      = 16.sp,
            fontWeight    = FontWeight.ExtraBold,
            color         = C.Ink,
            letterSpacing = (-0.3).sp,
        )
        Text(
            label,
            fontSize   = 10.sp,
            color      = C.Slate,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.padding(top = 1.dp),
        )
        Text(
            trend,
            fontSize   = 10.sp,
            color      = trendColor,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun VertDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(40.dp)
            .background(C.Line),
    )
}

// ── Payout method row (inside grouped card) ───────────────────────────────────

@Composable
private fun PayoutMethodRow(
    method: SavedPaymentMethod,
    showDivider: Boolean,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MethodIcon(method)
        Column(Modifier.weight(1f)) {
            Text(
                method.displayName,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = C.Ink,
            )
            if (method.lastFour != null) {
                Text(
                    "••${method.lastFour}",
                    fontSize = 12.sp,
                    color    = C.Slate,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (method.isDefault) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(C.GreenSoft)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
                Text(
                    "DEFAULT",
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = C.GreenText,
                    letterSpacing = 0.4.sp,
                )
            }
        }
        Box {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(C.Subtle)
                    .clickable { menuExpanded = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint     = C.Mute,
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(
                expanded         = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                if (!method.isDefault) {
                    DropdownMenuItem(
                        text    = { Text("Set as default", fontSize = 13.sp) },
                        onClick = { menuExpanded = false; onSetDefault() },
                    )
                }
                DropdownMenuItem(
                    text    = { Text("Remove", fontSize = 13.sp, color = Color(0xFFDC2626)) },
                    onClick = { menuExpanded = false; onDelete() },
                )
            }
        }
    }
    if (showDivider) {
        HorizontalDivider(
            color    = C.Line,
            modifier = Modifier.padding(start = 64.dp),
        )
    }
}

@Composable
internal fun MethodIcon(method: SavedPaymentMethod, size: Int = 38) {
    val bg = when (method.type) {
        PaymentMethodType.BANK -> C.Blue
        PaymentMethodType.CARD -> C.Ink
    }
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = when (method.type) {
                PaymentMethodType.BANK -> Icons.Filled.AccountBalance
                PaymentMethodType.CARD -> Icons.Filled.CreditCard
            },
            contentDescription = null,
            tint     = Color.White,
            modifier = Modifier.size((size * 0.5f).dp),
        )
    }
}

// ── Add method row (separate card under methods list) ─────────────────────────

@Composable
private fun AddMethodRow(onClick: () -> Unit) {
    Row(
        Modifier
            .padding(start = 20.dp, end = 20.dp, top = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, C.Line, RoundedCornerShape(12.dp))
            .background(C.Bg)
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(C.BlueSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "+",
                fontSize   = 22.sp,
                color      = C.Blue,
                fontWeight = FontWeight.Light,
            )
        }
        Text(
            "Add payout method",
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = C.Blue,
            modifier   = Modifier.weight(1f),
        )
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint     = C.Mute,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ── Auto-payout notice (blue soft pill) ────────────────────────────────────────

@Composable
private fun AutoPayoutNotice(method: SavedPaymentMethod, balance: BigDecimal) {
    Row(
        Modifier
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(C.BlueSoft)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(C.Blue),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Event,
                contentDescription = null,
                tint     = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                "Automatic payout scheduled",
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = C.BlueDark,
            )
            Text(
                "${nextFridayLabel()} · ${formatMoney(balance)} → ${method.shortLabel}",
                fontSize = 11.sp,
                color    = C.BlueDark.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

// ── Recent payout row (inside grouped card) ────────────────────────────────────

@Composable
private fun PayoutHistoryRow(tx: WalletTransaction, showDivider: Boolean) {
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
        .format(DateTimeFormatter.ofPattern("MMM d"))

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconBox(bg = if (isIncome) C.GreenSoft else C.BlueSoft) {
            Icon(
                if (isIncome) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = null,
                tint     = if (isIncome) C.GreenText else C.Blue,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
            Text(date, fontSize = 11.sp, color = C.Mute, modifier = Modifier.padding(top = 1.dp))
        }
        Text(
            "$sign${formatMoney(tx.amount.abs())}",
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            color      = if (isIncome) C.GreenText else C.Ink,
        )
    }
    if (showDivider) HorizontalDivider(color = C.Line)
}

// ── Shared helpers ─────────────────────────────────────────────────────────────

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

/** "Fri Apr 25" — the next Friday relative to today. */
private fun nextFridayLabel(): String {
    val today = LocalDate.now()
    val daysUntilFriday = ((DayOfWeek.FRIDAY.value - today.dayOfWeek.value + 7) % 7)
        .let { if (it == 0) 7 else it }
    val next = today.plusDays(daysUntilFriday.toLong())
    return next.format(DateTimeFormatter.ofPattern("EEE MMM d"))
}

/** True when the transaction's createdAt falls in the current ISO week. */
private fun isThisWeek(tx: WalletTransaction): Boolean {
    val zone   = ZoneId.systemDefault()
    val today  = LocalDate.now(zone)
    val txDate = tx.createdAt.toJavaInstant().atZone(zone).toLocalDate()
    val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
    return !txDate.isBefore(weekStart) && !txDate.isAfter(today)
}