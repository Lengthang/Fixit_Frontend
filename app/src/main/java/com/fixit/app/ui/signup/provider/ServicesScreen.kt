package com.fixit.app.ui.signup.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.PrimaryButton
import com.fixit.app.ui.components.Progress
import com.fixit.app.ui.components.ScreenTitle
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.signup.SignupDraftViewModel
import com.fixit.app.ui.theme.C

@Composable
fun ServicesScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    draftVm: SignupDraftViewModel,
    vm: ServicesViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    FixItScreen {
        TopBar(step = 7, total = 11, onBack = onBack)
        Progress(step = 7, total = 11)
        ScreenTitle(
            "What service will you provide?",
            "Select what you specialize in. You can edit this anytime.",
        )

        Column(Modifier.padding(horizontal = 24.dp)) {
            Text("Service type", fontSize = 12.sp, color = C.Slate, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            if (state.loading) {
                Text("Loading…", fontSize = 13.sp, color = C.Mute)
            } else {
                CategoryGrid(
                    items = state.categories.map { it.id to it.name },
                    selected = state.selectedIds,
                    onToggle = vm::toggle,
                )
            }
        }

        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
            Text("Years of experience", fontSize = 12.sp, color = C.Slate, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, C.Line, RoundedCornerShape(12.dp)).padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${state.years}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = C.Blue)
                    Text("years", fontSize = 14.sp, color = C.Slate)
                }
                Spacer(Modifier.height(8.dp))
                YearsSlider(value = state.years, onValueChange = vm::onYearsChange)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0", fontSize = 11.sp, color = C.Mute)
                    Text("25+", fontSize = 11.sp, color = C.Mute)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = "Continue",
            enabled = state.selectedIds.isNotEmpty(),
            onClick = {
                draftVm.update {
                    it.copy(
                        categoryIds = state.selectedIds.toList(),
                        yearsExperience = state.years,
                    )
                }
                onContinue()
            },
        )
    }
}

@Composable
private fun CategoryGrid(
    items: List<Pair<String, String>>,   // id to name
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (id, name) ->
                    Chip(label = name, active = id in selected, onClick = { onToggle(id) })
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, active: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (active) C.BlueSoft else Color.White)
            .border(1.5.dp, if (active) C.Blue else C.Line, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (active) Text("✓", fontSize = 12.sp, color = C.Blue, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 13.sp, color = if (active) C.Blue else C.Slate, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun YearsSlider(value: Int, onValueChange: (Int) -> Unit) {
    var trackWidthPx by remember { mutableStateOf(1f) }
    val fraction = value / 25f

    Box(
        Modifier.fillMaxWidth().height(28.dp).pointerInput(Unit) {
            detectHorizontalDragGestures { change, _ ->
                val pct = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                onValueChange((pct * 25f).toInt())
            }
        },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                .background(C.Line)
                .onGloballyPositioned { trackWidthPx = it.size.width.toFloat() },
        ) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight()
                .clip(RoundedCornerShape(2.dp)).background(C.Blue))
        }
        Box(Modifier.fillMaxWidth(fraction), contentAlignment = Alignment.CenterEnd) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(Color.White)
                .border(3.dp, C.Blue, CircleShape))
        }
    }
}