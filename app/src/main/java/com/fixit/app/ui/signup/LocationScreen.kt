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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
) {
    // Request the OS permission. Whatever the user picks (allow/deny),
    // we advance — the screens that need a real lat/lng (ServiceAreaScreen
    // on the provider branch) read it from FusedLocationProviderClient later
    // and gracefully fall back if permission was denied.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { onContinue() },
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
        PrimaryButton("Allow location") {
            launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        OutlineButton("Not now", onContinue)
    }
}