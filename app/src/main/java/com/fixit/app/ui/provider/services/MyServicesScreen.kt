package com.fixit.app.ui.provider.services

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fixit.app.domain.model.Service
import com.fixit.app.ui.components.EmptyState
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.ProviderTabBar
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.avatarColorFor
import com.fixit.app.ui.util.formatMoney

@Composable
fun MyServicesScreen(
    onBack: () -> Unit,
    onServiceClick: (String) -> Unit,
    onNewService: () -> Unit,
    onTabClick: (String) -> Unit,
    viewModel: MyServicesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // FixItScreen provides statusBarsPadding + navigationBarsPadding + fills size.
    // ProviderTabBar is placed outside the weight(1f) box so it always hugs the bottom.
    FixItScreen(bg = C.Subtle) {

        // ── top bar — matches ProviderEarningsScreen / ProviderReviewsScreen exactly ──
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
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = C.Ink,
                modifier = Modifier.size(22.dp).clickable { onBack() },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "My services",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.Ink,
                    letterSpacing = (-0.2).sp,
                )
                Text(
                    text = "${state.services.size} services · ${state.activeCount} active",
                    fontSize = 12.sp,
                    color = C.Slate,
                )
            }
        }

        // ── content area — fills remaining height; FAB floats inside ──
        Box(modifier = Modifier.weight(1f)) {

            if (state.isLoading && state.services.isEmpty()) {
                CircularProgressIndicator(
                    color = C.Blue,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {

                    // ── summary stats card ────────────────────────────
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, C.Line, RoundedCornerShape(14.dp))
                                .background(C.Bg)
                                .padding(14.dp),
                        ) {
                            StatCell(value = state.totalBookings.toString(), label = "Total bookings")
                            VerticalDivider()
                            StatCell(value = formatMoney(state.walletBalance), label = "Balance")
                            VerticalDivider()
                            StatCell(
                                value = "%.1f".format(state.avgRating),
                                label = "Avg rating",
                                showStar = true,
                            )
                        }
                    }

                    // ── filter row ────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterPill(
                            label  = "All",
                            count  = state.services.size,
                            active = state.activeFilter == ServiceFilter.ALL,
                            onClick = { viewModel.setFilter(ServiceFilter.ALL) },
                        )
                        FilterPill(
                            label  = "Active",
                            count  = state.activeCount,
                            active = state.activeFilter == ServiceFilter.ACTIVE,
                            onClick = { viewModel.setFilter(ServiceFilter.ACTIVE) },
                        )
                        FilterPill(
                            label  = "Paused",
                            count  = state.pausedCount,
                            active = state.activeFilter == ServiceFilter.PAUSED,
                            onClick = { viewModel.setFilter(ServiceFilter.PAUSED) },
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── service list or empty state ───────────────────
                    if (state.filteredServices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            // EmptyState is the existing shared component from
                            // ui/components/EmptyState.kt — used identically to
                            // how ProviderJobsScreen uses it.
                            EmptyState(
                                title    = "No services here",
                                subtitle = when (state.activeFilter) {
                                    ServiceFilter.PAUSED -> "You have no paused services"
                                    ServiceFilter.ACTIVE -> "You have no active services"
                                    ServiceFilter.ALL    -> "Tap \"New service\" to add your first service"
                                },
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                start = 20.dp, end = 20.dp, top = 4.dp, bottom = 88.dp,
                            ),
                        ) {
                            items(state.filteredServices, key = { it.id }) { service ->
                                ServiceListItem(
                                    service = service,
                                    onClick = { onServiceClick(service.id) },
                                )
                            }
                        }
                    }
                }
            }

            // ── FAB ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp)
                    .height(52.dp)
                    .shadow(
                        elevation    = 12.dp,
                        shape        = RoundedCornerShape(26.dp),
                        ambientColor = Color(0x661E4FD9),
                        spotColor    = Color(0x661E4FD9),
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .background(C.Blue)
                    .clickable { onNewService() }
                    .padding(start = 16.dp, end = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector    = Icons.Default.Add,
                    contentDescription = null,
                    tint           = Color.White,
                    modifier       = Modifier.size(18.dp),
                )
                Text(
                    text       = "New service",
                    color      = Color.White,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier  = Modifier.align(Alignment.BottomCenter),
            )
        }

        // ── tab bar — outside Box so it always sits at the very bottom ──
        ProviderTabBar(active = "profile", onTabClick = onTabClick)
    }
}

// ── private composables ───────────────────────────────────────────────────

@Composable
private fun RowScope.StatCell(value: String, label: String, showStar: Boolean = false) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text           = value,
                fontSize       = 15.sp,
                fontWeight     = FontWeight.ExtraBold,
                color          = C.Ink,
                letterSpacing  = (-0.3).sp,
            )
            if (showStar) {
                Text(text = "★", color = C.Orange, fontSize = 12.sp)
            }
        }
        Text(
            text       = label,
            fontSize   = 11.sp,
            color      = C.Slate,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(C.Line),
    )
}

@Composable
private fun FilterPill(label: String, count: Int, active: Boolean, onClick: () -> Unit) {
    val bg          = if (active) C.Blue else C.Bg
    val borderColor = if (active) C.Blue else C.Line
    val textColor   = if (active) Color.White else C.Slate
    val badgeBg     = if (active) Color(0x38FFFFFF) else C.Subtle

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textColor)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeBg)
                .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
            Text(
                text       = count.toString(),
                fontSize   = 10.sp,
                fontWeight = FontWeight.Bold,
                color      = textColor,
            )
        }
    }
}

@Composable
private fun ServiceListItem(service: Service, onClick: () -> Unit) {
    val isPaused = !service.isActive

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .background(C.Bg)
            .clickable { onClick() }
            .alpha(if (isPaused) 0.75f else 1f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ServiceCover(
                imageUrl  = service.imageUrl,
                title     = service.title,
                serviceId = service.id,
                size      = 56,
                isPaused  = isPaused,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text       = service.title,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = C.Ink,
                    )
                    if (isPaused) {
                        ServiceBadge(
                            text      = "Paused",
                            textColor = C.Slate,
                            bgColor   = C.Subtle,
                        )
                    }
                }
                if (!service.categoryName.isNullOrBlank()) {
                    Text(text = service.categoryName, fontSize = 12.sp, color = C.Slate)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (service.durationLabel != null) {
                        Text(text = "⏱ ${service.durationLabel}", fontSize = 11.sp, color = C.Mute)
                        Text(text = "·", fontSize = 11.sp, color = C.Mute)
                    }
                    Text(text = "${service.bookingCount} bookings", fontSize = 11.sp, color = C.Mute)
                }
            }

            Text(
                text          = formatMoney(service.price),
                fontSize      = 15.sp,
                fontWeight    = FontWeight.ExtraBold,
                color         = C.Orange,
                letterSpacing = (-0.3).sp,
            )
        }
    }
}

/**
 * Service cover photo tile.
 * Declared internal so ServiceDetailScreen (same package, different file) can reuse it
 * without duplication. Nothing outside this package needs it.
 */
@Composable
internal fun ServiceCover(
    imageUrl: String?,
    title: String,
    serviceId: String,
    size: Int,
    isPaused: Boolean,
) {
    Box(modifier = Modifier.size(size.dp)) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(avatarColorFor(serviceId))),
            contentAlignment = Alignment.Center,
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model              = imageUrl,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .size(size.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
            } else {
                Text(
                    text       = title.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color      = Color.White,
                    fontSize   = (size / 3).sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (isPaused) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x73000000)),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceBadge(text: String, textColor: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text          = text.uppercase(),
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Bold,
            color         = textColor,
            letterSpacing = 0.3.sp,
        )
    }
}