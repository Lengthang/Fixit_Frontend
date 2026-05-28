package com.fixit.app.ui.customer.bookings

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import com.fixit.app.domain.model.Booking
import com.fixit.app.domain.model.BookingStatus
import com.fixit.app.ui.components.Avatar
import com.fixit.app.ui.components.CustomerTabBar
import com.fixit.app.ui.components.EmptyState
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.StatusBadge
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.OnLifecycleStart
import com.fixit.app.ui.util.avatarColorFor
import com.fixit.app.ui.util.formatBookingMeta
import com.fixit.app.ui.util.formatMoney
import com.fixit.app.ui.util.initialsFor

@Composable
fun CustomerBookingsScreen(
    onTabClick: (String) -> Unit,
    onBookingClick: (bookingId: String) -> Unit,
    onLeaveReviewClick: (bookingId: String) -> Unit,
    viewModel: CustomerBookingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingCancelId by remember { mutableStateOf<String?>(null) }

    OnLifecycleStart(viewModel::refresh)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CustomerBookingsEffect.Message ->
                    snackbarHostState.showSnackbar(effect.text)
            }
        }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    FixItScreen {
        // ── Title ─────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "My bookings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                color = C.Ink,
                modifier = Modifier.weight(1f),
            )
        }

        // ── Tabs ──────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            TabChip(
                label = "Upcoming",
                active = state.tab == BookingsTab.UPCOMING,
                onClick = { viewModel.selectTab(BookingsTab.UPCOMING) },
            )
            TabChip(
                label = "History",
                active = state.tab == BookingsTab.HISTORY,
                onClick = { viewModel.selectTab(BookingsTab.HISTORY) },
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))

        // ── Body ──────────────────────────────────────────────────────────
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading && state.bookings.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = C.Blue)
                    }

                state.visible.isEmpty() ->
                    Box(
                        Modifier.padding(20.dp).fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        EmptyState(
                            title = when (state.tab) {
                                BookingsTab.UPCOMING -> "No upcoming bookings"
                                BookingsTab.HISTORY  -> "No past bookings yet"
                            },
                            subtitle = when (state.tab) {
                                BookingsTab.UPCOMING ->
                                    "Bookings you make will appear here until they're complete."
                                BookingsTab.HISTORY ->
                                    "Once a booking is fully completed, it'll show up here."
                            },
                        )
                    }

                else ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                            .padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        state.visible.forEach { booking ->
                            BookingCard(
                                booking      = booking,
                                isMutating   = state.mutatingId == booking.id,
                                onViewDetails = { onBookingClick(booking.id) },
                                onCancelClick = { pendingCancelId = booking.id },
                                onConfirmClick = { viewModel.confirmCompletion(booking.id) },
                                onLeaveReviewClick = { onLeaveReviewClick(booking.id) },
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
            }
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.padding(horizontal = 16.dp))
        CustomerTabBar(active = "bookings", onTabClick = onTabClick)
    }

    pendingCancelId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingCancelId = null },
            title = { Text("Cancel this booking?") },
            text  = {
                Text("Your payment will be refunded to your wallet. This can't be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingCancelId = null
                    viewModel.cancelBooking(id)
                }) { Text("Yes, cancel") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancelId = null }) {
                    Text("Keep booking")
                }
            },
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Pieces
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun TabChip(label: String, active: Boolean, onClick: () -> Unit) {
    Column {
        Box(
            Modifier
                .clickable { onClick() }
                .padding(vertical = 10.dp, horizontal = 4.dp),
        ) {
            Text(
                label,
                fontSize = 14.sp,
                color = if (active) C.Blue else C.Slate,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            )
        }
        if (active) {
            Box(Modifier.height(2.dp).width(72.dp).background(C.Blue))
        } else {
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun BookingCard(
    booking: Booking,
    isMutating: Boolean,
    onViewDetails: () -> Unit,
    onCancelClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onLeaveReviewClick: () -> Unit,
) {
    val providerName = booking.provider?.name?.takeIf { it.isNotBlank() } ?: "Provider"
    val seed = booking.provider?.id ?: booking.id

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .clickable { onViewDetails() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Top row: avatar + name/badge/service + price ──
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(
                initials = initialsFor(providerName),
                color    = Color(avatarColorFor(seed)),
                photoUrl = booking.provider?.profilePhotoUrl,
                size     = 44,
                fontSize = 14,
            )
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        providerName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = C.Ink,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    StatusBadge(customerBadgeKeyFor(booking.status))
                }
                Text(
                    booking.service?.title ?: "Service",
                    fontSize = 13.sp,
                    color = C.Ink,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    formatBookingMeta(booking),
                    fontSize = 11.5.sp,
                    color = C.Mute,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                formatMoney(booking.totalAmount),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = C.Orange,
                letterSpacing = (-0.2).sp,
            )
        }

        // ── Action row ──
        BookingActionRow(
            booking            = booking,
            isMutating         = isMutating,
            onViewDetails      = onViewDetails,
            onCancelClick      = onCancelClick,
            onConfirmClick     = onConfirmClick,
            onLeaveReviewClick = onLeaveReviewClick,
        )
    }
}

@Composable
private fun BookingActionRow(
    booking: Booking,
    isMutating: Boolean,
    onViewDetails: () -> Unit,
    onCancelClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onLeaveReviewClick: () -> Unit,
) {
    val cancellable = booking.isCustomerCancellable()

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (booking.status) {
            BookingStatus.PENDING -> {
                CardButton(
                    label = "View details",
                    variant = CardButtonVariant.OUTLINE,
                    enabled = !isMutating,
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                )
                CardButton(
                    label = "Cancel booking",
                    variant = CardButtonVariant.DESTRUCTIVE,
                    enabled = !isMutating,
                    loading = isMutating,
                    onClick = onCancelClick,
                    modifier = Modifier.weight(1f),
                )
            }
            BookingStatus.IN_PROGRESS -> {
                CardButton(
                    label = "View details",
                    variant = CardButtonVariant.OUTLINE,
                    enabled = !isMutating,
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                )
                if (cancellable) {
                    CardButton(
                        label = "Cancel booking",
                        variant = CardButtonVariant.DESTRUCTIVE,
                        enabled = !isMutating,
                        loading = isMutating,
                        onClick = onCancelClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            BookingStatus.AWAITING_CONFIRMATION -> {
                CardButton(
                    label = "View details",
                    variant = CardButtonVariant.OUTLINE,
                    enabled = !isMutating,
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                )
                CardButton(
                    label = "Confirm",
                    variant = CardButtonVariant.PRIMARY_SUCCESS,
                    enabled = !isMutating,
                    loading = isMutating,
                    onClick = onConfirmClick,
                    modifier = Modifier.weight(1f),
                )
            }
            BookingStatus.COMPLETED -> {
                CardButton(
                    label = "View details",
                    variant = CardButtonVariant.OUTLINE,
                    enabled = true,
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                )
                CardButton(
                    label = "Leave review",
                    variant = CardButtonVariant.PRIMARY,
                    enabled = true,
                    onClick = onLeaveReviewClick,
                    modifier = Modifier.weight(1f),
                )
            }
            BookingStatus.DISPUTED,
            BookingStatus.CANCELLED,
            BookingStatus.CONFIRMED,
            BookingStatus.AWAITING_PAYMENT -> {
                CardButton(
                    label = "View details",
                    variant = CardButtonVariant.OUTLINE,
                    enabled = true,
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private enum class CardButtonVariant { OUTLINE, PRIMARY, PRIMARY_SUCCESS, DESTRUCTIVE }

@Composable
private fun CardButton(
    label: String,
    variant: CardButtonVariant,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    val bg: Color
    val fg: Color
    val borderColor: Color?
    when (variant) {
        CardButtonVariant.OUTLINE -> {
            bg = Color.White; fg = C.Slate; borderColor = C.Line
        }
        CardButtonVariant.PRIMARY -> {
            bg = C.Blue; fg = Color.White; borderColor = null
        }
        CardButtonVariant.PRIMARY_SUCCESS -> {
            bg = Color(0xFF10B981); fg = Color.White; borderColor = null
        }
        CardButtonVariant.DESTRUCTIVE -> {
            bg = Color.White; fg = Color(0xFFDC2626); borderColor = Color(0xFFDC2626)
        }
    }

    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (enabled) bg else C.Subtle)
            .let { m ->
                if (borderColor != null) m.border(1.5.dp, borderColor, RoundedCornerShape(22.dp))
                else m
            }
            .clickable(enabled = enabled && !loading) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = fg,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) fg else C.Mute,
            )
        }
    }
}

/** Maps a [BookingStatus] to the customer-side StatusBadge key. */
internal fun customerBadgeKeyFor(status: BookingStatus): String = when (status) {
    BookingStatus.PENDING               -> "pending"
    BookingStatus.IN_PROGRESS           -> "inprogress"
    BookingStatus.AWAITING_CONFIRMATION -> "complete"
    BookingStatus.COMPLETED             -> "completed"
    BookingStatus.DISPUTED              -> "disputed"
    BookingStatus.CANCELLED             -> "completed"
    BookingStatus.CONFIRMED,
    BookingStatus.AWAITING_PAYMENT      -> "upcoming"
}