package com.fixit.app.ui.provider.services

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.fixit.app.ui.components.Avatar
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.OnLifecycleStart
import com.fixit.app.ui.util.avatarColorFor
import com.fixit.app.ui.util.formatMoney
import com.fixit.app.ui.util.initialsFor
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun ServiceDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: ServiceDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    OnLifecycleStart(viewModel::refresh)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ServiceDetailEffect.NavigateBack  -> onBack()
                is ServiceDetailEffect.ShowMessage   -> snackbarHostState.showSnackbar(effect.text)
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text("Delete service?") },
            text    = { Text("This removes the service from your profile. Existing bookings are not affected.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.deleteService() }) {
                    Text("Delete", color = Color(0xFFDC2626))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }

    FixItScreen(bg = C.Subtle) {

        // ── top bar — same pattern as ProviderEarningsScreen ─────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(C.Bg)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint               = C.Ink,
                modifier           = Modifier.size(22.dp).clickable { onBack() },
            )
            Text(
                text          = "Service details",
                fontSize      = 16.sp,
                fontWeight    = FontWeight.Bold,
                color         = C.Ink,
                modifier      = Modifier.weight(1f),
                letterSpacing = (-0.2).sp,
            )
        }

        // ── content + snackbar ────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {

            if (state.isLoading) {
                CircularProgressIndicator(
                    color    = C.Blue,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                val service = state.service

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {

                    // ── hero card ─────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, C.Line, RoundedCornerShape(16.dp))
                            .background(C.Bg)
                            .padding(16.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            ServiceCover(
                                imageUrl  = service?.imageUrl,
                                title     = service?.title ?: "",
                                serviceId = service?.id ?: "",
                                size      = 72,
                                isPaused  = !(service?.isActive ?: true),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text          = service?.title ?: "—",
                                        fontSize      = 17.sp,
                                        fontWeight    = FontWeight.Bold,
                                        color         = C.Ink,
                                        letterSpacing = (-0.3).sp,
                                    )
                                    val isActive = service?.isActive ?: true
                                    val (badgeBg, badgeFg, badgeText) =
                                        if (isActive) Triple(C.GreenSoft, C.GreenText, "Active")
                                        else          Triple(C.Subtle,    C.Slate,     "Paused")
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(badgeBg)
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text          = badgeText.uppercase(),
                                            fontSize      = 10.sp,
                                            fontWeight    = FontWeight.Bold,
                                            color         = badgeFg,
                                            letterSpacing = 0.3.sp,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                if (!service?.categoryName.isNullOrBlank()) {
                                    Text(
                                        text     = service?.categoryName ?: "",
                                        fontSize = 12.sp,
                                        color    = C.Slate,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    if (service?.durationLabel != null) {
                                        Text(
                                            text       = "⏱ ${service.durationLabel}",
                                            fontSize   = 11.sp,
                                            color      = C.Mute,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(text = "·", fontSize = 11.sp, color = C.Mute)
                                    }
                                    if (!service?.updatedAt.isNullOrBlank()) {
                                        Text(
                                            text       = "Updated ${service?.updatedAt?.take(10) ?: ""}",
                                            fontSize   = 11.sp,
                                            color      = C.Mute,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── stat tiles ────────────────────────────────────
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DetailStatTile(
                            label      = "Flat rate",
                            value      = service?.let { formatMoney(it.price) } ?: "—",
                            valueColor = C.Orange,
                            modifier   = Modifier.weight(1f),
                        )
                        DetailStatTile(
                            label      = "Bookings",
                            value      = service?.bookingCount?.toString() ?: "0",
                            valueColor = C.Ink,
                            modifier   = Modifier.weight(1f),
                        )
                    }

                    // ── description ───────────────────────────────────
                    if (!service?.description.isNullOrBlank()) {
                        SectionLabel(
                            text     = "Description",
                            modifier = Modifier.padding(
                                start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp,
                            ),
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, C.Line, RoundedCornerShape(12.dp))
                                .background(C.Bg)
                                .padding(14.dp),
                        ) {
                            Text(
                                text       = service?.description ?: "",
                                fontSize   = 13.sp,
                                color      = C.Ink,
                                lineHeight = 20.sp,
                            )
                        }
                    }

                    // ── availability toggle ───────────────────────────
                    SectionLabel(
                        text     = "Availability",
                        modifier = Modifier.padding(
                            start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp,
                        ),
                    )
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
                            .background(C.Bg),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text       = "Visible to customers",
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = C.Ink,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text     = "Customers can book this service",
                                    fontSize = 11.sp,
                                    color    = C.Slate,
                                )
                            }
                            Switch(
                                checked         = state.isVisible,
                                onCheckedChange = { viewModel.setVisibility(it) },
                                enabled         = !state.isMutating,
                                colors          = SwitchDefaults.colors(
                                    checkedThumbColor    = Color.White,
                                    checkedTrackColor    = C.Blue,
                                    uncheckedThumbColor  = Color.White,
                                    uncheckedTrackColor  = C.Line,
                                    uncheckedBorderColor = C.Line,
                                ),
                            )
                        }
                    }

                    // ── recent booking ────────────────────────────────
                    val recent = state.recentBooking
                    if (recent != null) {
                        SectionLabel(
                            text     = "Recent booking",
                            modifier = Modifier.padding(
                                start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp,
                            ),
                        )
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, C.Line, RoundedCornerShape(12.dp))
                                .background(C.Bg)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Avatar is the existing shared component from ui/components/Avatar.kt
                            Avatar(
                                initials = initialsFor(recent.customerName),
                                color    = Color(avatarColorFor(recent.customerId)),
                                size     = 38,
                                fontSize = 12,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text       = recent.customerName,
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = C.Ink,
                                )
                                Text(
                                    text     = timeAgo(recent.createdAt),
                                    fontSize = 11.sp,
                                    color    = C.Slate,
                                )
                            }
                            Text(
                                text       = formatMoney(recent.amount),
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = C.Orange,
                            )
                        }
                    }

                    // ── action buttons ────────────────────────────────
                    Column(
                        modifier = Modifier.padding(
                            start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Edit
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(25.dp))
                                    .background(C.Blue)
                                    .clickable { service?.id?.let { onEdit(it) } },
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("✏", fontSize = 14.sp)
                                    Text(
                                        text       = "Edit service",
                                        color      = Color.White,
                                        fontSize   = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            // Pause / Resume
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, C.Line, CircleShape)
                                    .background(C.Bg)
                                    .clickable(enabled = !state.isMutating) {
                                        viewModel.setVisibility(!state.isVisible)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (state.isVisible) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        repeat(2) {
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(14.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(C.Slate),
                                            )
                                        }
                                    }
                                } else {
                                    Text("▶", color = C.Blue, fontSize = 16.sp)
                                }
                            }
                        }

                        // Delete
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .border(1.5.dp, Color(0xFFFCA5A5), RoundedCornerShape(25.dp))
                                .background(C.Bg)
                                .clickable { showDeleteDialog = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("🗑", fontSize = 15.sp)
                                Text(
                                    text       = "Delete service",
                                    color      = Color(0xFFDC2626),
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier  = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

// ── private composables ───────────────────────────────────────────────────

@Composable
private fun DetailStatTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, C.Line, RoundedCornerShape(12.dp))
            .background(C.Bg)
            .padding(14.dp),
    ) {
        Text(
            text          = label.uppercase(),
            fontSize      = 11.sp,
            color         = C.Slate,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text          = value,
            fontSize      = 22.sp,
            fontWeight    = FontWeight.ExtraBold,
            color         = valueColor,
            letterSpacing = (-0.3).sp,
        )
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text          = text.uppercase(),
        fontSize      = 13.sp,
        color         = C.Slate,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 0.4.sp,
        modifier      = modifier,
    )
}

private fun timeAgo(instant: Instant): String {
    val diff = (Clock.System.now() - instant).inWholeSeconds
    return when {
        diff < 60         -> "Just now"
        diff < 3_600      -> "${diff / 60} min ago"
        diff < 86_400     -> "${diff / 3_600}h ago"
        diff < 86_400 * 7 -> "${diff / 86_400} days ago"
        else              -> "${diff / (86_400 * 7)} weeks ago"
    }
}