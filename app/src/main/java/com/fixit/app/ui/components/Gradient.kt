package com.fixit.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import com.fixit.app.ui.theme.C

@Composable fun BlueGradient(): Brush = Brush.linearGradient(listOf(C.Blue, C.BlueDark))