package com.fixit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixit.app.ui.theme.C
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

/**
 * Shared Google-Maps location picker. Single source of truth for every screen
 * that needs the user to visually choose a point (provider service area,
 * provider edit-profile, customer saved addresses).
 *
 * Design (fixes the "tap not stored / wrong location" bug):
 *  - [selected] is the ONE source of truth. The marker and camera follow it.
 *  - User gestures report UP, never via a feedback loop:
 *      • tapping the map  -> onMapClick -> onPick(latLng)
 *      • dragging the pin -> onMarkerDragEnd via markerState position read
 *    Both call [onPick]; the parent updates [selected]; the marker re-renders
 *    from [selected]. There is no second effect pushing the marker position
 *    back into onPick, so a picked value can no longer be clobbered.
 *  - The camera animates to [selected] only when [selected] actually changes,
 *    so it never fights the user mid-gesture.
 *
 * The composable holds NO business logic and NO network/Geocoder calls — those
 * stay in the ViewModels, exactly as the existing code organises them.
 *
 * @param selected   current pin position, or null if nothing chosen yet.
 * @param onPick     called with the new LatLng whenever the user picks a point.
 * @param radiusKm   optional service-area radius to draw; null hides the circle.
 * @param locating   true while the caller is resolving a GPS fix (spins the FAB).
 * @param onRecenterRequest tapped "locate me"; caller resolves + feeds back via [selected].
 * @param defaultCenter where to point the camera before any selection exists.
 */
@Composable
fun LocationPickerMap(
    selected: LatLng?,
    onPick: (LatLng) -> Unit,
    modifier: Modifier = Modifier,
    radiusKm: Int? = null,
    locating: Boolean = false,
    onRecenterRequest: (() -> Unit)? = null,
    defaultCenter: LatLng = PHNOM_PENH,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selected ?: defaultCenter, 15f)
    }

    // Marker is a pure mirror of [selected]; we read its position only on
    // drag-end to report a pick. We never write the marker back into onPick
    // from an effect, which is what previously created the clobbering loop.
    val markerState = rememberMarkerState(position = selected ?: defaultCenter)

    // Whenever the parent's selection changes (tap, drag, or a resolved GPS
    // fix arriving as [selected]), move the pin AND animate the camera there.
    LaunchedEffect(selected) {
        val target = selected ?: return@LaunchedEffect
        markerState.position = target
        cameraPositionState.animate(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(target, 15f)
            ),
            durationMs = 500,
        )
    }

    Box(modifier.clip(RoundedCornerShape(16.dp))) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = MapType.NORMAL),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,   // we draw our own themed FAB
                mapToolbarEnabled = false,
            ),
            // Tapping anywhere commits that point straight to the parent.
            onMapClick = { latLng -> onPick(latLng) },
        ) {
            if (selected != null && radiusKm != null) {
                Circle(
                    center = selected,
                    radius = radiusKm.toDouble() * 1000.0,   // km → metres
                    fillColor = C.Blue.copy(alpha = 0.12f),
                    strokeColor = C.Blue,
                    strokeWidth = 4f,
                )
            }
            Marker(
                state = markerState,
                draggable = true,
                title = "Selected location",
                // When the drag finishes, read the final marker position once
                // and report it up. (onMarkerDragEnd-style: a single commit.)
                onInfoWindowClick = { },
            )
        }

        // Drag-end commit: observe the marker only while it is actively being
        // dragged, then push the final position up exactly once.
        DragEndReporter(markerStatePosition = markerState.position, isDragging = markerState.isDragging, onPick = onPick, selected = selected)

        if (radiusKm != null) {
            // Top-left radius badge — same chrome ServiceAreaScreen used.
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    "Radius · $radiusKm km",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = C.Ink,
                )
            }
        }

        if (onRecenterRequest != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .clickable(enabled = !locating) { onRecenterRequest() },
                contentAlignment = Alignment.Center,
            ) {
                if (locating) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = C.Blue)
                } else {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = "Use my current location",
                        tint = C.Blue,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

/**
 * Reports a marker drag exactly once, when dragging transitions from active to
 * idle. Reading [com.google.maps.android.compose.MarkerState.isDragging] lets
 * us avoid committing on every intermediate frame (which is what caused the
 * old feedback loop). When the user lets go and the new position differs from
 * the current [selected], we report it up.
 */
@Composable
private fun DragEndReporter(
    markerStatePosition: LatLng,
    isDragging: Boolean,
    selected: LatLng?,
    onPick: (LatLng) -> Unit,
) {
    // Fire only on the falling edge of isDragging (true -> false).
    val wasDragging = remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(isDragging) {
        if (wasDragging.value && !isDragging) {
            val p = markerStatePosition
            if (selected == null ||
                p.latitude != selected.latitude ||
                p.longitude != selected.longitude
            ) {
                onPick(p)
            }
        }
        wasDragging.value = isDragging
    }
}

/** Phnom Penh — sensible default camera before any GPS fix / selection. */
val PHNOM_PENH = LatLng(11.5564, 104.9282)