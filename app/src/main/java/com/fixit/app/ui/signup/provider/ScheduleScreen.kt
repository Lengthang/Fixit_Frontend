package com.fixit.app.ui.signup.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.PrimaryButton
import com.fixit.app.ui.components.Progress
import com.fixit.app.ui.components.ScreenTitle
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.signup.AvailabilitySlot
import com.fixit.app.ui.signup.SignupDraftViewModel
import com.fixit.app.ui.theme.C

private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")
private val DAY_NAMES  = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    draftVm: SignupDraftViewModel,
    vm: ScheduleViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val anyDayOn = state.activeDays.any { it }

    FixItScreen {
        TopBar(step = 8, total = 11, onBack = onBack)
        Progress(step = 8, total = 11)
        ScreenTitle(
            "When are you available?",
            "Set your availability. Customers can only book within these hours.",
        )

        Column(Modifier.padding(horizontal = 24.dp)) {
            Text("Days of the week", fontSize = 12.sp, color = C.Slate, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DAY_LABELS.forEachIndexed { i, label ->
                    DayPill(label, state.activeDays[i]) { vm.toggleDay(i) }
                }
            }
        }

        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
            Text("Working hours", fontSize = 12.sp, color = C.Slate, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            // Times are static-display in your original screen; we keep that visual
            // but bind to the real values from state.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TimeBox(label = "Start", time = state.openTime, modifier = Modifier.weight(1f))
                TimeBox(label = "End", time = state.closeTime, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(C.BlueSoft).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(6.dp).offset(y = 4.dp).clip(CircleShape).background(C.Blue))
                Text(
                    buildAnnotatedString {
                        append("You'll be shown as ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("available") }
                        append(" ${state.openTime}–${state.closeTime} on selected days.")
                    },
                    fontSize = 12.5.sp, color = C.BlueDark, lineHeight = 18.sp,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = "Continue",
            enabled = anyDayOn,
            onClick = {
                val slots = state.activeDays
                    .mapIndexedNotNull { i, on ->
                        if (!on) null
                        else AvailabilitySlot(DAY_NAMES[i], state.openTime, state.closeTime)
                    }
                draftVm.update { it.copy(availability = slots) }
                onContinue()
            },
        )
    }
}

@Composable
private fun DayPill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape)
            .background(if (active) C.Blue else Color.White)
            .border(1.5.dp, if (active) C.Blue else C.Line, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (active) Color.White else C.Slate,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TimeBox(label: String, time: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, C.Line, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, fontSize = 11.sp, color = C.Mute)
        Spacer(Modifier.height(2.dp))
        Text(time, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.Ink)
    }
}