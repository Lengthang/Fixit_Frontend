package com.fixit.app.ui.provider.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import com.fixit.app.domain.model.Booking
import com.fixit.app.domain.model.BookingStatus
import com.fixit.app.ui.components.Avatar
import com.fixit.app.ui.components.EmptyState
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.ProviderTabBar
import com.fixit.app.ui.components.StatusBadge
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.avatarColorFor
import com.fixit.app.ui.util.formatMoney
import com.fixit.app.ui.util.formatScheduled
import com.fixit.app.ui.util.initialsFor
import java.math.BigDecimal
import kotlin.collections.forEach
import kotlin.let
import kotlin.takeIf

@Composable
fun ProviderJobsScreen(
    onTabClick: (String) -> Unit,
    onJobClick: (bookingId: String) -> Unit,
    viewModel: ProviderJobsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    FixItScreen {
        // Title + search
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Jobs",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                color = C.Ink,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.Search, null, tint = C.Ink, modifier = Modifier.size(22.dp))
        }

        // Tabs
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            TabChip(
                label = "New",
                active = state.tab == JobsTab.NEW,
                badge = state.newCount.takeIf { it > 0 }?.toString(),
                onClick = { viewModel.selectTab(JobsTab.NEW) }
            )
            TabChip(
                label = "Scheduled",
                active = state.tab == JobsTab.SCHEDULED,
                onClick = { viewModel.selectTab(JobsTab.SCHEDULED) }
            )
            TabChip(
                label = "Completed",
                active = state.tab == JobsTab.COMPLETED,
                onClick = { viewModel.selectTab(JobsTab.COMPLETED) }
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))

        // Body
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading && state.bookings.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = C.Blue) }

                state.visible.isEmpty() -> Box(
                    Modifier.padding(20.dp).fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    EmptyState(
                        title = when (state.tab) {
                            JobsTab.NEW -> "No new requests"
                            JobsTab.SCHEDULED -> "Nothing scheduled"
                            JobsTab.COMPLETED -> "No completed jobs yet"
                        },
                        subtitle = when (state.tab) {
                            JobsTab.NEW -> "Customers' booking requests will appear here."
                            JobsTab.SCHEDULED -> "Confirmed and in-progress jobs show up here."
                            JobsTab.COMPLETED -> "Once you finish a job, it'll move here."
                        }
                    )
                }

                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp)
                ) {
                    state.visible.forEach { booking ->
                        JobListItem(booking, onClick = { onJobClick(booking.id) })
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.padding(horizontal = 16.dp))
        ProviderTabBar(active = "jobs", onTabClick = onTabClick)
    }
}

@Composable
private fun TabChip(label: String, active: Boolean, badge: String? = null, onClick: () -> Unit) {
    Column {
        Row(
            Modifier
                .clickable { onClick() }
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                label,
                fontSize = 13.sp,
                color = if (active) C.Blue else C.Slate,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
            )
            if (badge != null) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(C.Orange)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(badge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (active) {
            Box(Modifier.height(2.dp).width(40.dp).background(C.Blue))
        }
    }
}

@Composable
private fun JobListItem(booking: Booking, onClick: () -> Unit) {
    val customerName = booking.customer?.name ?: "Customer"
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, C.Line, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Avatar(
            initials = initialsFor(customerName),
            color = Color(avatarColorFor(booking.customer?.id ?: booking.id)),
            photoUrl = booking.customer?.profilePhotoUrl
        )
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    customerName,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = C.Ink
                )
                StatusBadge(badgeKeyFor(booking.status))
            }
            Text(
                booking.service?.title ?: "Service",
                fontSize = 12.5.sp,
                color = C.Ink,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(formatScheduled(booking.scheduledAt), fontSize = 11.sp, color = C.Mute)
        }
        Text(
            formatMoney(booking.service?.price ?: BigDecimal.ZERO),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = C.Orange
        )
    }
    Spacer(Modifier.height(8.dp))
}

private fun badgeKeyFor(status: BookingStatus): String = when (status) {
    BookingStatus.PENDING -> "new"
    BookingStatus.IN_PROGRESS, BookingStatus.AWAITING_CONFIRMATION -> "inprogress"
    BookingStatus.COMPLETED -> "completed"
    else -> "upcoming"
}