package com.fixit.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixit.app.ui.theme.C

@Composable
fun ScreenTitle(title: String, sub: String? = null) {
    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp)) {
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp, lineHeight = 30.sp, color = C.Ink)
        if (sub != null) {
            Spacer(Modifier.height(8.dp))
            Text(sub, fontSize = 14.sp, color = C.Slate, lineHeight = 20.sp)
        }
    }
}