package com.fixit.app.ui.signup.provider

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.layout.onGloballyPositioned
import com.fixit.app.ui.components.LocationPickerMap
import com.google.android.gms.maps.model.LatLng

@Composable
fun ServiceAreaScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    draftVm: SignupDraftViewModel,
    vm: ServiceAreaViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.resolveLocation() }

    FixItScreen {
        TopBar(step = 6, total = 11, onBack = onBack)
        Progress(step = 6, total = 11)
        ScreenTitle(
            "Select your service area",
            "Drag the pin or adjust the radius to set where you want to take jobs.",
        )
        LocationPickerMap(
            selected = state.latitude?.let { lat ->
                state.longitude?.let { lng -> LatLng(lat, lng) }
            },
            onPick = { latLng -> vm.onPick(latLng.latitude, latLng.longitude) },
            radiusKm = state.radiusKm,
            locating = state.locating,
            onRecenterRequest = vm::resolveLocation,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(320.dp),
        )
//        Box(
//            Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(320.dp)
//                .clip(RoundedCornerShape(16.dp))
//                .border(1.dp, C.Line, RoundedCornerShape(16.dp)),
//        ) {
//            MapBackground()
//            Box(
//                Modifier.align(Alignment.Center).size(220.dp).clip(CircleShape)
//                    .background(C.Blue.copy(alpha = 0.12f))
//                    .border(2.dp, C.Blue, CircleShape),
//            )
//            Icon(
//                Icons.Filled.LocationOn, null, tint = C.Orange,
//                modifier = Modifier.align(Alignment.Center).size(48.dp),
//            )
//            Box(
//                Modifier.align(Alignment.TopStart).padding(12.dp)
//                    .clip(RoundedCornerShape(20.dp)).background(Color.White)
//                    .padding(horizontal = 12.dp, vertical = 8.dp),
//            ) {
//                Text(
//                    "Radius · ${state.radiusKm} km",
//                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = C.Ink,
//                )
//            }
//        }

        // Functional radius slider
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp)) {
            Text("Service radius", fontSize = 12.sp, color = C.Slate, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            RadiusSlider(value = state.radiusKm, onValueChange = vm::onRadiusChange)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1 km", fontSize = 11.sp, color = C.Mute)
                Text("50 km", fontSize = 11.sp, color = C.Mute)
            }
            if (state.latitude == null && !state.locating) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Couldn't read your location. Tap to retry.",
                    fontSize = 12.sp,
                    color = C.Orange,
                    modifier = Modifier.clickable { vm.resolveLocation() },
                )
            }
        }

        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = "Confirm area",
            enabled = state.latitude != null && state.longitude != null,
            onClick = {
                draftVm.update {
                    it.copy(
                        latitude = state.latitude,
                        longitude = state.longitude,
                        serviceRadiusKm = state.radiusKm,
                    )
                }
                onContinue()
            },
        )
    }
}

/**
 * Drag-aware 1..50 km slider. Exposed (no longer private) so
 * EditProfileScreen reuses the same component instead of duplicating it.
 */
@Composable
fun RadiusSlider(value: Int, onValueChange: (Int) -> Unit) {
    var trackWidthPx by remember { mutableStateOf(1f) }
    val density = LocalDensity.current
    val fraction = (value - 1).coerceAtLeast(0) / 49f

    Box(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val pct = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                    onValueChange(1 + (pct * 49f).toInt())
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(C.Line)
                .onGloballyPositionedTrackWidth { trackWidthPx = it.toFloat() },
        ) {
            Box(
                Modifier.fillMaxWidth(fraction).fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp)).background(C.Blue),
            )
        }
        Box(Modifier.fillMaxWidth(fraction), contentAlignment = Alignment.CenterEnd) {
            Box(
                Modifier.size(22.dp).clip(CircleShape).background(Color.White)
                    .border(3.dp, C.Blue, CircleShape),
            )
        }
    }
}

// Tiny inline helper because we can't import a Modifier from within the file in one line nicely.
// Stays private — only RadiusSlider needs it.
private fun Modifier.onGloballyPositionedTrackWidth(report: (Int) -> Unit): Modifier =
    this.onGloballyPositioned { coordinates ->
        report(coordinates.size.width)
    }

/**
 * Static visual map background — roads + park/water rects on a pale blue
 * canvas. Exposed (no longer private) so EditProfileScreen reuses it.
 */
@Composable
fun MapBackground() {
    Canvas(Modifier.fillMaxSize().background(Color(0xFFE8EEF5))) {
        fun road(p: Path, width: Float) = drawPath(p, color = Color.White, style = Stroke(width = width))
        val w = size.width
        val h = size.height
        road(Path().apply {
            moveTo(-20f, 120f * h / 500f)
            quadraticBezierTo(150f * w / 400f, 140f * h / 500f, 420f * w / 400f, 100f * h / 500f)
        }, 14f)
        road(Path().apply {
            moveTo(-20f, 260f * h / 500f)
            quadraticBezierTo(200f * w / 400f, 220f * h / 500f, 420f * w / 400f, 280f * h / 500f)
        }, 10f)
        road(Path().apply {
            moveTo(80f * w / 400f, -20f); lineTo(120f * w / 400f, 520f * h / 500f)
        }, 12f)
        road(Path().apply {
            moveTo(280f * w / 400f, -20f); lineTo(320f * w / 400f, 520f * h / 500f)
        }, 8f)
        drawRect(
            color = Color(0xFFD7E4D2),
            topLeft = Offset(140f * w / 400f, 150f * h / 500f),
            size = Size(110f * w / 400f, 80f * h / 500f),
        )
        drawRect(
            color = Color(0xFFCFE0EC),
            topLeft = Offset(340f * w / 400f, 320f * h / 500f),
            size = Size(80f * w / 400f, 130f * h / 500f),
        )
    }
}