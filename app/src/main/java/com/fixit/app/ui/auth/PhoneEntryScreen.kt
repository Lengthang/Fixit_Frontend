package com.fixit.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.PrimaryButton
import com.fixit.app.ui.components.Progress
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.signup.SignupDraftViewModel
import com.fixit.app.ui.theme.C

@Composable
fun PhoneEntryScreen(
    onBack: () -> Unit,
    onCodeSent: (phone: String) -> Unit,
    draftVm: SignupDraftViewModel,
    vm: PhoneEntryViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.effects.collect {
            when (it) {
                PhoneEntryEffect.OtpSent -> {
                    draftVm.update { d -> d.copy(phone = vm.fullPhone()) }
                    onCodeSent(vm.fullPhone())
                }
            }
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); vm.dismissError() }
    }

    FixItScreen {
        TopBar(step = 1, total = 11, onBack = onBack)
        Progress(step = 1, total = 11)
        Column(Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
            Text("What's your mobile number?",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = C.Ink, lineHeight = 30.sp)
            Spacer(Modifier.height(8.dp))
            Text("We'll text you a code. Standard rates may apply.",
                fontSize = 14.sp, color = C.Slate, lineHeight = 20.sp)
        }
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text("Mobile number", fontSize = 12.sp, color = C.Blue, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.widthIn(min = 92.dp).clip(RoundedCornerShape(10.dp))
                        .background(Color.White).border(1.5.dp, C.Line, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(width = 22.dp, height = 16.dp).clip(RoundedCornerShape(2.dp))
                        .background(Brush.verticalGradient(
                            0f to Color(0xFFB22234), 0.33f to Color(0xFFB22234),
                            0.33f to Color.White, 0.66f to Color.White,
                            0.66f to Color(0xFF3C3B6E), 1f to Color(0xFF3C3B6E))))
                    Text(state.countryCode, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
                    Text("▾", fontSize = 10.sp, color = C.Slate)
                }
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(Color.White).border(1.5.dp, C.Blue, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = state.phone,
                        onValueChange = vm::onPhoneChange,
                        textStyle = TextStyle(color = C.Ink, fontSize = 16.sp,
                            fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
                        cursorBrush = SolidColor(C.Blue),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (state.phone.isEmpty()) Text("(415) 555-0192", color = C.Mute, fontSize = 16.sp)
                            inner()
                        },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(C.BlueSoft).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Lock, null, tint = C.Blue, modifier = Modifier.size(16.dp))
                Text("Your number is only used for verification and account recovery.",
                    fontSize = 12.sp, color = C.BlueDark, lineHeight = 18.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        SnackbarHost(snackbar, modifier = Modifier.padding(horizontal = 16.dp))
        PrimaryButton(
            text = if (state.sending) "Sending…" else "Send code",
            enabled = !state.sending && state.phone.length >= 6,
            onClick = vm::send,
        )
    }
}