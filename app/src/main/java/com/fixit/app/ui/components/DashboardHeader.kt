package com.fixit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
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

@Composable
fun DashboardHeader(
    greeting: String,
    name: String,
    initials: String,
    avatarUrl: String? = null,
    avatarColor: Color = C.Orange,
    dark: Boolean = false,
    bellDot: Boolean = false,
    onAvatarClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val fg = if (dark) Color.White else C.Ink
    val sub = if (dark) Color.White.copy(alpha = 0.75f) else C.Slate

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .clickable { onAvatarClick() }
        ) {
            Avatar(
                initials = initials,
                color = avatarColor,
                size = 44,
                fontSize = 16,
                photoUrl = avatarUrl
            )
        }

        Column(Modifier.weight(1f)) {
            Text(greeting, fontSize = 12.5.sp, color = sub)
            Text(
                name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                letterSpacing = (-0.3).sp
            )
        }

        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (dark) Color.White.copy(alpha = 0.12f) else C.Subtle)
                .clickable { onNotificationsClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = "Notifications",
                tint = fg,
                modifier = Modifier.size(20.dp)
            )
            if (bellDot) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-10).dp, y = 8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(C.Orange)
                        .border(2.dp, if (dark) C.Blue else C.Subtle, CircleShape)
                )
            }
        }
    }
}