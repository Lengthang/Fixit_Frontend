package com.fixit.app.ui.provider.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
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
import com.fixit.app.domain.model.PaymentMethodType
import com.fixit.app.domain.model.SavedPaymentMethod
import com.fixit.app.ui.components.FixItTextField
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.formatMoney
import java.math.BigDecimal

// ── Withdraw select sheet (no-default path) ───────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawSelectSheet(
    balance: BigDecimal,
    methods: List<SavedPaymentMethod>,
    selectedMethodId: String?,
    saveAsDefault: Boolean,
    isWithdrawing: Boolean,
    withdrawAmountInput: String,
    withdrawAmountError: String?,
    onAmountChange: (String) -> Unit,
    onSetMax: () -> Unit,
    onSelectMethod: (String) -> Unit,
    onSaveAsDefaultChange: (Boolean) -> Unit,
    onAddMethod: () -> Unit,
    onWithdraw: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val canSubmit  = withdrawAmountError == null &&
            withdrawAmountInput.isNotBlank() &&
            selectedMethodId != null &&
            !isWithdrawing

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        dragHandle       = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(C.Line)
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        "Withdraw funds",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = C.Ink,
                        letterSpacing = (-0.3).sp,
                    )
                    Text(
                        if (methods.isEmpty())
                            "Add a method to send your payout"
                        else
                            "No default method set — pick where to send",
                        fontSize = 12.sp,
                        color = C.Slate,
                        modifier = Modifier.padding(top = 2.dp),
                        lineHeight = 17.sp,
                    )
                }
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(C.Subtle)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", fontSize = 13.sp, color = C.Slate)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Amount input ──────────────────────────────────────────────────
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Amount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (withdrawAmountError != null) Color(0xFFDC2626) else C.Slate,
                    )
                    Text(
                        "Available: ${formatMoney(balance)}",
                        fontSize = 11.5.sp,
                        color = C.Mute,
                    )
                }
                Spacer(Modifier.height(6.dp))

                // Input row with $ prefix and Max button
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(
                            1.5.dp,
                            if (withdrawAmountError != null) Color(0xFFDC2626) else C.Line,
                            RoundedCornerShape(10.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("$", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
                    BasicTextField(
                        value = withdrawAmountInput,
                        onValueChange = onAmountChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(
                            color      = C.Ink,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        cursorBrush = SolidColor(C.Blue),
                        modifier    = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (withdrawAmountInput.isEmpty()) {
                                Text("0.00", color = C.Mute, fontSize = 16.sp)
                            }
                            inner()
                        },
                    )
                    // Max button
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(C.BlueSoft)
                            .clickable { onSetMax() }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "Max",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = C.Blue,
                        )
                    }
                }

                // Inline error
                if (withdrawAmountError != null) {
                    Text(
                        withdrawAmountError,
                        fontSize = 11.5.sp,
                        color = Color(0xFFDC2626),
                        modifier = Modifier.padding(top = 4.dp, start = 2.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Method radio list ──────────────────────────────────────────────
            if (methods.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.Subtle)
                        .border(1.dp, C.Line, RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No payment methods saved yet.\nAdd one below.",
                        fontSize = 13.sp,
                        color = C.Slate,
                        lineHeight = 20.sp,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    methods.forEach { method ->
                        MethodRadioRow(
                            method   = method,
                            selected = method.id == selectedMethodId,
                            onClick  = { onSelectMethod(method.id) },
                        )
                    }
                }
            }

            // ── Add new method ─────────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clickable { onAddMethod() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(C.BlueSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", fontSize = 16.sp, color = C.Blue, fontWeight = FontWeight.Light)
                }
                Text(
                    "Add new payment method",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = C.Blue,
                )
            }

            // ── Set as default ─────────────────────────────────────────────────
            if (methods.isNotEmpty() && selectedMethodId != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (saveAsDefault) C.BlueSoft else C.Subtle)
                        .clickable { onSaveAsDefaultChange(!saveAsDefault) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (saveAsDefault) C.Blue else Color.White)
                            .border(
                                1.5.dp,
                                if (saveAsDefault) C.Blue else C.Line,
                                RoundedCornerShape(6.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (saveAsDefault) {
                            Text("✓", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Set as default for future withdrawals",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (saveAsDefault) C.BlueDark else C.Ink,
                        )
                        Text(
                            "Skip this step next time",
                            fontSize = 11.sp,
                            color = if (saveAsDefault) C.BlueDark else C.Slate,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // ── Confirm button ─────────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (canSubmit) C.Blue else C.Mute)
                    .clickable(enabled = canSubmit) { onWithdraw() },
                contentAlignment = Alignment.Center,
            ) {
                if (isWithdrawing) {
                    CircularProgressIndicator(
                        color       = Color.White,
                        modifier    = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    val displayAmount = withdrawAmountInput
                        .toBigDecimalOrNull()
                        ?.takeIf { it.signum() > 0 }
                        ?.let { formatMoney(it) }
                        ?: "—"
                    Text(
                        "Withdraw $$displayAmount",
                        color      = Color.White,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun MethodRadioRow(
    method: SavedPaymentMethod,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.5.dp, if (selected) C.Blue else C.Line, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (method.type == PaymentMethodType.BANK) C.Blue else C.Ink),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (method.type == PaymentMethodType.BANK)
                    Icons.Filled.AccountBalance else Icons.Filled.CreditCard,
                contentDescription = null,
                tint     = Color.White,
                modifier = Modifier.size(19.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(method.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Ink)
            if (method.lastFour != null) {
                Text(
                    "••${method.lastFour}",
                    fontSize = 11.sp,
                    color    = C.Slate,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) C.Blue else C.Line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(C.Blue))
            }
        }
    }
}

// ── Add payment method sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentMethodSheet(
    methodType: String,
    displayName: String,
    lastFour: String,
    isDefault: Boolean,
    isSubmitting: Boolean,
    onTypeChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onLastFourChange: (String) -> Unit,
    onIsDefaultChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        dragHandle       = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(C.Line)
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Add payment method",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = C.Ink,
                    letterSpacing = (-0.3).sp,
                )
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(C.Subtle)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", fontSize = 13.sp, color = C.Slate)
                }
            }

            // Type selector
            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Subtle)
                    .padding(4.dp),
            ) {
                listOf("bank" to "Bank account", "card" to "Card").forEach { (type, label) ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (methodType == type) Color.White else Color.Transparent)
                            .clickable { onTypeChange(type) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            fontSize   = 13.5.sp,
                            fontWeight = if (methodType == type) FontWeight.SemiBold else FontWeight.Normal,
                            color      = if (methodType == type) C.Ink else C.Slate,
                        )
                    }
                }
            }

            FixItTextField(
                label         = if (methodType == "bank") "Bank name" else "Card name",
                value         = displayName,
                onValueChange = onDisplayNameChange,
                placeholder   = if (methodType == "bank") "e.g. Chase Bank" else "e.g. Visa Debit",
            )

            FixItTextField(
                label         = "Last 4 digits (optional)",
                value         = lastFour,
                onValueChange = onLastFourChange,
                placeholder   = "e.g. 4829",
                keyboardType  = KeyboardType.Number,
            )

            // Set as default toggle
            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDefault) C.BlueSoft else C.Subtle)
                    .clickable { onIsDefaultChange(!isDefault) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDefault) C.Blue else Color.White)
                        .border(
                            1.5.dp,
                            if (isDefault) C.Blue else C.Line,
                            RoundedCornerShape(6.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDefault) {
                        Text("✓", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Set as default payout method",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isDefault) C.BlueDark else C.Ink,
                    )
                    Text(
                        "Withdrawals will use this method automatically",
                        fontSize = 11.sp,
                        color    = if (isDefault) C.BlueDark else C.Slate,
                        modifier = Modifier.padding(top = 1.dp),
                        lineHeight = 15.sp,
                    )
                }
            }

            Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (displayName.isNotBlank() && !isSubmitting) C.Blue else C.Mute)
                        .clickable(enabled = displayName.isNotBlank() && !isSubmitting) { onSubmit() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color       = Color.White,
                            modifier    = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Add method", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}