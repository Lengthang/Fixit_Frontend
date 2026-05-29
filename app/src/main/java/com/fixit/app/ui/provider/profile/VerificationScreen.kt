package com.fixit.app.ui.provider.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.IconBox
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.OnLifecycleStart
import com.fixit.app.ui.util.normalizeMediaUrl

/* ─────────────────────────────────────────────────────────────────────────
 * Verification screen — reachable from ProviderProfileScreen's "Verification"
 * row. Sole writer of the provider's certification / certification_url /
 * national_id_url fields (moved here out of EditProfileScreen).
 *
 * Reused (no duplication, all imports):
 *   • FixItScreen  — screen wrapper (status/nav bar padding handled)
 *   • TopBar       — back arrow + slot row; title overlaid via Box (same
 *                    technique EditProfileScreen uses)
 *   • IconBox      — rounded tile for icon chrome
 *   • C            — theme colors
 *   • normalizeMediaUrl / OnLifecycleStart — shared utils
 *
 * Backend constraints (confirmed against Service_Provider_Backend):
 *   • One National ID  → national_id_url   (single image, replace/remove)
 *   • One Certificate  → certification_url + certification (custom name)
 *   • Image-only uploads (JPG/PNG/GIF/WEBP); POST /uploads/image rejects PDFs.
 *   • Per-document immediate save: every upload/remove/rename PATCHes
 *     /providers/me right away (no bottom Save button).
 *   • is_verified is derived server-side: approved AND ≥1 uploaded document.
 * ──────────────────────────────────────────────────────────────────────── */
@Composable
fun VerificationScreen(
    onBack: () -> Unit,
    viewModel: VerificationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    OnLifecycleStart(viewModel::refresh)

    // Single picker contract — the VM remembers which slot is being filled.
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? -> uri?.let { viewModel.onImagePicked(it) } }

    fun pickFor(target: VerificationTarget) {
        viewModel.setTarget(target)
        imagePicker.launch("image/*")
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    FixItScreen(bg = C.Subtle) {
        // ── Top bar: reuse shared TopBar, overlay a centered title ──────────
        Box(modifier = Modifier.fillMaxWidth().background(Color.White)) {
            TopBar(onBack = onBack)
            Text(
                text          = "Verification",
                fontSize      = 17.sp,
                fontWeight    = FontWeight.Bold,
                color         = C.Ink,
                letterSpacing = (-0.2).sp,
                modifier      = Modifier.align(Alignment.Center),
            )
        }
        HorizontalDivider(color = C.Line)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (state.isLoading && !state.hasLoaded) {
                CircularProgressIndicator(
                    color    = C.Blue,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    // ── Status banner ────────────────────────────────────
                    StatusBanner(
                        status     = state.status,
                        isVerified = state.isVerified,
                    )

                    // ── NATIONAL ID ──────────────────────────────────────
                    VeriSection(
                        title = "National ID",
                        hint  = "Required for verification",
                    ) {
                        Text(
                            "Upload a clear photo of your government-issued ID. " +
                                    "This is used to verify your identity.",
                            fontSize   = 12.5.sp,
                            color      = C.Slate,
                            lineHeight = 18.sp,
                            modifier   = Modifier.padding(bottom = 12.dp),
                        )
                        DocumentRow(
                            title       = "National ID",
                            emptyIcon   = Icons.Filled.Badge,
                            url         = state.nationalIdUrl,
                            uploading   = state.uploadingNationalId,
                            onChoose    = { pickFor(VerificationTarget.NATIONAL_ID) },
                            onRemove    = viewModel::removeNationalId,
                            displayName = "National ID",
                        )
                    }

                    // ── CERTIFICATE ──────────────────────────────────────
                    VeriSection(
                        title = "Certificate",
                        hint  = "Verified pros book 3× more",
                    ) {
                        Text(
                            "Add a license, trade certificate, or insurance document, " +
                                    "then give it a name so customers know what it is.",
                            fontSize   = 12.5.sp,
                            color      = C.Slate,
                            lineHeight = 18.sp,
                            modifier   = Modifier.padding(bottom = 12.dp),
                        )

                        // Custom name input — persisted on focus-loss. The VM
                        // no-ops if there's no uploaded file yet, and defaults
                        // a blank name to "Certificate".
                        VeriNameInput(
                            label         = "Certificate name",
                            value         = state.certificationName,
                            onValueChange = viewModel::onCertNameChange,
                            onCommit      = viewModel::commitCertName,
                            placeholder   = "e.g. CA Plumbing License C-36",
                            enabled       = state.certificationUrl != null,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DocumentRow(
                            title       = "Certificate",
                            emptyIcon   = Icons.Filled.WorkspacePremium,
                            url         = state.certificationUrl,
                            uploading   = state.uploadingCert,
                            onChoose    = { pickFor(VerificationTarget.CERTIFICATE) },
                            onRemove    = viewModel::removeCertificate,
                            displayName = state.certificationName.ifBlank { "Certificate" },
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            SnackbarHost(
                hostState = snackbar,
                modifier  = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp),
            )
        }
    }
}

// ── Status banner — reflects backend status + derived verified flag ─────────
@Composable
private fun StatusBanner(status: String, isVerified: Boolean) {
    data class Look(
        val icon: ImageVector,
        val bg: Color,
        val fg: Color,
        val iconTint: Color,
        val title: String,
        val subtitle: String,
    )

    val look = when {
        isVerified -> Look(
            icon     = Icons.Filled.VerifiedUser,
            bg       = C.GreenSoft,
            fg       = C.GreenText,
            iconTint = C.Green,
            title    = "Verified",
            subtitle = "Your account is verified. You're all set.",
        )
        status == "rejected" -> Look(
            icon     = Icons.Filled.HourglassEmpty,
            bg       = Color(0xFFFEE2E2),
            fg       = C.Red,
            iconTint = C.Red,
            title    = "Verification rejected",
            subtitle = "Re-upload clear, valid documents to try again.",
        )
        status == "approved" -> Look(
            icon     = Icons.Filled.CheckCircle,
            bg       = C.BlueSoft,
            fg       = C.BlueDark,
            iconTint = C.Blue,
            title    = "Approved",
            subtitle = "Upload a document to earn your verified badge.",
        )
        else -> Look(
            icon     = Icons.Filled.HourglassEmpty,
            bg       = C.OrangeSoft,
            fg       = C.OrangeText,
            iconTint = C.Orange,
            title    = "Pending review",
            subtitle = "Upload your documents below to get verified.",
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(look.bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(look.icon, null, tint = look.iconTint, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(look.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = look.fg)
            Text(
                look.subtitle,
                fontSize   = 12.sp,
                color      = C.Slate,
                lineHeight = 17.sp,
                modifier   = Modifier.padding(top = 2.dp),
            )
        }
    }
}

// ── Section wrapper — same chrome as EditProfileScreen's EpSection ──────────
@Composable
private fun VeriSection(
    title: String,
    hint: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    HorizontalDivider(color = C.Line)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(top = 14.dp, start = 20.dp, end = 20.dp, bottom = 18.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Bottom,
        ) {
            Text(
                title.uppercase(),
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                color         = C.Slate,
                letterSpacing = 0.8.sp,
            )
            if (hint != null) Text(hint, fontSize = 11.sp, color = C.Mute)
        }
        content()
    }
    HorizontalDivider(color = C.Line)
    Spacer(modifier = Modifier.height(10.dp))
}

// ── Name input — label + bordered field, commits on focus-loss ─────────────
@Composable
private fun VeriNameInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    placeholder: String,
    enabled: Boolean,
) {
    var focused by remember { mutableStateOf(false) }

    Column {
        Text(
            label,
            fontSize   = 12.sp,
            color      = when {
                !enabled -> C.Mute
                focused  -> C.Blue
                else     -> C.Slate
            },
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.padding(bottom = 6.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.5.dp,
                    if (focused) C.Blue else C.Line,
                    RoundedCornerShape(10.dp),
                )
                .background(if (enabled) Color.White else C.Subtle, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value         = value,
                onValueChange = onValueChange,
                enabled       = enabled,
                singleLine    = true,
                textStyle     = TextStyle(color = C.Ink, fontSize = 15.sp, lineHeight = 22.sp),
                cursorBrush   = SolidColor(C.Blue),
                modifier      = Modifier
                    .weight(1f)
                    .onFocusChanged {
                        // Commit when focus leaves the field after having held it.
                        if (focused && !it.isFocused) onCommit()
                        focused = it.isFocused
                    },
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            if (enabled) placeholder else "Upload a certificate first",
                            color    = C.Mute,
                            fontSize = 15.sp,
                        )
                    }
                    inner()
                },
            )
        }
    }
}

// ── Document row — empty upload card OR filled preview/status/replace/remove ─
//
// Same visual language as EditProfileScreen's EpFileRow (which this replaces
// for cert/ID), kept file-local so the screen is self-contained.
@Composable
private fun DocumentRow(
    title: String,
    emptyIcon: ImageVector,
    url: String?,
    uploading: Boolean,
    onChoose: () -> Unit,
    onRemove: () -> Unit,
    displayName: String,
) {
    if (url == null) {
        // ── Empty state — tappable upload card ──────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, C.Line, RoundedCornerShape(12.dp))
                .background(C.Subtle)
                .clickable(enabled = !uploading) { onChoose() }
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBox(bg = C.BlueSoft) {
                if (uploading) {
                    CircularProgressIndicator(
                        color       = C.Blue,
                        strokeWidth = 2.dp,
                        modifier    = Modifier.size(18.dp),
                    )
                } else {
                    Icon(emptyIcon, null, tint = C.Blue, modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Upload $title",
                    fontSize   = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = C.Ink,
                )
                Text(
                    "JPG, PNG, GIF or WEBP · Max 10 MB",
                    fontSize = 11.5.sp,
                    color    = C.Slate,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(C.Blue)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text("Choose", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    } else {
        // ── Filled state — preview + name + status + Replace/Remove ──────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, C.Line, RoundedCornerShape(12.dp))
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model              = normalizeMediaUrl(url),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .width(48.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(C.Subtle),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        displayName,
                        fontSize   = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = C.Ink,
                        modifier   = Modifier.weight(1f, fill = false),
                    )
                    // Green check status indicator.
                    Box(
                        modifier = Modifier.size(14.dp).clip(CircleShape).background(C.Green),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
                            val s = this.size.width / 24f
                            val p = Path().apply {
                                moveTo(5f * s, 12f * s); lineTo(10f * s, 17f * s); lineTo(20f * s, 7f * s)
                            }
                            drawPath(
                                p, Color.White,
                                style = Stroke(3.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                        }
                    }
                }
                Text(
                    "Uploaded",
                    fontSize = 11.5.sp,
                    color    = C.GreenText,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            // Replace
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, C.Blue, RoundedCornerShape(16.dp))
                    .clickable(enabled = !uploading) { onChoose() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                if (uploading) {
                    CircularProgressIndicator(
                        color       = C.Blue,
                        strokeWidth = 2.dp,
                        modifier    = Modifier.size(12.dp),
                    )
                } else {
                    Text("Replace", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = C.Blue)
                }
            }
            // Remove
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(C.RedSoft)
                    .clickable(enabled = !uploading) { onRemove() },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
                    val s      = this.size.width / 24f
                    val stroke = Stroke(2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    drawLine(C.Red, androidx.compose.ui.geometry.Offset(3f * s, 6f * s), androidx.compose.ui.geometry.Offset(21f * s, 6f * s), 2.2f * s, StrokeCap.Round)
                    val body = Path().apply {
                        moveTo(5f * s, 6f * s);   lineTo(7f * s, 20f * s)
                        lineTo(17f * s, 20f * s); lineTo(19f * s, 6f * s)
                    }
                    drawPath(body, C.Red, style = stroke)
                    drawLine(C.Red, androidx.compose.ui.geometry.Offset(10f * s, 11f * s), androidx.compose.ui.geometry.Offset(10f * s, 17f * s), 2.2f * s, StrokeCap.Round)
                    drawLine(C.Red, androidx.compose.ui.geometry.Offset(14f * s, 11f * s), androidx.compose.ui.geometry.Offset(14f * s, 17f * s), 2.2f * s, StrokeCap.Round)
                }
            }
        }
    }
}