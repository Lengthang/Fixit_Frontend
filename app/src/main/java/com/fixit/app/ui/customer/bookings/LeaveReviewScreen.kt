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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
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
fun LeaveReviewScreen(
    onBack: () -> Unit,
    viewModel: LeaveReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LeaveReviewEffect.Message ->
                    snackbarHostState.showSnackbar(effect.text)
                LeaveReviewEffect.Submitted -> onBack()
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
                "Leave a review",
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
                                    size     = 48,
                                    fontSize = 16,
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        providerName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = C.Ink,
                                    )
                                    Text(
                                        b.service?.title ?: "Service",
                                        fontSize = 13.sp,
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

                        // Rating
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "How was your experience?",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = C.Ink,
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (1..5).forEach { star ->
                                    val filled = state.rating >= star
                                    Icon(
                                        imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = "Rate $star stars",
                                        tint = if (filled) C.Orange else C.Line,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clickable { viewModel.onRatingChange(star) },
                                    )
                                }
                            }
                            if (state.rating > 0) {
                                Text(
                                    ratingHint(state.rating),
                                    fontSize = 12.sp,
                                    color = C.Slate,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 10.dp),
                                )
                            }
                        }

                        // Comment
                        Column {
                            Text(
                                "Add a comment (optional)",
                                fontSize = 13.sp,
                                color = C.Slate,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, C.Line, RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                            ) {
                                BasicTextField(
                                    value = state.comment,
                                    onValueChange = viewModel::onCommentChange,
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
                                        if (state.comment.isEmpty()) {
                                            Text(
                                                "What went well? What could have been better?",
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
                                "${state.comment.length}/2000",
                                fontSize = 11.sp,
                                color = C.Mute,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }

                        Spacer(Modifier.height(4.dp))

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
                                    "Submit review",
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

private fun ratingHint(rating: Int): String = when (rating) {
    1 -> "Poor"
    2 -> "Fair"
    3 -> "Good"
    4 -> "Very good"
    5 -> "Excellent"
    else -> ""
}