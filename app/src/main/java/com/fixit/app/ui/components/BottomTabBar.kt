package com.fixit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixit.app.ui.theme.C
import kotlin.collections.forEach

data class TabItem(
    val id: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun BottomTabBar(
    tabs: List<TabItem>,
    active: String,
    onTabClick: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(C.Bg)
            .border(width = 1.dp, color = C.Line, shape = RectangleShape)
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        tabs.forEach { t ->
            val on = t.id == active
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onTabClick(t.id) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    t.icon,
                    contentDescription = t.label,
                    tint = if (on) C.Blue else C.Mute,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    t.label,
                    fontSize = 10.5.sp,
                    color = if (on) C.Blue else C.Mute,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ProviderTabBar(active: String, onTabClick: (String) -> Unit = {}) {
    BottomTabBar(
        tabs = listOf(
            TabItem("home", "Home", Icons.Filled.Home),
            TabItem("jobs", "Jobs", Icons.Filled.WorkOutline),
            TabItem("calendar", "Calendar", Icons.Filled.CalendarMonth),
            TabItem("messages", "Inbox", Icons.AutoMirrored.Filled.Chat),
            TabItem("profile", "Profile", Icons.Filled.Person)
        ),
        active = active,
        onTabClick = onTabClick
    )
}

@Composable
fun CustomerTabBar(active: String, onTabClick: (String) -> Unit = {}) {
    BottomTabBar(
        tabs = listOf(
            TabItem("home", "Home", Icons.Filled.Home),
            TabItem("bookings", "Bookings", Icons.Filled.CalendarMonth),
            TabItem("messages", "Messages", Icons.AutoMirrored.Filled.Chat),
            TabItem("profile", "Profile", Icons.Filled.Person)
        ),
        active = active,
        onTabClick = onTabClick
    )
}