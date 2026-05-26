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
import androidx.compose.material.icons.filled.Bolt
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
import androidx.compose.ui.text.style.TextAlign
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = C.Bg,
        dragHandle       = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(C.Line),
            )
        },
    ) {
        if (methods.isEmpty()) {
            // ── Empty state: no payout method added ───────────────────────────
            NoMethodSheetContent(
                onAddMethod = onAddMethod,
                onDismiss   = onDismiss,
            )
        } else {
            // ── Picker state: methods exist but none is the default ───────────
            MethodPickerSheetContent(
                balance               = balance,
                methods               = methods,
                selectedMethodId      = selectedMethodId,
                saveAsDefault         = saveAsDefault,
                isWithdrawing         = isWithdrawing,
                withdrawAmountInput   = withdrawAmountInput,
                withdrawAmountError   = withdrawAmountError,
                onAmountChange        = onAmountChange,
                onSetMax              = onSetMax,
                onSelectMethod        = onSelectMethod,
                onSaveAsDefaultChange = onSaveAsDefaultChange,
                onAddMethod           = onAddMethod,
                onWithdraw            = onWithdraw,
            )
        }
    }
}

// ── No-method sheet content (matches the static design exactly) ───────────────

@Composable
private fun NoMethodSheetContent(
    onAddMethod: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
    ) {

        // Icon + title + subtitle
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(C.Subtle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint     = C.Blue,
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "No payout method added",
                fontSize      = 20.sp,
                fontWeight    = FontWeight.Bold,
                color         = C.Ink,
                textAlign     = TextAlign.Center,
                letterSpacing = (-0.4).sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Link a bank account to receive your earnings directly from FixIt.",
                fontSize  = 13.sp,
                color     = C.Slate,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
        }

        // Method options card
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, C.Line, RoundedCornerShape(16.dp))
                .background(C.Bg),
        ) {
            SheetMethodOption(
                icon        = Icons.Filled.AccountBalance,
                iconBg      = C.Blue,
                title       = "Bank account (ACH)",
                subtitle    = "Free · 1–2 business days",
                badge       = "MOST POPULAR",
                onClick     = onAddMethod,
                showDivider = true,
            )
            SheetMethodOption(
                icon        = Icons.Filled.Bolt,
                iconBg      = Color(0xFF7C3AED),
                title       = "Instant to debit card",
                subtitle    = "1.5% fee · Arrives today",
                badge       = null,
                onClick     = onAddMethod,
                showDivider = true,
            )
            SheetMethodOption(
                icon        = Icons.Filled.CreditCard,
                iconBg      = Color(0xFF003087),
                title       = "PayPal",
                subtitle    = "Free · 1–2 business days",
                badge       = null,
                onClick     = onAddMethod,
                showDivider = false,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Cancel
        Box(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, C.Line, RoundedCornerShape(24.dp))
                .background(C.Bg)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Cancel",
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = C.Slate,
            )
        }
    }
}

@Composable
private fun SheetMethodOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    badge: String?,
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint     = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    title,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = C.Ink,
                )
                if (badge != null) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(C.OrangeSoft)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            badge,
                            fontSize      = 9.sp,
                            fontWeight    = FontWeight.Bold,
                            color         = C.OrangeText,
                            letterSpacing = 0.3.sp,
                        )
                    }
                }
            }
            Text(
                subtitle,
                fontSize = 12.sp,
                color    = C.Slate,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text("›", fontSize = 20.sp, color = C.Mute)
    }
    if (showDivider) HorizontalDivider(color = C.Line)
}

// ── Picker sheet content (methods exist but no default) ──────────────────────

@Composable
private fun MethodPickerSheetContent(
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
) {
    val canSubmit = withdrawAmountError == null &&
            withdrawAmountInput.isNotBlank() &&
            selectedMethodId != null &&
            !isWithdrawing

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        // Header
        Text(
            "Withdraw funds",
            fontSize      = 18.sp,
            fontWeight    = FontWeight.Bold,
            color         = C.Ink,
            letterSpacing = (-0.3).sp,
        )
        Text(
            "No default method set — pick where to send",
            fontSize   = 12.sp,
            color      = C.Slate,
            lineHeight = 17.sp,
            modifier   = Modifier.padding(top = 2.dp),
        )

        Spacer(Modifier.height(16.dp))

        // Amount input
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                "Amount",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = if (withdrawAmountError != null) Color(0xFFDC2626) else C.Slate,
            )
            Text(
                "Available: ${formatMoney(balance)}",
                fontSize = 11.5.sp,
                color    = C.Mute,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(C.Bg)
                .border(
                    1.5.dp,
                    if (withdrawAmountError != null) Color(0xFFDC2626) else C.Line,
                    RoundedCornerShape(10.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("$", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
            BasicTextField(
                value           = withdrawAmountInput,
                onValueChange   = onAmountChange,
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle       = TextStyle(
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
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(C.BlueSoft)
                    .clickable { onSetMax() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("Max", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = C.Blue)
            }
        }
        if (withdrawAmountError != null) {
            Text(
                withdrawAmountError,
                fontSize = 11.5.sp,
                color    = Color(0xFFDC2626),
                modifier = Modifier.padding(top = 4.dp, start = 2.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Method radio list
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            methods.forEach { method ->
                MethodRadioRow(
                    method   = method,
                    selected = method.id == selectedMethodId,
                    onClick  = { onSelectMethod(method.id) },
                )
            }
        }

        // Add new method
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clickable { onAddMethod() }
                .padding(vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
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
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = C.Blue,
            )
        }

        // Set as default toggle
        if (selectedMethodId != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (saveAsDefault) C.BlueSoft else C.Subtle)
                    .clickable { onSaveAsDefaultChange(!saveAsDefault) }
                    .padding(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (saveAsDefault) C.Blue else C.Bg)
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
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (saveAsDefault) C.BlueDark else C.Ink,
                    )
                    Text(
                        "Skip this step next time",
                        fontSize = 11.sp,
                        color    = if (saveAsDefault) C.BlueDark else C.Slate,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // Confirm button
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
                    "Withdraw $displayAmount",
                    color      = Color.White,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
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
            .background(if (selected) C.BlueSoft.copy(alpha = 0.55f) else C.Bg)
            .border(1.5.dp, if (selected) C.Blue else C.Line, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
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
            Text(
                method.displayName,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = C.Ink,
            )
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

// ── Add payment method sheet (untouched apart from CircleShape/badge polish) ──

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
        containerColor   = C.Bg,
        dragHandle       = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(C.Line),
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
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    "Add payment method",
                    fontSize      = 18.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = C.Ink,
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
                            .background(if (methodType == type) C.Bg else Color.Transparent)
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
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDefault) C.Blue else C.Bg)
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
                        fontSize   = 11.sp,
                        color      = if (isDefault) C.BlueDark else C.Slate,
                        modifier   = Modifier.padding(top = 1.dp),
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
                        Text(
                            "Add method",
                            color      = Color.White,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}