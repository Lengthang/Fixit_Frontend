package com.fixit.app.ui.customer.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.ui.components.Avatar
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.FixItTextField
import com.fixit.app.ui.components.PrimaryButton
import com.fixit.app.ui.components.ScreenTitle
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.OnLifecycleStart
import com.fixit.app.ui.util.initialsFor

@Composable
fun CustomerEditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CustomerEditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    OnLifecycleStart(viewModel::load)

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let(viewModel::onPhotoPicked) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CustomerEditProfileEffect.SavedOk -> onSaved()
                is CustomerEditProfileEffect.ShowError ->
                    snackbar.showSnackbar(effect.message)
            }
        }
    }

    FixItScreen(bg = C.Subtle) {
        TopBar(onBack = onBack)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                ScreenTitle(
                    title = "Personal info",
                    sub = "Update your name, email and photo.",
                )

                // ── Avatar with edit affordance ────────────────────────────
                Box(
                    Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        val displayName = state.name.takeIf { it.isNotBlank() } ?: "Customer"
                        Avatar(
                            initials = initialsFor(displayName),
                            color = C.Blue,
                            size = 96,
                            fontSize = 32,
                            photoUrl = state.profilePhotoUrl,
                        )
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(C.Blue)
                                .clickable(enabled = !state.isUploadingPhoto) {
                                    photoPicker.launch("image/*")
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (state.isUploadingPhoto) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp),
                                )
                            } else {
                                Icon(
                                    Icons.Filled.PhotoCamera,
                                    contentDescription = "Change photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                FixItTextField(
                    label = "Full name",
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    placeholder = "Your name",
                )
                FixItTextField(
                    label = "Email",
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    placeholder = "you@example.com",
                    keyboardType = KeyboardType.Email,
                )

                // Phone is the login identity — shown but not editable here.
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp),
                ) {
                    Text("Phone", fontSize = 12.sp, color = C.Slate, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                            .background(C.Line.copy(alpha = 0.35f))
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                    ) {
                        Text(
                            state.phone.ifBlank { "—" },
                            fontSize = 15.sp,
                            color = C.Slate,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Your phone number is used to sign in and can't be changed here.",
                        fontSize = 11.sp,
                        color = C.Mute,
                    )
                }

                if (state.errorMessage != null) {
                    Text(
                        state.errorMessage!!,
                        color = C.Red,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))
                Box(Modifier.padding(horizontal = 24.dp)) {
                    PrimaryButton(
                        text = if (state.isSaving) "Saving…" else "Save changes",
                        enabled = state.canSave,
                        onClick = viewModel::save,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = C.Blue)
                }
            }
        }
        SnackbarHost(snackbar)
    }
}