package com.fixit.app.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixit.app.ui.theme.C
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
/**
 * Shared Google-Maps location picker. Single source of truth for every screen
 * that needs the user to visually choose a point (provider service area,
 * provider edit-profile, customer saved addresses).
 *
 * Behaviour:
 *  - Renders a Google Map clipped to the same 16dp rounded card the old fake
 *    [com.fixit.app.ui.signup.provider.MapBackground] used, so the layout of
 *    callers is unchanged.
 *  - A draggable orange pin marks the selection. Dragging the pin OR tapping
 *    anywhere on the map updates [selected] via [onPick].
 *  - When [radiusKm] is non-null a translucent blue circle (Blue @ 12% with a
 *    2dp Blue stroke) is drawn around the pin — the service-area visual that
 *    ServiceArea / EditProfile previously faked with a static circle.
 *  - A "locate me" FAB (bottom-right) calls [onRecenterRequest]; the caller
 *    runs the existing FusedLocationProviderClient flow and feeds the fix back
 *    in through [selected]. The button shows a spinner while [locating].
 *
 * The composable holds NO business logic and NO network/Geocoder calls — those
 * stay in the ViewModels, exactly as the existing code organises them.
 *
 * @param selected   current pin position, or null if nothing chosen yet.
 * @param onPick     called with the new LatLng whenever the user moves the pin.
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
        position = CameraPosition.fromLatLngZoom(selected ?: defaultCenter, 14f)
    }

    // Keep a marker state in sync with the externally-controlled selection so
    // a "locate me" result (which arrives via [selected]) jumps the pin too.
    val markerState: MarkerState = rememberMarkerState(position = selected ?: defaultCenter)

    LaunchedEffect(selected) {
        if (selected != null) {
            markerState.position = selected
            cameraPositionState.position =
                CameraPosition.fromLatLngZoom(selected, cameraPositionState.position.zoom)
        }
    }

    // Dragging the pin commits on each move.
    LaunchedEffect(markerState.position) {
        val p = markerState.position
        if (selected == null || p.latitude != selected.latitude || p.longitude != selected.longitude) {
            onPick(p)
        }
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
            )
        }

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

/** Phnom Penh — sensible default camera before any GPS fix / selection. */
val PHNOM_PENH = LatLng(11.5564, 104.9282)