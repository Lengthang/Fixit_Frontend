package com.fixit.app.ui.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixit.app.ui.components.BlueGradient
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.PrimaryButton
import com.fixit.app.ui.components.Progress
import com.fixit.app.ui.components.ScreenTitle
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.theme.C
import kotlinx.coroutines.launch

private const val WELCOME_PROMO_CODE = "WELCOME25"

@Composable
fun PromoScreen(
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    FixItScreen(bg = C.Subtle) {
        TopBar(step = 6, total = 11, onBack = onBack)
        Progress(step = 6, total = 11)
        ScreenTitle(
            "You're all set",
            "Here's a welcome gift to get you started on your first booking.",
        )

        Column(Modifier.padding(horizontal = 24.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BlueGradient())
                    .padding(24.dp),
            ) {
                Box(
                    Modifier
                        .size(140.dp)
                        .offset(x = 180.dp, y = (-30).dp)
                        .clip(CircleShape)
                        .background(C.Orange.copy(alpha = 0.25f)),
                )
                Column {
                    Text(
                        "WELCOME OFFER",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "25% OFF",
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp,
                    )
                    Text(
                        "On any service",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(22.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            WELCOME_PROMO_CODE,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White)
                                .clickable {
                                    clipboard.setText(AnnotatedString(WELCOME_PROMO_CODE))
                                    scope.launch { snackbarHostState.showSnackbar("Code copied") }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(
                                "COPY",
                                color = C.Orange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.OrangeSoft)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(C.Orange),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Info, null,
                        tint = Color.White, modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = C.Ink)) {
                            append("Valid for 30 days. ")
                        }
                        append("Apply at checkout on your first booking of any service.")
                    },
                    fontSize = 12.5.sp,
                    color = C.Ink,
                    lineHeight = 18.sp,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        SnackbarHost(snackbarHostState, modifier = Modifier.padding(horizontal = 16.dp))
        PrimaryButton("Start exploring", onClick = onFinish)
    }
}