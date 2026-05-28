package com.fixit.app.ui.customer.bookings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.ui.components.Avatar
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.util.avatarColorFor
import com.fixit.app.ui.util.formatBookingMeta
import com.fixit.app.ui.util.initialsFor

@Composable
fun OpenDisputeScreen(
    onBack: () -> Unit,
    viewModel: OpenDisputeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
                "Open dispute",
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
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {

                        // Booking summary
                        state.booking?.let { b ->
                            val providerName = b.provider?.name?.takeIf { it.isNotBlank() } ?: "Provider"
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
                                        b.service?.title ?: "Service",
                                        fontSize = 12.5.sp,
                                        color = C.Ink,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                    Text(
                                        formatBookingMeta(b),
                                        fontSize = 11.5.sp,
                                        color = C.Mute,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                        }

                        Text(
                            "Tell us what went wrong",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = C.Ink,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            "Describe what didn't match what you agreed to. An admin will review your dispute and the provider's response. (10–2000 characters)",
                            fontSize = 12.5.sp,
                            color = C.Slate,
                            lineHeight = 18.sp,
                        )

                        // Reason text field (multi-line)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, C.Line, RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            BasicTextField(
                                value = state.reason,
                                onValueChange = viewModel::onReasonChange,
                                textStyle = TextStyle(
                                    color = C.Ink,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                ),
                                cursorBrush = SolidColor(C.Blue),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 140.dp),
                                decorationBox = { inner ->
                                    if (state.reason.isEmpty()) {
                                        Text(
                                            "e.g. The provider didn't finish the job, or the work doesn't match what was agreed.",
                                            color = C.Mute,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                        )
                                    }
                                    inner()
                                },
                            )
                        }

                        Text(
                            "${state.reason.trim().length}/2000",
                            fontSize = 11.sp,
                            color = C.Mute,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(12.dp))

                        // Submit button
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (state.canSubmit) C.Blue else C.Mute)
                                .clickable(enabled = state.canSubmit) { viewModel.submit() },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (state.isSubmitting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                Text(
                                    "File dispute",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }
                    }
            }
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.padding(horizontal = 16.dp))
    }
}