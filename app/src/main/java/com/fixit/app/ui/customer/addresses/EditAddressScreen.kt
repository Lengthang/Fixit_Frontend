package com.fixit.app.ui.customer.addresses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixit.app.ui.components.FixItTextField
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.LocationPickerMap
import com.fixit.app.ui.components.PrimaryButton
import com.fixit.app.ui.components.ScreenTitle
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.theme.C
import com.google.android.gms.maps.model.LatLng

@Composable
fun EditAddressScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditAddressViewModel,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EditAddressEffect.SavedOk -> onSaved()
            }
        }
    }

    val isEditing = state.editingId != null

    FixItScreen {
        TopBar(onBack = onBack)
        ScreenTitle(
            if (isEditing) "Edit address" else "Add address",
            "Drop a pin where you want your service, then name it.",
        )

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            LocationPickerMap(
                selected = state.latitude?.let { lat ->
                    state.longitude?.let { lng -> LatLng(lat, lng) }
                },
                onPick = { latLng -> viewModel.onPick(latLng.latitude, latLng.longitude) },
                locating = state.locating,
                onRecenterRequest = viewModel::resolveLocation,
                modifier = Modifier.fillMaxWidth().height(300.dp),
            )

            Spacer(Modifier.height(10.dp))
            Text(
                state.address.ifBlank { "Tap the map or use \u201clocate me\u201d to choose a point." },
                fontSize = 12.5.sp,
                color = if (state.address.isBlank()) C.Mute else C.Slate,
            )
            Spacer(Modifier.height(18.dp))
        }

        // Label field reuses the shared FixItTextField (which applies its own
        // horizontal padding, so it sits outside the scroll column's padding).
        FixItTextField(
            label = "Label",
            value = state.label,
            onValueChange = viewModel::onLabelChange,
            placeholder = "Home, Office, Mom's place…",
        )

        DefaultToggleRow(
            checked = state.isDefault,
            onChange = viewModel::onDefaultChange,
        )

        state.errorMessage?.let { err ->
            Text(
                err,
                fontSize = 12.sp,
                color = C.Red,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }

        PrimaryButton(
            text = if (state.isSaving) "Saving…" else "Confirm address",
            enabled = state.canSave,
            onClick = viewModel::save,
        )
    }
}

@Composable
private fun DefaultToggleRow(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) C.Blue else Color.White)
                .border(2.dp, if (checked) C.Blue else C.Line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White))
            }
        }
        Text("Set as default address", fontSize = 13.sp, color = C.Ink, fontWeight = FontWeight.Medium)
    }
}