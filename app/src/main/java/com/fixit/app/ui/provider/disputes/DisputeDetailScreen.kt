package com.fixit.app.ui.provider.disputes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fixit.app.domain.model.Dispute
import com.fixit.app.domain.model.DisputeStatus
import com.fixit.app.ui.components.Avatar
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.avatarColorFor
import com.fixit.app.ui.util.initialsFor

// ── Colour constants (scoped to this file) ────────────────────────────────
private val RedSoft    = Color(0xFFFEE2E2)
private val RedText    = Color(0xFFB91C1C)
private val RedDark    = Color(0xFF991B1B)
private val RedFill    = Color(0xFFFEF2F2)
private val RedBorder  = Color(0xFFEF4444)
private val ReviewSoft = Color(0xFFFFF1E8)
private val ReviewText = Color(0xFFB8430B)
private val ReviewDark = Color(0xFF7C2D12)

@Composable
fun DisputeDetailScreen(
    onBack: () -> Unit,
    viewModel: DisputeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSubmitConfirm by remember { mutableStateOf(false) }

    // Photo picker — up to 5 images at once
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris: List<Uri> ->
        viewModel.onImagesSelected(uris)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DisputeDetailEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                DisputeDetailEffect.Submitted       -> onBack()
            }
        }
    }

    FixItScreen(bg = C.Subtle) {
        val dispute = state.dispute

        // ── Top bar ───────────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(C.Subtle)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = C.Ink, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = dispute?.let { "Dispute #${it.id.takeLast(8).uppercase()}" } ?: "Dispute",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    color = C.Ink,
                )
                dispute?.scheduledAt?.let {
                    Text(
                        formatDisputeDate(it),
                        fontSize = 11.5.sp,
                        color = C.Slate,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
            dispute?.let { DisputeStatusBadge(it.disputeStatus, large = true) }
        }

        // ── Body ──────────────────────────────────────────────────────────
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading && dispute == null -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = C.Blue) }

                dispute != null -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        // Section 1 — Service information
                        SectionHeader(n = "1", title = "Service information")
                        ServiceInfoCard(dispute)

                        // Section 2 — Customer's dispute
                        SectionHeader(n = "2", title = "Customer's dispute")
                        CustomerClaimCard(
                            dispute = dispute,
                            onViewImages = { viewModel.showImages(dispute.reasonImageUrls) },
                        )

                        // Section 3 — Provider response
                        SectionHeader(n = "3", title = "Your response")
                        when (dispute.disputeStatus) {
                            DisputeStatus.PENDING_RESPONSE -> PendingResponseForm(
                                state = state,
                                onTextChange = viewModel::onResponseTextChange,
                                onAttachImages = {
                                    imagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onRemoveImage = viewModel::removeUploadedImage,
                                onViewUploadedImages = { viewModel.showImages(state.uploadedImageUrls) },
                            )

                            DisputeStatus.AWAITING_REVIEW, DisputeStatus.RESOLVED -> SubmittedResponseCard(
                                dispute = dispute,
                                onViewImages = { viewModel.showImages(dispute.providerResponseImageUrls) },
                            )
                        }

                        // "Response received" banner (awaiting review only)
                        if (dispute.disputeStatus == DisputeStatus.AWAITING_REVIEW) {
                            ResponseReceivedBanner()
                        }

                        // Resolution card (resolved only)
                        if (dispute.disputeStatus == DisputeStatus.RESOLVED) {
                            ResolutionCard(dispute)
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.padding(horizontal = 16.dp))

        // ── Bottom action bar ─────────────────────────────────────────────
        dispute?.let { d ->
            when (d.disputeStatus) {
                DisputeStatus.PENDING_RESPONSE -> PendingResponseActionBar(
                    isSubmitting = state.isSubmitting,
                    isUploading  = state.isUploading,
                    canSubmit    = state.canSubmit,
                    onSaveDraft  = viewModel::saveDraft,
                    onSubmit     = { showSubmitConfirm = true },
                )
                DisputeStatus.AWAITING_REVIEW -> AwaitingReviewActionBar()
                DisputeStatus.RESOLVED        -> { /* no action bar for resolved */ }
            }
        }
    }

    // ── Submit confirmation dialog ─────────────────────────────────────────
    if (showSubmitConfirm) {
        AlertDialog(
            onDismissRequest = { showSubmitConfirm = false },
            title = { Text("Submit your response?") },
            text = {
                Text(
                    "You can only respond once. Make sure your response is complete and accurate — " +
                            "our support team will review both sides before making a decision.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSubmitConfirm = false
                    viewModel.submitResponse()
                }) { Text("Submit", color = C.Blue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirm = false }) { Text("Review again") }
            },
        )
    }

    // ── Image viewer dialog ────────────────────────────────────────────────
    state.viewingImages?.let { urls ->
        ImageViewerDialog(
            urls = urls,
            onDismiss = viewModel::dismissImageViewer,
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Shared section header
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(n: String, title: String) {
    Row(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(20.dp).clip(CircleShape).background(C.Blue),
            contentAlignment = Alignment.Center,
        ) {
            Text(n, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Ink, letterSpacing = (-0.1).sp)
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Section 1 — Service information
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun ServiceInfoCard(dispute: Dispute) {
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
    ) {
        InfoRow(label = "Service",      value = dispute.serviceName ?: "—")
        InfoRow(label = "Date & time",  value = dispute.scheduledAt?.let { formatDisputeDate(it) } ?: "—")
        InfoRow(label = "Customer",     value = dispute.customerName ?: "—")
        InfoRow(label = "Booking ref",  value = "#${dispute.bookingId.takeLast(8).uppercase()}", last = true)
    }
}

@Composable
private fun InfoRow(label: String, value: String, last: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .then(if (!last) Modifier.border(
                width = 1.dp,
                color = C.Line,
                shape = RoundedCornerShape(0.dp),
            ) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 12.sp, color = C.Slate)
        Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
    }
    if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
}

// ────────────────────────────────────────────────────────────────────────────
// Section 2 — Customer's claim
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun CustomerClaimCard(dispute: Dispute, onViewImages: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        // Customer header row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val name = dispute.customerName ?: "Customer"
            Avatar(
                initials = initialsFor(name),
                color    = Color(avatarColorFor(dispute.raisedBy)),
                size     = 32,
                fontSize = 11,
                photoUrl = dispute.customerPhotoUrl,
            )
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = C.Ink)
                Text(
                    "Raised ${formatDisputeDate(dispute.createdAt)}",
                    fontSize = 10.5.sp,
                    color = C.Mute,
                )
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(RedSoft)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    "CUSTOMER'S CLAIM",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedText,
                    letterSpacing = 0.4.sp,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Reason block
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(RedFill)
                .border(
                    width = 3.dp,
                    color = RedBorder.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 10.dp, bottomEnd = 10.dp),
                )
                .padding(10.dp, 10.dp, 10.dp, 10.dp),
        ) {
            // Left-border effect via inner padding
            Row {
                Box(
                    Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(RedText, RoundedCornerShape(2.dp))
                )
                Column(Modifier.padding(start = 10.dp)) {
                    Text(
                        "Reason: ${dispute.reason}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RedDark,
                    )
                }
            }
        }

        // Simpler approach for the red left-border block:
        // (The above Box-within-Row is a bit complex; let me simplify it inline)
        Spacer(Modifier.height(10.dp))

        // Photos count pill (tappable)
        val imgCount = dispute.reasonImageUrls.size
        if (imgCount > 0) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(C.Subtle)
                    .clickable { onViewImages() }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Image, null, tint = C.Slate, modifier = Modifier.size(14.dp))
                Text(
                    "$imgCount ${if (imgCount == 1) "photo" else "photos"} attached · tap to view",
                    fontSize = 11.5.sp,
                    color = C.Slate,
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Section 3a — Pending response form (editable)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun PendingResponseForm(
    state: DisputeDetailState,
    onTextChange: (String) -> Unit,
    onAttachImages: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onViewUploadedImages: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        // Warning banner
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ReviewSoft)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Filled.Info,
                null,
                tint = ReviewText,
                modifier = Modifier.size(14.dp).padding(top = 1.dp),
            )
            Text(
                text = "You can only respond once. Be thorough — admin will review both sides before deciding.",
                fontSize = 11.sp,
                color = ReviewDark,
                lineHeight = 16.sp,
            )
        }

        Spacer(Modifier.height(10.dp))

        // Text input with blue focus ring
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.5.dp, C.Blue, RoundedCornerShape(12.dp))
                .padding(14.dp),
        ) {
            if (state.responseText.isEmpty()) {
                Text(
                    "Describe your side of the situation in detail…",
                    fontSize = 12.5.sp,
                    color = C.Mute,
                    lineHeight = 19.sp,
                )
            }
            androidx.compose.foundation.text.BasicTextField(
                value = state.responseText,
                onValueChange = onTextChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 12.5.sp,
                    color = C.Ink,
                    lineHeight = 19.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                ),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 130.dp),
            )
        }

        // Char count row
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Min 10 characters",
                fontSize = 11.sp,
                color = if (state.charCount < 10 && state.charCount > 0) RedText else C.Mute,
            )
            Text("${state.charCount}/2000", fontSize = 11.sp, color = C.Mute)
        }

        Spacer(Modifier.height(10.dp))

        // Uploaded image thumbnails
        if (state.uploadedImageUrls.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.uploadedImageUrls.take(4).forEach { url ->
                    Box(Modifier.size(60.dp)) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onViewUploadedImages() },
                        )
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(C.Ink.copy(alpha = 0.7f))
                                .clickable { onRemoveImage(url) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(9.dp))
                        }
                    }
                }
                if (state.uploadedImageUrls.size > 4) {
                    Box(
                        Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(C.Subtle)
                            .border(1.dp, C.Line, RoundedCornerShape(8.dp))
                            .clickable { onViewUploadedImages() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "+${state.uploadedImageUrls.size - 4}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = C.Slate,
                        )
                    }
                }
            }
        }

        // Attach photos button
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, if (state.isUploading) C.Blue else C.Line, RoundedCornerShape(20.dp))
                .clickable(enabled = !state.isUploading) { onAttachImages() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = C.Blue,
                )
                Spacer(Modifier.width(8.dp))
                Text("Uploading…", fontSize = 12.5.sp, color = C.Blue, fontWeight = FontWeight.Medium)
            } else {
                Icon(Icons.Filled.AddPhotoAlternate, null, tint = C.Slate, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (state.uploadedImageUrls.isEmpty()) "Attach photos or files (optional)"
                    else "Add more photos",
                    fontSize = 12.5.sp,
                    color = C.Slate,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Section 3b — Submitted response card (read-only / locked)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun SubmittedResponseCard(dispute: Dispute, onViewImages: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        // Provider header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(C.Orange),
                contentAlignment = Alignment.Center,
            ) {
                Text("You", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text("Your response", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = C.Ink)
                Text(
                    dispute.providerRespondedAt?.let { "Submitted ${formatDisputeDate(it)}" } ?: "Submitted",
                    fontSize = 10.5.sp,
                    color = C.Mute,
                )
            }
            // "Submitted" green chip
            Row(
                Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(C.GreenSoft)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(Icons.Filled.Check, null, tint = Color(0xFF047857), modifier = Modifier.size(9.dp))
                Text(
                    "SUBMITTED",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF047857),
                    letterSpacing = 0.4.sp,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Response text block (blue left border)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(C.BlueSoft)
                .padding(start = 3.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                    .background(C.BlueSoft)
                    .padding(10.dp),
            ) {
                Text(
                    dispute.providerResponse ?: "—",
                    fontSize = 12.5.sp,
                    color = C.Ink,
                    lineHeight = 19.sp,
                )
            }
        }

        // Photos pill (tappable)
        val imgCount = dispute.providerResponseImageUrls.size
        if (imgCount > 0) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(C.Subtle)
                    .clickable { onViewImages() }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Image, null, tint = C.Slate, modifier = Modifier.size(14.dp))
                Text(
                    "$imgCount ${if (imgCount == 1) "photo" else "photos"} attached · tap to view",
                    fontSize = 11.5.sp,
                    color = C.Slate,
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// "Response received" confirmation banner (Awaiting Review only)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResponseReceivedBanner() {
    Row(
        Modifier
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(C.BlueSoft)
            .border(1.dp, C.Blue.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(C.Blue),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                "Response received",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = C.BlueDark,
            )
            Text(
                "Your response has been submitted and is currently under review by our support team. " +
                        "We will reach out to you directly should any further clarification be required.",
                fontSize = 12.sp,
                color = C.BlueDark.copy(alpha = 0.85f),
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Resolution card (Resolved only)
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResolutionCard(dispute: Dispute) {
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(C.GreenSoft)
            .border(1.dp, Color(0xFF047857).copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Resolution", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
        val resolutionLabel = when (dispute.resolution) {
            "release" -> "Full payment released to you"
            "refund"  -> "Full refund issued to customer"
            "partial" -> "Partial split — see amounts below"
            else      -> dispute.resolution ?: "Resolved"
        }
        Text(resolutionLabel, fontSize = 12.5.sp, color = Color(0xFF065F46))
        dispute.resolutionNote?.let {
            Text(it, fontSize = 12.sp, color = Color(0xFF047857).copy(alpha = 0.8f), lineHeight = 18.sp)
        }
        if (dispute.resolution == "partial" || dispute.resolution == "release") {
            dispute.providerPayout?.let { payout ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Your payout", fontSize = 12.sp, color = Color(0xFF065F46))
                    Text(
                        "SGD ${payout.toPlainString()}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857),
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Action bars
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun PendingResponseActionBar(
    isSubmitting: Boolean,
    isUploading: Boolean,
    canSubmit: Boolean,
    onSaveDraft: () -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(width = 1.dp, color = C.Line, shape = androidx.compose.ui.graphics.RectangleShape)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Save draft
        Box(
            Modifier
                .weight(1f)
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(Color.White)
                .border(1.5.dp, C.Line, RoundedCornerShape(25.dp))
                .clickable(enabled = !isSubmitting) { onSaveDraft() },
            contentAlignment = Alignment.Center,
        ) {
            Text("Save draft", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.Slate)
        }

        // Submit response
        Box(
            Modifier
                .weight(1.6f)
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(if (canSubmit) C.Blue else C.Mute)
                .clickable(enabled = canSubmit && !isSubmitting && !isUploading) { onSubmit() },
            contentAlignment = Alignment.Center,
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.5.dp,
                    color = Color.White,
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Submit response",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AwaitingReviewActionBar() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(width = 1.dp, color = C.Line, shape = androidx.compose.ui.graphics.RectangleShape)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(Color.White)
                .border(1.5.dp, C.Line, RoundedCornerShape(25.dp))
                .clickable { /* TODO: open support channel / email deeplink */ },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.HelpOutline, null, tint = C.Ink, modifier = Modifier.size(15.dp))
                Text("Contact support", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Full-screen image viewer dialog
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun ImageViewerDialog(urls: List<String>, onDismiss: () -> Unit) {
    var currentIndex by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable { onDismiss() },
        ) {
            // Current image
            AsyncImage(
                model = urls.getOrNull(currentIndex),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
                    .clickable { /* consume tap so it doesn't dismiss */ },
            )

            // Close button
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }

            // Counter + prev/next (only when multiple images)
            if (urls.size > 1) {
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "${currentIndex + 1} / ${urls.size}",
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (currentIndex > 0) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable { currentIndex-- },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("‹", fontSize = 22.sp, color = Color.White)
                            }
                        }
                        if (currentIndex < urls.size - 1) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable { currentIndex++ },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("›", fontSize = 22.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Shared date helper (also used in DisputeListScreen) ───────────────────

internal fun formatDisputeDate(instant: kotlin.time.Instant): String = runCatching {
    val javaInstant = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds())
    val zdt = javaInstant.atZone(java.time.ZoneId.systemDefault())
    java.time.format.DateTimeFormatter
        .ofPattern("MMM d, yyyy · h:mm a", java.util.Locale.ENGLISH)
        .format(zdt)
}.getOrElse { "—" }