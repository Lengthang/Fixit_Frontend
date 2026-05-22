package com.fixit.app.ui.components

import androidx.compose.foundation.BorderStroke
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
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit = {}) {
    Box(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp, top = 12.dp)) {
        Box(
            Modifier.fillMaxWidth().height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(if (enabled) C.Blue else C.Mute)
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit = {}) {
    Box(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
        Box(
            Modifier.fillMaxWidth().height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White)
                .border(BorderStroke(1.5.dp, C.Line), RoundedCornerShape(26.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) { Text(text, color = C.Slate, fontSize = 15.sp, fontWeight = FontWeight.Medium) }
    }
}