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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixit.app.domain.model.SavedLocation
import com.fixit.app.ui.components.EmptyState
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.PrimaryButton
import com.fixit.app.ui.components.ScreenTitle
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.OnLifecycleStart

@Composable
fun SavedAddressesScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: SavedAddressesViewModel,
) {
    val state by viewModel.state.collectAsState()
    OnLifecycleStart { viewModel.refresh() }

    FixItScreen {
        TopBar(onBack = onBack)
        ScreenTitle("Saved addresses", "Add the places we'll send your pros to.")

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = C.Blue) }

                state.addresses.isEmpty() -> EmptyState(
                    title = "No saved addresses yet",
                    subtitle = "Add your first address to book faster next time.",
                )

                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.addresses.forEach { addr ->
                        AddressCard(
                            address = addr,
                            deleting = state.deletingId == addr.id,
                            onClick = { onEdit(addr.id) },
                            onDelete = { viewModel.delete(addr.id) },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        PrimaryButton("Add address", onClick = onAdd)
    }
}

@Composable
private fun AddressCard(
    address: SavedLocation,
    deleting: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, C.Line, RoundedCornerShape(12.dp))
            .clickable(enabled = !deleting) { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(C.BlueSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.LocationOn, null, tint = C.Blue, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(address.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
                if (address.isDefault) {
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(C.GreenSoft)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text("DEFAULT", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = C.GreenText, letterSpacing = 0.3.sp)
                    }
                }
            }
            if (!address.address.isNullOrBlank()) {
                Text(
                    address.address!!,
                    fontSize = 11.5.sp,
                    color = C.Slate,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (deleting) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp), color = C.Red)
        } else {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).clickable { onDelete() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = C.Mute, modifier = Modifier.size(18.dp))
            }
        }
    }
}