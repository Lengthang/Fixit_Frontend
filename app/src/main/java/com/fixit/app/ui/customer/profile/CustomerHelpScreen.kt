package com.fixit.app.ui.customer.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.IconBox
import com.fixit.app.ui.components.ScreenTitle
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.theme.C

// Admin support contact details. Centralised here so they're easy to update.
private const val SUPPORT_PHONE = "+855 23 999 888"
private const val SUPPORT_PHONE_DIAL = "+85523999888"
private const val SUPPORT_EMAIL = "support@fixit.com"

@Composable
fun CustomerHelpScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    FixItScreen(bg = C.Subtle) {
        TopBar(onBack = onBack)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                ScreenTitle(
                    title = "Help & support",
                    sub = "We're here to help. Reach out any time and our team will get back to you.",
                )

                // ── Friendly hero ──────────────────────────────────────────
                Column(
                    Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(C.Blue)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "Have a question?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        "Whether it's a booking, a payment, or anything else — call us or send an email and we'll sort it out together.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 19.sp,
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Contact us",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = C.Slate,
                    modifier = Modifier.padding(start = 24.dp, bottom = 10.dp),
                )

                Column(
                    Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ContactCard(
                        icon = Icons.Filled.Phone,
                        title = "Call us",
                        value = SUPPORT_PHONE,
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$SUPPORT_PHONE_DIAL")
                            }
                            context.startActivity(intent)
                        },
                    )
                    ContactCard(
                        icon = Icons.Filled.Email,
                        title = "Email us",
                        value = SUPPORT_EMAIL,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$SUPPORT_EMAIL")
                                putExtra(Intent.EXTRA_SUBJECT, "FixIt Support Request")
                            }
                            context.startActivity(intent)
                        },
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    "Support hours: Mon–Sat, 8:00 AM – 8:00 PM",
                    fontSize = 12.sp,
                    color = C.Mute,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ContactCard(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, C.Line, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBox(bg = C.BlueSoft) {
            Icon(icon, null, tint = C.Blue, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
            Text(value, fontSize = 12.5.sp, color = C.Slate, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = C.Mute,
            modifier = Modifier.size(14.dp),
        )
    }
}