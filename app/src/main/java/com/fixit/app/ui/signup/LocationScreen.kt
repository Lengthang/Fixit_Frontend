package com.fixit.app.ui.signup

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.OutlineButton
import com.fixit.app.ui.components.PrimaryButton
import com.fixit.app.ui.components.Progress
import com.fixit.app.ui.components.ScreenTitle
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.theme.C

@Composable
fun LocationScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    step: Int = 5,
    total: Int = 11,
    vm: LocationViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Advance only once the ViewModel has finished its save attempt (or skip).
    LaunchedEffect(Unit) {
        vm.effects.collect { effect ->
            when (effect) { is SignupLocationEffect.Done -> onContinue() }
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); vm.dismissError() }
    }

    // Request the OS permission. On grant we resolve + persist the location;
    // on denial we still advance (skip) so the user is never trapped — they can
    // add an address later from Saved Addresses.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) vm.resolveAndSave() else vm.skip()
        },
    )

    FixItScreen {
        TopBar(step = step, total = total, onBack = onBack)
        Progress(step = step, total = total)

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(140.dp).clip(CircleShape).background(C.BlueSoft),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = C.Blue,
                        modifier = Modifier.size(56.dp),
                    )
                }
            }
        }

        ScreenTitle(
            "Allow your location",
            "We use your location to show you nearby services and ensure accurate recommendations. Your location is never shared without permission.",
        )

        Spacer(Modifier.weight(1f))
        SnackbarHost(snackbar, modifier = Modifier.padding(horizontal = 16.dp))

        val busy = state.locating || state.saving
        PrimaryButton(
            text = when {
                state.locating -> "Locating…"
                state.saving -> "Saving…"
                else -> "Allow location"
            },
            enabled = !busy,
        ) {
            launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        OutlineButton("Not now") {
            if (!busy) vm.skip()
        }
    }
}