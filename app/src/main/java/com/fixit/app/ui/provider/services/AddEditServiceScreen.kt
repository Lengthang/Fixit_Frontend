package com.fixit.app.ui.provider.services

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.theme.C

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditServiceScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddEditServiceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? -> uri?.let { viewModel.onPhotoPicked(it) } }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AddEditServiceEffect.SavedOk -> onSaved()
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

        // ── top bar — same pattern as ProviderEarningsScreen ─────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(C.Bg)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint               = C.Ink,
                modifier           = Modifier.size(22.dp).clickable { onBack() },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text          = if (state.isEditing) "Edit service" else "Add new service",
                    fontSize      = 16.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = C.Ink,
                    letterSpacing = (-0.2).sp,
                )
                Text(
                    text     = "This will appear on your profile",
                    fontSize = 12.sp,
                    color    = C.Slate,
                )
            }
            Text(
                text       = "Save draft",
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = C.Mute,
                modifier   = Modifier.clickable { /* TODO: save draft */ },
            )
        }

        // ── content + snackbar ────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {

            if (state.isLoading) {
                CircularProgressIndicator(
                    color    = C.Blue,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {

                    // ── cover photo ───────────────────────────────────
                    Column {
                        FormLabel(label = "Cover photo", hint = "Optional")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!state.imageUrl.isNullOrBlank()) {
                                Box(modifier = Modifier.size(86.dp)) {
                                    AsyncImage(
                                        model              = state.imageUrl,
                                        contentDescription = null,
                                        contentScale       = ContentScale.Crop,
                                        modifier           = Modifier
                                            .size(86.dp)
                                            .clip(RoundedCornerShape(14.dp)),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xB3000000))
                                            .clickable { viewModel.onRemovePhoto() },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("✕", color = Color.White, fontSize = 9.sp)
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.5.dp, C.Line, RoundedCornerShape(14.dp))
                                    .background(C.Bg)
                                    .clickable(enabled = !state.isUploadingPhoto) {
                                        photoPickerLauncher.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (state.isUploadingPhoto) {
                                    CircularProgressIndicator(
                                        modifier    = Modifier.size(24.dp),
                                        color       = C.Blue,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text("+", color = C.Blue, fontSize = 20.sp, fontWeight = FontWeight.Light)
                                        Text("Add photo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = C.Slate)
                                    }
                                }
                            }
                        }
                    }

                    // ── service name ──────────────────────────────────
                    Column {
                        FormLabel(label = "Service name", hint = "${state.title.length}/150")
                        TextField(
                            value         = state.title,
                            onValueChange = viewModel::onTitleChange,
                            placeholder   = {
                                Text("e.g. Bathroom faucet replacement", color = C.Mute, fontSize = 14.sp)
                            },
                            modifier   = Modifier.fillMaxWidth(),
                            shape      = RoundedCornerShape(12.dp),
                            colors     = TextFieldDefaults.colors(
                                focusedContainerColor   = C.Bg,
                                unfocusedContainerColor = C.Subtle,
                                focusedIndicatorColor   = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor        = C.Ink,
                                unfocusedTextColor      = C.Ink,
                            ),
                            singleLine = true,
                            textStyle  = LocalTextStyle.current.copy(
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }

                    // ── category chips ────────────────────────────────
                    Column {
                        FormLabel(label = "Category")
                        if (state.availableCategories.isEmpty()) {
                            Text(
                                text     = "No categories found on your profile",
                                fontSize = 13.sp,
                                color    = C.Mute,
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement   = Arrangement.spacedBy(6.dp),
                            ) {
                                state.availableCategories.forEach { category ->
                                    CategoryChip(
                                        label   = category.name,
                                        active  = state.selectedCategoryId == category.id,
                                        onClick = { viewModel.onCategorySelected(category.id) },
                                    )
                                }
                            }
                        }
                    }

                    // ── description ───────────────────────────────────
                    Column {
                        FormLabel(label = "Description", hint = "${state.description.length}/500")
                        TextField(
                            value         = state.description,
                            onValueChange = viewModel::onDescriptionChange,
                            placeholder   = {
                                Text(
                                    "Describe your service, what's included, and what customers can expect…",
                                    color      = C.Mute,
                                    fontSize   = 13.sp,
                                    lineHeight = 20.sp,
                                )
                            },
                            modifier  = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 96.dp),
                            shape     = RoundedCornerShape(12.dp),
                            colors    = TextFieldDefaults.colors(
                                focusedContainerColor   = C.Subtle,
                                unfocusedContainerColor = C.Subtle,
                                focusedIndicatorColor   = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor        = C.Ink,
                                unfocusedTextColor      = C.Ink,
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize   = 14.sp,
                                lineHeight = 21.sp,
                            ),
                            maxLines  = 8,
                        )
                    }

                    // ── price + duration ──────────────────────────────
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            FormLabel(label = "Price")
                            PricingField(
                                value         = state.price,
                                onValueChange = viewModel::onPriceChange,
                                placeholder   = "0.00",
                                prefix        = "$",
                                suffix        = "SGD",
                                keyboardType  = KeyboardType.Decimal,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            FormLabel(label = "Duration")
                            PricingField(
                                value         = state.durationMinutes,
                                onValueChange = viewModel::onDurationChange,
                                placeholder   = "60",
                                suffix        = "min",
                                keyboardType  = KeyboardType.Number,
                            )
                        }
                    }

                    // ── visibility toggle ─────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, C.Line, RoundedCornerShape(14.dp))
                            .background(C.Bg),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text       = "Visible to customers",
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = C.Ink,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text     = "New requests can book this service",
                                    fontSize = 11.sp,
                                    color    = C.Slate,
                                )
                            }
                            Switch(
                                checked         = state.isVisible,
                                onCheckedChange = viewModel::onVisibilityChange,
                                colors          = SwitchDefaults.colors(
                                    checkedThumbColor    = Color.White,
                                    checkedTrackColor    = C.Blue,
                                    uncheckedThumbColor  = Color.White,
                                    uncheckedTrackColor  = C.Line,
                                    uncheckedBorderColor = C.Line,
                                ),
                            )
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier  = Modifier.align(Alignment.BottomCenter),
            )
        }

        // ── sticky bottom bar ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(C.Bg),
        ) {
            HorizontalDivider(color = C.Line)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .border(1.5.dp, C.Line, RoundedCornerShape(25.dp))
                        .background(C.Bg)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Cancel", color = C.Slate, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .weight(1.6f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(if (state.isSaving) C.Mute else C.Blue)
                        .clickable(enabled = !state.isSaving && !state.isLoading) { viewModel.save() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(22.dp),
                            color       = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text       = if (state.isEditing) "Save changes" else "Add service",
                                color      = Color.White,
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text("→", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── private composables ───────────────────────────────────────────────────

@Composable
private fun FormLabel(label: String, hint: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, fontSize = 13.sp, color = C.Slate, fontWeight = FontWeight.SemiBold)
        if (hint != null) {
            Text(text = hint, fontSize = 11.sp, color = C.Mute)
        }
    }
}

@Composable
private fun CategoryChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 1.5.dp,
                color = if (active) C.Blue else C.Line,
                shape = RoundedCornerShape(10.dp),
            )
            .background(if (active) C.BlueSoft else C.Bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text       = label,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (active) C.Blue else C.Slate,
        )
    }
}

@Composable
private fun PricingField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    prefix: String? = null,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, C.Line, RoundedCornerShape(12.dp))
            .background(C.Subtle)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (prefix != null) {
            Text(text = prefix, fontSize = 14.sp, color = C.Slate, fontWeight = FontWeight.SemiBold)
        }
        TextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text(placeholder, color = C.Mute, fontSize = 14.sp) },
            modifier      = Modifier.weight(1f),
            colors        = TextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor        = C.Ink,
                unfocusedTextColor      = C.Ink,
            ),
            singleLine      = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle       = LocalTextStyle.current.copy(
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        if (suffix != null) {
            Text(text = suffix, fontSize = 12.sp, color = C.Mute)
        }
    }
}