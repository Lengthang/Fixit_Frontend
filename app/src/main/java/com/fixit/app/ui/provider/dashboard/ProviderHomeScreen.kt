package com.fixit.app.ui.provider.dashboard

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.domain.model.DashboardStats
import com.fixit.app.domain.model.JobRequest
import com.fixit.app.domain.model.ProviderStatus
import com.fixit.app.domain.model.UpcomingJob
import com.fixit.app.ui.components.Avatar
import com.fixit.app.ui.components.DashboardHeader
import com.fixit.app.ui.components.EmptyState
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.ProviderTabBar
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.OnLifecycleStart
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProviderHomeScreen(
    onTabClick: (String) -> Unit,
    onNewRequestClick: (JobRequest) -> Unit,
    onUpcomingClick: (UpcomingJob) -> Unit,
    onWithdrawClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSeeAllRequests: () -> Unit,
    onSeeAllUpcoming: () -> Unit,
    viewModel: ProviderHomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    OnLifecycleStart(viewModel::refresh)

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    // Show the pending-approval surface only once we KNOW the provider isn't
    // approved. A null status means the profile call hasn't returned yet —
    // keeping the regular layout in that window avoids a flash of the
    // pending UI before real data lands.
    val showPending = state.providerStatus != null &&
            state.providerStatus != ProviderStatus.APPROVED

    FixItScreen {
        // ── Scrollable body ──
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                DashboardHeader(
                    greeting = state.greeting.ifBlank { "Welcome back" },
                    name = state.displayName,
                    initials = state.initials,
                    avatarUrl = state.avatarUrl,
                    bellDot = false,                          // TODO: real notifications signal
                    onNotificationsClick = onNotificationsClick
                )



                if (showPending) {
                    // Replaces the "New requests" + "Upcoming today" pair while
                    // the provider is awaiting approval (or has been rejected).
                    PendingApprovalSection()
                } else {
                    BalanceHero(
                        balance = state.balance,
                        onWithdrawClick = onWithdrawClick
                    )

                    StatsRow(state.stats)

                    SectionHeader(
                        title = "New requests",
                        badge = state.newRequests.size
                            .takeIf { it > 0 }
                            ?.toString(),
                        actionText = "See all".takeIf { state.newRequests.isNotEmpty() },
                        onActionClick = onSeeAllRequests
                    )
                    if (state.newRequests.isEmpty()) {
                        Box(Modifier.padding(horizontal = 20.dp)) {
                            EmptyState(
                                title = "No new requests yet",
                                subtitle = "When customers book your services, they'll show up here."
                            )
                        }
                    } else {
                        Column(
                            Modifier.padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.newRequests.forEach { req ->
                                RequestRow(req, onClick = { onNewRequestClick(req) })
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    SectionHeader(
                        title = "Upcoming today",
                        actionText = "View all".takeIf { state.upcoming.isNotEmpty() },
                        onActionClick = onSeeAllUpcoming
                    )
                    if (state.upcoming.isEmpty()) {
                        Box(Modifier.padding(horizontal = 20.dp)) {
                            EmptyState(
                                title = "Nothing on the schedule",
                                subtitle = "Your day is clear. Confirmed bookings will appear here."
                            )
                        }
                    } else {
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            state.upcoming.forEachIndexed { i, u ->
                                UpcomingRow(
                                    u,
                                    last = i == state.upcoming.lastIndex,
                                    onClick = { onUpcomingClick(u) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // First-load spinner overlay (after that, refreshes happen quietly)
            if (state.isLoading && state.displayName.isBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = C.Blue)
                }
            }
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.padding(horizontal = 16.dp))

        ProviderTabBar(active = "home", onTabClick = onTabClick)
    }
}

// ── Balance ──

@Composable
private fun BalanceHero(
    balance: BigDecimal,
    onWithdrawClick: () -> Unit
) {
    Box(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(C.Blue, C.BlueDark)))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "AVAILABLE BALANCE",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.4.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    formatMoney(balance),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(C.Orange)
                    .clickable { onWithdrawClick() }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    "Withdraw",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Stats ──

@Composable
private fun StatsRow(stats: DashboardStats) {
    Row(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatTile(
            title = "This week",
            value = formatMoney(stats.weekEarnings),
            sub = formatDelta(stats.weekDeltaPercent),
            subColor = if (stats.weekDeltaPercent >= 0) Color(0xFF166534) else C.Slate,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            title = "Jobs done",
            value = stats.jobsDoneTotal.toString(),
            sub = "${stats.jobsDoneThisWeek} this week",
            subColor = C.Slate,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            title = "Rating",
            value = if (stats.ratingReviewCount == 0) "—"
            else "%.1f".format(stats.ratingAverage),
            valueSuffix = if (stats.ratingReviewCount == 0) "" else " ★",
            suffixColor = C.Orange,
            sub = if (stats.ratingReviewCount == 0) "No reviews yet"
            else "${stats.ratingReviewCount} reviews",
            subColor = C.Slate,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    sub: String,
    subColor: Color,
    valueSuffix: String = "",
    suffixColor: Color = C.Ink,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, C.Line, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(title, fontSize = 11.sp, color = C.Slate, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.Ink)
            if (valueSuffix.isNotEmpty()) {
                Text(valueSuffix, fontSize = 14.sp, color = suffixColor)
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(sub, fontSize = 10.5.sp, color = subColor, fontWeight = FontWeight.SemiBold)
    }
}

// ── Section header ──

@Composable
private fun SectionHeader(
    title: String,
    badge: String? = null,
    actionText: String? = null,
    onActionClick: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = C.Ink)
        if (badge != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Orange)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    badge,
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (actionText != null) {
            Text(
                actionText,
                fontSize = 12.sp,
                color = C.Blue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

// ── Rows ──

@Composable
private fun RequestRow(req: JobRequest, onClick: () -> Unit) {
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
            initials = req.customerInitials,
            color = Color(req.avatarColorHex)
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    req.customerName,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = C.Ink,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    formatMoney(req.price),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.Orange
                )
            }
            Text(req.issue, fontSize = 12.sp, color = C.Ink)
            Text(req.meta, fontSize = 11.sp, color = C.Mute)
        }
    }
}

@Composable
private fun UpcomingRow(u: UpcomingJob, last: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            u.time,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = C.Blue,
            modifier = Modifier.width(58.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(u.title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
            Text(u.sub, fontSize = 11.5.sp, color = C.Slate)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = C.Mute,
            modifier = Modifier.size(16.dp)
        )
    }
    if (!last) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
    }
}

// ── Pending approval ─────────────────────────────────────────────────────
// Mirrors ReceivedScreen exactly (check icon with star badge, headline,
// subtitle, "WHAT'S NEXT" card with three steps), minus the FixItScreen
// shell and the "Go to dashboard" PrimaryButton, neither of which belong
// inside the dashboard scroll area.

@Composable
private fun PendingApprovalSection() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Box(
                Modifier.size(120.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(88.dp).clip(CircleShape).background(C.Blue),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(C.Orange)
                    .border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("★", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Application received",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp,
            color = C.Ink,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Your application has been received. You'll get a confirmation message from our staff.",
            fontSize = 14.sp,
            color = C.Slate,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, C.Line, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(
                "WHAT'S NEXT",
                fontSize = 11.sp,
                color = C.Slate,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            )
            Spacer(Modifier.height(12.dp))
            PendingStep("1", C.BlueSoft, C.Blue, buildAnnotatedString {
                append("We review your details within ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("24–48 hours") }
                append(".")
            })
            Spacer(Modifier.height(12.dp))
            PendingStep("2", C.BlueSoft, C.Blue, buildAnnotatedString {
                append("You'll receive a confirmation SMS & email.")
            })
            Spacer(Modifier.height(12.dp))
            PendingStep("3", C.OrangeSoft, C.Orange, buildAnnotatedString {
                append("Start receiving job requests.")
            })
        }
    }
}

@Composable
private fun PendingStep(
    number: String,
    bg: Color,
    fg: Color,
    text: AnnotatedString,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(text, fontSize = 13.sp, color = C.Ink, lineHeight = 19.sp)
    }
}

// ── Helpers ──

private fun formatMoney(amount: BigDecimal): String {
    val nf = NumberFormat.getCurrencyInstance(Locale("en", "SG"))
    nf.maximumFractionDigits = 2
    return nf.format(amount)
}

private fun formatDelta(percent: Int): String =
    if (percent == 0) "—" else (if (percent > 0) "+$percent% vs last" else "$percent% vs last")