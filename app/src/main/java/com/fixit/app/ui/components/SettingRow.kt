package com.fixit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixit.app.ui.theme.C

/**
 * One row in a settings list. Reused by the provider and customer profile
 * screens.
 *
 *  - [badge]    optional pill (green text/bg by default; orange if [badgeAccent])
 *               for transient counts like "1" on promos.
 *  - [subtitle] optional small grey line under the label, e.g. "2 addresses".
 *  - [danger]   red icon + label, used for destructive rows like "Sign out".
 *               When [danger] is true the trailing chevron is hidden.
 */
@Composable
fun SettingRow(
    icon: ImageVector,
    label: String,
    badge: String? = null,
    badgeAccent: Boolean = false,
    subtitle: String? = null,
    danger: Boolean = false,
    onClick: () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, C.Line, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBox(bg = if (danger) Color(0xFFFEE2E2) else C.BlueSoft) {
            Icon(
                icon, null,
                tint = if (danger) Color(0xFFDC2626) else C.Blue,
                modifier = Modifier.size(16.dp),
            )
        }
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (danger) Color(0xFFDC2626) else C.Ink,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    fontSize = 11.5.sp,
                    color = C.Slate,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (badge != null) {
            val bg = if (badgeAccent) C.Orange else C.GreenSoft
            val fg = if (badgeAccent) Color.White else C.GreenText
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    badge.uppercase(),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = fg,
                    letterSpacing = 0.3.sp,
                )
            }
        }
        if (!danger) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = C.Mute,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}