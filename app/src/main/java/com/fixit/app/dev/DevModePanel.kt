package com.fixit.app.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

@Composable
fun DevModePanel(
    onDevNewUser: () -> Unit,
    onDevExistingUser: (DevTestUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF7ED))
            .border(1.dp, Color(0xFFFDBA74), RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Text(
            "DEV MODE — bypass OTP",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = Color(0xFF9A3412),
        )
        Spacer(Modifier.height(10.dp))

        DevButton(
            label = "New user → run onboarding",
            onClick = onDevNewUser,
        )

        Spacer(Modifier.height(12.dp))
        Text("Or sign in as existing test user:", fontSize = 12.sp, color = C.Slate)
        Spacer(Modifier.height(6.dp))

        DevConfig.testUsers.forEach { u ->
            Spacer(Modifier.height(6.dp))
            DevButton(label = u.label, onClick = { onDevExistingUser(u) })
        }
    }
}

@Composable
private fun DevButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFFDBA74), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF9A3412),
        )
    }
}