package com.fixit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fixit.app.ui.theme.C
import kotlin.text.isNullOrBlank

/**
 * Circular avatar.
 * If [photoUrl] is non-null, loads the remote image; otherwise renders [initials]
 * on a coloured circle. Coil falls back to the initials block if the load fails.
 */
@Composable
fun Avatar(
    initials: String,
    color: Color = C.Slate,
    size: Int = 40,
    fontSize: Int = 13,
    photoUrl: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size.dp).clip(CircleShape)
            )
        } else {
            Text(
                initials,
                color = Color.White,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun IconBox(
    bg: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(bg),
        contentAlignment = Alignment.Center
    ) { content() }
}