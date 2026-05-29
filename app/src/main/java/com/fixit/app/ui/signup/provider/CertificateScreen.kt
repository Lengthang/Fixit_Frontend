package com.fixit.app.ui.signup.provider

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.PrimaryButton
import com.fixit.app.ui.components.Progress
import com.fixit.app.ui.components.ScreenTitle
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.signup.SignupDraftViewModel
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.normalizeMediaUrl

@Composable
fun CertificateScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    draftVm: SignupDraftViewModel,
    vm: CertificateViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    val certificatePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? -> uri?.let { vm.onCertificatePicked(it) } }

    val nationalIdPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? -> uri?.let { vm.onNationalIdPicked(it) } }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it); vm.dismissError() }
    }

    // Both Continue and Skip advance with whatever has been uploaded so far
    // (each field is optional and defaults to null).
    val persist = {
        draftVm.update {
            it.copy(
                certificationUrl = state.certificateUrl,
                nationalIdUrl = state.nationalIdUrl,
            )
        }
    }

    FixItScreen {
        TopBar(step = 9, total = 11, onBack = onBack)
        Progress(step = 9, total = 11)
        ScreenTitle(
            "Upload a certificate",
            "Verified providers get a badge and 3× more bookings. You can skip and add later.",
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            // ── Certificate upload (optional) ──────────────────────────────
            UploadCard(
                icon = Icons.Filled.FileUpload,
                title = "Upload your certificate",
                subtitle = "PDF, JPG, or PNG\nMax 10 MB",
                uploadedUrl = state.certificateUrl,
                isUploading = state.isUploadingCertificate,
                onChoose = { certificatePicker.launch("image/*") },
                onRemove = { vm.onRemoveCertificate() },
            )

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .border(1.dp, C.Line, RoundedCornerShape(12.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(C.OrangeSoft),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.EmojiEvents, null, tint = C.Orange, modifier = Modifier.size(18.dp)) }
                Column(Modifier.weight(1f)) {
                    Text("Get verified faster", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
                    Text("Licenses, trade certs, insurance all count.", fontSize = 11.5.sp, color = C.Slate)
                }
            }

            // ── National ID upload (optional, new) ─────────────────────────
            Spacer(Modifier.height(24.dp))
            Text("National ID", fontSize = 12.sp, color = C.Slate, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            UploadCard(
                icon = Icons.Filled.Badge,
                title = "Upload your National ID",
                subtitle = "JPG or PNG\nMax 10 MB",
                uploadedUrl = state.nationalIdUrl,
                isUploading = state.isUploadingNationalId,
                onChoose = { nationalIdPicker.launch("image/*") },
                onRemove = { vm.onRemoveNationalId() },
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(C.BlueSoft).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.VerifiedUser, null, tint = C.Blue, modifier = Modifier.size(18.dp)) }
                Text(
                    "Uploading your National ID will get your account verified",
                    fontSize = 12.5.sp, color = C.BlueDark, lineHeight = 18.sp,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        SnackbarHost(snackbar, modifier = Modifier.padding(horizontal = 16.dp))
        PrimaryButton(
            "Continue",
            enabled = !state.isUploadingCertificate && !state.isUploadingNationalId,
            onClick = { persist(); onContinue() },
        )
        Text(
            "Skip for now",
            fontSize = 14.sp, color = C.Slate, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 16.dp)
                .clickable { persist(); onSkip() },
        )
    }
}

@Composable
private fun UploadCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    uploadedUrl: String?,
    isUploading: Boolean,
    onChoose: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)).background(C.Subtle)
            .border(2.dp, C.Line, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            isUploading -> {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(C.BlueSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        color = C.Blue,
                        strokeWidth = 2.dp,
                    )
                }
                Text("Uploading…", fontSize = 13.sp, color = C.Slate)
            }

            !uploadedUrl.isNullOrBlank() -> {
                Box(Modifier.size(96.dp)) {
                    AsyncImage(
                        model = normalizeMediaUrl(uploadedUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(96.dp).clip(RoundedCornerShape(14.dp)),
                    )
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp)
                            .clip(CircleShape).background(Color(0xB3000000))
                            .clickable { onRemove() },
                        contentAlignment = Alignment.Center,
                    ) { Text("✕", color = Color.White, fontSize = 9.sp) }
                }
                Text("Uploaded", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.GreenText)
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White)
                        .border(1.5.dp, C.Blue, RoundedCornerShape(20.dp))
                        .clickable { onChoose() }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) { Text("Replace file", color = C.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }

            else -> {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(C.BlueSoft),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = C.Blue, modifier = Modifier.size(26.dp)) }
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
                Text(subtitle, fontSize = 12.sp, color = C.Slate,
                    textAlign = TextAlign.Center, lineHeight = 18.sp)
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White)
                        .border(1.5.dp, C.Blue, RoundedCornerShape(20.dp))
                        .clickable { onChoose() }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) { Text("Choose file", color = C.Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}