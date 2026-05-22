package com.fixit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.fixit.app.ui.theme.C

@Composable
fun FixItScreen(
    bg: Color = C.Bg,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(bg).statusBarsPadding().navigationBarsPadding(),
        content = content,
    )
}