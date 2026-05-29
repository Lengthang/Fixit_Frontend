package com.fixit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.normalizeMediaUrl

/**
 * Round category button used on Customer Home + "All categories" screens.
 * When [iconUrl] is present it loads the remote category icon (Coil), matching
 * the Avatar/ServiceIcon pattern used elsewhere. When it's null/blank we fall
 * back to a built-in vector resolved via [iconForCategory] so the disc never
 * renders empty.
 */
@Composable
fun CategoryCircle(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconUrl: String? = null,
) {
    val resolvedIconUrl = normalizeMediaUrl(iconUrl)
    Column(
        modifier
            .width(72.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(C.Subtle),
            contentAlignment = Alignment.Center,
        ) {
            if (!resolvedIconUrl.isNullOrBlank()) {
                AsyncImage(
                    model = resolvedIconUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            } else {
                Icon(
                    imageVector = iconForCategory(name),
                    contentDescription = name,
                    tint = C.Ink,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Text(
            name,
            fontSize = 11.5.sp,
            color = C.Slate,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}