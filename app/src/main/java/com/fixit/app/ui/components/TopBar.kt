package com.fixit.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixit.app.ui.theme.C

@Composable
fun TopBar(
    showBack: Boolean = true,
    step: Int? = null,
    total: Int? = null,
    onBack: () -> Unit = {},
    endContent: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = C.Ink) }
        }
        Spacer(Modifier.weight(1f))
        if (step != null && total != null) {
            Text("$step/$total", color = C.Mute, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        endContent?.invoke()
    }
}