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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
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

@Composable
fun CustomerPaymentMethodsScreen(
    onBack: () -> Unit,
    viewModel: CustomerPaymentMethodsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    OnLifecycleStart(viewModel::refresh)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CustomerPaymentMethodsEffect.ShowError -> snackbar.showSnackbar(effect.message)
                is CustomerPaymentMethodsEffect.ShowMessage -> snackbar.showSnackbar(effect.message)
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
                ScreenTitle(
                    title = "Payment methods",
                    sub = "Manage the cards and bank accounts you pay with.",
                )

                Column(
                    Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (state.methods.isEmpty() && !state.isLoading) {
                        EmptyState(
                            title = "No payment methods yet",
                            subtitle = "Add a card or bank account to pay faster at checkout.",
                        )
                    } else {
                        state.methods.forEach { method ->
                            PaymentMethodCard(
                                method = method,
                                onSetDefault = { viewModel.setDefault(method.id) },
                                onDelete = { viewModel.deleteMethod(method.id) },
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    // Add button — reuses the SettingRow visual language via a tappable row.
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, C.Line, RoundedCornerShape(12.dp))
                            .clickable { viewModel.openSheet() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconBox(bg = C.BlueSoft) {
                            Icon(
                                Icons.Filled.Add, null,
                                tint = C.Blue, modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            "Add payment method",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = C.Blue,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            if (state.isLoading && state.methods.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = C.Blue)
                }
            }
        }
        SnackbarHost(snackbar)
    }

    if (state.sheetOpen) {
        AddPaymentMethodSheet(
            state = state,
            onDismiss = viewModel::closeSheet,
            onTypeChange = viewModel::setNewType,
            onNameChange = viewModel::setNewDisplayName,
            onLastFourChange = viewModel::setNewLastFour,
            onToggleDefault = viewModel::toggleNewIsDefault,
            onSubmit = viewModel::addMethod,
        )
    }
}

@Composable
private fun PaymentMethodCard(
    method: SavedPaymentMethod,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, C.Line, RoundedCornerShape(12.dp))
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
        Column(Modifier.weight(1f)) {
            Text(
                method.shortLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = C.Ink,
            )
            if (method.isDefault) {
                Text(
                    "Default",
                    fontSize = 11.5.sp,
                    color = C.GreenText,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        if (!method.isDefault) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(C.BlueSoft)
                    .clickable { onSetDefault() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    "Set default",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = C.Blue,
                )
            }
        } else {
            Icon(
                Icons.Filled.Check, null,
                tint = C.Green, modifier = Modifier.size(18.dp),
            )
        }

        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onDelete() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Delete, contentDescription = "Remove",
                tint = C.Red, modifier = Modifier.size(16.dp),
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AddPaymentMethodSheet(
    state: CustomerPaymentMethodsState,
    onDismiss: () -> Unit,
    onTypeChange: (PaymentMethodType) -> Unit,
    onNameChange: (String) -> Unit,
    onLastFourChange: (String) -> Unit,
    onToggleDefault: () -> Unit,
    onSubmit: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "Add payment method",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = C.Ink,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )

            // Type toggle (card / bank)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TypePill(
                    label = "Card",
                    selected = state.newType == PaymentMethodType.CARD,
                    onClick = { onTypeChange(PaymentMethodType.CARD) },
                    modifier = Modifier.weight(1f),
                )
                TypePill(
                    label = "Bank",
                    selected = state.newType == PaymentMethodType.BANK,
                    onClick = { onTypeChange(PaymentMethodType.BANK) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(8.dp))

            FixItTextField(
                label = if (state.newType == PaymentMethodType.BANK) "Bank name" else "Card name",
                value = state.newDisplayName,
                onValueChange = onNameChange,
                placeholder = if (state.newType == PaymentMethodType.BANK) "e.g. ABA Bank" else "e.g. Visa",
            )
            FixItTextField(
                label = "Last 4 digits (optional)",
                value = state.newLastFour,
                onValueChange = onLastFourChange,
                placeholder = "1234",
                keyboardType = KeyboardType.Number,
            )

            // Default toggle row
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggleDefault() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (state.newIsDefault) C.Blue else C.Line),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.newIsDefault) {
                        Icon(
                            Icons.Filled.Check, null,
                            tint = Color.White, modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Text("Set as default", fontSize = 14.sp, color = C.Ink)
            }

            Spacer(Modifier.height(16.dp))
            Box(Modifier.padding(horizontal = 24.dp)) {
                PrimaryButton(
                    text = if (state.isSaving) "Adding…" else "Add method",
                    enabled = state.canSubmit,
                    onClick = onSubmit,
                )
            }
        }
    }
}

@Composable
private fun TypePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) C.Blue else C.Subtle)
            .border(
                1.dp,
                if (selected) C.Blue else C.Line,
                RoundedCornerShape(10.dp),
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else C.Slate,
        )
    }
}