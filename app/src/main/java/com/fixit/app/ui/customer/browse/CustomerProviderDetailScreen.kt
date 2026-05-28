package com.fixit.app.ui.customer.browse

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fixit.app.domain.model.PortfolioItem
import com.fixit.app.domain.model.ProviderDetail
import com.fixit.app.domain.model.ProviderStatus
import com.fixit.app.domain.model.RatingSummary
import com.fixit.app.domain.model.Review
import com.fixit.app.domain.model.Service
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.avatarColorFor
import com.fixit.app.ui.util.initialsFor
import java.math.BigDecimal
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerProviderDetailScreen(
    onBack: () -> Unit,
    onTabClick: (String) -> Unit,
    onChat: () -> Unit = {},
    onBookNow: () -> Unit = {},
    onShare: () -> Unit = {},
    onFavorite: () -> Unit = {},
    viewModel: CustomerProviderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    FixItScreen(bg = C.Subtle) {
        // ── Loading spinner while the very first detail fetch is in flight ──
        // Subsequent refreshes show stale data underneath rather than blanking.
        if (state.detail == null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = C.Blue)
                } else {
                    Text(
                        state.errorMessage ?: "Couldn't load provider",
                        color = C.Slate,
                        fontSize = 13.sp,
                    )
                }
            }
        } else {
            val detail = state.detail!!
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── Hero + Identity card ─────────────────────────────────
                Box(modifier = Modifier.fillMaxWidth()) {
                    ProHeroBanner(
                        onBack     = onBack,
                        onShare    = onShare,
                        onFavorite = onFavorite,
                    )
                    Column(
                        modifier = Modifier
                            .padding(top = 124.dp)
                            .padding(horizontal = 20.dp),
                    ) {
                        ProIdentityCard(
                            detail   = detail,
                            summary  = state.summary,
                            isTopPro = state.isTopPro,
                        )
                    }
                }

                // ── About ─────────────────────────────────────────────────
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
                    PpSectionHead("About")
                    val bio = detail.bio?.takeIf { it.isNotBlank() }
                        ?: "This provider hasn't added a bio yet."
                    Text(
                        bio,
                        fontSize = 13.sp,
                        color = C.Slate,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    val tagLabels = detail.categories
                        .map { it.name }
                        .distinct()
                        .take(4)
                    if (tagLabels.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 12.dp),
                        ) {
                            tagLabels.forEach { PpTag(it) }
                        }
                    }
                }

                // ── Recent work gallery (hidden when there's no portfolio) ──
                if (state.portfolio.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PpSectionHead("Recent work")
                            Text(
                                "See all (${state.portfolio.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = C.Blue,
                            )
                        }
                        Row(
                            modifier = Modifier.padding(start = 20.dp, top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.portfolio.take(4).forEach { PpPortfolioTile(it) }
                        }
                    }
                }

                // ── Services & rates ─────────────────────────────────────
                if (detail.activeServicesByPriceAsc.isNotEmpty()) {
                    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
                        PpSectionHead("Services & rates")
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            detail.activeServicesByPriceAsc.forEach { s ->
                                PpServiceCard(s, onBook = { onBookNow() })
                            }
                        }
                    }
                }

                // ── Reviews summary ──────────────────────────────────────
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
                    val totalReviews = state.summary?.totalReviews ?: state.reviews.size
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PpSectionHead("Reviews")
                        if (totalReviews > 0) {
                            Text(
                                "See all $totalReviews",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = C.Blue,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val avg = state.summary?.avgRating ?: detail.avgRating
                            Text(
                                if (totalReviews == 0) "—" else "%.1f".format(avg),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = C.Ink,
                                letterSpacing = (-1).sp,
                                lineHeight = 36.sp,
                            )
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                repeat(5) { i ->
                                    Text(
                                        "★",
                                        fontSize = 12.sp,
                                        color = if (i < avg.toInt()) C.Orange else C.Line,
                                    )
                                }
                            }
                            Text(
                                "$totalReviews reviews",
                                fontSize = 10.5.sp,
                                color = C.Slate,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            val total = state.reviews.size
                            (5 downTo 1).forEach { star ->
                                val count = state.distribution[star] ?: 0
                                val pct = if (total == 0) 0 else (count * 100) / total
                                PpBarRow(star, pct)
                            }
                        }
                    }
                }

                // ── Featured reviews ─────────────────────────────────────
                if (state.reviews.isNotEmpty()) {
                    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
                        // Take the first 2 most-recent reviews (backend already
                        // orders by created_at desc — see review_service.py).
                        state.reviews.take(2).forEachIndexed { idx, review ->
                            if (idx > 0) Spacer(modifier = Modifier.height(10.dp))
                            PpReviewCard(review, detail.services)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // ── Sticky bottom bar — only when we have the detail loaded ──
        if (state.detail != null) {
            HorizontalDivider(color = C.Line)
            ProDetailFooter(
                fromPrice = state.detail!!.lowestPrice,
                onChat    = onChat,
                onBookNow = onBookNow,
            )
        }
    }
}

// ── Hero banner ───────────────────────────────────────────────────────────────

@Composable
private fun ProHeroBanner(
    onBack: () -> Unit,
    onShare: () -> Unit,
    onFavorite: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF1E4FD9), Color(0xFF143AA8)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                )
            )
            drawCircle(Color(0x38FF7A3D), 90.dp.toPx(), Offset(w + 40.dp.toPx(), -40.dp.toPx()))
            drawCircle(Color(0x14FFFFFF), 80.dp.toPx(), Offset(-60.dp.toPx(), h + 60.dp.toPx()))

            val white15 = Color(0x26FFFFFF)
            val psx = w / 360f
            val psy = h / 180f
            val xOff = w * 0.44f

            val curvePath = Path().apply {
                moveTo(xOff + 30f * psx * 0.8f, 90f * psy)
                quadraticBezierTo(xOff + 50f * psx * 0.8f, 50f * psy, xOff + 90f * psx * 0.8f, 60f * psy)
                quadraticBezierTo(xOff + 130f * psx * 0.8f, 70f * psy, xOff + 170f * psx * 0.8f, 90f * psy)
            }
            drawPath(curvePath, white15, style = Stroke(2f * psx))
            drawCircle(white15, 4f * psx, Offset(xOff + 160f * psx * 0.8f, 40f * psy))
            drawCircle(white15, 3f * psx, Offset(xOff + 40f * psx * 0.8f, 140f * psy))
            drawCircle(white15, 2.5f * psx, Offset(xOff + 100f * psx * 0.8f, 20f * psy))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PpHeroIconBtn(onClick = onBack) { backArrow() }
            Spacer(modifier = Modifier.weight(1f))
            PpHeroIconBtn(onClick = onShare) { shareIcon() }
            Spacer(modifier = Modifier.width(8.dp))
            PpHeroIconBtn(onClick = onFavorite) { heartIcon() }
        }
    }
}

@Composable
private fun PpHeroIconBtn(onClick: () -> Unit, icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0x2EFFFFFF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { icon() }
}

@Composable
private fun backArrow() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val s = size.width / 24f
        val path = Path().apply { moveTo(15f * s, 18f * s); lineTo(9f * s, 12f * s); lineTo(15f * s, 6f * s) }
        drawPath(path, Color.White, style = Stroke(2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun shareIcon() {
    Canvas(modifier = Modifier.size(17.dp)) {
        val s = size.width / 24f
        val stroke = Stroke(2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val base = Path().apply {
            moveTo(4f * s, 12f * s); lineTo(4f * s, 20f * s)
            lineTo(20f * s, 20f * s); lineTo(20f * s, 12f * s)
        }
        drawPath(base, Color.White, style = stroke)
        drawLine(Color.White, Offset(12f * s, 15f * s), Offset(12f * s, 2f * s), 2f * s, StrokeCap.Round)
        val arrow = Path().apply { moveTo(7f * s, 7f * s); lineTo(12f * s, 2f * s); lineTo(17f * s, 7f * s) }
        drawPath(arrow, Color.White, style = stroke)
    }
}

@Composable
private fun heartIcon() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val s = size.width / 24f
        val path = Path().apply {
            moveTo(12f * s, 21f * s)
            lineTo(4.2f * s, 13.4f * s)
            cubicTo(1.5f * s, 10.7f * s, 1.5f * s, 6.3f * s, 4.2f * s, 3.6f * s)
            cubicTo(6.9f * s, 0.9f * s, 11.3f * s, 0.9f * s, 12f * s, 4.2f * s)
            cubicTo(12.7f * s, 0.9f * s, 17.1f * s, 0.9f * s, 19.8f * s, 3.6f * s)
            cubicTo(22.5f * s, 6.3f * s, 22.5f * s, 10.7f * s, 19.8f * s, 13.4f * s)
            close()
        }
        drawPath(path, Color.White, style = Stroke(2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ── Identity card ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProIdentityCard(
    detail: ProviderDetail,
    summary: RatingSummary?,
    isTopPro: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(18.dp))
            .background(Color.White, RoundedCornerShape(18.dp))
            .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // ── Avatar (initials fallback, photo if available, with the
                //    floating verified-tick badge when the provider is approved).
                Box(
                    modifier = Modifier
                        .size(82.dp),
                ) {
                    val seedName = detail.name?.takeIf { it.isNotBlank() } ?: "Provider"
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(avatarColorFor(detail.id)))
                            .border(4.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        val photo = detail.profilePhotoUrl
                        if (!photo.isNullOrBlank()) {
                            AsyncImage(
                                model = photo,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                            )
                        } else {
                            Text(
                                initialsFor(seedName),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                    if (detail.status == ProviderStatus.APPROVED) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(C.Blue)
                                .border(2.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(modifier = Modifier.size(11.dp)) {
                                val s = size.width / 24f
                                val path = Path().apply {
                                    moveTo(5f * s, 12f * s); lineTo(10f * s, 17f * s); lineTo(20f * s, 7f * s)
                                }
                                drawPath(
                                    path,
                                    Color.White,
                                    style = Stroke(3f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
                                )
                            }
                        }
                    }
                }

                // ── Name + role + online indicator ────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 4.dp),
                ) {
                    Text(
                        detail.name?.takeIf { it.isNotBlank() } ?: "Provider",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = C.Ink,
                        letterSpacing = (-0.3).sp,
                    )
                    // Role text = first category name when present, else generic.
                    val role = detail.categories.firstOrNull()?.name?.let { "Licensed $it" }
                        ?: "Service provider"
                    Text(
                        role,
                        fontSize = 12.5.sp,
                        color = C.Slate,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        val dotColor = if (detail.isOnline) C.Green else C.Mute
                        Canvas(modifier = Modifier.size(11.dp)) {
                            drawCircle(dotColor, size.width / 2f)
                        }
                        Text(
                            if (detail.isOnline) "Online now" else "Currently unavailable",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = dotColor,
                        )
                        if (detail.yearsExperience > 0) {
                            Text("·", fontSize = 11.sp, color = C.Slate)
                            Text(
                                "${detail.yearsExperience} yrs experience",
                                fontSize = 11.sp,
                                color = C.Slate,
                            )
                        }
                    }
                }
            }

            // ── Badge chips. Background-checked is intentionally omitted —
            //    backend doesn't surface that flag, so claiming it would be
            //    false advertising. Top Pro is derived client-side.
            FlowRow(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (detail.status == ProviderStatus.APPROVED) {
                    PpChip("Verified", C.Blue, shieldIcon = true)
                }
                if (isTopPro) {
                    PpChip("Top Pro", C.Orange, awardIcon = true)
                }
                if (!detail.certification.isNullOrBlank()) {
                    PpChip("Licensed", C.Green, certIcon = true)
                }
            }

            // ── Stat ribbon. Distance falls back to "—" when location is
            //    unavailable; jobs done now comes from the backend.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Subtle)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val ratingLabel =
                    if ((summary?.totalReviews ?: 0) == 0) "—"
                    else "%.1f".format(summary?.avgRating ?: detail.avgRating)
                PpStat(
                    value = ratingLabel,
                    label = "Rating",
                    star = ratingLabel != "—",
                    modifier = Modifier.weight(1f),
                )
                StatDivider()
                PpStat(
                    value = (summary?.totalReviews ?: 0).toString(),
                    label = "Reviews",
                    modifier = Modifier.weight(1f),
                )
                StatDivider()
                PpStat(
                    value = detail.totalJobsCompleted.toString(),
                    label = "Jobs done",
                    modifier = Modifier.weight(1f),
                )
                StatDivider()
                PpStat(
                    value = formatDistance(detail.distanceKm),
                    label = "Away",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .padding(horizontal = 4.dp)
            .background(C.Line),
    )
}

@Composable
private fun PpChip(
    label: String,
    tint: Color,
    shieldIcon: Boolean = false,
    awardIcon: Boolean = false,
    certIcon: Boolean = false,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(tint.copy(alpha = 0.08f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Canvas(modifier = Modifier.size(11.dp)) {
            val s = size.width / 24f
            val stroke = Stroke(2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when {
                shieldIcon -> {
                    val path = Path().apply {
                        moveTo(12f * s, 2f * s)
                        lineTo(20f * s, 6f * s)
                        lineTo(20f * s, 12f * s)
                        cubicTo(20f * s, 17f * s, 16.5f * s, 21f * s, 12f * s, 22f * s)
                        cubicTo(7.5f * s, 21f * s, 4f * s, 17f * s, 4f * s, 12f * s)
                        lineTo(4f * s, 6f * s)
                        close()
                    }
                    drawPath(path, tint, style = stroke)
                }
                awardIcon -> {
                    drawCircle(tint, 6f * s, Offset(12f * s, 9f * s), style = stroke)
                    val path = Path().apply {
                        moveTo(8.2f * s, 13.5f * s); lineTo(7f * s, 22f * s); lineTo(12f * s, 19f * s)
                        lineTo(17f * s, 22f * s); lineTo(15.8f * s, 13.5f * s)
                    }
                    drawPath(path, tint, style = stroke)
                }
                certIcon -> {
                    drawRoundRect(tint, Offset(3f * s, 3f * s), Size(18f * s, 18f * s), CornerRadius(2f * s), style = stroke)
                    val path = Path().apply { moveTo(9f * s, 12f * s); lineTo(11f * s, 14f * s); lineTo(15f * s, 10f * s) }
                    drawPath(path, tint, style = stroke)
                }
            }
        }
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tint)
    }
}

@Composable
private fun PpStat(
    value: String,
    label: String,
    star: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = C.Ink,
                letterSpacing = (-0.3).sp,
            )
            if (star) {
                Text("★", fontSize = 12.sp, color = C.Orange)
            }
        }
        Text(
            label,
            fontSize = 10.5.sp,
            color = C.Slate,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

// ── Section head / tag ────────────────────────────────────────────────────────

@Composable
private fun PpSectionHead(title: String) {
    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = C.Ink, letterSpacing = (-0.2).sp)
}

@Composable
private fun PpTag(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(C.Subtle)
            .border(1.dp, C.Line, RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(label, fontSize = 11.5.sp, color = C.Slate, fontWeight = FontWeight.SemiBold)
    }
}

// ── Portfolio tile ────────────────────────────────────────────────────────────

@Composable
private fun PpPortfolioTile(item: PortfolioItem) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(C.Line),
    ) {
        AsyncImage(
            model = item.afterPhotoUrl,
            contentDescription = item.caption,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// ── Service card ──────────────────────────────────────────────────────────────

@Composable
private fun PpServiceCard(service: Service, onBook: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Thumbnail ────────────────────────────────────────
        // Falls back to a tinted placeholder when the service has no photo,
        // so the row keeps its visual rhythm even for older records.
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(C.Green.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            val img = service.imageUrl
            if (!img.isNullOrBlank()) {
                AsyncImage(
                    model = img,
                    contentDescription = service.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // Simple diagonal-stripe placeholder, matches the mock.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stripe = 8.dp.toPx()
                    var x = -size.height
                    while (x < size.width) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.25f),
                            start = Offset(x, size.height),
                            end = Offset(x + size.height, 0f),
                            strokeWidth = stripe / 2f,
                        )
                        x += stripe * 1.6f
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(service.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = C.Ink)
            service.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    fontSize = 11.5.sp,
                    color = C.Slate,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            service.durationLabel?.let { label ->
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Canvas(modifier = Modifier.size(11.dp)) {
                        val s = size.width / 24f
                        drawCircle(C.Mute, 10f * s, center = Offset(12f * s, 12f * s), style = Stroke(2f * s))
                        val hand = Path().apply { moveTo(12f * s, 6f * s); lineTo(12f * s, 12f * s); lineTo(16f * s, 14f * s) }
                        drawPath(hand, C.Mute, style = Stroke(2f * s, cap = StrokeCap.Round))
                    }
                    Text(label, fontSize = 11.sp, color = C.Mute)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatPrice(service.price),
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = C.Orange,
                letterSpacing = (-0.3).sp,
            )

            // Local quantity state. `service.id` as the remember key so quantities
            // reset cleanly if the list of services itself changes (e.g. switching
            // providers in the same composition).
            var quantity by remember(service.id) { mutableStateOf(0) }

            Row(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, C.Blue, RoundedCornerShape(14.dp))
                    .animateContentSize(), // smooth width animation on expand/collapse
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (quantity == 0) {
                    // Collapsed state — just the "+" button, same padding as the old Book button.
                    Box(
                        modifier = Modifier
                            .clickable { quantity = 1 }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = C.Blue)
                    }
                } else {
                    // Expanded state — [−] [count] [+]
                    Box(
                        modifier = Modifier
                            .clickable { quantity-- }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text("−", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = C.Blue)
                    }
                    Text(
                        quantity.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = C.Ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 16.dp),
                    )
                    Box(
                        modifier = Modifier
                            .clickable { quantity++ }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = C.Blue)
                    }
                }
            }
        }
    }
}

// ── Bar row ───────────────────────────────────────────────────────────────────

@Composable
private fun PpBarRow(stars: Int, pct: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            stars.toString(),
            fontSize = 10.sp,
            color = C.Slate,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(8.dp),
        )
        Text("★", fontSize = 10.sp, color = C.Orange)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(C.Line),
        ) {
            if (pct > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pct / 100f)
                        .fillMaxHeight()
                        .background(C.Orange),
                )
            }
        }
        Text(
            "$pct%",
            fontSize = 10.sp,
            color = C.Slate,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(22.dp),
        )
    }
}

// ── Review card ───────────────────────────────────────────────────────────────

@Composable
private fun PpReviewCard(review: Review, services: List<Service>) {
    val name = review.customerName?.takeIf { it.isNotBlank() } ?: "Customer"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(avatarColorFor(name))),
                contentAlignment = Alignment.Center,
            ) {
                val photo = review.customerPhotoUrl
                if (!photo.isNullOrBlank()) {
                    AsyncImage(
                        model = photo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    Text(initialsFor(name), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Ink)
                Text(
                    relativeAge(review.createdAt),
                    fontSize = 11.sp,
                    color = C.Mute,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                repeat(5) { i ->
                    Text(
                        "★",
                        fontSize = 11.sp,
                        color = if (i < review.rating) C.Orange else C.Line,
                    )
                }
            }
        }
        review.comment?.takeIf { it.isNotBlank() }?.let { comment ->
            Text(
                comment,
                fontSize = 12.5.sp,
                color = C.Slate,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

// ── Footer ────────────────────────────────────────────────────────────────────

@Composable
private fun ProDetailFooter(
    fromPrice: BigDecimal?,
    onChat: () -> Unit,
    onBookNow: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .border(1.5.dp, C.Line, CircleShape)
                .clickable { onChat() },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val s = size.width / 24f
                val path = Path().apply {
                    moveTo(21f * s, 15f * s)
                    lineTo(21f * s, 19f * s)
                    cubicTo(21f * s, 20.1f * s, 20.1f * s, 21f * s, 19f * s, 21f * s)
                    lineTo(7f * s, 21f * s)
                    lineTo(3f * s, 25f * s)
                    lineTo(3f * s, 5f * s)
                    cubicTo(3f * s, 3.9f * s, 3.9f * s, 3f * s, 5f * s, 3f * s)
                    lineTo(19f * s, 3f * s)
                    cubicTo(20.1f * s, 3f * s, 21f * s, 3.9f * s, 21f * s, 5f * s)
                    close()
                }
                drawPath(path, C.Blue, style = Stroke(2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        Column {
            Text(
                "FROM",
                fontSize = 10.5.sp,
                color = C.Slate,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp,
            )
            Text(
                fromPrice?.let { formatPrice(it) } ?: "—",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = C.Orange,
                letterSpacing = (-0.3).sp,
                lineHeight = 24.sp,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(C.Blue)
                .clickable { onBookNow() },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Book now", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Canvas(modifier = Modifier.size(14.dp)) {
                    val s = size.width / 24f
                    val stroke = Stroke(2.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    drawLine(Color.White, Offset(5f * s, 12f * s), Offset(19f * s, 12f * s), 2.5f * s, StrokeCap.Round)
                    val arrowPath = Path().apply {
                        moveTo(13f * s, 5f * s); lineTo(20f * s, 12f * s); lineTo(13f * s, 19f * s)
                    }
                    drawPath(arrowPath, Color.White, style = stroke)
                }
            }
        }
    }
}

// ── Local formatters ──────────────────────────────────────────────────────────

/** "$75" / "$12.50" — strips the trailing ".00" so whole dollars look clean. */
private fun formatPrice(price: BigDecimal): String {
    val plain = price.stripTrailingZeros().toPlainString()
    // toPlainString on whole-number scale=0 gives "75"; on scale=2 gives "75.00".
    // After stripTrailingZeros, scale=-N for round numbers — wrap that case.
    val normalized = if (price.scale() <= 0 || plain.indexOf('.') == -1) {
        plain
    } else {
        plain
    }
    return "\$$normalized"
}

/** Backend gives km. Below 1 km → "<1 km"; above → one-decimal "X.Y km". */
private fun formatDistance(km: Double?): String {
    if (km == null) return "—"
    if (km < 0.1) return "<0.1 km"
    if (km < 10)  return "%.1f km".format(km)
    return "${km.roundToInt()} km"
}

/** "today" / "5d" / "3w" / "2mo" / "1y" — same formula as ProviderReviewsScreen. */
private fun relativeAge(createdAt: kotlin.time.Instant): String {
    val now = kotlin.time.Clock.System.now()
    val seconds = (now - createdAt).inWholeSeconds
    val days = seconds / 86_400
    return when {
        days < 1   -> "today"
        days < 7   -> "${days}d"
        days < 30  -> "${days / 7}w"
        days < 365 -> "${days / 30}mo"
        else       -> "${days / 365}y"
    }
}