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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.fixit.app.ui.util.OnLifecycleStart
import com.fixit.app.ui.util.avatarColorFor
import com.fixit.app.ui.util.formatMoney
import com.fixit.app.ui.util.initialsFor
import kotlin.time.Instant

@Composable
fun DisputeDetailScreen(
    onBack: () -> Unit,
    viewModel: DisputeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSubmitConfirm by remember { mutableStateOf(false) }

    // Photo picker — up to 5 images
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris: List<Uri> ->
        viewModel.onImagesSelected(uris)
    }
    OnLifecycleStart(viewModel::refresh)

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
        DspDetailTopBar(
            disputeId = dispute?.id,
            onBack    = onBack,
        )

        // ── Body ──────────────────────────────────────────────────────────
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading && dispute == null -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = C.Blue) }

                dispute != null -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        when (dispute.disputeStatus) {
                            DisputeStatus.PENDING_RESPONSE -> PendingBody(
                                dispute = dispute,
                                state = state,
                                onTextChange = viewModel::onResponseTextChange,
                                onAttachImages = {
                                    imagePicker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                onRemoveImage = viewModel::removeUploadedImage,
                                onViewClaimImages = { viewModel.showImages(dispute.reasonImageUrls) },
                                onViewUploadedImages = { viewModel.showImages(state.uploadedImageUrls) },
                            )

                            DisputeStatus.AWAITING_REVIEW,
                            DisputeStatus.RESOLVED -> RespondedBody(
                                dispute = dispute,
                                onViewClaimImages = { viewModel.showImages(dispute.reasonImageUrls) },
                                onViewResponseImages = { viewModel.showImages(dispute.providerResponseImageUrls) },
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.padding(horizontal = 16.dp))

        // ── Sticky bottom action bar (only when provider can respond) ──────
        dispute?.let { d ->
            if (d.disputeStatus == DisputeStatus.PENDING_RESPONSE) {
                PendingResponseActionBar(
                    isSubmitting = state.isSubmitting,
                    isUploading  = state.isUploading,
                    canSubmit    = state.canSubmit,
                    onSaveDraft  = viewModel::saveDraft,
                    onSubmit     = { showSubmitConfirm = true },
                )
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

// ──────────────────────────────────────────────────────────────────────────
// Top bar
// ──────────────────────────────────────────────────────────────────────────
@Composable
private fun DspDetailTopBar(disputeId: String?, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(C.Bg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(C.Subtle)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = C.Ink,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = disputeId
                        ?.let { "Dispute #${it.takeLast(8).uppercase()}" }
                        ?: "Dispute",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.Ink,
                    letterSpacing = (-0.2).sp,
                )
                Text(
                    "Filed by customer · Fixit team reviewing",
                    fontSize = 12.sp,
                    color = C.Slate,
                )
            }
            // Overflow indicator (placeholder for future actions menu)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(C.Subtle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = null,
                    tint = C.Slate,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        HorizontalDivider(color = C.Line)
    }
}

// ──────────────────────────────────────────────────────────────────────────
// State A — PENDING_RESPONSE body
// ──────────────────────────────────────────────────────────────────────────
@Composable
private fun PendingBody(
    dispute: Dispute,
    state: DisputeDetailState,
    onTextChange: (String) -> Unit,
    onAttachImages: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onViewClaimImages: () -> Unit,
    onViewUploadedImages: () -> Unit,
) {
    // ── Red warning banner ────────────────────────────────────────────────
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DspRedSoft)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(DspRed),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PriorityHigh,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Respond within 72 hours",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DspRedText,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Dispute filed ${formatDisputeDate(dispute.createdAt)} · ${formatRelativeAgo(dispute.createdAt)}",
                fontSize = 12.sp,
                color = DspRedDark,
                lineHeight = 17.sp,
            )
        }
    }

    // ── Booking info card ─────────────────────────────────────────────────
    DspBookingCard(dispute)

    // ── Customer's claim ──────────────────────────────────────────────────
    DspSectionLabel(
        "Customer's claim",
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
    )
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .background(C.Bg),
    ) {
        // Reason row
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(DspRed),
            )
            Text(
                dispute.reason,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = C.Ink,
                modifier = Modifier.weight(1f),
            )
        }
        // Optional description (separator + text), if reason is short and
        // the API ever returns a longer note we still surface the photos.
        if (dispute.reasonImageUrls.isNotEmpty()) {
            HorizontalDivider(color = C.Line)
            // Photo evidence
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Evidence photos",
                    fontSize = 11.sp,
                    color = C.Mute,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                EvidencePhotoRow(
                    urls = dispute.reasonImageUrls,
                    onClick = onViewClaimImages,
                )
            }
        }
    }

    // ── Your response (editable) ──────────────────────────────────────────
    DspSectionLabel(
        "Your response",
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
    )
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, C.Blue, RoundedCornerShape(14.dp))
            .background(C.Bg)
            .padding(14.dp),
    ) {
        // The placeholder + BasicTextField overlap pattern keeps the input
        // editable in-place while showing helper text when empty.
        Box(Modifier.fillMaxWidth().defaultMinSize(minHeight = 130.dp)) {
            if (state.responseText.isEmpty()) {
                Text(
                    "Describe your side of the situation in detail. " +
                            "Include any facts, dates and evidence that support your response.",
                    fontSize = 13.sp,
                    color = C.Mute,
                    lineHeight = 20.sp,
                )
            }
            androidx.compose.foundation.text.BasicTextField(
                value = state.responseText,
                onValueChange = onTextChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 13.sp,
                    color = C.Ink,
                    lineHeight = 20.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = when {
                    state.charCount == 0                   -> "Tap to write"
                    state.charCount < 10                   -> "Min 10 characters"
                    else                                    -> "Looking good"
                },
                fontSize = 11.sp,
                color = when {
                    state.charCount == 0     -> C.Blue
                    state.charCount < 10     -> DspRedText
                    else                     -> DspGreenDark
                },
                fontWeight = FontWeight.Medium,
            )
            Text("${state.charCount}/2000", fontSize = 11.sp, color = C.Mute)
        }

        // Uploaded image thumbnails
        if (state.uploadedImageUrls.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(C.Ink.copy(alpha = 0.7f))
                                .clickable { onRemoveImage(url) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(9.dp),
                            )
                        }
                    }
                }
                if (state.uploadedImageUrls.size > 4) {
                    Box(
                        modifier = Modifier
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
    }

    // ── Attach photos CTA ─────────────────────────────────────────────────
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.5.dp, C.Line, RoundedCornerShape(10.dp))
            .background(C.Bg)
            .clickable(enabled = !state.isUploading) { onAttachImages() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(C.BlueSoft),
            contentAlignment = Alignment.Center,
        ) {
            if (state.isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = C.Blue,
                )
            } else {
                Icon(
                    Icons.Filled.AddPhotoAlternate,
                    contentDescription = null,
                    tint = C.Blue,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (state.isUploading) "Uploading…"
                else if (state.uploadedImageUrls.isEmpty()) "Attach your job photos"
                else "Add more photos",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = C.Ink,
            )
            Text(
                "Before/after evidence strengthens your case",
                fontSize = 11.sp,
                color = C.Mute,
            )
        }
        Text(
            "+",
            fontSize = 20.sp,
            color = C.Blue,
            fontWeight = FontWeight.Light,
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────
// State B — AWAITING_REVIEW / RESOLVED body
// ──────────────────────────────────────────────────────────────────────────
@Composable
private fun RespondedBody(
    dispute: Dispute,
    onViewClaimImages: () -> Unit,
    onViewResponseImages: () -> Unit,
) {
    val isResolved = dispute.disputeStatus == DisputeStatus.RESOLVED

    // ── Top status banner ─────────────────────────────────────────────────
    val (bannerBg, bannerAccent, accentText, headerText, subText) = if (isResolved) {
        Quintuple(
            C.GreenSoft,
            DspGreen,
            DspGreenDark,
            "Dispute resolved",
            dispute.resolvedAt
                ?.let { "Decision issued ${formatDisputeDate(it)}" }
                ?: "Resolution available",
        )
    } else {
        Quintuple(
            C.BlueSoft,
            C.Blue,
            C.BlueDark,
            "Under review",
            dispute.providerRespondedAt?.let {
                "Response submitted ${formatShortDate(it)} · Decision pending"
            } ?: "Response submitted · Decision pending",
        )
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bannerBg)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(bannerAccent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isResolved) Icons.Filled.Check else Icons.Filled.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                headerText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accentText,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                subText,
                fontSize = 12.sp,
                color = accentText.copy(alpha = 0.85f),
                lineHeight = 17.sp,
            )
        }
    }

    // ── Case timeline ─────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .background(C.Bg)
            .padding(16.dp),
    ) {
        Text(
            "CASE TIMELINE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = C.Mute,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(bottom = 14.dp),
        )
        DspTimelineStep(
            label = "Customer filed dispute",
            date  = formatShortDate(dispute.createdAt),
            done  = true,
            isLast = false,
        )
        DspTimelineStep(
            label = "You submitted response",
            date  = dispute.providerRespondedAt?.let { formatShortDate(it) } ?: "Submitted",
            done  = true,
            isLast = false,
        )
        DspTimelineStep(
            label = "Under review by Fixit team",
            date  = if (isResolved) "Completed" else "In progress",
            done  = isResolved,
            active = !isResolved,
            isLast = false,
        )
        DspTimelineStep(
            label = "Decision & resolution",
            date  = if (isResolved)
                dispute.resolvedAt?.let { formatShortDate(it) } ?: "Resolved"
            else
                "Pending review",
            done  = isResolved,
            isLast = true,
        )
    }

    // ── Booking info card ─────────────────────────────────────────────────
    DspBookingCard(dispute)

    // ── Customer's claim (read-only) ──────────────────────────────────────
    DspSectionLabel(
        "Customer's claim",
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
    )
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .background(C.Bg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(DspRed),
            )
            Text(
                dispute.reason,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = C.Ink,
                modifier = Modifier.weight(1f),
            )
        }
        if (dispute.reasonImageUrls.isNotEmpty()) {
            HorizontalDivider(color = C.Line)
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Evidence photos",
                    fontSize = 11.sp,
                    color = C.Mute,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                EvidencePhotoRow(
                    urls = dispute.reasonImageUrls,
                    onClick = onViewClaimImages,
                )
            }
        }
    }

    // ── Your response (locked) ────────────────────────────────────────────
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DspSectionLabel("Your response")
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(C.GreenSoft)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = DspGreenDark,
                modifier = Modifier.size(11.dp),
            )
            Text(
                dispute.providerRespondedAt
                    ?.let { "Submitted ${formatShortDate(it)}" }
                    ?: "Submitted",
                fontSize = 10.sp,
                color = DspGreenDark,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
    ) {
        // Locked response text box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, C.Line, RoundedCornerShape(14.dp))
                .background(C.Subtle)
                .padding(14.dp),
        ) {
            Text(
                dispute.providerResponse ?: "—",
                fontSize = 13.sp,
                color = C.Slate,
                lineHeight = 20.sp,
            )
            if (dispute.providerResponseImageUrls.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(C.Bg)
                        .clickable { onViewResponseImages() }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = null,
                        tint = C.Slate,
                        modifier = Modifier.size(14.dp),
                    )
                    val n = dispute.providerResponseImageUrls.size
                    Text(
                        "$n ${if (n == 1) "photo" else "photos"} attached · tap to view",
                        fontSize = 11.5.sp,
                        color = C.Slate,
                    )
                }
            }
        }
        // Lock overlay pill
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xBBFFFFFF))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = C.Slate,
                modifier = Modifier.size(11.dp),
            )
            Text(
                "Locked",
                fontSize = 10.sp,
                color = C.Slate,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    // ── Resolution card (RESOLVED only) ───────────────────────────────────
    if (isResolved) {
        ResolutionCard(dispute)
    }

    // ── What happens next notice ──────────────────────────────────────────
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, C.Line, RoundedCornerShape(12.dp))
            .background(C.Subtle)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(C.BlueSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Image, // chat-bubble style decoration
                contentDescription = null,
                tint = C.Blue,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isResolved) "Need to discuss the outcome?" else "What happens next?",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = C.Ink,
            )
            Spacer(Modifier.height(4.dp))
            val moneyHint = dispute.totalAmount?.let { " (${formatMoney(it)})" } ?: ""
            Text(
                if (isResolved) {
                    "If you have questions about this resolution, our support team is here to help."
                } else {
                    "Our support team is reviewing both sides. You'll receive a push notification and " +
                            "email once a decision is made. Disputed funds$moneyHint are on hold."
                },
                fontSize = 12.sp,
                color = C.Slate,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Contact support  →",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = C.Blue,
                modifier = Modifier.clickable { /* TODO: open support deeplink */ },
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Shared body components
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun DspBookingCard(dispute: Dispute) {
    val name = dispute.customerName ?: "Customer"
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
            .background(C.Bg)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            initials = initialsFor(name),
            color    = Color(avatarColorFor(dispute.raisedBy)),
            size     = 44,
            fontSize = 14,
            photoUrl = dispute.customerPhotoUrl,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = C.Ink)
            Text(
                text = buildString {
                    append(dispute.serviceName ?: "Service")
                    dispute.scheduledAt?.let { append(" · ${formatDisputeDate(it)}") }
                },
                fontSize = 12.sp,
                color = C.Slate,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                "#${dispute.bookingId.takeLast(8).uppercase()}",
                fontSize = 11.sp,
                color = C.Mute,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Text(
            dispute.totalAmount?.let { formatMoney(it) } ?: "—",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = C.Orange,
        )
    }
}

@Composable
private fun DspSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = C.Slate,
        letterSpacing = 0.5.sp,
        modifier = modifier,
    )
}

@Composable
private fun EvidencePhotoRow(urls: List<String>, onClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val visible = urls.take(3)
        visible.forEach { url ->
            Box(
                modifier = Modifier
                    .width(88.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Subtle)
                    .border(1.dp, C.Line, RoundedCornerShape(10.dp))
                    .clickable { onClick() },
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                )
            }
        }
        if (urls.size > 3) {
            Box(
                modifier = Modifier
                    .width(88.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Subtle)
                    .border(1.dp, C.Line, RoundedCornerShape(10.dp))
                    .clickable { onClick() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "+${urls.size - 3}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.Slate,
                )
            }
        }
    }
}

@Composable
private fun DspTimelineStep(
    label: String,
    date: String,
    done: Boolean = false,
    active: Boolean = false,
    isLast: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Dot + connector line
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            done   -> C.Blue
                            active -> C.Blue
                            else   -> C.Bg
                        }
                    )
                    .border(
                        2.dp,
                        if (done || active) C.Blue else C.Line,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    done -> Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                    active -> Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                    else -> Unit
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(if (done) C.Blue else C.Line),
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(top = 2.dp)
                .padding(bottom = if (isLast) 0.dp else 16.dp),
        ) {
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = if (done || active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (done || active) C.Ink else C.Mute,
            )
            Text(
                date,
                fontSize = 11.sp,
                color = if (active) C.Blue else C.Mute,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Resolution card (RESOLVED state)
// ──────────────────────────────────────────────────────────────────────────
@Composable
private fun ResolutionCard(dispute: Dispute) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(C.GreenSoft)
            .border(1.dp, DspGreenDark.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "Resolution",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = DspGreenDark,
        )
        val resolutionLabel = when (dispute.resolution) {
            "release" -> "Full payment released to you"
            "refund"  -> "Full refund issued to customer"
            "partial" -> "Partial split — see amounts below"
            else      -> dispute.resolution ?: "Resolved"
        }
        Text(resolutionLabel, fontSize = 12.5.sp, color = Color(0xFF065F46))
        dispute.resolutionNote?.let {
            Text(
                it,
                fontSize = 12.sp,
                color = DspGreenDark.copy(alpha = 0.8f),
                lineHeight = 18.sp,
            )
        }
        if (dispute.resolution == "partial" || dispute.resolution == "release") {
            dispute.providerPayout?.let { payout ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Your payout", fontSize = 12.sp, color = Color(0xFF065F46))
                    Text(
                        formatMoney(payout),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = DspGreenDark,
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Sticky bottom bar — only shown for PENDING_RESPONSE
// ──────────────────────────────────────────────────────────────────────────
@Composable
private fun PendingResponseActionBar(
    isSubmitting: Boolean,
    isUploading: Boolean,
    canSubmit: Boolean,
    onSaveDraft: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().background(C.Bg)) {
        HorizontalDivider(color = C.Line)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Save draft
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .border(1.5.dp, C.Line, RoundedCornerShape(25.dp))
                    .background(C.Bg)
                    .clickable(enabled = !isSubmitting) { onSaveDraft() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Save draft",
                    color = C.Slate,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            // Submit response
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(if (canSubmit) DspRed else C.Mute)
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
                    Text(
                        "Submit response",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Full-screen image viewer dialog
// ──────────────────────────────────────────────────────────────────────────
@Composable
private fun ImageViewerDialog(urls: List<String>, onDismiss: () -> Unit) {
    var currentIndex by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable { onDismiss() },
        ) {
            AsyncImage(
                model = urls.getOrNull(currentIndex),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
                    .clickable { /* consume tap */ },
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (urls.size > 1) {
                Column(
                    modifier = Modifier
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
                                modifier = Modifier
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
                                modifier = Modifier
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

// ──────────────────────────────────────────────────────────────────────────
// Date helpers (also used by DisputeListScreen via internal visibility)
// ──────────────────────────────────────────────────────────────────────────

/** "Apr 19, 2026 · 1:00 PM" — long form. */
internal fun formatDisputeDate(instant: Instant): String = runCatching {
    val javaInstant = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds())
    val zdt = javaInstant.atZone(java.time.ZoneId.systemDefault())
    java.time.format.DateTimeFormatter
        .ofPattern("MMM d, yyyy · h:mm a", java.util.Locale.ENGLISH)
        .format(zdt)
}.getOrElse { "—" }

/** "Apr 19" — short form used by the timeline. */
internal fun formatShortDate(instant: Instant): String = runCatching {
    val javaInstant = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds())
    val zdt = javaInstant.atZone(java.time.ZoneId.systemDefault())
    java.time.format.DateTimeFormatter
        .ofPattern("MMM d", java.util.Locale.ENGLISH)
        .format(zdt)
}.getOrElse { "—" }

// ──────────────────────────────────────────────────────────────────────────
// Tiny tuple helper (Kotlin only ships Pair/Triple by default)
// ──────────────────────────────────────────────────────────────────────────
private data class Quintuple<A, B, C, D, E>(
    val first: A, val second: B, val third: C, val fourth: D, val fifth: E,
)