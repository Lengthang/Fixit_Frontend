package com.fixit.app.ui.provider.reviews

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.fixit.app.domain.model.Review
import com.fixit.app.ui.components.Avatar
import com.fixit.app.ui.components.EmptyState
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.ProviderTabBar
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.avatarColorFor
import com.fixit.app.ui.util.initialsFor
import kotlin.time.Clock
import java.time.Duration
import java.time.ZoneId
import kotlin.time.toJavaInstant
import java.time.Instant
import kotlin.collections.forEach
import kotlin.let
import kotlin.ranges.downTo
import kotlin.takeIf
import kotlin.text.format
import kotlin.text.isNotBlank
import kotlin.time.toJavaInstant

@Composable
fun ProviderReviewsScreen(
    onBack: () -> Unit,
    onTabClick: (String) -> Unit,
    viewModel: ProviderReviewsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    FixItScreen(bg = C.Subtle) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                null,
                tint = C.Ink,
                modifier = Modifier.size(22.dp).clickable { onBack() }
            )
            Text(
                "Reviews",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = C.Ink,
                modifier = Modifier.weight(1f)
            )
        }

        Box(Modifier.weight(1f)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Summary header
                Box(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .border(1.dp, C.Line, RoundedCornerShape(18.dp))
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val summary = state.summary
                            Text(
                                if (summary == null || summary.totalReviews == 0) "—"
                                else "%.1f".format(summary.avgRating),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-2).sp,
                                color = C.Ink,
                                lineHeight = 48.sp
                            )
                            Row(
                                Modifier.padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val avg = summary?.avgRating ?: 0.0
                                repeat(5) { i ->
                                    Text(
                                        "★",
                                        color = if (i < avg.toInt()) C.Orange else C.Line,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            Text(
                                "${summary?.totalReviews ?: 0} reviews",
                                fontSize = 11.sp,
                                color = C.Mute,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            (5 downTo 1).forEach { star ->
                                val count = state.distribution[star] ?: 0
                                val total = state.reviews.size
                                val percent = if (total == 0) 0 else (count * 100) / total
                                RatingBar(star = star, percent = percent)
                            }
                        }
                    }
                }

                if (state.reviews.isEmpty() && !state.isLoading) {
                    Box(Modifier.padding(horizontal = 20.dp).padding(top = 24.dp)) {
                        EmptyState(
                            title = "No reviews yet",
                            subtitle = "After you complete jobs, customers will leave feedback here."
                        )
                    }
                } else {
                    Column(
                        Modifier.padding(horizontal = 20.dp).padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        state.reviews.forEach { ReviewItem(it) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (state.isLoading && state.reviews.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = C.Blue)
                }
            }
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.padding(horizontal = 16.dp))
        ProviderTabBar(active = "profile", onTabClick = onTabClick)
    }
}

@Composable
private fun RatingBar(star: Int, percent: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(star.toString(), fontSize = 10.sp, color = C.Mute, modifier = Modifier.width(10.dp))
        Box(
            Modifier
                .weight(1f)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(C.Line)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(percent / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(C.Orange)
            )
        }
    }
}

@Composable
private fun ReviewItem(review: Review) {
    val name = review.customerName ?: "Customer"
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Avatar(
                initials = initialsFor(name),
                color = Color(avatarColorFor(name)),
                photoUrl = review.customerPhotoUrl
            )
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = C.Ink)
                Row(
                    Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { i ->
                        Text(
                            "★",
                            color = if (i < review.rating) C.Orange else C.Line,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        "· ${relativeTime(review.createdAt)}",
                        fontSize = 11.sp,
                        color = C.Mute,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
        review.comment?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                fontSize = 12.5.sp,
                color = C.Slate,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
private fun relativeTime(instant: kotlinx.datetime.Instant): String {
    val now = Clock.System.now()
    val duration = Duration.between(instant.toJavaInstant(), now.toJavaInstant())
    val days = duration.toDays()
    return when {
        days < 1 -> "today"
        days < 7 -> "${days}d"
        days < 30 -> "${days / 7}w"
        days < 365 -> "${days / 30}mo"
        else -> "${days / 365}y"
    }
}