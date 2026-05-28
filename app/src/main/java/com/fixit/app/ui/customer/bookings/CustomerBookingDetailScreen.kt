package com.fixit.app.ui.customer.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fixit.app.domain.model.Booking
import com.fixit.app.domain.model.BookingStatus
import com.fixit.app.ui.components.Avatar
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.InfoPill
import com.fixit.app.ui.components.StatusBadge
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.OnLifecycleStart
import com.fixit.app.ui.util.avatarColorFor
import com.fixit.app.ui.util.formatBookingMeta
import com.fixit.app.ui.util.formatMoney
import com.fixit.app.ui.util.initialsFor

@Composable
fun CustomerBookingDetailScreen(
    onBack: () -> Unit,
    onOpenDispute: (bookingId: String) -> Unit,
    onLeaveReview: (bookingId: String) -> Unit,
    viewModel: CustomerBookingDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingCancel by remember { mutableStateOf(false) }

    OnLifecycleStart(viewModel::refresh)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CustomerBookingDetailEffect.Message ->
                    snackbarHostState.showSnackbar(effect.text)
                CustomerBookingDetailEffect.Dismiss -> onBack()
            }
        }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    FixItScreen(bg = C.Subtle) {

        // ── Header ────────────────────────────────────────────────────────
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
                null,
                tint = C.Ink,
                modifier = Modifier.size(22.dp).clickable { onBack() },
            )
            Text(
                "Booking details",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = C.Ink,
                modifier = Modifier.weight(1f),
            )
            state.booking?.let { b ->
                StatusBadge(customerBadgeKeyFor(b.status))
            }
        }

        // ── Body / action bar ─────────────────────────────────────────────
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading && state.booking == null ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = C.Blue)
                    }
                state.booking == null ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Couldn't load this booking.", color = C.Slate, fontSize = 14.sp)
                    }
                else ->
                    DetailBody(booking = state.booking!!)
            }
        }

        // Action bar pinned above the snackbar
        state.booking?.let { b ->
            DetailActionBar(
                booking       = b,
                isMutating    = state.isMutating,
                hasReview     = state.hasReview,
                onCancel      = { pendingCancel = true },
                onConfirm     = { viewModel.confirmCompletion() },
                onOpenDispute = { onOpenDispute(b.id) },
                onLeaveReview = { onLeaveReview(b.id) },
            )
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.padding(horizontal = 16.dp))
    }

    if (pendingCancel) {
        AlertDialog(
            onDismissRequest = { pendingCancel = false },
            title = { Text("Cancel this booking?") },
            text  = {
                Text("Your payment will be refunded to your wallet. This can't be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingCancel = false
                    viewModel.cancelBooking()
                }) { Text("Yes, cancel") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancel = false }) { Text("Keep booking") }
            },
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Body
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun DetailBody(booking: Booking) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        // ── Provider card ─────────────────────────────────────────────────
        val providerName = booking.provider?.name?.takeIf { it.isNotBlank() } ?: "Provider"
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, C.Line, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(
                initials = initialsFor(providerName),
                color    = Color(avatarColorFor(booking.provider?.id ?: booking.id)),
                photoUrl = booking.provider?.profilePhotoUrl,
                size     = 48,
                fontSize = 16,
            )
            Column(Modifier.weight(1f)) {
                Text(providerName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = C.Ink)
                Text(
                    "Booking #${booking.id.take(8).uppercase()}",
                    fontSize = 12.sp,
                    color = C.Slate,
                    modifier = Modifier.padding(top = 2.dp),
                )
                booking.provider?.avgRating?.takeIf { it > 0.0 }?.let { rating ->
                    Row(
                        Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.Star, null, tint = C.Orange, modifier = Modifier.size(14.dp))
                        Text(
                            "%.1f".format(rating),
                            fontSize = 12.sp,
                            color = C.Slate,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        // ── Job card ──────────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, C.Line, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(
                "THE JOB",
                fontSize = 11.sp,
                color = C.Slate,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
            )
            Text(
                booking.service?.title ?: "Service",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = C.Ink,
                letterSpacing = (-0.2).sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                formatBookingMeta(booking),
                fontSize = 13.sp,
                color = C.Slate,
                modifier = Modifier.padding(top = 4.dp),
            )
            booking.notes?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    fontSize = 13.sp,
                    color = C.Slate,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            booking.service?.durationMinutes?.let {
                Row(
                    Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InfoPill("Est. ${formatDuration(it)}")
                }
            }
        }

        // ── Address card ──────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, C.Line, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.LocationOn, null, tint = C.Blue, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "ADDRESS",
                    fontSize = 11.sp,
                    color = C.Slate,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp,
                )
                Text(
                    booking.address,
                    fontSize = 13.5.sp,
                    color = C.Ink,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        // ── Price breakdown ───────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, C.Line, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "PRICE",
                fontSize = 11.sp,
                color = C.Slate,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            PriceLine("Subtotal", formatMoney(booking.subtotal))
            if (booking.discountAmount.signum() > 0) {
                PriceLine("Discount", "-${formatMoney(booking.discountAmount)}")
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
            PriceLine("Total paid", formatMoney(booking.totalAmount), emphasized = true)
        }

        // ── Photos gallery ────────────────────────────────────────────────
        if (booking.photos.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, C.Line, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Text(
                    "PHOTOS",
                    fontSize = 11.sp,
                    color = C.Slate,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp,
                )
                Row(
                    Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    booking.photos.take(4).forEach { photo ->
                        AsyncImage(
                            model = photo.url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(C.Subtle),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceLine(label: String, value: String, emphasized: Boolean = false) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = if (emphasized) 14.sp else 13.sp,
            color = if (emphasized) C.Ink else C.Slate,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            fontSize = if (emphasized) 16.sp else 13.sp,
            color = if (emphasized) C.Orange else C.Ink,
            fontWeight = if (emphasized) FontWeight.ExtraBold else FontWeight.SemiBold,
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Action bar
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun DetailActionBar(
    booking: Booking,
    isMutating: Boolean,
    hasReview: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onOpenDispute: () -> Unit,
    onLeaveReview: () -> Unit,
) {
    val cancellable = booking.isCustomerCancellable()
    val hasAnyAction = when (booking.status) {
        BookingStatus.PENDING               -> true
        BookingStatus.IN_PROGRESS           -> cancellable
        BookingStatus.AWAITING_CONFIRMATION -> true
        BookingStatus.COMPLETED             -> !hasReview
        else                                -> false
    }
    if (!hasAnyAction) return

    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, C.Line)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (booking.status) {
            BookingStatus.PENDING ->
                DestructiveButton(
                    label = "Cancel booking",
                    isLoading = isMutating,
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                )
            BookingStatus.IN_PROGRESS ->
                if (cancellable) {
                    DestructiveButton(
                        label = "Cancel booking",
                        isLoading = isMutating,
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                    )
                }
            BookingStatus.AWAITING_CONFIRMATION -> {
                BarSecondaryButton(
                    label = "Open dispute",
                    enabled = !isMutating,
                    onClick = onOpenDispute,
                    modifier = Modifier.weight(1f),
                )
                BarPrimaryButton(
                    label = "Confirm",
                    isLoading = isMutating,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                )
            }
            BookingStatus.COMPLETED ->
                if (!hasReview) {
                    BarPrimaryButton(
                        label = "Leave review",
                        isLoading = false,
                        onClick = onLeaveReview,
                        modifier = Modifier.weight(1f),
                    )
                }
            else -> Unit
        }
    }
}

@Composable
private fun RowScope.BarPrimaryButton(
    label: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(C.Blue)
            .clickable(enabled = !isLoading) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun RowScope.BarSecondaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.5.dp, C.Line, RoundedCornerShape(24.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.Slate)
    }
}

@Composable
private fun RowScope.DestructiveButton(
    label: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val red = Color(0xFFDC2626)
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.5.dp, red, RoundedCornerShape(24.dp))
            .clickable(enabled = !isLoading) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = red,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = red)
        }
    }
}

private fun formatDuration(minutes: Int): String =
    when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} h"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }