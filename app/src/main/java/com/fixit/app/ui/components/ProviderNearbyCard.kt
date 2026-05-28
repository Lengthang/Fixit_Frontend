package com.fixit.app.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fixit.app.domain.model.NearbyProvider
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.formatMoney

/**
 * Row card for the "Provider near you" list. Layout matches the customer
 * home mock: square media on the left (with optional TOP PRO bar), middle
 * column with name + rating, right column with price + Book button.
 */
@Composable
fun ProviderNearbyCard(
    provider: NearbyProvider,
    onCardClick: () -> Unit,
    onBookClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, C.Line, RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { onCardClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── media block with optional TOP PRO ribbon ──
        Box(
            Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(C.BlueSoft),
            contentAlignment = Alignment.Center,
        ) {
            if (!provider.profilePhotoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = provider.profilePhotoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = C.Blue,
                    modifier = Modifier.size(32.dp),
                )
            }
            if (provider.isTopPro) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(C.Orange),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "TOP PRO",
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                    )
                }
            }
        }

        // ── middle column: name + subtitle + rating ──
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    provider.name.ifBlank { "Provider" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.Ink,
                )
                Spacer(Modifier.size(4.dp))
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Verified",
                    tint = C.Blue,
                    modifier = Modifier.size(12.dp),
                )
            }
            Text(
                buildProviderSubtitle(provider),
                fontSize = 12.sp,
                color = C.Slate,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = C.Orange,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.size(3.dp))
                Text(
                    "%.1f".format(provider.avgRating),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = C.Ink,
                )
                Text(
                    " (${provider.reviewCount})",
                    fontSize = 11.5.sp,
                    color = C.Slate,
                )
                Text(
                    " (${provider.distanceKm})",
                    fontSize = 11.5.sp,
                    color = C.Slate,
                )
//                provider.distanceKm?.let { d ->
//                    Text(
//                        " · %.1f km".format(d),
//                        fontSize = 11.5.sp,
//                        color = C.Slate,
//                    )
//                }
            }
        }

        // ── right column: price + Book ──
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            provider.minPrice?.let { p ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Price",
                        fontSize = 9.sp,
                        color = C.Mute,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                    )
                    Text(
                        formatMoney(p),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = C.Orange,
                    )
                }
            }
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(C.Blue)
                    .clickable { onBookClick() }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                Text(
                    "Book",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun buildProviderSubtitle(p: NearbyProvider): String {
    val role = p.primaryCategoryName?.takeIf { it.isNotBlank() } ?: "Service Provider"
    return if (p.yearsExperience > 0) "$role · ${p.yearsExperience}+ yrs" else role
}