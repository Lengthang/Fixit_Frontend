package com.fixit.app.ui.customer.bookings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fixit.app.ui.components.Avatar
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.avatarColorFor
import com.fixit.app.ui.util.formatBookingMeta
import com.fixit.app.ui.util.formatMoney
import com.fixit.app.ui.util.initialsFor
import java.math.BigDecimal

// ── Dispute-specific soft-red palette (the theme's RedSoft is peach-toned;
//    these match the pink banner + red accents in the mockup, mirroring the
//    file-local colour convention used by the provider DisputeListScreen). ──
private val DspBannerBg   = Color(0xFFFEE2E2)
private val DspBannerText = Color(0xFFB91C1C)

@Composable
fun OpenDisputeScreen(
    onBack: () -> Unit,
    viewModel: OpenDisputeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Photo picker — up to 10 images (backend cap on reason_image_urls).
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        viewModel.onImagesSelected(uris)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is OpenDisputeEffect.Message ->
                    snackbarHostState.showSnackbar(effect.text)
                OpenDisputeEffect.Submitted -> onBack()
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

        // ── Header ────────────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                null,
                tint = C.Ink,
                modifier = Modifier.size(22.dp).clickable { onBack() },
            )
            Text(
                "Open a dispute",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = C.Ink,
                modifier = Modifier.weight(1f),
            )
        }

        // ── Body ──────────────────────────────────────────────────────────
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading && state.booking == null ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = C.Blue)
                    }

                else ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {

                        // ── Info banner ───────────────────────────────────
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DspBannerBg)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = DspBannerText,
                                modifier = Modifier.size(18.dp).padding(top = 1.dp),
                            )
                            Text(
                                "Disputes are reviewed by our support team within 24 hours. " +
                                        "The pro will be notified.",
                                fontSize = 12.5.sp,
                                color = DspBannerText,
                                lineHeight = 17.sp,
                            )
                        }

                        // ── Booking summary card ──────────────────────────
                        state.booking?.let { b ->
                            val providerName =
                                b.provider?.name?.takeIf { it.isNotBlank() } ?: "Provider"
                            val metaLine = buildString {
                                append(b.service?.title ?: "Service")
                                append(" · ")
                                append(formatBookingMeta(b))
                                if (b.totalAmount > BigDecimal.ZERO) {
                                    append(" · ")
                                    append(formatMoney(b.totalAmount))
                                }
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .border(1.dp, C.Line, RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Avatar(
                                    initials = initialsFor(providerName),
                                    color    = Color(avatarColorFor(b.provider?.id ?: b.id)),
                                    photoUrl = b.provider?.profilePhotoUrl,
                                    size     = 44,
                                    fontSize = 14,
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        providerName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = C.Ink,
                                    )
                                    Text(
                                        metaLine,
                                        fontSize = 12.5.sp,
                                        color = C.Slate,
                                        modifier = Modifier.padding(top = 3.dp),
                                    )
                                }
                            }
                        }

                        // ── Reason for dispute ────────────────────────────
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Reason for dispute",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = C.Slate,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )

                            val cats = DisputeReasonCategory.entries
                            cats.forEachIndexed { index, cat ->
                                ReasonRadioRow(
                                    label = cat.label,
                                    selected = state.category == cat,
                                    onClick = { viewModel.onCategorySelect(cat) },
                                )
                                if (index < cats.lastIndex) {
                                    HorizontalDivider(color = C.Line)
                                }
                            }
                        }

                        // ── "Other" custom reason box ─────────────────────
                        if (state.category == DisputeReasonCategory.OTHER) {
                            Column {
                                Text(
                                    "Your reason",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = C.Slate,
                                    modifier = Modifier.padding(bottom = 6.dp),
                                )
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.5.dp, C.Line, RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                ) {
                                    BasicTextField(
                                        value = state.otherText,
                                        onValueChange = viewModel::onOtherTextChange,
                                        textStyle = TextStyle(
                                            color = C.Ink,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                        ),
                                        cursorBrush = SolidColor(C.Blue),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp),
                                        decorationBox = { inner ->
                                            if (state.otherText.isEmpty()) {
                                                Text(
                                                    "Briefly describe the issue",
                                                    color = C.Mute,
                                                    fontSize = 14.sp,
                                                )
                                            }
                                            inner()
                                        },
                                    )
                                }
                                if (state.otherText.isNotBlank() &&
                                    state.composedReason.length < 10
                                ) {
                                    Text(
                                        "Please add a little more detail (min 10 characters).",
                                        fontSize = 11.sp,
                                        color = C.Red,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                            }
                        }

                        // ── Describe what happened (optional) ─────────────
                        Column {
                            Text(
                                "Describe what happened",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = C.Slate,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, C.Line, RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                            ) {
                                BasicTextField(
                                    value = state.description,
                                    onValueChange = viewModel::onDescriptionChange,
                                    textStyle = TextStyle(
                                        color = C.Ink,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                    ),
                                    cursorBrush = SolidColor(C.Blue),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 120.dp),
                                    decorationBox = { inner ->
                                        if (state.description.isEmpty()) {
                                            Text(
                                                "Add any details that help our team understand " +
                                                        "the situation (optional).",
                                                color = C.Mute,
                                                fontSize = 14.sp,
                                                lineHeight = 20.sp,
                                            )
                                        }
                                        inner()
                                    },
                                )
                            }
                        }

                        // ── Attached photo thumbnails ─────────────────────
                        if (state.uploadedImageUrls.isNotEmpty()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                state.uploadedImageUrls.take(4).forEach { url ->
                                    Box(Modifier.size(64.dp)) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(10.dp)),
                                        )
                                        Box(
                                            Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-4).dp)
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(C.Ink.copy(alpha = 0.7f))
                                                .clickable { viewModel.removeUploadedImage(url) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp),
                                            )
                                        }
                                    }
                                }
                                if (state.uploadedImageUrls.size > 4) {
                                    Box(
                                        Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(C.Subtle)
                                            .border(1.dp, C.Line, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "+${state.uploadedImageUrls.size - 4}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = C.Slate,
                                        )
                                    }
                                }
                            }
                        }

                        // ── Attach photos button ──────────────────────────
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, C.Line, RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .clickable(enabled = !state.isUploading) {
                                    imagePicker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                                .padding(vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (state.isUploading) {
                                CircularProgressIndicator(
                                    color = C.Slate,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    "Uploading…",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = C.Slate,
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Image,
                                    contentDescription = null,
                                    tint = C.Slate,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    if (state.uploadedImageUrls.isEmpty())
                                        "Attach photos (optional)"
                                    else
                                        "Add more photos",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = C.Slate,
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                    }
            }
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.padding(horizontal = 16.dp))

        // ── Sticky submit bar ──────────────────────────────────────────────
        Column(Modifier.fillMaxWidth().background(Color.White)) {
            HorizontalDivider(color = C.Line)
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(if (state.canSubmit) C.Red else C.Mute)
                        .clickable(enabled = state.canSubmit) { viewModel.submit() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text(
                            "Submit dispute",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Reason radio row — custom circle to match the mockup's red accent.
// ──────────────────────────────────────────────────────────────────────────
@Composable
private fun ReasonRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) C.Red else C.Line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(C.Red),
                )
            }
        }
        Text(
            label,
            fontSize = 14.5.sp,
            color = C.Ink,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}