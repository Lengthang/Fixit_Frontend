package com.fixit.app.ui.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixit.app.domain.model.UserRole
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.PrimaryButton
import com.fixit.app.ui.components.Progress
import com.fixit.app.ui.components.ScreenTitle
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.theme.C

@Composable
fun RoleScreen(
    onBack: () -> Unit,
    onContinueCustomer: () -> Unit,
    onContinueProvider: () -> Unit,
    draftVm: SignupDraftViewModel,
) {
    var selected by remember { mutableStateOf(UserRole.CUSTOMER) }

    FixItScreen {
        TopBar(step = 4, total = 11, onBack = onBack)
        Progress(step = 4, total = 11)
        ScreenTitle(
            "Pick a role",
            "Choose the role that best describes how you'll use FixIt.",
        )

        Column(Modifier.padding(horizontal = 24.dp)) {
            RoleCard(
                icon = Icons.Filled.Build,
                title = "Service Provider",
                desc = "I offer professional services.",
                accent = C.Blue,
                softBg = C.BlueSoft,
                selected = selected == UserRole.PROVIDER,
                onClick = { selected = UserRole.PROVIDER },
            )
            Spacer(Modifier.height(14.dp))
            RoleCard(
                icon = Icons.Filled.Home,
                title = "Looking for service",
                desc = "I am looking for home services.",
                accent = C.Orange,
                softBg = C.OrangeSoft,
                selected = selected == UserRole.CUSTOMER,
                onClick = { selected = UserRole.CUSTOMER },
            )
        }

        Spacer(Modifier.weight(1f))
        PrimaryButton("Continue") {
            draftVm.update { it.copy(role = selected) }
            when (selected) {
                UserRole.CUSTOMER -> onContinueCustomer()
                UserRole.PROVIDER -> onContinueProvider()
                UserRole.ADMIN    -> Unit
            }
        }
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    title: String,
    desc: String,
    accent: Color,
    softBg: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) softBg else Color.White)
            .border(2.dp, if (selected) accent else C.Line, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(accent),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
            Spacer(Modifier.height(4.dp))
            Text(desc, fontSize = 13.sp, color = C.Slate, lineHeight = 19.sp)
        }
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) accent else Color.Transparent)
                .border(2.dp, if (selected) accent else C.Line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(
                Icons.Filled.Check, null,
                tint = Color.White, modifier = Modifier.size(14.dp),
            )
        }
    }
}