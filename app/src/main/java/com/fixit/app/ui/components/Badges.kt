package com.fixit.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixit.app.ui.theme.C

/**
 * Status pill. The string keys are domain-agnostic on purpose — provider
 * screens and customer screens both render this component, mapping their
 * own [BookingStatus] enums to one of the keys below.
 *
 * Existing provider keys (do not rename):
 *  - "new"        — NEW REQUEST (orange)
 *  - "upcoming"   — UPCOMING (blue)
 *  - "inprogress" — IN PROGRESS (green)
 *  - default else — COMPLETED (gray)
 *
 * Customer-side additions:
 *  - "pending"  — PENDING CONFIRMATION (orange, like "new" but with a
 *                 phrasing the customer recognises — "your provider hasn't
 *                 accepted yet")
 *  - "complete" — COMPLETE (blue) for awaiting_confirmation — the work is
 *                 done from the customer's perspective but they still need
 *                 to tap Confirm. Note this is distinct from the gray
 *                 default-else "COMPLETED" used for the History tab.
 *  - "disputed" — DISPUTED (orange) for the few in-flight disputed bookings
 *                 that surface in Upcoming with no action button.
 */
@Composable
fun StatusBadge(status: String) {
    val (bg, fg, label) = when (status) {
        "new"        -> Triple(C.OrangeSoft, C.OrangeText, "NEW REQUEST")
        "pending"    -> Triple(C.OrangeSoft, C.OrangeText, "PENDING CONFIRMATION")
        "upcoming"   -> Triple(C.BlueSoft, C.BlueDark, "UPCOMING")
        "inprogress" -> Triple(C.GreenSoft, C.GreenText, "IN PROGRESS")
        "complete"   -> Triple(C.BlueSoft, C.BlueDark, "COMPLETE")
        "disputed"   -> Triple(C.OrangeSoft, C.OrangeText, "DISPUTED")
        else         -> Triple(C.Subtle, C.Slate, "COMPLETED")
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = fg, letterSpacing = 0.2.sp)
    }
}

@Composable
fun InfoPill(label: String, accent: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (accent) C.OrangeSoft else C.Subtle)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (accent) C.OrangeText else C.Slate
        )
    }
}

@Composable
fun Chip(label: String, active: Boolean) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) C.BlueSoft else Color.White)
            .border(BorderStroke(1.5.dp, if (active) C.Blue else C.Line), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (active) Text("✓", fontSize = 12.sp, color = C.Blue, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 13.sp, color = if (active) C.Blue else C.Slate, fontWeight = FontWeight.Medium)
    }
}