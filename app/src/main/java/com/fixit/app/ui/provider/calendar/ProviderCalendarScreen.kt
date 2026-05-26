package com.fixit.app.ui.provider.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.ProviderTabBar
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.OnLifecycleStart
import com.fixit.app.ui.util.formatTime
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.collections.chunked
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.collections.plus
import kotlin.let
import kotlin.ranges.until

@Composable
fun ProviderCalendarScreen(
    onTabClick: (String) -> Unit,
    onJobClick: (bookingId: String) -> Unit,
    viewModel: ProviderCalendarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    OnLifecycleStart(viewModel::refresh)

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    FixItScreen {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                color = C.Ink,
                modifier = Modifier.weight(1f)
            )
            // Prev/next arrows intentionally omitted for v1; tracked as TODO.
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            CalendarGrid(
                month = state.month,
                today = LocalDate.now(),
                selected = state.selectedDate,
                bookedDates = state.bookedDates,
                onDateSelected = viewModel::selectDate
            )

            val date = state.selectedDate
            val jobs = state.bookingsForSelectedDate
            Text(
                "${date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))} · " +
                        "${jobs.size} ${if (jobs.size == 1) "job" else "jobs"}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = C.Ink,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)
            )
            if (jobs.isEmpty()) {
                Text(
                    "Nothing booked.",
                    fontSize = 12.sp,
                    color = C.Mute,
                    modifier = Modifier.padding(start = 20.dp, top = 6.dp)
                )
            } else {
                Column(
                    Modifier.padding(horizontal = 20.dp).padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    jobs.forEach { booking ->
                        CalendarJobRow(booking, onClick = { onJobClick(booking.id) })
                    }
                }
            }

            // "Block off time" affordance — UI only for now.
            // TODO: needs a backend endpoint for provider time blocks (separate from availability).
            Box(Modifier.padding(horizontal = 20.dp).padding(top = 14.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.BlueSoft)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(C.Blue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Add, null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        "Block off time",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = C.BlueDark
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.padding(horizontal = 16.dp))
        ProviderTabBar(active = "calendar", onTabClick = onTabClick)
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    bookedDates: Set<Int>,
    onDateSelected: (LocalDate) -> Unit
) {
    val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
    // First-day-of-month, calendar starts Sunday.
    val firstDay = month.atDay(1)
    val leading = firstDay.dayOfWeek.value % 7  // Sun=0 in our header
    val daysInMonth = month.lengthOfMonth()

    Column(Modifier.padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            weekdays.forEach { d ->
                Box(
                    Modifier.weight(1f).padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(d, fontSize = 11.sp, color = C.Mute, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        val cells = (0 until leading).map { 0 } + (1..daysInMonth) + List(42 - leading - daysInMonth) { 0 }
        cells.chunked(7).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { day ->
                    Box(
                        Modifier.weight(1f).aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != 0) {
                            val date = month.atDay(day)
                            val isToday = date == today
                            val isSelected = date == selected
                            val isBooked = day in bookedDates
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> C.Blue
                                            isToday    -> C.BlueSoft
                                            else       -> Color.Transparent
                                        }
                                    )
                                    .clickable { onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    day.toString(),
                                    color = when {
                                        isSelected -> Color.White
                                        isToday    -> C.Blue
                                        else       -> C.Ink
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                            if (isBooked) {
                                Box(
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 4.dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else C.Orange)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarJobRow(booking: Booking, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, C.Line, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(46.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(C.Orange)
        )
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    formatTime(booking.scheduledAt),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.Blue
                )
                booking.service?.durationMinutes?.let {
                    Text("· ${it / 60}h", fontSize = 11.sp, color = C.Mute)
                }
            }
            Text(
                booking.service?.title ?: "Service",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = C.Ink,
                modifier = Modifier.padding(top = 2.dp)
            )
            val sub = booking.customer?.name ?: booking.address
            Text(sub, fontSize = 11.5.sp, color = C.Slate, modifier = Modifier.padding(top = 1.dp))
        }
    }
}