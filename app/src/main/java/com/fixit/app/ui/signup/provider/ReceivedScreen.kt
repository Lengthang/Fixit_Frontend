package com.fixit.app.ui.signup.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.PrimaryButton
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.signup.SignupDraftViewModel
import com.fixit.app.ui.theme.C

@Composable
fun ReceivedScreen(
    onDashboard: () -> Unit,
    draftVm: SignupDraftViewModel,
) {
    val draft by draftVm.state.collectAsState()
    val primaryCategoryName = "Plumbing"  // backend doesn't tell us the picked name back yet;
    // safe default. Replace by looking up category by id when needed.

    FixItScreen(bg = C.Subtle) {
        TopBar(showBack = false)
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box {
                Box(
                    Modifier.size(120.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(88.dp).clip(CircleShape).background(C.Blue),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(44.dp))
                    }
                }
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)
                        .size(32.dp).clip(CircleShape).background(C.Orange)
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("★", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(32.dp))
            Text(
                "Application received",
                fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center, lineHeight = 32.sp, color = C.Ink,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                buildAnnotatedString {
                    append("Your application has been received. You'll get a confirmation message from our staff.")
                },
                fontSize = 15.sp, color = C.Slate,
                textAlign = TextAlign.Center, lineHeight = 22.sp,
            )
            Spacer(Modifier.height(28.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
                    .border(1.dp, C.Line, RoundedCornerShape(16.dp)).padding(18.dp),
            ) {
                Text("WHAT'S NEXT",
                    fontSize = 12.sp, color = C.Slate,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
                Spacer(Modifier.height(12.dp))
                Step("1", C.BlueSoft, C.Blue, buildAnnotatedString {
                    append("We review your details within ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("24–48 hours") }
                    append(".")
                })
                Spacer(Modifier.height(12.dp))
                Step("2", C.BlueSoft, C.Blue, buildAnnotatedString {
                    append("You'll receive a confirmation SMS & email.")
                })
                Spacer(Modifier.height(12.dp))
                Step("3", C.OrangeSoft, C.Orange, buildAnnotatedString {
                    append("Start receiving job requests.")
                })
            }
        }
        PrimaryButton("Go to dashboard", onClick = onDashboard)
    }
}

@Composable
private fun Step(number: String, bg: Color, fg: Color, text: androidx.compose.ui.text.AnnotatedString) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(bg),
            contentAlignment = Alignment.Center,
        ) { Text(number, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        Text(text, fontSize = 13.5.sp, color = C.Ink, lineHeight = 19.sp)
    }
}