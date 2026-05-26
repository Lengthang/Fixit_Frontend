package com.fixit.app.ui.provider.disputes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.domain.model.Dispute
import com.fixit.app.domain.model.DisputeStatus
import com.fixit.app.ui.components.Avatar
import com.fixit.app.ui.components.EmptyState
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.ProviderTabBar
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.avatarColorFor
import com.fixit.app.ui.util.initialsFor
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Dispute-specific colour constants ─────────────────────────────────────
private val RedSoft        = Color(0xFFFEE2E2)
private val RedText        = Color(0xFFB91C1C)
private val RedDark        = Color(0xFF991B1B)
private val RedFill        = Color(0xFFFEF2F2)
private val ReviewSoft     = Color(0xFFFFF1E8)
private val ReviewText     = Color(0xFFB8430B)

// ── Status badge ──────────────────────────────────────────────────────────
@Composable
fun DisputeStatusBadge(status: DisputeStatus, large: Boolean = false) {
    val (bg, fg, label) = when (status) {
        DisputeStatus.PENDING_RESPONSE -> Triple(RedSoft,    RedText,    "Pending response")
        DisputeStatus.AWAITING_REVIEW  -> Triple(ReviewSoft, ReviewText, "Awaiting review")
        DisputeStatus.RESOLVED         -> Triple(C.GreenSoft, Color(0xFF047857), "Resolved")
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = if (large) 10.dp else 8.dp, vertical = if (large) 5.dp else 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(fg)
        )
        Text(
            label,
            fontSize = if (large) 11.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            letterSpacing = 0.3.sp,
        )
    }
}

// ── Screen ────────────────────────────────────────────────────────────────
@Composable
fun DisputeListScreen(
    onBack: () -> Unit,
    onDisputeClick: (disputeId: String) -> Unit,
    onTabClick: (String) -> Unit,
    viewModel: DisputeListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
            Column(Modifier.weight(1f)) {
                Text("Dispute History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C.Ink)
                if (!state.isLoading) {
                    val awaitingText = if (state.pendingCount > 0)
                        " · ${state.pendingCount} awaiting your response"
                    else ""
                    Text(
                        "${state.disputes.size} total$awaitingText",
                        fontSize = 11.5.sp,
                        color = C.Slate,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── Summary card ──────────────────────────────────────────
                Box(Modifier.padding(horizontal = 20.dp).padding(top = 14.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SummaryCell(
                            count = state.pendingCount,
                            label = "Need response",
                            tint = RedText,
                        )
                        Box(Modifier.width(1.dp).height(36.dp).background(C.Line))
                        SummaryCell(
                            count = state.reviewCount,
                            label = "In review",
                            tint = ReviewText,
                        )
                        Box(Modifier.width(1.dp).height(36.dp).background(C.Line))
                        SummaryCell(
                            count = state.resolvedCount,
                            label = "Resolved",
                            tint = Color(0xFF047857),
                        )
                    }
                }

                // ── Tabs ──────────────────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    DisputeTab(
                        label = "All",
                        active = state.tab == DisputeListTab.ALL,
                        onClick = { viewModel.selectTab(DisputeListTab.ALL) },
                    )
                    DisputeTab(
                        label = "Open",
                        active = state.tab == DisputeListTab.OPEN,
                        badge = state.openCount.takeIf { it > 0 }?.toString(),
                        onClick = { viewModel.selectTab(DisputeListTab.OPEN) },
                    )
                    DisputeTab(
                        label = "Resolved",
                        active = state.tab == DisputeListTab.RESOLVED,
                        onClick = { viewModel.selectTab(DisputeListTab.RESOLVED) },
                    )
                }
                Box(
                    Modifier.fillMaxWidth().height(1.dp).background(C.Line),
                )

                // ── List ──────────────────────────────────────────────────
                Column(
                    Modifier.padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val list = state.displayed
                    if (list.isEmpty() && !state.isLoading) {
                        EmptyState(
                            title = "No disputes here",
                            subtitle = when (state.tab) {
                                DisputeListTab.OPEN     -> "You have no open disputes."
                                DisputeListTab.RESOLVED -> "No resolved disputes yet."
                                DisputeListTab.ALL      -> "You have no disputes — keep up the great work!"
                            },
                        )
                    } else {
                        list.forEach { dispute ->
                            DisputeListCard(
                                dispute = dispute,
                                onClick = { onDisputeClick(dispute.id) },
                            )
                        }
                    }
                }
            }

            if (state.isLoading && state.disputes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = C.Blue)
                }
            }
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.padding(horizontal = 16.dp))
        ProviderTabBar(active = "profile", onTabClick = onTabClick)
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────

@Composable
private fun RowScope.SummaryCell(count: Int, label: String, tint: Color) {
    Column(
        Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            count.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = tint,
            letterSpacing = (-0.4).sp,
        )
        Text(label, fontSize = 10.5.sp, color = C.Slate, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DisputeTab(label: String, active: Boolean, badge: String? = null, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                color = if (active) C.Blue else C.Slate,
            )
            if (badge != null) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(C.Orange)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (active) C.Blue else Color.Transparent),
        )
    }
}

@Composable
private fun DisputeListCard(dispute: Dispute, onClick: () -> Unit) {
    val isDimmed = dispute.disputeStatus != DisputeStatus.PENDING_RESPONSE
    val cardBg   = if (isDimmed) C.Subtle else Color.White

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
    ) {
        // ── Row 1: avatar + name + status badge ───────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val name = dispute.customerName ?: "Customer"
            Avatar(
                initials = initialsFor(name),
                color    = Color(avatarColorFor(dispute.raisedBy)),
                size     = 40,
                fontSize = 13,
                photoUrl = dispute.customerPhotoUrl,
                modifier = if (isDimmed) Modifier.then(Modifier) else Modifier,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDimmed) C.Slate else C.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "#${dispute.id.takeLast(8).uppercase()}",
                    fontSize = 11.sp,
                    color = C.Mute,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            DisputeStatusBadge(dispute.disputeStatus)
        }

        // ── Row 2: service info block ──────────────────────────────────
        val infoBg     = if (isDimmed) Color.White else C.Subtle
        val infoBorder = if (isDimmed) C.Line else Color.Transparent
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(infoBg)
                .border(1.dp, infoBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            val serviceText = dispute.serviceName ?: "Service"
            Text(serviceText, fontSize = 12.5.sp, color = C.Ink, fontWeight = FontWeight.SemiBold)
            Text(
                dispute.scheduledAt?.let { formatDisputeDate(it) } ?: "—",
                fontSize = 11.sp,
                color = C.Slate,
            )
            // Reason excerpt
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(top = 3.dp),
            ) {
                Icon(
                    Icons.Filled.Info,
                    null,
                    tint = RedText,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    dispute.reason,
                    fontSize = 11.5.sp,
                    color = C.Slate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── Row 3: footer ─────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val footerText = when (dispute.disputeStatus) {
                DisputeStatus.PENDING_RESPONSE -> "Action required · respond within 48h"
                DisputeStatus.AWAITING_REVIEW  -> "Under support team review"
                DisputeStatus.RESOLVED         -> dispute.resolvedAt
                    ?.let { "Closed ${formatDisputeDate(it)}" } ?: "Closed"
            }
            val footerColor = when (dispute.disputeStatus) {
                DisputeStatus.PENDING_RESPONSE -> RedText
                else                           -> C.Mute
            }
            Text(
                footerText,
                fontSize = 11.sp,
                color = footerColor,
                fontWeight = if (dispute.disputeStatus == DisputeStatus.PENDING_RESPONSE) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("View", fontSize = 12.sp, color = C.Blue, fontWeight = FontWeight.Bold)
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    null,
                    tint = C.Blue,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

// ── Date formatting ───────────────────────────────────────────────────────

private val disputeDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.ENGLISH)
//
//private fun formatDisputeDate(instant: kotlin.time.Instant): String = runCatching {
//    val javaInstant = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds())
//    val zdt = javaInstant.atZone(ZoneId.systemDefault())
//    disputeDateFormatter.format(zdt)
//}.getOrElse { "—" }