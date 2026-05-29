package com.fixit.app.ui.customer.booking

import android.app.DatePickerDialog
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fixit.app.domain.model.Service
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.theme.C
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import java.time.format.TextStyle as JavaTextStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerBookingScreen(
    onBack: () -> Unit,
    onBooked: (bookingId: String) -> Unit,
    viewModel: CustomerBookingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Once the booking lands, hand off to the detail screen.
    LaunchedEffect(state.createdBookingId) {
        state.createdBookingId?.let { onBooked(it) }
    }

    FixItScreen(bg = C.Subtle) {
        TopBar(showBack = true, onBack = onBack)

        when {
            state.isLoading && state.detail == null -> {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = C.Blue)
                }
            }

            state.detail == null -> {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        state.errorMessage ?: "Couldn't start booking",
                        color = C.Slate,
                        fontSize = 13.sp,
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Text(
                        "Book service",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = C.Ink,
                        letterSpacing = (-0.4).sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                    )

                    // ── Provider header card (itemized list now lives in the
                    //    editable service-list column below) ───────────────
                    BookingProviderCard(state)

                    Spacer(Modifier.height(20.dp))

                    // ── Editable service list (Task 1) ───────────────────
                    SectionLabel("Services")
                    Spacer(Modifier.height(10.dp))
                    if (state.selectedServices.isEmpty()) {
                        HelperText("No services selected.")
                    } else {
                        ServiceListColumn(
                            services = state.selectedServices,
                            onIncrement = viewModel::increment,
                            onDecrement = viewModel::decrement,
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Date selection ───────────────────────────────────
                    SectionLabel("Select date")
                    Spacer(Modifier.height(10.dp))
                    DateRow(
                        state = state,
                        onPick = viewModel::selectDate,
                        onOpenCalendar = {
                            openDatePicker(
                                context = context,
                                initial = state.selectedDate ?: LocalDate.now(),
                                onPicked = viewModel::selectDate,
                            )
                        },
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Time selection ───────────────────────────────────
                    SectionLabel("Select time")
                    Spacer(Modifier.height(10.dp))
                    when {
                        state.selectedDate == null ->
                            HelperText("Pick an available date to see time slots.")
                        state.slots.isEmpty() ->
                            HelperText("No time slots for this day.")
                        else ->
                            TimeGrid(state = state, onPick = viewModel::selectTime)
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Service address ──────────────────────────────────
                    SectionLabel("Service address")
                    Spacer(Modifier.height(10.dp))
                    if (state.locations.isEmpty()) {
                        HelperText("No saved addresses. Add one in your profile first.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.locations.forEach { loc ->
                                AddressRow(
                                    label = loc.label,
                                    address = loc.address,
                                    selected = loc.id == state.selectedLocationId,
                                    onClick = { viewModel.selectLocation(loc.id) },
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Notes for pro (optional) ─────────────────────────
                    SectionLabel("Notes for pro (optional)")
                    Spacer(Modifier.height(10.dp))
                    NotesBox(value = state.notes, onChange = viewModel::updateNotes)

                    Spacer(Modifier.height(20.dp))

                    // ── Promo code (Task 4: Apply/Remove toggle) ─────────
                    SectionLabel("Promo code")
                    Spacer(Modifier.height(10.dp))
                    PromoRow(
                        value = state.promoInput,
                        applied = state.appliedPromo != null,
                        onChange = viewModel::updatePromoInput,
                        onApply = viewModel::applyPromo,
                        onClear = viewModel::clearPromo,
                    )
                    state.appliedPromo?.let { promo ->
                        Text(
                            "${promo.percentLabel}% off applied · −${formatPrice(state.discount)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = C.Green,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    state.promoError?.let { err ->
                        Text(
                            err,
                            fontSize = 12.sp,
                            color = C.Red,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Payment method ───────────────────────────────────
                    SectionLabel("Payment method")
                    Spacer(Modifier.height(10.dp))
                    if (state.paymentMethods.isEmpty()) {
                        HelperText("No payment methods. Add one in your profile first.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.paymentMethods.forEach { pm ->
                                PaymentRow(
                                    label = pm.shortLabel,
                                    selected = pm.id == state.selectedMethodId,
                                    onClick = { viewModel.selectMethod(pm.id) },
                                )
                            }
                        }
                    }

                    state.submitError?.let { err ->
                        Spacer(Modifier.height(16.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(C.Red.copy(alpha = 0.08f))
                                .padding(12.dp),
                        ) {
                            Text(err, fontSize = 12.5.sp, color = C.Red)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Subtotal summary (Task 2: moved to bottom) ───────
                    SummarySection(state)

                    Spacer(Modifier.height(24.dp))
                }

                // ── Sticky confirm bar ───────────────────────────────────
                HorizontalDivider(color = C.Line)
                ConfirmBar(
                    total = state.total,
                    enabled = state.canConfirm,
                    submitting = state.isSubmitting,
                    onConfirm = viewModel::confirm,
                )
            }
        }
    }
}

// ── Provider header ───────────────────────────────────────────────────────────
// Trimmed to provider identity only — the per-service breakdown moved to the
// editable ServiceListColumn so quantities can be changed inline.

@Composable
private fun BookingProviderCard(state: CustomerBookingState) {
    val detail = state.detail ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(C.BlueSoft)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(C.Blue),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (detail.name?.firstOrNull() ?: 'P').uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    detail.name?.takeIf { it.isNotBlank() } ?: "Provider",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.Ink,
                )
                val firstService = state.selectedServices.firstOrNull()?.first?.title
                val extra = (state.selectedServices.size - 1).coerceAtLeast(0)
                val summary = when {
                    firstService == null -> "No services selected"
                    extra > 0            -> "$firstService +$extra more"
                    else                 -> firstService
                }
                Text(summary, fontSize = 12.sp, color = C.Slate, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

// ── Editable service list column (Task 1) ─────────────────────────────────────
// One row per selected service, stacked vertically. Each row: image, name,
// price, and a +/− quantity stepper. Reuses the visual language of the
// provider-detail stepper so the affordance stays consistent across screens.

@Composable
private fun ServiceListColumn(
    services: List<Pair<Service, Int>>,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        services.forEach { (svc, qty) ->
            ServiceListRow(
                service = svc,
                quantity = qty,
                onIncrement = { onIncrement(svc.id) },
                onDecrement = { onDecrement(svc.id) },
            )
        }
    }
}

@Composable
private fun ServiceListRow(
    service: Service,
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Thumbnail (image_url) ────────────────────────────
        // Tinted placeholder with a diagonal-stripe motif when the service
        // has no photo, matching the provider-detail card behavior.
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(C.Green.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            val img = service.imageUrl
            if (!img.isNullOrBlank()) {
                AsyncImage(
                    model = img,
                    contentDescription = service.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stripe = 8.dp.toPx()
                    var x = -size.height
                    while (x < size.width) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.25f),
                            start = Offset(x, size.height),
                            end = Offset(x + size.height, 0f),
                            strokeWidth = stripe / 2f,
                        )
                        x += stripe * 1.6f
                    }
                }
            }
        }

        // ── Name + per-unit price ────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                service.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = C.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                formatPrice(service.price),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = C.Orange,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            // Line total when more than one unit, so the row reads clearly.
            if (quantity > 1) {
                Text(
                    "${formatPrice(service.price.multiply(BigDecimal(quantity)))} total",
                    fontSize = 11.sp,
                    color = C.Slate,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        // ── Quantity stepper ─────────────────────────────────
        QtyStepper(
            quantity = quantity,
            onIncrement = onIncrement,
            onDecrement = onDecrement,
        )
    }
}

@Composable
private fun QtyStepper(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, C.Blue, RoundedCornerShape(14.dp))
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clickable { onDecrement() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.Blue)
        }
        Text(
            quantity.toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = C.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 20.dp),
        )
        Box(
            modifier = Modifier
                .clickable { onIncrement() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.Blue)
        }
    }
}

// ── Subtotal summary (Task 2 + Task 3) ────────────────────────────────────────
// Lives at the bottom of the scroll area, above the sticky Confirm bar. All
// figures (subtotal, discount, total) are derived in the ViewModel and update
// live as quantities change or a promo is applied/removed.

@Composable
private fun SummarySection(state: CustomerBookingState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        SummaryRow(label = "Subtotal", value = formatPrice(state.subtotal))

        state.appliedPromo?.let { promo ->
            Spacer(Modifier.height(10.dp))
            SummaryRow(
                label = "Promo (${promo.code})",
                value = "−${formatPrice(state.discount)}",
                valueColor = C.Green,
            )
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = C.Line)
        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Total", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = C.Ink)
            Text(
                formatPrice(state.total),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = C.Ink,
                letterSpacing = (-0.3).sp,
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = C.Ink,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, color = C.Slate)
        Text(value, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

// ── Date row ──────────────────────────────────────────────────────────────────

@Composable
private fun DateRow(
    state: CustomerBookingState,
    onPick: (LocalDate) -> Unit,
    onOpenCalendar: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.dates, key = { it.date.toString() }) { option ->
                DateChip(
                    option = option,
                    selected = option.date == state.selectedDate,
                    onClick = { onPick(option.date) },
                )
            }
        }
        // Calendar-icon control → full date picker.
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, C.Line, RoundedCornerShape(12.dp))
                .clickable { onOpenCalendar() },
            contentAlignment = Alignment.Center,
        ) {
            CalendarGlyph()
        }
    }
}

@Composable
private fun DateChip(
    option: DateOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val enabled = option.available
    val bg = when {
        selected -> C.BlueSoft
        else     -> Color.White
    }
    val borderColor = when {
        selected -> C.Blue
        else     -> C.Line
    }
    val contentColor = when {
        !enabled -> C.Mute
        selected -> C.Blue
        else     -> C.Ink
    }
    val dow = option.date.dayOfWeek
        .getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())

    Column(
        modifier = Modifier
            .width(58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(dow, fontSize = 11.sp, color = if (enabled) C.Slate else C.Mute, fontWeight = FontWeight.Medium)
        Text(
            option.date.dayOfMonth.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun CalendarGlyph() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(width = 22.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(C.Blue),
        )
        Box(
            Modifier
                .padding(top = 2.dp)
                .size(width = 22.dp, height = 16.dp)
                .border(2.dp, C.Blue, RoundedCornerShape(3.dp)),
        )
    }
}

// ── Time grid ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeGrid(
    state: CustomerBookingState,
    onPick: (java.time.LocalTime) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.slots.forEach { slot ->
            val selected = slot.time == state.selectedTime
            val bg = if (selected) C.BlueSoft else Color.White
            val border = if (selected) C.Blue else C.Line
            val textColor = when {
                !slot.enabled -> C.Mute
                selected      -> C.Blue
                else          -> C.Ink
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .border(1.5.dp, border, RoundedCornerShape(20.dp))
                    .then(if (slot.enabled) Modifier.clickable { onPick(slot.time) } else Modifier)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text(
                    slot.label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                )
            }
        }
    }
}

// ── Address / payment rows ────────────────────────────────────────────────────

@Composable
private fun AddressRow(
    label: String,
    address: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) C.BlueSoft else Color.White)
            .border(1.5.dp, if (selected) C.Blue else C.Line, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SelectDot(selected)
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Ink)
            address?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 11.5.sp, color = C.Slate, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun PaymentRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) C.BlueSoft else Color.White)
            .border(1.5.dp, if (selected) C.Blue else C.Line, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SelectDot(selected)
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
    }
}

@Composable
private fun SelectDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .border(2.dp, if (selected) C.Blue else C.Line, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(C.Blue))
        }
    }
}

// ── Notes / promo inputs ──────────────────────────────────────────────────────

@Composable
private fun NotesBox(value: String, onChange: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.5.dp, C.Line, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(color = C.Ink, fontSize = 14.sp, lineHeight = 20.sp),
            cursorBrush = SolidColor(C.Blue),
            modifier = Modifier.fillMaxWidth().height(64.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        "e.g. Under-sink pipe drips steadily, water pooling in cabinet.",
                        color = C.Mute,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun PromoRow(
    value: String,
    applied: Boolean,
    onChange: (String) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.5.dp, if (applied) C.Green else C.Line, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                enabled = !applied,
                singleLine = true,
                textStyle = TextStyle(color = C.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(C.Blue),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text("Enter promo code", color = C.Mute, fontSize = 14.sp)
                    }
                    inner()
                },
            )
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (applied) C.Subtle else C.Blue)
                .clickable { if (applied) onClear() else onApply() }
                .padding(horizontal = 18.dp, vertical = 13.dp),
        ) {
            Text(
                if (applied) "Remove" else "Apply",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (applied) C.Slate else Color.White,
            )
        }
    }
}

// ── Confirm bar ───────────────────────────────────────────────────────────────

@Composable
private fun ConfirmBar(
    total: BigDecimal,
    enabled: Boolean,
    submitting: Boolean,
    onConfirm: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(if (enabled) C.Blue else C.Mute)
                .clickable(enabled = enabled) { onConfirm() },
            contentAlignment = Alignment.Center,
        ) {
            if (submitting) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    "Confirm & pay ${formatPrice(total)}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ── Small shared bits ─────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Ink, letterSpacing = (-0.2).sp)
}

@Composable
private fun HelperText(text: String) {
    Text(text, fontSize = 12.5.sp, color = C.Slate, textAlign = TextAlign.Start)
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Same "$75" / "$12.50" formatting the detail screen uses. */
private fun formatPrice(price: BigDecimal): String {
    val plain = price.stripTrailingZeros().toPlainString()
    return "\$$plain"
}

/** Opens the platform date picker, bounded to today onward. */
private fun openDatePicker(
    context: android.content.Context,
    initial: LocalDate,
    onPicked: (LocalDate) -> Unit,
) {
    val dialog = DatePickerDialog(
        context,
        { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day)) },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth,
    )
    // Don't allow past dates.
    dialog.datePicker.minDate = System.currentTimeMillis()
    dialog.show()
}