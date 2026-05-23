package com.fixit.app.ui.signup.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.PrimaryButton
import com.fixit.app.ui.components.Progress
import com.fixit.app.ui.components.ScreenTitle
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.signup.SignupDraftViewModel
import com.fixit.app.ui.theme.C

@Composable
fun CertificateScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    draftVm: SignupDraftViewModel,
) {
    // Upload endpoint doesn't exist yet, so "Choose file" is a no-op for now.
    // Both Continue and Skip just advance with certification = null.
    val advance = {
        draftVm.update { it.copy(certification = null) }
        onContinue()
    }

    FixItScreen {
        TopBar(step = 9, total = 11, onBack = onBack)
        Progress(step = 9, total = 11)
        ScreenTitle(
            "Upload a certificate",
            "Verified providers get a badge and 3× more bookings. You can skip and add later.",
        )
        Column(Modifier.padding(horizontal = 24.dp)) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)).background(C.Subtle)
                    .border(2.dp, C.Line, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(C.BlueSoft),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.FileUpload, null, tint = C.Blue, modifier = Modifier.size(26.dp)) }
                Text("Upload your certificate", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
                Text("PDF, JPG, or PNG\nMax 10 MB", fontSize = 12.sp, color = C.Slate,
                    textAlign = TextAlign.Center, lineHeight = 18.sp)
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White)
                        .border(1.5.dp, C.Blue, RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) { Text("Choose file", color = C.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .border(1.dp, C.Line, RoundedCornerShape(12.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(C.OrangeSoft),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.EmojiEvents, null, tint = C.Orange, modifier = Modifier.size(18.dp)) }
                Column(Modifier.weight(1f)) {
                    Text("Get verified faster", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
                    Text("Licenses, trade certs, insurance all count.", fontSize = 11.5.sp, color = C.Slate)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("Continue", onClick = advance)
        Text(
            "Skip for now",
            fontSize = 14.sp, color = C.Slate, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 16.dp)
                .clickable {
                    draftVm.update { it.copy(certification = null) }
                    onSkip()
                },
        )
    }
}