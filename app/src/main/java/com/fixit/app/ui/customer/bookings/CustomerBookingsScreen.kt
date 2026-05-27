package com.fixit.app.ui.customer.bookings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fixit.app.ui.components.CustomerTabBar
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.theme.C

@Composable
fun CustomerBookingsScreen(onTabClick: (String) -> Unit) {
    FixItScreen {
        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Bookings — coming soon", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.Slate)
        }
        CustomerTabBar(active = "bookings", onTabClick = onTabClick)
    }
}