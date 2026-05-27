package com.fixit.app.ui.customer.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import com.fixit.app.ui.components.Avatar
import com.fixit.app.ui.components.CustomerTabBar
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.ProviderTabBar
import com.fixit.app.ui.theme.C

@Composable
fun CustomerMessagesScreen(
    onTabClick: (String) -> Unit,
    onMessageClick: () -> Unit = {}
) {
    FixItScreen {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Messages",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                color = C.Ink,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.Search, null, tint = C.Ink, modifier = Modifier.size(22.dp))
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp)
        ) {
            // Static placeholder list — chat router will replace this.
            MessageRow("Sarah Lee", "SL", Color(0xFF0EA5E9), "Hi Marcus! What time works for you?", "2m", unread = 2, onClick = onMessageClick)
            MessageRow("Rebecca Chen", "RC", Color(0xFFF59E0B), "You: I'll be there at 2 PM sharp.", "1h", unread = 0)
            MessageRow("Jay Patel", "JP", Color(0xFFEF4444), "Thanks, see you at 4:30!", "3h", unread = 0)
            MessageRow("David Kim", "DK", Color(0xFF8B5CF6), "Is today still good for the appointment?", "1d", unread = 1)
            MessageRow("Ana Soto", "AS", Color(0xFF10B981), "You: All done, thanks for booking!", "3d", unread = 0)
        }
        CustomerTabBar(active = "messages", onTabClick = onTabClick)
    }
}

@Composable
private fun MessageRow(
    name: String,
    initials: String,
    color: Color,
    preview: String,
    time: String,
    unread: Int,
    onClick: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Avatar(initials, color)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    fontSize = 14.sp,
                    color = C.Ink,
                    fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    time,
                    fontSize = 11.sp,
                    color = if (unread > 0) C.Blue else C.Mute,
                    fontWeight = if (unread > 0) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            Row(
                Modifier.padding(top = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    preview,
                    fontSize = 12.5.sp,
                    color = if (unread > 0) C.Ink else C.Slate,
                    fontWeight = if (unread > 0) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                if (unread > 0) {
                    Box(
                        Modifier.size(20.dp).clip(CircleShape).background(C.Orange),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            unread.toString(),
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
}