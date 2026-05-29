package com.fixit.app.ui.customer.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.fixit.app.ui.components.EmptyState
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.FixItTextField
import com.fixit.app.ui.components.IconBox
import com.fixit.app.ui.components.PrimaryButton
import com.fixit.app.ui.components.ScreenTitle
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.OnLifecycleStart
import com.fixit.app.ui.util.formatMoney

@Composable
fun WalletTopUpScreen(
    onBack: () -> Unit,
    onAddPaymentMethod: () -> Unit,
    onTopUpComplete: () -> Unit,
    viewModel: WalletTopUpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    OnLifecycleStart(viewModel::refresh)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WalletTopUpEffect.Success -> {
                    snackbar.showSnackbar("Wallet topped up — new balance ${formatMoney(effect.newBalance)}")
                    onTopUpComplete()
                }
                is WalletTopUpEffect.ShowError -> snackbar.showSnackbar(effect.message)
            }
        }
    }

    FixItScreen(bg = C.Subtle) {
        TopBar(onBack = onBack)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                ScreenTitle(title = "Top up wallet", sub = "Add funds to your FixIt Wallet.")

                Column(Modifier.padding(horizontal = 20.dp)) {
                    // Current balance card
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(C.Blue)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Current balance", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                            Text(
                                formatMoney(state.balance),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    SectionLabel("Amount")
                    Spacer(Modifier.height(8.dp))
                }

                FixItTextField(
                    label = "Enter amount (USD)",
                    value = state.amountInput,
                    onValueChange = viewModel::onAmountChange,
                    placeholder = "0.00",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                )

                // Quick amounts
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(5, 10, 20, 50).forEach { amt ->
                        QuickAmountChip(
                            label = "$$amt",
                            onClick = { viewModel.onQuickAmount(amt) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Column(Modifier.padding(horizontal = 20.dp)) {
                    SectionLabel("Pay with")
                    Spacer(Modifier.height(8.dp))

                    if (!state.hasMethods && !state.isLoading) {
                        EmptyState(
                            title = "No payment method",
                            subtitle = "Add a card or bank account to top up your wallet.",
                        )
                        Spacer(Modifier.height(10.dp))
                        com.fixit.app.ui.components.OutlineButton(
                            text = "Add payment method",
                            onClick = onAddPaymentMethod,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.methods.forEach { method ->
                                SelectableMethodRow(
                                    method = method,
                                    selected = method.id == state.selectedMethodId,
                                    onClick = { viewModel.selectMethod(method.id) },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Box(Modifier.padding(horizontal = 24.dp)) {
                    PrimaryButton(
                        text = if (state.isSubmitting) "Processing…"
                        else state.amount?.let { "Top up ${formatMoney(it)}" } ?: "Top up",
                        enabled = state.canSubmit,
                        onClick = viewModel::submit,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = C.Blue)
                }
            }
        }
        SnackbarHost(snackbar)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.Slate)
}

@Composable
private fun QuickAmountChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, C.Line, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
    }
}

@Composable
private fun SelectableMethodRow(
    method: SavedPaymentMethod,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) C.Blue else C.Line,
                RoundedCornerShape(12.dp),
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBox(bg = C.BlueSoft) {
            Icon(
                if (method.type == PaymentMethodType.BANK) Icons.Filled.AccountBalance
                else Icons.Filled.CreditCard,
                null,
                tint = C.Blue,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            method.shortLabel,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = C.Ink,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(Icons.Filled.Check, null, tint = C.Blue, modifier = Modifier.size(18.dp))
        }
    }
}
