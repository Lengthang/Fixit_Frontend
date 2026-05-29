package com.fixit.app.ui.customer.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fixit.app.domain.model.HomeCategory
import com.fixit.app.domain.model.NearbyProvider
import com.fixit.app.ui.components.CategoryCircle
import com.fixit.app.ui.components.CustomerTabBar
import com.fixit.app.ui.components.DashboardHeader
import com.fixit.app.ui.components.EmptyState
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.PromoBanner
import com.fixit.app.ui.components.ProviderNearbyCard
import com.fixit.app.ui.components.SearchBar
import com.fixit.app.ui.components.iconForCategory
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.normalizeMediaUrl
import kotlinx.coroutines.launch

@Composable
fun CustomerHomeScreen(
    onTabClick: (String) -> Unit,
    onLocationClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    onSeeAllCategories: () -> Unit,
    onCategoryClick: (HomeCategory) -> Unit,
    onSeeAllProviders: () -> Unit,
    onProviderClick: (NearbyProvider) -> Unit,
    onProviderBookClick: (NearbyProvider) -> Unit,
    viewModel: CustomerHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    FixItScreen {
        // ── Scrollable body ──
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                DashboardHeader(
                    greeting = "",                                   // no top line — name owns the prominence
                    name = "Hi, ${state.firstName.ifBlank { "there" }} 👋",
                    initials = state.initials,
                    avatarUrl = state.avatarUrl,
                    avatarColor = C.Blue,
                    bellDot = state.notificationsDot,
                    subtitle = state.addressLabel ?: if (state.isLocating) "Locating…" else "Set your location",
                    subtitleLeadingIcon = Icons.Filled.LocationOn,
                    onSubtitleClick = onLocationClick,
                    onNotificationsClick = onNotificationsClick,
                )

                SearchBar(
                    onClick = onSearchClick,
                    onFilterClick = onFilterClick,
                )

                Spacer(Modifier.height(16.dp))

                PromoBanner(
                    promos = state.promos,
                    onUseClick = { promo ->
                        clipboard.setText(AnnotatedString(promo.code))
                        scope.launch { snackbarHostState.showSnackbar("Code ${promo.code} copied") }
                    },
                )

                Spacer(Modifier.height(18.dp))

                CategoriesSection(
                    categories = state.categories,
                    selectedCategoryId = state.selectedCategoryId,
                    onSeeAll = onSeeAllCategories,
                    onCategorySelected = { categoryId -> viewModel.onCategorySelected(categoryId) },
                )

                Spacer(Modifier.height(20.dp))

                ProvidersSection(
                    providers = state.nearby,
                    activeCategoryName = state.categories
                        .firstOrNull { it.id == state.selectedCategoryId }?.name,
                    onSeeAll = onSeeAllProviders,
                    onProviderClick = onProviderClick,
                    onBookClick = onProviderBookClick,
                )

                Spacer(Modifier.height(16.dp))
            }

            SnackbarHost(
                snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        CustomerTabBar(active = "home", onTabClick = onTabClick)
    }
}

// ── Sections (file-private; tightly coupled to this screen's layout) ────


@Composable
private fun CategoriesSection(
    categories: List<HomeCategory>,
    selectedCategoryId: String?,
    onSeeAll: () -> Unit,
    onCategorySelected: (String?) -> Unit,
) {
    SectionHeader(title = "Categories", actionText = "See all", onActionClick = onSeeAll)
    if (categories.isEmpty()) {
        Box(Modifier.padding(horizontal = 20.dp)) {
            EmptyState(
                title = "No categories yet",
                subtitle = "We'll show service categories here as they become available.",
            )
        }
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // "All" clears any active filter. Selected when no category is active.
            SelectableCategoryCircle(
                name = "All",
                iconUrl = null,
                fallbackIcon = Icons.Filled.Apps,
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) },
            )
            categories.forEach { cat ->
                SelectableCategoryCircle(
                    name = cat.name,
                    iconUrl = cat.iconUrl,
                    fallbackIcon = iconForCategory(cat.name),
                    selected = cat.id == selectedCategoryId,
                    // Tapping a category toggles the in-place filter.
                    // Re-tapping the active one clears it (handled in VM).
                    onClick = { onCategorySelected(cat.id) },
                )
            }
        }
    }
}

/**
 * Selectable variant of CategoryCircle used on Customer Home for in-place
 * category filtering. Matches CategoryCircle's footprint (72.dp column,
 * 58.dp disc) and the "All" highlight treatment: when [selected], the disc
 * gets a soft-blue fill + blue ring and the icon/label turn blue. When a
 * remote [iconUrl] is present it loads via Coil (same as CategoryCircle);
 * otherwise it falls back to [fallbackIcon].
 *
 * Kept local so the shared CategoryCircle component stays selection-agnostic.
 */
@Composable
private fun SelectableCategoryCircle(
    name: String,
    iconUrl: String?,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val resolvedIconUrl = normalizeMediaUrl(iconUrl)
    Column(
        Modifier
            .width(72.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(CircleShape)
                .then(
                    if (selected) {
                        Modifier
                            .background(C.BlueSoft)
                            .border(2.dp, C.Blue, CircleShape)
                    } else {
                        Modifier.background(C.Subtle)
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (!resolvedIconUrl.isNullOrBlank()) {
                AsyncImage(
                    model = resolvedIconUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            } else {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = name,
                    tint = if (selected) C.Blue else C.Ink,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Text(
            name,
            fontSize = 11.5.sp,
            color = if (selected) C.Blue else C.Slate,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}



@Composable
private fun ProvidersSection(
    providers: List<NearbyProvider>,
    activeCategoryName: String?,
    onSeeAll: () -> Unit,
    onProviderClick: (NearbyProvider) -> Unit,
    onBookClick: (NearbyProvider) -> Unit,
) {
    SectionHeader(
        title = if (activeCategoryName != null) "Providers · $activeCategoryName" else "Providers near you",
        actionText = "See all".takeIf { providers.isNotEmpty() },
        onActionClick = onSeeAll,
    )
    if (providers.isEmpty()) {
        Box(Modifier.padding(horizontal = 20.dp)) {
            EmptyState(
                title = if (activeCategoryName != null)
                    "No $activeCategoryName providers nearby"
                else
                    "No providers nearby yet",
                subtitle = if (activeCategoryName != null)
                    "Try a different category or tap “All” to see everyone."
                else
                    "Check back soon — we're adding pros in your area.",
            )
        }
    } else {
        Column(
            Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            providers.forEach { p ->
                ProviderNearbyCard(
                    provider = p,
                    onCardClick = { onProviderClick(p) },
                    onBookClick = { onBookClick(p) },
                )
            }
        }
    }
}

// Local section header — kept private to mirror ProviderHomeScreen's pattern.
@Composable
private fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = C.Ink)
        Spacer(Modifier.weight(1f))
        if (actionText != null) {
            Text(
                actionText,
                fontSize = 12.sp,
                color = C.Blue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onActionClick() }
                    .background(C.Bg)
                    .padding(4.dp),
            )
        }
    }
}