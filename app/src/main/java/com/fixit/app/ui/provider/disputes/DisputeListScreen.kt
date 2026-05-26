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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.fixit.app.ui.util.OnLifecycleStart
import com.fixit.app.ui.util.avatarColorFor
import com.fixit.app.ui.util.formatMoney
import com.fixit.app.ui.util.initialsFor
import java.math.BigDecimal
import kotlin.time.Clock
import kotlin.time.Instant

// ── Dispute-specific colour constants (shared with DisputeDetailScreen) ───
internal val DspRed        = Color(0xFFEF4444)
internal val DspRedSoft    = Color(0xFFFEE2E2)
internal val DspRedText    = Color(0xFFB91C1C)
internal val DspRedDark    = Color(0xFF991B1B)
internal val DspGreen      = Color(0xFF10B981)
internal val DspGreenDark  = Color(0xFF047857)
internal val DspOrangeText = Color(0xFFB8430B)

// ──────────────────────────────────────────────────────────────────────────
// Status badge (also reused by DisputeDetailScreen)
// ──────────────────────────────────────────────────────────────────────────
@Composable
fun DisputeStatusBadge(status: DisputeStatus, large: Boolean = false) {
    val (text, bg, fg) = when (status) {
        DisputeStatus.PENDING_RESPONSE -> Triple("Pending response", C.OrangeSoft, DspOrangeText)
        DisputeStatus.AWAITING_REVIEW  -> Triple("Awaiting review",  C.BlueSoft,   C.BlueDark)
        DisputeStatus.RESOLVED         -> Triple("Resolved",          C.GreenSoft,  DspGreenDark)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .padding(
                horizontal = if (large) 8.dp else 6.dp,
                vertical   = if (large) 3.dp else 2.dp,
            ),
    ) {
        Text(
            text.uppercase(),
            fontSize = if (large) 10.sp else 9.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            letterSpacing = 0.3.sp,
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Screen
// ──────────────────────────────────────────────────────────────────────────
@Composable
fun DisputeListScreen(
    onBack: () -> Unit,
    onDisputeClick: (disputeId: String) -> Unit,
    onTabClick: (String) -> Unit,
    viewModel: DisputeListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    OnLifecycleStart(viewModel::refresh)

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // ── Aggregated total disputed amount (UI-only derived value) ────────────
    val totalDisputed: BigDecimal = remember(state.disputes) {
        state.disputes
            .filter { it.disputeStatus != DisputeStatus.RESOLVED }
            .mapNotNull { it.totalAmount }
            .fold(BigDecimal.ZERO) { acc, v -> acc + v }
    }

    FixItScreen(bg = C.Subtle) {

        // ── Top bar ───────────────────────────────────────────────────────
        Column(Modifier.fillMaxWidth().background(C.Bg)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(C.Subtle)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = C.Ink,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Disputes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = C.Ink,
                        letterSpacing = (-0.2).sp,
                    )
                    if (!state.isLoading) {
                        val total = state.disputes.size
                        val open  = state.openCount
                        val label = when {
                            total == 0 -> "No disputes"
                            else       -> "$total ${if (total == 1) "dispute" else "disputes"} · $open open"
                        }
                        Text(label, fontSize = 12.sp, color = C.Slate)
                    }
                }
            }
            HorizontalDivider(color = C.Line)
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize()) {

                // ── Summary stats card ────────────────────────────────────
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, C.Line, RoundedCornerShape(14.dp))
                        .background(C.Bg)
                        .padding(14.dp),
                ) {
                    DspStat(
                        value = state.openCount.toString(),
                        label = "Open",
                        valueColor = C.Orange,
                    )
                    DspVertDivider()
                    DspStat(
                        value = state.resolvedCount.toString(),
                        label = "Resolved",
                        valueColor = DspGreen,
                    )
                    DspVertDivider()
                    DspStat(
                        value = if (totalDisputed > BigDecimal.ZERO) formatMoney(totalDisputed) else "—",
                        label = "Disputed",
                        valueColor = C.Ink,
                    )
                }

                // ── Filter pills ──────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DspFilterPill(
                        label = "All",
                        count = state.disputes.size,
                        active = state.tab == DisputeListTab.ALL,
                        onClick = { viewModel.selectTab(DisputeListTab.ALL) },
                    )
                    DspFilterPill(
                        label = "Open",
                        count = state.openCount,
                        active = state.tab == DisputeListTab.OPEN,
                        onClick = { viewModel.selectTab(DisputeListTab.OPEN) },
                    )
                    DspFilterPill(
                        label = "Resolved",
                        count = state.resolvedCount,
                        active = state.tab == DisputeListTab.RESOLVED,
                        onClick = { viewModel.selectTab(DisputeListTab.RESOLVED) },
                    )
                }

                // ── List ──────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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

// ──────────────────────────────────────────────────────────────────────────
// Sub-composables
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun RowScope.DspStat(value: String, label: String, valueColor: Color) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor,
            letterSpacing = (-0.3).sp,
            maxLines = 1,
        )
        Text(label, fontSize = 11.sp, color = C.Slate, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DspVertDivider() {
    Box(modifier = Modifier.width(1.dp).height(36.dp).background(C.Line))
}

@Composable
private fun DspFilterPill(
    label: String,
    count: Int,
    active: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.5.dp,
                if (active) C.Blue else C.Line,
                RoundedCornerShape(8.dp),
            )
            .background(if (active) C.Blue else C.Bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (active) Color.White else C.Slate,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (active) Color(0x38FFFFFF) else C.Subtle)
                .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
            Text(
                count.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) Color.White else C.Slate,
            )
        }
    }
}

@Composable
private fun DisputeListCard(dispute: Dispute, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .background(C.Bg)
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        val name = dispute.customerName ?: "Customer"
        Avatar(
            initials = initialsFor(name),
            color    = Color(avatarColorFor(dispute.raisedBy)),
            size     = 44,
            fontSize = 14,
            photoUrl = dispute.customerPhotoUrl,
        )

        // Middle content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                DisputeStatusBadge(dispute.disputeStatus)
            }
            Text(
                text = "${dispute.serviceName ?: "Service"} · #${dispute.bookingId.takeLast(8).uppercase()}",
                fontSize = 12.sp,
                color = C.Slate,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = dispute.reason,
                fontSize = 12.sp,
                color = C.Mute,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Right side: amount + time-ago + chevron
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                dispute.totalAmount?.let { formatMoney(it) } ?: "—",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = C.Orange,
                maxLines = 1,
            )
            Text(
                formatRelativeAgo(dispute.createdAt),
                fontSize = 11.sp,
                color = C.Mute,
            )
            Text("›", fontSize = 18.sp, color = C.Mute, fontWeight = FontWeight.Light)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Relative-time helper (used by the list card)
// Also exposed as internal so DisputeDetailScreen can reuse it for the banner.
// ──────────────────────────────────────────────────────────────────────────
internal fun formatRelativeAgo(instant: Instant): String {
    val nowMs    = Clock.System.now().toEpochMilliseconds()
    val targetMs = instant.toEpochMilliseconds()
    val diffMs   = (nowMs - targetMs).coerceAtLeast(0L)

    val minutes = diffMs / 60_000L
    val hours   = minutes / 60L
    val days    = hours / 24L
    val weeks   = days / 7L

    return when {
        weeks   >= 1 -> "${weeks}w ago"
        days    >= 1 -> "${days}d ago"
        hours   >= 1 -> "${hours}h ago"
        minutes >= 1 -> "${minutes}m ago"
        else         -> "just now"
    }
}