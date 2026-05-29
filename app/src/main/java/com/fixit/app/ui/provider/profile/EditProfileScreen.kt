package com.fixit.app.ui.provider.profile

import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fixit.app.ui.components.Avatar
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.IconBox
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.signup.provider.RadiusSlider
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.OnLifecycleStart
import com.fixit.app.ui.components.LocationPickerMap
import com.google.android.gms.maps.model.LatLng
/* ─────────────────────────────────────────────────────────────────────────
 * Public screen — wired by NavGraph at Routes.PROVIDER_EDIT_PROFILE.
 *
 * Reused components (no duplication, all imports):
 *   • FixItScreen      — screen wrapper (status/nav bar padding handled)
 *   • TopBar           — back arrow + slot row; title overlaid via Box
 *   • Avatar / IconBox — circular avatar + rounded tile for icon chrome
 *   • MapBackground    — imported from ServiceAreaScreen.kt
 *   • RadiusSlider     — imported from ServiceAreaScreen.kt
 *
 * Backend-driven adjustments (from earlier Q&A):
 *   • Q3: single cert row + single national ID row (matches DB columns).
 *   • Q4: image-only uploads (JPG/PNG/GIF/WEBP); the upload API rejects PDFs.
 *   • Q5: no "specific unavailable dates" block (no backend support yet).
 *   • Q6: location behaves like ServiceAreaScreen — auto-resolve on enter,
 *     "Change" pill re-resolves, Geocoder upgrades coords → friendly address.
 *   • Q9: no top-right Save; sticky bottom is the single canonical action.
 * ──────────────────────────────────────────────────────────────────────── */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Single picker contract — VM remembers which slot is being filled.
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? -> uri?.let { viewModel.onImagePicked(it) } }

    fun pickFor(target: UploadTarget) {
        viewModel.setUploadTarget(target)
        imagePicker.launch("image/*")
    }
    OnLifecycleStart(viewModel::refresh)

    // Auto-resolve location on first composition, same as ServiceAreaScreen.
    // The VM no-ops if already resolved or a fix is in-flight.
    LaunchedEffect(Unit) {
        if (state.latitude == null && state.longitude == null) {
            viewModel.resolveLocation()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect {
            when (it) { EditProfileEffect.SavedOk -> onSaved() }
        }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    FixItScreen(bg = C.Subtle) {
        // ── Top bar: reuse the shared TopBar component, overlay the title ──
        // TopBar has no title slot, so we Box-overlay a centered Text on it.
        // This avoids duplicating TopBar's chrome (back arrow, padding,
        // height) and keeps the screen consistent with the rest of the app.
        Box(modifier = Modifier.fillMaxWidth().background(Color.White)) {
            TopBar(onBack = onBack)
            Text(
                text          = "Edit profile",
                fontSize      = 17.sp,
                fontWeight    = FontWeight.Bold,
                color         = C.Ink,
                letterSpacing = (-0.2).sp,
                modifier      = Modifier.align(Alignment.Center),
            )
        }
        HorizontalDivider(color = C.Line)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (state.isLoading) {
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
                    // ── PROFILE ──────────────────────────────────────────
                    EpSection(title = "Profile") {
                        PhotoBlock(
                            photoUrl    = state.profilePhotoUrl,
                            name        = state.name.ifBlank { state.originalName },
                            uploading   = state.uploadingPhoto,
                            onReplace   = { pickFor(UploadTarget.PHOTO) },
                            onRemove    = viewModel::removePhoto,
                            onCameraTap = { pickFor(UploadTarget.PHOTO) },
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        EpInput(
                            label         = "Full name",
                            value         = state.name,
                            onValueChange = viewModel::onNameChange,
                            placeholder   = "Your full name",
                        )
                        EpInput(
                            label         = "Bio",
                            value         = state.bio,
                            onValueChange = viewModel::onBioChange,
                            placeholder   = "Tell customers about your work and experience",
                            multiline     = true,
                            rows          = 4,
                            counter       = "${state.bio.length} / 280",
                        )
                        EpInput(
                            label         = "Years of experience",
                            value         = state.yearsExperience,
                            onValueChange = viewModel::onYearsChange,
                            placeholder   = "0",
                            suffix        = "years",
                            keyboardType  = KeyboardType.Number,
                        )
                    }

                    // ── SERVICE CATEGORIES ──────────────────────────────
                    EpSection(
                        title = "Service categories",
                        hint  = "${state.selectedCategoryIds.size} selected",
                    ) {
                        Text(
                            "What you offer. Tap × to remove. Customers can find you under any of these.",
                            fontSize   = 12.5.sp,
                            color      = C.Slate,
                            lineHeight = 18.sp,
                            modifier   = Modifier.padding(bottom = 12.dp),
                        )
                        val selected = state.availableCategories
                            .filter { it.id in state.selectedCategoryIds }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement   = Arrangement.spacedBy(8.dp),
                        ) {
                            selected.forEach { cat ->
                                EpCategoryChip(
                                    label    = cat.name,
                                    onRemove = { viewModel.removeCategory(cat.id) },
                                )
                            }
                            EpAddCategoryChip(onClick = viewModel::openCategoryPicker)
                        }
                    }

                    // ── CERTIFICATES & NATIONAL ID ───────────────────────
                    EpSection(
                        title = "Certificates & ID",
                        hint  = "Verified pros book 3× more",
                    ) {
                        // Cert name input (the human-readable label; the
                        // file URL is uploaded separately just below).
                        EpInput(
                            label         = "Certification name",
                            value         = state.certificationName,
                            onValueChange = viewModel::onCertNameChange,
                            placeholder   = "e.g. CA Plumbing License C-36",
                        )
                        EpFileRow(
                            title     = "Certificate",
                            url       = state.certificationUrl,
                            uploading = state.uploadingCert,
                            onChoose  = { pickFor(UploadTarget.CERTIFICATE) },
                            onRemove  = viewModel::removeCertificate,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        EpFileRow(
                            title     = "National ID",
                            url       = state.nationalIdUrl,
                            uploading = state.uploadingNationalId,
                            onChoose  = { pickFor(UploadTarget.NATIONAL_ID) },
                            onRemove  = viewModel::removeNationalId,
                        )
                    }

                    // ── WORK LOCATION ────────────────────────────────────
                    // Mirrors ServiceAreaScreen's layout exactly:
                    // big map block (320.dp) + RadiusSlider + endpoint
                    // labels. The radius circle is centered on the map
                    // canvas, visually anchoring the service area to the
                    // selected location (which the VM holds as lat/lng).
                    EpSection(title = "Work location") {
                        EpSection(title = "Work location") {
                            LocationPickerMap(
                                selected = state.latitude?.let { lat ->
                                    state.longitude?.let { lng -> LatLng(lat, lng) }
                                },
                                onPick = { latLng -> viewModel.onPick(latLng.latitude, latLng.longitude) },
                                radiusKm = state.serviceRadiusKm,
                                locating = state.locating,
                                onRecenterRequest = viewModel::resolveLocation,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                state.locationLabel.ifBlank { "Drop a pin to set your service area" },
                                fontSize = 13.sp,
                                color = if (state.locationLabel.isBlank()) C.Mute else C.Ink,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Text("Service radius", fontSize = 12.sp, color = C.Slate, fontWeight = FontWeight.Medium)
                                Text("${state.serviceRadiusKm} km", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = C.Blue)
                            }
                            RadiusSlider(
                                value = state.serviceRadiusKm,
                                onValueChange = viewModel::onRadiusChange,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("1 km", fontSize = 11.sp, color = C.Mute)
                                Text("50 km", fontSize = 11.sp, color = C.Mute)
                            }
                            if (state.latitude == null && !state.locating) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Tap \u201cLocate me\u201d or drop a pin to set your location.",
                                    fontSize = 12.sp,
                                    color = C.Orange,
                                    modifier = Modifier.clickable { viewModel.resolveLocation() },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            modifier              = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.Bottom,
                        ) {
                            Text(
                                "Service radius",
                                fontSize   = 12.sp,
                                color      = C.Slate,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "${state.serviceRadiusKm} km",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color      = C.Blue,
                            )
                        }
                        // Imported from ServiceAreaScreen — identical drag
                        // behaviour, identical visuals.
//                        RadiusSlider(
//                            value         = state.serviceRadiusKm,
//                            onValueChange = viewModel::onRadiusChange,
//                        )
//                        Row(
//                            modifier              = Modifier.fillMaxWidth().padding(top = 4.dp),
//                            horizontalArrangement = Arrangement.SpaceBetween,
//                        ) {
//                            Text("1 km",  fontSize = 11.sp, color = C.Mute)
//                            Text("50 km", fontSize = 11.sp, color = C.Mute)
//                        }
//                        if (state.latitude == null && !state.locating) {
//                            Spacer(modifier = Modifier.height(12.dp))
//                            Text(
//                                "Couldn't read your location. Tap to retry.",
//                                fontSize = 12.sp,
//                                color    = C.Orange,
//                                modifier = Modifier.clickable { viewModel.resolveLocation() },
//                            )
//                        }
                    }

                    // ── AVAILABILITY ─────────────────────────────────────
                    EpSection(title = "Availability") {
                        Text(
                            "Recurring days",
                            fontSize     = 12.sp,
                            color        = C.Slate,
                            fontWeight   = FontWeight.Medium,
                            modifier     = Modifier.padding(bottom = 10.dp),
                        )
                        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier              = Modifier.padding(bottom = 18.dp),
                        ) {
                            dayLabels.forEachIndexed { i, label ->
                                EpDayPill(
                                    d       = label,
                                    active  = state.activeDays[i],
                                    onClick = { viewModel.toggleDay(i) },
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier              = Modifier.padding(bottom = 4.dp),
                        ) {
                            EpTimeBox(
                                label    = "Start",
                                hhmm     = state.openTime,
                                onChange = viewModel::onOpenTimeChange,
                                modifier = Modifier.weight(1f),
                            )
                            EpTimeBox(
                                label    = "End",
                                hhmm     = state.closeTime,
                                onChange = viewModel::onCloseTimeChange,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            SnackbarHost(
                hostState = snackbar,
                modifier  = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp),
            )
        }

        // ── Sticky bottom bar — Cancel + Save changes ───────────────────
        // Not extracted to /components/ because the project doesn't have a
        // shared two-button sticky pattern yet, and PrimaryButton /
        // OutlineButton are full-width composables. Keeping it inline
        // matches the same approach used in AddEditServiceScreen.
        HorizontalDivider(color = C.Line)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.5.dp, C.Line, RoundedCornerShape(24.dp))
                    .clickable(enabled = !state.isSaving) { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Cancel",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = C.Slate,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (state.isSaving) C.Mute else C.Blue)
                    .clickable(enabled = !state.isSaving) { viewModel.save() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (state.isSaving) "Saving…" else "Save changes",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                )
            }
        }
    }

    // ── Category picker dialog ──────────────────────────────────────────
    if (state.categoryPickerOpen) {
        AlertDialog(
            onDismissRequest = viewModel::closeCategoryPicker,
            title = {
                Text(
                    "Select service categories",
                    fontWeight = FontWeight.Bold,
                    color      = C.Ink,
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.availableCategories.forEach { cat ->
                        val active = cat.id in state.selectedCategoryIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.5.dp,
                                    if (active) C.Blue else C.Line,
                                    RoundedCornerShape(10.dp),
                                )
                                .background(if (active) C.BlueSoft else Color.White)
                                .clickable { viewModel.toggleCategory(cat.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                cat.name,
                                fontSize   = 13.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                color      = if (active) C.BlueDark else C.Ink,
                                modifier   = Modifier.weight(1f),
                            )
                            if (active) {
                                Box(
                                    modifier = Modifier.size(18.dp).clip(CircleShape).background(C.Blue),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Canvas(modifier = Modifier.size(10.dp)) {
                                        val s = size.width / 24f
                                        val p = Path().apply {
                                            moveTo(5f * s, 12f * s)
                                            lineTo(10f * s, 17f * s)
                                            lineTo(20f * s, 7f * s)
                                        }
                                        drawPath(
                                            p, Color.White,
                                            style = Stroke(3f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::closeCategoryPicker) {
                    Text("Done", color = C.Blue, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}

// ── Section wrapper ──────────────────────────────────────────────────────
@Composable
private fun EpSection(
    title   : String,
    hint    : String? = null,
    content : @Composable ColumnScope.() -> Unit,
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
            if (hint != null) {
                Text(hint, fontSize = 11.sp, color = C.Mute)
            }
        }
        content()
    }
    HorizontalDivider(color = C.Line)
    Spacer(modifier = Modifier.height(10.dp))
}

// ── Photo block — reuses shared Avatar component ─────────────────────────
@Composable
private fun PhotoBlock(
    photoUrl   : String?,
    name       : String,
    uploading  : Boolean,
    onReplace  : () -> Unit,
    onRemove   : () -> Unit,
    onCameraTap: () -> Unit,
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box {
            Avatar(
                initials = initialsForLocal(name),
                color    = C.Orange,
                size     = 84,
                fontSize = 28,
                photoUrl = photoUrl,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 2.dp, end = 2.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(C.Blue)
                    .border(3.dp, Color.White, CircleShape)
                    .clickable(enabled = !uploading) { onCameraTap() },
                contentAlignment = Alignment.Center,
            ) {
                if (uploading) {
                    CircularProgressIndicator(
                        color       = Color.White,
                        strokeWidth = 2.dp,
                        modifier    = Modifier.size(14.dp),
                    )
                } else {
                    Canvas(modifier = Modifier.size(14.dp)) {
                        val s      = size.width / 24f
                        val stroke = Stroke(2.4f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        drawRoundRect(
                            Color.White,
                            Offset(1f * s, 6f * s),
                            androidx.compose.ui.geometry.Size(22f * s, 14f * s),
                            CornerRadius(2f * s), style = stroke,
                        )
                        val bump = Path().apply {
                            moveTo(7f * s, 6f * s);  lineTo(9f * s, 3f * s)
                            lineTo(15f * s, 3f * s); lineTo(17f * s, 6f * s)
                        }
                        drawPath(bump, Color.White, style = stroke)
                        drawCircle(Color.White, 4f * s, Offset(12f * s, 13f * s), style = stroke)
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Profile photo",
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = C.Ink,
            )
            Text(
                "JPG or PNG · 1:1 ratio recommended.",
                fontSize   = 12.sp,
                color      = C.Slate,
                lineHeight = 17.sp,
                modifier   = Modifier.padding(top = 3.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.padding(top = 10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.5.dp, C.Blue, RoundedCornerShape(18.dp))
                        .clickable(enabled = !uploading) { onReplace() }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Canvas(modifier = Modifier.size(13.dp)) {
                        val s      = size.width / 24f
                        val stroke = Stroke(2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        val base   = Path().apply {
                            moveTo(21f * s, 15f * s); lineTo(21f * s, 19f * s)
                            lineTo(3f * s, 19f * s);  lineTo(3f * s, 15f * s)
                        }
                        drawPath(base, C.Blue, style = stroke)
                        drawLine(C.Blue, Offset(12f * s, 15f * s), Offset(12f * s, 3f * s), 2.2f * s, StrokeCap.Round)
                        val head = Path().apply {
                            moveTo(7f * s, 8f * s); lineTo(12f * s, 3f * s); lineTo(17f * s, 8f * s)
                        }
                        drawPath(head, C.Blue, style = stroke)
                    }
                    Text(
                        if (photoUrl == null) "Upload" else "Replace",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = C.Blue,
                    )
                }
                if (photoUrl != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.5.dp, C.Line, RoundedCornerShape(18.dp))
                            .clickable(enabled = !uploading) { onRemove() }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            "Remove",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = C.Slate,
                        )
                    }
                }
            }
        }
    }
}

// ── Input field (label + bordered text area, supports multiline/suffix/counter) ──
//
// Not using FixItTextField from /components/ — it hardcodes
// padding(horizontal = 24.dp) which conflicts with this screen's section
// cards (20.dp padding). Same approach AddEditServiceScreen takes with its
// own PricingField helper.
@Composable
private fun EpInput(
    label        : String,
    value        : String,
    onValueChange: (String) -> Unit,
    placeholder  : String = "",
    suffix       : String? = null,
    multiline    : Boolean = false,
    rows         : Int = 3,
    counter      : String? = null,
    keyboardType : KeyboardType = KeyboardType.Text,
) {
    var focused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Bottom,
        ) {
            Text(
                label,
                fontSize   = 12.sp,
                color      = if (focused) C.Blue else C.Slate,
                fontWeight = FontWeight.Medium,
            )
            if (counter != null) {
                Text(counter, fontSize = 11.sp, color = C.Mute)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.5.dp,
                    if (focused) C.Blue else C.Line,
                    RoundedCornerShape(10.dp),
                )
                .background(Color.White, RoundedCornerShape(10.dp))
                .then(if (multiline) Modifier.heightIn(min = (rows * 22).dp) else Modifier)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment     = if (multiline) Alignment.Top else Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value         = value,
                onValueChange = onValueChange,
                singleLine    = !multiline,
                textStyle     = TextStyle(
                    color      = C.Ink,
                    fontSize   = 15.sp,
                    lineHeight = if (multiline) (15 * 1.45).sp else 22.sp,
                ),
                cursorBrush      = SolidColor(C.Blue),
                keyboardOptions  = KeyboardOptions(keyboardType = keyboardType),
                modifier         = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color      = C.Mute,
                            fontSize   = 15.sp,
                            lineHeight = if (multiline) (15 * 1.45).sp else 22.sp,
                        )
                    }
                    inner()
                },
            )
            if (suffix != null) {
                Text(
                    suffix,
                    fontSize   = 13.sp,
                    color      = C.Slate,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ── Category chips ───────────────────────────────────────────────────────
@Composable
private fun EpCategoryChip(label: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, C.Blue, RoundedCornerShape(20.dp))
            .background(C.BlueSoft)
            .padding(start = 14.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            label,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            color      = C.BlueDark,
        )
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(C.Blue)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(9.dp)) {
                val s = size.width / 24f
                drawLine(Color.White, Offset(6f * s, 6f * s),   Offset(18f * s, 18f * s), 3f * s, StrokeCap.Round)
                drawLine(Color.White, Offset(18f * s, 6f * s),  Offset(6f * s, 18f * s),  3f * s, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun EpAddCategoryChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .dashedBorder(1.5.dp, C.Blue, 20.dp)
            .background(C.BlueSoft)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(modifier = Modifier.size(13.dp)) {
            val s = size.width / 24f
            drawLine(C.Blue, Offset(12f * s, 5f * s),  Offset(12f * s, 19f * s), 2.6f * s, StrokeCap.Round)
            drawLine(C.Blue, Offset(5f * s,  12f * s), Offset(19f * s, 12f * s), 2.6f * s, StrokeCap.Round)
        }
        Text(
            "Add category",
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = C.Blue,
        )
    }
}

// ── File row (one row per file slot — cert or national ID) ───────────────
@Composable
private fun EpFileRow(
    title    : String,
    url      : String?,
    uploading: Boolean,
    onChoose : () -> Unit,
    onRemove : () -> Unit,
) {
    if (url == null) {
        // Empty state — tappable upload card.
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
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val s      = this.size.width / 24f
                        val stroke = Stroke(2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        val base   = Path().apply {
                            moveTo(21f * s, 15f * s); lineTo(21f * s, 19f * s)
                            lineTo(3f * s, 19f * s);  lineTo(3f * s, 15f * s)
                        }
                        drawPath(base, C.Blue, style = stroke)
                        drawLine(C.Blue, Offset(12f * s, 15f * s), Offset(12f * s, 3f * s), 2.2f * s, StrokeCap.Round)
                        val head = Path().apply {
                            moveTo(7f * s, 8f * s); lineTo(12f * s, 3f * s); lineTo(17f * s, 8f * s)
                        }
                        drawPath(head, C.Blue, style = stroke)
                    }
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
                Text(
                    "Choose",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                )
            }
        }
    } else {
        // Filled state — thumbnail + filename + Replace/Remove.
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
                model              = url,
                contentDescription = null,
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
                        title,
                        fontSize   = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = C.Ink,
                        modifier   = Modifier.weight(1f, fill = false),
                    )
                    Box(
                        modifier = Modifier.size(14.dp).clip(CircleShape).background(C.Green),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.size(8.dp)) {
                            val s    = this.size.width / 24f
                            val path = Path().apply {
                                moveTo(5f * s, 12f * s); lineTo(10f * s, 17f * s); lineTo(20f * s, 7f * s)
                            }
                            drawPath(
                                path, Color.White,
                                style = Stroke(3.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                        }
                    }
                }
                Text(
                    fileNameFromUrl(url),
                    fontSize = 11.5.sp,
                    color    = C.Slate,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

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
                    Text(
                        "Replace",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = C.Blue,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(C.RedSoft)
                    .clickable(enabled = !uploading) { onRemove() },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(14.dp)) {
                    val s      = this.size.width / 24f
                    val stroke = Stroke(2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    drawLine(C.Red, Offset(3f * s, 6f * s),  Offset(21f * s, 6f * s), 2.2f * s, StrokeCap.Round)
                    val body = Path().apply {
                        moveTo(5f * s, 6f * s);   lineTo(7f * s, 20f * s)
                        lineTo(17f * s, 20f * s); lineTo(19f * s, 6f * s)
                    }
                    drawPath(body, C.Red, style = stroke)
                    drawLine(C.Red, Offset(10f * s, 11f * s), Offset(10f * s, 17f * s), 2.2f * s, StrokeCap.Round)
                    drawLine(C.Red, Offset(14f * s, 11f * s), Offset(14f * s, 17f * s), 2.2f * s, StrokeCap.Round)
                }
            }
        }
    }
}

// ── Location block — same map/pin/circle/radius-badge as ServiceAreaScreen ─
//
// Reuses MapBackground (imported). The circle + Icons.Filled.LocationOn pin
// (C.Orange, 48dp) and the "Radius · N km" badge are layered on top of the
// shared canvas exactly like ServiceAreaScreen does it. The only addition
// is the bottom-left address pill and the top-right "Change" pill, which
// give the user a way to see and refresh their location without leaving
// the edit screen.
//@Composable
//private fun LocationBlock(
//    radiusKm     : Int,
//    locationLabel: String,
//    locating     : Boolean,
//    onChange     : () -> Unit,
//) {
//    Box(
//        Modifier
//            .fillMaxWidth()
//            .height(320.dp)
//            .clip(RoundedCornerShape(16.dp))
//            .border(1.dp, C.Line, RoundedCornerShape(16.dp)),
//    ) {
//        // Shared MapBackground from ServiceAreaScreen.kt.
//        MapBackground()
//
//        // Service-area circle — visually anchored to the centre of the map.
//        Box(
//            Modifier
//                .align(Alignment.Center)
//                .size(220.dp)
//                .clip(CircleShape)
//                .background(C.Blue.copy(alpha = 0.12f))
//                .border(2.dp, C.Blue, CircleShape),
//        )
//
//        // Same icon style ServiceAreaScreen uses — orange location pin.
//        Icon(
//            imageVector        = Icons.Filled.LocationOn,
//            contentDescription = null,
//            tint               = C.Orange,
//            modifier           = Modifier.align(Alignment.Center).size(48.dp),
//        )
//
//        // Top-left radius badge (matches ServiceAreaScreen).
//        Box(
//            Modifier
//                .align(Alignment.TopStart)
//                .padding(12.dp)
//                .clip(RoundedCornerShape(20.dp))
//                .background(Color.White)
//                .padding(horizontal = 12.dp, vertical = 8.dp),
//        ) {
//            Text(
//                "Radius · $radiusKm km",
//                fontSize   = 12.sp,
//                fontWeight = FontWeight.SemiBold,
//                color      = C.Ink,
//            )
//        }
//
//        // Top-right "Change" pill — kicks off the same resolveLocation()
//        // flow ServiceAreaScreen uses. Tapping refreshes lat/lng and
//        // re-geocodes the address. Disabled while a fix is in flight.
//        Row(
//            modifier = Modifier
//                .align(Alignment.TopEnd)
//                .padding(12.dp)
//                .clip(RoundedCornerShape(20.dp))
//                .background(Color.White)
//                .clickable(enabled = !locating) { onChange() }
//                .padding(horizontal = 12.dp, vertical = 6.dp),
//            verticalAlignment     = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.spacedBy(6.dp),
//        ) {
//            if (locating) {
//                CircularProgressIndicator(
//                    color       = C.Blue,
//                    strokeWidth = 2.dp,
//                    modifier    = Modifier.size(12.dp),
//                )
//            }
//            Text(
//                if (locating) "Locating…" else "Change",
//                fontSize   = 12.sp,
//                fontWeight = FontWeight.SemiBold,
//                color      = C.Blue,
//            )
//        }
//
//        // Bottom-left address pill — reflects locationLabel from state.
//        // Updates instantly when coords change, then upgrades to the
//        // geocoded address when reverse-geocoding lands.
//        Row(
//            modifier = Modifier
//                .align(Alignment.BottomStart)
//                .padding(12.dp)
//                .clip(RoundedCornerShape(20.dp))
//                .background(Color.White)
//                .padding(horizontal = 12.dp, vertical = 6.dp),
//            verticalAlignment     = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.spacedBy(6.dp),
//        ) {
//            Icon(
//                imageVector        = Icons.Filled.LocationOn,
//                contentDescription = null,
//                tint               = C.Blue,
//                modifier           = Modifier.size(12.dp),
//            )
//            Text(
//                locationLabel,
//                fontSize   = 11.5.sp,
//                fontWeight = FontWeight.SemiBold,
//                color      = C.Ink,
//            )
//        }
//    }
//}

// ── Day pill (Monday-first; controlled by VM toggle) ─────────────────────
@Composable
private fun EpDayPill(d: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (active) C.Blue else Color.White)
            .border(1.5.dp, if (active) C.Blue else C.Line, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            d,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (active) Color.White else C.Slate,
        )
    }
}

// ── Time box — tap to open framework TimePickerDialog ───────────────────
@Composable
private fun EpTimeBox(
    label    : String,
    hhmm     : String,
    onChange : (String) -> Unit,
    modifier : Modifier = Modifier,
) {
    val context = LocalContext.current
    val (h, m) = parseHhMm(hhmm)
    val (displayTime, displayPeriod) = formatTwelveHour(h, m)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, C.Line, RoundedCornerShape(12.dp))
            .clickable {
                TimePickerDialog(
                    context,
                    { _, hr, mn -> onChange("%02d:%02d".format(hr, mn)) },
                    h, m, false,
                ).show()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, fontSize = 10.5.sp, color = C.Mute)
        Row(
            modifier              = Modifier.padding(top = 1.dp),
            verticalAlignment     = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                displayTime,
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = C.Ink,
                modifier   = Modifier.alignByBaseline(),
            )
            Text(
                displayPeriod,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = C.Slate,
                modifier   = Modifier.alignByBaseline(),
            )
        }
    }
}

// ── Dashed border modifier (reused only inside this file) ───────────────
private fun Modifier.dashedBorder(width: Dp, color: Color, cornerRadius: Dp): Modifier =
    drawWithContent {
        drawContent()
        val strokeW    = width.toPx()
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    left         = strokeW / 2f,
                    top          = strokeW / 2f,
                    right        = size.width  - strokeW / 2f,
                    bottom       = size.height - strokeW / 2f,
                    cornerRadius = CornerRadius(cornerRadius.toPx()),
                ),
            )
        }
        drawPath(path, color, style = Stroke(strokeW, pathEffect = pathEffect))
    }

// ── Small helpers (file-local so the screen stays self-contained) ───────

/** Parses "HH:MM" or "HH:MM:SS" into (hour, minute). */
private fun parseHhMm(hhmm: String): Pair<Int, Int> {
    val parts = hhmm.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return h to m
}

/** Returns ("8:00", "AM") from (8, 0); ("12:00", "PM") from (12, 0); etc. */
private fun formatTwelveHour(hour24: Int, minute: Int): Pair<String, String> {
    val period = if (hour24 < 12) "AM" else "PM"
    val h12 = when {
        hour24 == 0  -> 12
        hour24 > 12  -> hour24 - 12
        else         -> hour24
    }
    return "%d:%02d".format(h12, minute) to period
}

/** "https://x.y/media/images/abc123.jpg" → "abc123.jpg". */
private fun fileNameFromUrl(url: String): String =
    url.substringAfterLast('/').ifBlank { "uploaded file" }

private fun initialsForLocal(name: String?): String {
    if (name.isNullOrBlank()) return "?"
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else            -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}