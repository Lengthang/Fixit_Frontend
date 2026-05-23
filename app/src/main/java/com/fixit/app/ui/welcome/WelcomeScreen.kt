package com.fixit.app.ui.welcome

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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixit.app.dev.DevConfig
import com.fixit.app.dev.DevModePanel
import com.fixit.app.dev.DevTestUser
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.theme.C

@Composable
fun WelcomeScreen(
    onCreate: () -> Unit,
    onSignIn: () -> Unit,
    onDevNewUser: () -> Unit = {},
    onDevExistingUser: (DevTestUser) -> Unit = {},
) {
    FixItScreen {
        TopBar(showBack = false)
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(C.Blue),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(28.dp).rotate(45f)
                    .clip(RoundedCornerShape(6.dp)).background(C.Orange))
            }
            Spacer(Modifier.height(28.dp))
            Text("Welcome to Servly",
                fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp,
                textAlign = TextAlign.Center, color = C.Ink)
            Spacer(Modifier.height(12.dp))
            Text("Find trusted pros or grow your business — all in one app.",
                fontSize = 15.sp, color = C.Slate,
                textAlign = TextAlign.Center, lineHeight = 22.sp)
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Box(
                Modifier.fillMaxWidth().height(52.dp)
                    .clip(RoundedCornerShape(26.dp)).background(C.Blue)
                    .clickable { onCreate() },
                contentAlignment = Alignment.Center,
            ) { Text("Create an account", color = Color.White, fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold) }
        }
        Box(Modifier.fillMaxWidth().padding(20.dp)) {
            Box(
                Modifier.fillMaxWidth().height(52.dp)
                    .clip(RoundedCornerShape(26.dp)).background(Color.White)
                    .border(1.5.dp, C.Line, RoundedCornerShape(26.dp))
                    .clickable { onSignIn() },
                contentAlignment = Alignment.Center,
            ) { Text("Sign in", color = C.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }

        if (DevConfig.DEV_MODE) {
            DevModePanel(
                onDevNewUser = onDevNewUser,
                onDevExistingUser = onDevExistingUser,
            )
        }

        Text(
            buildAnnotatedString {
                append("By continuing you agree to our ")
                withStyle(SpanStyle(color = C.Blue, fontWeight = FontWeight.Medium)) { append("Terms") }
                append(" & ")
                withStyle(SpanStyle(color = C.Blue, fontWeight = FontWeight.Medium)) { append("Privacy Policy") }
                append(".")
            },
            fontSize = 11.sp, color = C.Mute, textAlign = TextAlign.Center, lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(bottom = 24.dp),
        )
    }
}