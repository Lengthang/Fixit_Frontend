package com.fixit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixit.app.domain.model.ActivePromo
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.theme.FixItTheme
import java.math.BigDecimal

/**
 * Carousel of active promo banners with bottom-dot pagination. When [promos]
 * has one entry the dots row collapses. When empty the whole component
 * renders nothing — caller decides whether to leave the gap.
 */
@Composable
fun PromoBanner(
    promos: List<ActivePromo>,
    onUseClick: (ActivePromo) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (promos.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { promos.size })

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
        ) { page ->
            PromoCard(
                promo = promos[page],
                onUseClick = { onUseClick(promos[page]) },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        if (promos.size > 1) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(promos.size) { i ->
                    val on = i == pagerState.currentPage
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (on) 18.dp else 6.dp, 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (on) C.Blue else C.Line),
                    )
                }
            }
        }
    }
}

@Composable
private fun PromoCard(
    promo: ActivePromo,
    onUseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(BlueGradient())
            .padding(20.dp),
    ) {
        // Decorative orange disc — same flourish PromoScreen uses.
        Box(
            Modifier
                .size(140.dp)
                .offset(x = 200.dp, y = (-30).dp)
                .clip(CircleShape)
                .background(C.Orange.copy(alpha = 0.25f)),
        )
        Column {
            Text(
                "WELCOME OFFER",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${promo.percentLabel}% off your",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )
            Text(
                "first service",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Orange)
                    .clickable { onUseClick() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    "USE ${promo.code}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {

    val samplePromos = listOf(
        ActivePromo(
            id = "1",
            code = "SAVE25",
            discountPercentage = BigDecimal("25"),
            expiresAtIso = "2026-12-31T23:59:59Z"
        ),
        ActivePromo(
            id = "2",
            code = "WELCOME10",
            discountPercentage = BigDecimal("10"),
            expiresAtIso = null
        )
    )

    FixItTheme {
        PromoBanner(
            promos = samplePromos,
            onUseClick = {},
            modifier = Modifier
        )
    }
}