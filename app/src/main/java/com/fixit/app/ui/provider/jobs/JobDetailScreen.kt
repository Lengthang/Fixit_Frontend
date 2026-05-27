package com.fixit.app.ui.provider.jobs

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fixit.app.domain.model.Booking
import com.fixit.app.domain.model.BookingPayout
import com.fixit.app.domain.model.BookingPhoto
import com.fixit.app.domain.model.BookingStatus
import com.fixit.app.domain.model.EscrowStatus
import com.fixit.app.domain.model.PhotoKind
import com.fixit.app.ui.components.Avatar
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.IconBox
import com.fixit.app.ui.components.InfoPill
import com.fixit.app.ui.components.StatusBadge
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.OnLifecycleStart
import com.fixit.app.ui.util.avatarColorFor
import com.fixit.app.ui.util.formatMoney
import com.fixit.app.ui.util.formatScheduled
import com.fixit.app.ui.util.initialsFor
import java.math.BigDecimal

@Composable
fun JobDetailScreen(
    onBack: () -> Unit,
    onActionCompleted: () -> Unit,
    viewModel: JobDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingCancelConfirm by remember { mutableStateOf(false) }
    OnLifecycleStart(viewModel::refresh)

    // One picker contract for both BEFORE and AFTER. `pendingKind` is the
    // slot that initiated the pick — mirrors the UploadTarget pattern
    // already used by EditProfileScreen.
    var pendingKind by remember { mutableStateOf<PhotoKind?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        val kind = pendingKind
        pendingKind = null
        if (uri != null && kind != null) {
            viewModel.onPhotoPicked(uri, kind)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is JobDetailEffect.StatusUpdated -> snackbarHostState.showSnackbar(effect.message)
                JobDetailEffect.Dismiss -> onActionCompleted()
            }
        }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    FixItScreen(bg = C.Subtle) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                null,
                tint = C.Ink,
                modifier = Modifier.size(22.dp).clickable { onBack() }
            )
            Text(
                titleFor(state.booking?.status),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = C.Ink,
                modifier = Modifier.weight(1f)
            )
            state.booking?.status?.let { StatusBadge(badgeKeyFor(it)) }
        }

        // Body
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val booking = state.booking
            when {
                state.isLoading && booking == null -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = C.Blue) }

                booking != null -> JobDetailBody(
                    booking = booking,
                    payout = state.payout,
                    uploadingKind = state.uploadingKind,
                    deletingPhotoId = state.deletingPhotoId,
                    onPickPhoto = { kind ->
                        pendingKind = kind
                        photoPickerLauncher.launch("image/*")
                    },
                    onRemovePhoto = viewModel::removePhoto,
                )
            }
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.padding(horizontal = 16.dp))

        // Action bar — composition adapts to status
        state.booking?.let { booking ->
            JobActionBar(
                status = booking.status,
                isMutating = state.isMutating,
                isBusyWithPhoto = state.uploadingKind != null,
                onAccept = viewModel::accept,
                onDecline = viewModel::decline,
                onMarkReadyForPayment = viewModel::markReadyForPayment,
                onMarkJobDone = viewModel::markJobDone,
                onConfirmCompletion = viewModel::confirmCompletion,
                onCancel = { pendingCancelConfirm = true }
            )

            // Disclaimer beneath the action bar — only when we're in the
            // middle of in_progress and the AFTER slot is still empty.
            // Mirrors the screenshots' wording exactly.
            if (booking.status == BookingStatus.IN_PROGRESS &&
                booking.photos.none { it.kind == PhotoKind.AFTER }
            ) {
                Text(
                    "No 'after' photo yet — you can still mark done, but adding one is recommended.",
                    fontSize = 11.sp,
                    color = C.Slate,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }
    }

    if (pendingCancelConfirm) {
        AlertDialog(
            onDismissRequest = { pendingCancelConfirm = false },
            title = { Text("Cancel this booking?") },
            text = {
                Text(
                    "If the customer has already paid, the funds will be refunded to them. " +
                            "This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingCancelConfirm = false
                    viewModel.cancel()
                }) { Text("Yes, cancel") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancelConfirm = false }) {
                    Text("Keep booking")
                }
            }
        )
    }
}

private fun titleFor(status: BookingStatus?): String = when (status) {
    BookingStatus.PENDING               -> "Job request"
    BookingStatus.CONFIRMED             -> "Job accepted"
    BookingStatus.AWAITING_PAYMENT      -> "Awaiting payment"
    BookingStatus.IN_PROGRESS           -> "Job in progress"
    BookingStatus.AWAITING_CONFIRMATION -> "Awaiting confirmation"
    BookingStatus.COMPLETED             -> "Completed"
    BookingStatus.CANCELLED             -> "Cancelled"
    BookingStatus.DISPUTED              -> "In dispute"
    null                                -> "Job details"
}

private fun badgeKeyFor(status: BookingStatus): String = when (status) {
    BookingStatus.PENDING -> "new"
    BookingStatus.IN_PROGRESS, BookingStatus.AWAITING_CONFIRMATION -> "inprogress"
    BookingStatus.COMPLETED -> "completed"
    else -> "upcoming"
}

/**
 * Picks the small caption underneath the "You earn" amount. Drives one of
 * three states from the server-computed payout:
 *  - holding / no escrow yet (isEstimate=true): "Estimated"
 *  - released (isEstimate=false): "Paid out"
 *  - refunded: "Refunded to customer"  (provider earns 0)
 *
 * Defaults to "Estimated" when the payout is still loading — that's the
 * common case before the customer settles, so it's the least surprising
 * placeholder.
 */
private fun payoutCaption(payout: BookingPayout?): String = when {
    payout == null                                       -> "Estimated"
    payout.escrowStatus == EscrowStatus.REFUNDED         -> "Refunded to customer"
    payout.isEstimate                                    -> "Estimated"
    else                                                 -> "Paid out"
}

// ──────────────────────────────────────────────────────────────────────────
// Action bar — one component that picks its layout from booking status
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun JobActionBar(
    status: BookingStatus,
    isMutating: Boolean,
    isBusyWithPhoto: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onMarkReadyForPayment: () -> Unit,
    onMarkJobDone: () -> Unit,
    onConfirmCompletion: () -> Unit,
    onCancel: () -> Unit
) {
    when (status) {
        BookingStatus.PENDING -> ActionBarLayout {
            SecondaryButton("Decline", onClick = onDecline, isLoading = isMutating, modifier = Modifier.weight(1f))
            PrimaryActionButton("Accept job", onClick = onAccept, isLoading = isMutating, modifier = Modifier.weight(2f))
        }

        BookingStatus.CONFIRMED -> ActionBarLayout {
            SecondaryButton("Cancel", onClick = onCancel, isLoading = isMutating, modifier = Modifier.weight(1f))
            PrimaryActionButton(
                "Ready for payment",
                onClick = onMarkReadyForPayment,
                isLoading = isMutating,
                modifier = Modifier.weight(2f)
            )
        }

        BookingStatus.AWAITING_PAYMENT -> ActionBarLayout {
            SecondaryButton("Cancel", onClick = onCancel, isLoading = isMutating, modifier = Modifier.weight(1f))
            WaitingButton("Waiting for customer to pay", modifier = Modifier.weight(2f))
        }

        BookingStatus.IN_PROGRESS -> ActionBarLayout {
            // Block the transition while a photo upload is in flight so the
            // upload can't get orphaned by a status change race.
            PrimaryActionButton(
                "Mark job done",
                onClick = onMarkJobDone,
                isLoading = isMutating || isBusyWithPhoto,
                modifier = Modifier.weight(1f)
            )
        }

        BookingStatus.AWAITING_CONFIRMATION -> ActionBarLayout {
            PrimaryActionButton(
                "Confirm completion",
                onClick = onConfirmCompletion,
                isLoading = isMutating,
                modifier = Modifier.weight(1f)
            )
        }

        BookingStatus.COMPLETED,
        BookingStatus.CANCELLED,
        BookingStatus.DISPUTED -> Unit  // No action bar
    }
}

@Composable
private fun ActionBarLayout(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, C.Line)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun PrimaryActionButton(
    label: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(C.Blue)
            .clickable(enabled = !isLoading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.5.dp, C.Line, RoundedCornerShape(24.dp))
            .clickable(enabled = !isLoading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.Slate)
    }
}

@Composable
private fun WaitingButton(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(C.BlueSoft),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = C.BlueDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Body
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun JobDetailBody(
    booking: Booking,
    payout: BookingPayout?,
    uploadingKind: PhotoKind?,
    deletingPhotoId: String?,
    onPickPhoto: (PhotoKind) -> Unit,
    onRemovePhoto: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        Box(Modifier.padding(horizontal = 20.dp).padding(top = 14.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, C.Line, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val name = booking.customer?.name ?: "Customer"
                Avatar(
                    initials = initialsFor(name),
                    color = Color(avatarColorFor(booking.customer?.id ?: booking.id)),
                    photoUrl = booking.customer?.profilePhotoUrl
                )
                Column(Modifier.weight(1f)) {
                    Text(name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = C.Ink)
                    Text(
                        "Booking #${booking.id.take(8).uppercase()}",
                        fontSize = 12.sp,
                        color = C.Slate,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(C.BlueSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat,
                        null,
                        tint = C.Blue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Box(Modifier.padding(horizontal = 20.dp).padding(top = 12.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, C.Line, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "THE JOB",
                    fontSize = 11.sp,
                    color = C.Slate,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
                Text(
                    booking.service?.title ?: "Service",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.Ink,
                    letterSpacing = (-0.2).sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
                booking.notes?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        fontSize = 13.sp,
                        color = C.Slate,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Row(
                    Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    booking.service?.durationMinutes?.let {
                        InfoPill("Est. ${it / 60}h ${it % 60}m".replace(" 0m", ""))
                    }
                    InfoPill(
                        formatScheduled(booking.scheduledAt),
                        accent = booking.status == BookingStatus.PENDING
                    )
                }
            }
        }

        Box(Modifier.padding(horizontal = 20.dp).padding(top = 12.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, C.Line, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconBox(bg = C.BlueSoft) {
                    Icon(
                        Icons.Filled.LocationOn,
                        null,
                        tint = C.Blue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Address", fontSize = 12.5.sp, color = C.Slate)
                    Text(
                        booking.address,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = C.Ink,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // ── Photo evidence — only relevant while the job is in progress.
        // (The backend also accepts uploads in awaiting_confirmation, but
        // per spec this card is gated on IN_PROGRESS.)
        if (booking.status == BookingStatus.IN_PROGRESS) {
            PhotoEvidenceCard(
                photos          = booking.photos,
                uploadingKind   = uploadingKind,
                deletingPhotoId = deletingPhotoId,
                onPickPhoto     = onPickPhoto,
                onRemovePhoto   = onRemovePhoto,
            )
        }

        // ── Earnings (server-driven; no client-side commission math) ──
        val customerPays    = payout?.grossAmount
            ?: booking.service?.price
            ?: BigDecimal.ZERO
        val providerEarns   = payout?.providerPayout
        val caption         = payoutCaption(payout)

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, C.Line, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text("Customer pays", fontSize = 11.sp, color = C.Slate, fontWeight = FontWeight.SemiBold)
                Text(
                    formatMoney(customerPays),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = C.Ink,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "Customer Pays",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = C.OrangeText.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.OrangeSoft)
                    .padding(14.dp)
            ) {
                Text("You earn", fontSize = 11.sp, color = C.OrangeText, fontWeight = FontWeight.SemiBold)
                Text(
                    providerEarns?.let { formatMoney(it) } ?: "—",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = C.OrangeText,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    caption,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = C.OrangeText.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Photo evidence card — visible only while the booking is IN_PROGRESS.
// Layout matches the screenshots: a header row with an OPTIONAL pill, a
// description, two side-by-side BEFORE/AFTER slots, and a soft info note.
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun PhotoEvidenceCard(
    photos: List<BookingPhoto>,
    uploadingKind: PhotoKind?,
    deletingPhotoId: String?,
    onPickPhoto: (PhotoKind) -> Unit,
    onRemovePhoto: (String) -> Unit,
) {
    val beforePhoto = photos.firstOrNull { it.kind == PhotoKind.BEFORE }
    val afterPhoto  = photos.firstOrNull { it.kind == PhotoKind.AFTER }

    Box(Modifier.padding(horizontal = 20.dp).padding(top = 12.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, C.Line, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            // Header row: title + optional pill
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Photo evidence",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.Ink,
                    modifier = Modifier.weight(1f),
                )
                MicroBadge(label = "OPTIONAL", bg = C.Subtle, fg = C.Slate)
            }
            Text(
                "Optional, but pros who attach before & after photos get 2× more 5-star reviews and faster payouts.",
                fontSize = 12.sp,
                color = C.Slate,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 6.dp),
            )

            // BEFORE / AFTER labels above the tiles
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PhotoSlotHeader(
                    title = "BEFORE",
                    isAdded = beforePhoto != null,
                    addedColor = C.Slate,
                    modifier = Modifier.weight(1f),
                )
                PhotoSlotHeader(
                    title = "AFTER",
                    isAdded = afterPhoto != null,
                    addedColor = C.GreenText,
                    modifier = Modifier.weight(1f),
                )
            }

            // The tiles themselves
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PhotoSlot(
                    photo            = beforePhoto,
                    kind             = PhotoKind.BEFORE,
                    isUploading      = uploadingKind == PhotoKind.BEFORE,
                    isDeleting       = beforePhoto != null && deletingPhotoId == beforePhoto.id,
                    emptyTint        = C.BlueSoft,
                    emptyIconTint    = C.Blue,
                    emptySubtitle    = "Show the issue",
                    onPickPhoto      = onPickPhoto,
                    onRemovePhoto    = onRemovePhoto,
                    modifier         = Modifier.weight(1f),
                )
                PhotoSlot(
                    photo            = afterPhoto,
                    kind             = PhotoKind.AFTER,
                    isUploading      = uploadingKind == PhotoKind.AFTER,
                    isDeleting       = afterPhoto != null && deletingPhotoId == afterPhoto.id,
                    emptyTint        = C.GreenSoft,
                    emptyIconTint    = C.GreenText,
                    emptySubtitle    = "Show the fix",
                    onPickPhoto      = onPickPhoto,
                    onRemovePhoto    = onRemovePhoto,
                    modifier         = Modifier.weight(1f),
                )
            }

            // Info note beneath the tiles
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.BlueSoft)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.Info,
                    null,
                    tint = C.Blue,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    "Photos help resolve disputes in your favor and are shared with the customer.",
                    fontSize = 11.5.sp,
                    color = C.BlueDark,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun PhotoSlotHeader(
    title: String,
    isAdded: Boolean,
    addedColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            fontSize = 11.sp,
            color = C.Slate,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
            modifier = Modifier.weight(1f),
        )
        if (isAdded) {
            MicroBadge(label = "ADDED", bg = C.GreenSoft, fg = addedColor)
        } else {
            MicroBadge(label = "OPTIONAL", bg = C.Subtle, fg = C.Slate)
        }
    }
}

@Composable
private fun MicroBadge(
    label: String,
    bg: Color,
    fg: Color,
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
private fun PhotoSlot(
    photo: BookingPhoto?,
    kind: PhotoKind,
    isUploading: Boolean,
    isDeleting: Boolean,
    emptyTint: Color,
    emptyIconTint: Color,
    emptySubtitle: String,
    onPickPhoto: (PhotoKind) -> Unit,
    onRemovePhoto: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tileShape = RoundedCornerShape(12.dp)

    Box(
        modifier
            .aspectRatio(1f)
            .clip(tileShape)
            .background(if (photo != null) C.Subtle else emptyTint)
            .border(1.dp, C.Line, tileShape)
    ) {
        when {
            isUploading -> {
                // While uploading: show a spinner centred in the tile.
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color       = C.Blue,
                        strokeWidth = 2.dp,
                        modifier    = Modifier.size(28.dp),
                    )
                }
            }

            photo != null -> {
                // Filled tile: thumbnail + remove (X) button + filename hint.
                AsyncImage(
                    model              = photo.url,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )

                // Remove (X) overlay in the top-right corner.
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC0F172A))
                        .clickable(enabled = !isDeleting) { onRemovePhoto(photo.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            color       = Color.White,
                            strokeWidth = 1.5.dp,
                            modifier    = Modifier.size(12.dp),
                        )
                    } else {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove photo",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }

                // Filename hint along the bottom (last 12 chars of the URL,
                // which on this backend is `<uuid>.<ext>`).
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color(0x66000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        filenameOf(photo.url),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }

            else -> {
                // Empty tile: tap-to-pick.
                Column(
                    Modifier
                        .fillMaxSize()
                        .clickable { onPickPhoto(kind) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = emptyIconTint,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        "Add photo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = C.Ink,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        emptySubtitle,
                        fontSize = 10.5.sp,
                        color = C.Slate,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

/**
 * Trims the URL down to the last path segment (e.g. "abc123def.jpg") so the
 * filename hint along the bottom of the thumbnail matches the screenshots.
 * Falls back to a generic "image" label if the URL has no usable segment.
 */
private fun filenameOf(url: String): String {
    val trimmed = url.substringAfterLast('/').substringBefore('?').takeIf { it.isNotBlank() }
        ?: return "image"
    return if (trimmed.length > 16) trimmed.take(13) + "..." else trimmed
}