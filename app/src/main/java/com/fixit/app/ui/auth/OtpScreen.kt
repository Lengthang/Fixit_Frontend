package com.fixit.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
fun OtpScreen(
    phone: String,
    onBack: () -> Unit,
    onVerifiedNew: () -> Unit,
    onVerifiedExisting: (isProvider: Boolean) -> Unit,
    draftVm: SignupDraftViewModel,
    vm: OtpViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val draft by draftVm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(Unit) {
        vm.effects.collect {
            when (it) {
                is OtpEffect.Verified ->
                    if (it.isNewUser) onVerifiedNew()
                    else onVerifiedExisting(it.isProvider)
            }
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); vm.dismissError() }
    }

    FixItScreen {
        TopBar(step = 2, total = 11, onBack = onBack)
        Progress(step = 2, total = 11)
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp)) {
            Text("Enter the code", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp, color = C.Ink)
            Spacer(Modifier.height(8.dp))
            Text(buildAnnotatedString {
                append("Sent to ")
                withStyle(SpanStyle(color = C.Ink, fontWeight = FontWeight.Bold)) { append(phone) }
            }, fontSize = 14.sp, color = C.Slate, lineHeight = 20.sp)
        }

        // Six visual cells driven by a single hidden text field.
        Box(Modifier.padding(horizontal = 24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(6) { i ->
                    val char = state.code.getOrNull(i)?.toString() ?: ""
                    val active = i == state.code.length
                    Box(
                        Modifier.weight(1f).height(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (char.isNotEmpty()) C.BlueSoft else Color.White)
                            .border(2.dp, if (active) C.Blue else C.Line, RoundedCornerShape(12.dp))
                            .clickable { focus.requestFocus() },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (char.isNotEmpty()) Text(char, fontSize = 26.sp,
                            fontWeight = FontWeight.Bold, color = C.Ink)
                    }
                }
            }
            BasicTextField(
                value = state.code,
                onValueChange = vm::onCodeChange,
                textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
                cursorBrush = SolidColor(Color.Transparent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.matchParentSize().focusRequester(focus),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(buildAnnotatedString {
            append("Didn't get a code? ")
            withStyle(SpanStyle(color = C.Blue, fontWeight = FontWeight.SemiBold)) { append("Resend") }
        }, fontSize = 13.sp, color = C.Slate, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.weight(1f))
        SnackbarHost(snackbar, modifier = Modifier.padding(horizontal = 16.dp))
        PrimaryButton(
            text = if (state.verifying) "Verifying…" else "Verify",
            enabled = !state.verifying && state.code.length == 6,
            onClick = { vm.verify(phone, draft.name.ifBlank { null }) },
        )
    }
}